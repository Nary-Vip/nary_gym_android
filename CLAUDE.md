# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

"Nary Gym" (package `com.nary.gym`, Gradle root project name "Nary Gym") is a single-module native Android app for tracking gym workouts, built with Kotlin, Jetpack Compose, and Hilt. It talks to a remote backend at `https://nary-gym-backend.onrender.com/api/` for auth and workout data, and uses Room as a local cache/offline store synced from that backend.

## Build & run commands

All commands run from the repo root using the Gradle wrapper.

- Build debug APK: `./gradlew assembleDebug`
- Build release APK: `./gradlew assembleRelease` (signed via `gymnary.jks`, config in `app/build.gradle.kts`/`gradle.properties`; minify + resource shrinking enabled)
- Install debug build to a connected device/emulator: `./gradlew installDebug`
- Run unit tests (`app/src/test`): `./gradlew testDebugUnitTest`
- Run instrumented tests (`app/src/androidTest`, requires a device/emulator): `./gradlew connectedDebugAndroidTest`
- Run a single unit test class: `./gradlew testDebugUnitTest --tests "com.nary.gym.SomeTest"`
- Lint: `./gradlew lint`
- Clean: `./gradlew clean`

Note: only the default template tests exist (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) — there is no real test suite to model new tests after yet.

compileSdk/targetSdk = 37, minSdk = 28. Some features (AppFunctions in `appfunctions/`) are gated behind `@RequiresApi(36)` (Android 16+) and only run on devices that support them.

## Architecture

Layering follows a fairly standard MVVM + repository pattern, Hilt-wired throughout:

```
ui/screens (Compose)  →  viewModel/ (Hilt @HiltViewModel)  →  repository/  →  data/remote (Retrofit) and data/room_db (Room)
```

- **`navigation/`** — `AppNavigator` (Navigation.kt) is the single `NavHost`; routes are declared in `nav_setup.kt` (`NavRoutes`). Start destination is decided at launch by whether `TokenManager` has an access token (Login vs Home). The Home route also accepts deep links (`narygym://profile`, `https://narygym.web.app/profile`); the `https` app link is verified via `public/.well-known/assetlinks.json` served from Firebase Hosting (see `.firebaserc` project `narygym`).
- **`data/remote`** — `ApiService` (Retrofit interface) hits the backend for auth and workout CRUD. `data/remote/RetrofitInstance.kt` (package `com.nary.gym.di` despite its location) wires up two OkHttp/Retrofit stacks via Hilt qualifiers: the default `@AuthClient`/default `ApiService` attaches `AuthInterceptor` + `TokenAuthenticator`, and a `@NoAuthClient`/`@NoAuthApi` pair exists specifically so the token-refresh call itself doesn't recurse through auth interception.
- **Auth/token refresh flow** — `TokenManager` (`data/local`) stores access/refresh tokens and notification prefs in `EncryptedSharedPreferences`. `AuthInterceptor` attaches the bearer token to outgoing requests (skipping `auth/login`, `auth/create-account`, `auth/refresh-session`). `TokenAuthenticator` handles 401s: it calls `auth/refresh-session` via the no-auth `ApiService` (`runBlocking`, `synchronized` to avoid duplicate refreshes across threads), saves new tokens, and retries. On refresh failure it clears tokens/session and emits `UserSession.unauthorizedEvent`, which `AppNavigator` observes to force navigation back to Login. When touching auth, keep the no-auth/auth client split intact — routing the refresh call through the authenticated client would deadlock/loop.
- **`data/room_db`** — A single `RoomDatabase`, `WorkoutDatabase` (entity `WorkOuts`, table `workouts`, current version 5, DB name `workout_database`, `exportSchema = false`), provided via Hilt in `data/room_db/RoomModule.kt` (package `com.nary.gym.di`). It uses `fallbackToDestructiveMigration(true)`, so bumping the version wipes the local cache rather than migrating — acceptable only because the backend is the source of truth.
- **`repository/remote`** — `WorkOutRepository` and `AuthRepository` treat the backend as source of truth and Room as a cache: network calls happen first, then Room is updated with the server's response (e.g. `insert`/`update` write the server-assigned `id` back into Room).
- **Workout rotation** — `utils/WorkoutCycle` (file `WorkoutResources.kt.kt` — note the double extension) tracks "today's workout" as an index into `core/WorkOutType` (Leg → Chest → Back → Shoulder) in the `workout_cycles` SharedPreferences. `moveToNextDayWorkOut()` advances at most once per calendar day (guarded by `last_completed_epoch_day`). Used by both the Home screen and the widget.
- **Notifications** (`Notifications/`) — `AlarmScheduler` schedules a daily `AlarmManager` alarm (exact when permitted, `setAndAllowWhileIdle` fallback) received by `AlarmReceiver`. `NotificationHelper` builds the reminder notification with "Yes" (opens `MainActivity`) and "Snooze" actions, handled by `NotificationActionReceiver`. `BootReceiver` reschedules alarms after device reboot. `ReminderWatchdogWorker` (`@HiltWorker`, 12-hour unique periodic work) re-arms the alarm if reminders are enabled, as a safety net for missed/dropped alarms. Alarm enable/schedule state (enabled flag, hour, minute) lives in `TokenManager` and is re-evaluated in `NaryApplication.onCreate` based on current login state.
- **WorkManager setup** — WorkManager is initialized manually: `NaryApplication` implements `Configuration.Provider` with `HiltWorkerFactory`, and the default `WorkManagerInitializer` is removed in `AndroidManifest.xml`. Keep both in place or Hilt-injected workers will fail to construct.
- **`appfunctions/`** — Exposes an Android AppFunctions (`androidx.appfunctions`) service so the system/Assistant can invoke `scheduleGymReminder(hour, minute)` directly; requires API 36+. `GymAppFunctionService` dispatches by function ID to `GymAppFunctions`, which reuses `AlarmScheduler`/`TokenManager`.
- **`widget/`** — `WorkoutWidget` (file `WorkoutOfTheDay.kt`) is a Glance app widget (`GlanceAppWidget`) showing "today's workout" (via `utils/WorkoutCycle`), with a `CompleteWorkoutAction` callback to mark it done; registered through `WorkoutOfTheDayReceiver`; widget metadata is in `app/src/main/res/xml/workout_of_the_day_info.xml`.
- **`ui/`** — Compose screens under `ui/screens/{Home,Profile,Workouts,auth}`, shared components in `ui/common_widgets`, theme in `ui/theme`, field validation helpers in `ui/utils`.

## Gotchas specific to this repo

- `RetrofitInstance.kt` (under `data/remote`) and `RoomModule.kt` (under `data/room_db`) both declare package `com.nary.gym.di`; don't assume file location matches package.
- Room uses destructive migration — any schema change needs a version bump and will clear cached workouts on upgrade.
- Auth-exempt endpoint paths are hardcoded as substring checks in `AuthInterceptor` (`auth/login`, `auth/create-account`, `auth/refresh-session`) — new unauthenticated endpoints need to be added there too.
- The `https://narygym.web.app/profile` app link depends on `public/.well-known/assetlinks.json` staying correctly deployed via Firebase Hosting (`firebase deploy` from repo root, project alias `release` → `narygym`); changing the signing key or applicationId requires regenerating that file.

## Git workflow rules

- **Never add a `Co-Authored-By: Claude …` trailer** (or any Claude/AI attribution) to commit messages or PR descriptions.
- **Don't commit after finishing a task.** Leave the changes uncommitted in the working tree so the user can review the file changes first; commit only when the user explicitly asks.
