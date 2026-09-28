# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Nary Gym" (package `com.nary.gym`, Gradle root project name "Nary Gym") is a single-module native Android app for tracking gym workouts, built with Kotlin, Jetpack Compose, and Hilt. It talks to a remote backend at `https://nary-gym-backend.onrender.com/api/` for auth and workout data, and uses Room as a local cache/offline store synced from that backend.

## Build & run commands

All commands run from the repo root using the Gradle wrapper.

- Build debug APK: `./gradlew assembleDebug`
- Build release APK: `./gradlew assembleRelease` builds it **unsigned** — there is no `signingConfig` in `app/build.gradle.kts`. Signed release APKs are made with Android Studio's *Generate Signed App Bundle / APK* wizard using `gymnary.jks` (repo root). Minify + resource shrinking are enabled. A debug build can't be installed over a release-signed install (different key) without uninstalling it, which wipes local data.
- Install debug build to a connected device/emulator: `./gradlew installDebug`
- Run unit tests (`app/src/test`): `./gradlew testDebugUnitTest`
- Run instrumented tests (`app/src/androidTest`, requires a device/emulator): `./gradlew connectedDebugAndroidTest`
- Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.nary.gym.SomeTest"`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`

Tests: local JVM unit tests in `app/src/test` (`GymVisitRepositoryTest` with `FakeGymVisitDao` + `TestClock` is the pattern to follow for repository/logic tests; pure functions like `VisitStats`/`LocationPermissions` are tested directly) and instrumented tests in `app/src/androidTest` (`GymVisitDaoTest` on an in-memory Room DB, `WorkoutDatabaseMigrationTest` with `MigrationTestHelper`). Run one instrumented class with `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<FQN>` (`--tests` doesn't work for connected tests). Pre-check-in features (auth, workouts, reminders, widget) still have no tests.

compileSdk/targetSdk = 37, minSdk = 28. Some features (AppFunctions in `appfunctions/`) are gated behind `@RequiresApi(36)` (Android 16+) and only run on devices that support them.

## Architecture

Layering follows a fairly standard MVVM + repository pattern, Hilt-wired throughout:

```
ui/screens (Compose)  →  viewModel/ (Hilt @HiltViewModel)  →  repository/  →  data/remote (Retrofit) and data/room_db (Room)
```

- **`navigation/`** — `AppNavigator` (Navigation.kt) is the single `NavHost`; routes are declared in `nav_setup.kt` (`NavRoutes`). Start destination is decided at launch by whether `TokenManager` has an access token (Login vs Home). The Home route also accepts deep links (`narygym://profile`, `https://narygym.web.app/profile`); the `https` app link is verified via `public/.well-known/assetlinks.json` served from Firebase Hosting (see `.firebaserc` project `narygym`).
- **`data/remote`** — `ApiService` (Retrofit interface) hits the backend for auth and workout CRUD. `data/remote/RetrofitInstance.kt` (package `com.nary.gym.di` despite its location) wires up two OkHttp/Retrofit stacks via Hilt qualifiers: the default `@AuthClient`/default `ApiService` attaches `AuthInterceptor` + `TokenAuthenticator`, and a `@NoAuthClient`/`@NoAuthApi` pair exists specifically so the token-refresh call itself doesn't recurse through auth interception.
- **Auth/token refresh flow** — `TokenManager` (`data/local`) stores access/refresh tokens and notification prefs in `EncryptedSharedPreferences`. `AuthInterceptor` attaches the bearer token to outgoing requests (skipping `auth/login`, `auth/create-account`, `auth/refresh-session`). `TokenAuthenticator` handles 401s: it calls `auth/refresh-session` via the no-auth `ApiService` (`runBlocking`, `synchronized` to avoid duplicate refreshes across threads), saves new tokens, and retries. On refresh failure it clears tokens/session and emits `UserSession.unauthorizedEvent`, which `AppNavigator` observes to force navigation back to Login. When touching auth, keep the no-auth/auth client split intact — routing the refresh call through the authenticated client would deadlock/loop.
- **`data/room_db`** — A single `RoomDatabase`, `WorkoutDatabase` (entities `WorkOuts` → table `workouts`, and `GymVisit` → table `gym_visits`; current version 6; DB name `workout_database`), provided via Hilt in `data/room_db/RoomModule.kt` (package `com.nary.gym.di`). Schemas are exported to `app/schemas/` and upgrades use explicit migrations (`data/room_db/Migrations.kt`, registered with `addMigrations`). `fallbackToDestructiveMigration(true)` is still set, but only fires when no migration path exists — see the gotcha below.
- **`repository/remote`** — `WorkOutRepository` and `AuthRepository` treat the backend as source of truth and Room as a cache: network calls happen first, then Room is updated with the server's response (e.g. `insert`/`update` write the server-assigned `id` back into Room).
- **Workout rotation** — `utils/WorkoutCycle` (file `WorkoutResources.kt.kt` — note the double extension) tracks "today's workout" as an index into `core/WorkOutType` (Leg → Chest → Back → Shoulder) in the `workout_cycles` SharedPreferences. `moveToNextDayWorkOut()` advances at most once per calendar day (guarded by `last_completed_epoch_day`). Used by both the Home screen and the widget.
- **Notifications** (`Notifications/`) — `AlarmScheduler` schedules a daily `AlarmManager` alarm (exact when permitted, `setAndAllowWhileIdle` fallback) received by `AlarmReceiver`. `NotificationHelper` builds the reminder notification with "Yes" (opens `MainActivity`) and "Snooze" actions, handled by `NotificationActionReceiver`. `BootReceiver` reschedules alarms after device reboot. `ReminderWatchdogWorker` (`@HiltWorker`, 12-hour unique periodic work) re-arms the alarm if reminders are enabled, as a safety net for missed/dropped alarms. Alarm enable/schedule state (enabled flag, hour, minute) lives in `TokenManager` and is re-evaluated in `NaryApplication.onCreate` based on current login state.
- **Gym auto check-in** (`checkin/`, `ui/screens/CheckIn/`, `viewModel/CheckInViewModel.kt`) — The user saves their gym via fused "current location" (`CurrentLocationProvider` → `data/local/GymLocationStore`, plain prefs `gym_checkin`). `GymGeofenceManager.syncRegistration()` idempotently registers one DWELL(3 min)/EXIT geofence when auto check-in is on, a gym is saved and all location permissions (`checkin/LocationPermissions.kt`) are granted, otherwise removes it. It is re-run from `BootReceiver` under `goAsync` (reboot wipes geofences), from `ReminderWatchdogWorker` (12 h backstop, e.g. after location was toggled off) and on every process start from `NaryApplication`; logout turns the switch off and unregisters. `GeofenceBroadcastReceiver` → `repository/local/GymVisitRepository`, which owns all rules (no duplicate open visit, <5 min discarded, >4 h capped + `autoClosed`) under a `Mutex`; `CheckInNotificationHelper`/`CheckInActionReceiver` show check-in/out notifications with Undo. Visits are **local-only** (no API yet). The geofence `PendingIntent` must stay `FLAG_MUTABLE` (Play services writes the event into it); every other PendingIntent is `FLAG_IMMUTABLE`.
- **WorkManager setup** — WorkManager is initialized manually: `NaryApplication` implements `Configuration.Provider` with `HiltWorkerFactory`, and the default `WorkManagerInitializer` is removed in `AndroidManifest.xml`. Keep both in place or Hilt-injected workers will fail to construct.
- **`appfunctions/`** — Exposes an Android AppFunctions (`androidx.appfunctions`) service so the system/Assistant can invoke `scheduleGymReminder(hour, minute)` directly; requires API 36+. `GymAppFunctionService` dispatches by function ID to `GymAppFunctions`, which reuses `AlarmScheduler`/`TokenManager`.
- **`widget/`** — `WorkoutWidget` (file `WorkoutOfTheDay.kt`) is a Glance app widget (`GlanceAppWidget`) showing "today's workout" (via `utils/WorkoutCycle`), with a `CompleteWorkoutAction` callback to mark it done; registered through `WorkoutOfTheDayReceiver`; widget metadata is in `app/src/main/res/xml/workout_of_the_day_info.xml`.
- **`ui/`** — Compose screens under `ui/screens/{Home,Profile,Workouts,CheckIn,auth}` (Home hosts a 4-tab bottom bar by `pageIndex`: 0 workouts, 1 today, 2 profile, 3 check-ins — keep profile at 2, the profile deep link depends on it), shared components in `ui/common_widgets`, theme in `ui/theme`, field validation helpers in `ui/utils`.

## Gotchas specific to this repo

- `RetrofitInstance.kt` (under `data/remote`) and `RoomModule.kt` (under `data/room_db`) both declare package `com.nary.gym.di`; don't assume file location matches package.
- `gym_visits` is local-only data, so **every `WorkoutDatabase` version bump needs a real `Migration`** plus a case in `WorkoutDatabaseMigrationTest` — the `fallbackToDestructiveMigration` safety net would silently wipe users' visit history. Build once with the old version first so its schema JSON exists in `app/schemas/`.
- `room-testing` needs kotlinx-serialization ≥ 1.8; the app pulls 1.7.3 transitively and the test APK is pinned to the app's versions, so `app/build.gradle.kts` has a `constraints { }` block raising it. Removing it makes `MigrationTestHelper` crash with `AbstractMethodError`.
- Auto check-in requests `ACCESS_BACKGROUND_LOCATION`; publishing to Play requires the background-location declaration in Play Console.
- The app is **light-only**: `NaryToDoTheme` defaults `darkTheme = false` and `MainActivity` pins dark status-bar icons, because every screen uses the fixed light palette in `ui/theme/Color.kt`. Following system dark mode made headings/buttons unreadable; re-enabling it needs a real dark palette adopted by all screens first.
- Auth-exempt endpoint paths are hardcoded as substring checks in `AuthInterceptor` (`auth/login`, `auth/create-account`, `auth/refresh-session`) — new unauthenticated endpoints need to be added there too.
- The `https://narygym.web.app/profile` app link depends on `public/.well-known/assetlinks.json` staying correctly deployed via Firebase Hosting (`firebase deploy` from repo root, project alias `release` → `narygym`); changing the signing key or applicationId requires regenerating that file.

## Git workflow rules

- **Never add a `Co-Authored-By: Claude …` trailer** (or any Claude/AI attribution) to commit messages or PR descriptions.
- **Don't commit after finishing a task.** Leave the changes uncommitted in the working tree so the user can review the file changes first; commit only when the user explicitly asks.
