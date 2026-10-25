# Nary Gym

A native Android app for planning and tracking gym workouts. It works fully offline, syncs in the background, and checks you in automatically when you arrive at your gym.

Built with Kotlin and Jetpack Compose on a Material 3 design system, with Hilt, Room, Retrofit and WorkManager underneath.

## Features

- **Workout library.** Create, edit and delete exercises with a photo, grouped by muscle group (Leg, Chest, Back, Shoulder). Swipe to delete, with undo.
- **Daily rotation.** "Today" shows the muscle group due today and its exercises. Marking the day complete advances the rotation, at most once per calendar day.
- **Offline-first sync.** Every change is saved on the device instantly and uploaded later in the background. Each workout shows "Waiting to sync" or "Couldn't sync: <reason>" until the server confirms it.
- **Automatic gym check-in.** A geofence around your saved gym records visits: check-in after 3 minutes inside, check-out on exit. Notifications offer Undo.
- **Daily reminder.** A workout reminder at a time you choose, with Snooze. It survives reboots and dropped alarms.
- **Home-screen widget.** Shows today's workout; you can mark the day complete from the widget.
- **Accounts.** Email login and sign-up, a profile with an avatar, and transparent token refresh.

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.3, Coroutines and Flow |
| UI | Jetpack Compose, Material 3 (custom colour, type and shape system), Navigation Compose with type-safe routes |
| Architecture | MVVM with repositories, Hilt dependency injection |
| Local data | Room (versioned schema, explicit migrations), EncryptedSharedPreferences |
| Networking | Retrofit 3, OkHttp 5 (auth interceptor and token authenticator) |
| Background work | WorkManager (Hilt workers), AlarmManager, BroadcastReceivers |
| Location | Google Play Services fused location and Geofencing |
| Widget | Jetpack Glance |
| Images | Coil 3; picked photos re-encoded to JPEG with ImageDecoder |
| Observability | Firebase Crashlytics and Analytics |
| Platform | minSdk 28, targetSdk 37 |

## Architecture

```
Compose screens ──► ViewModels ──► Repositories ──┬──► Room (source of truth)
                                                  └──► Retrofit API (REST backend)
                                   WorkManager ──► WorkoutSyncWorker ──► Repository.sync()
```

- Each screen is a stateless `…Content` composable, tested with plain data, wrapped by a thin `…Screen` that connects it to its ViewModel.
- Room is the single source of truth for workouts. The UI observes Room through `Flow`, so it never waits on the network.
- Two OkHttp clients are kept apart: an authenticated one, and a plain one used only for the token-refresh call, so refreshing can't loop through its own authenticator.

## Engineering highlights

**Offline-first synchronisation**
- Each row carries a sync state (`PENDING_CREATE`, `PENDING_UPDATE`, `PENDING_DELETE` or `SYNCED`), a server ID and a version counter.
- Deletes are kept as hidden "tombstones" until the server confirms them.
- A sync pushes creates, then updates, then deletes, then pulls, all under a mutex.
- A server result is only written back if the row's version hasn't changed in the meantime, so edits made during an upload are never lost.
- A pull never overwrites a change that is still waiting to upload.
- Server errors are mapped to typed failures:
  - network and 5xx errors are retried with exponential backoff
  - 404 on an update re-creates the workout
  - 409 on a create links to the existing server workout
  - validation errors are shown on the workout until you edit it

**Account isolation**
- Every local change is tagged with the account that made it.
- Sync only uploads the logged-in account's changes. Another account's pending changes stay on the device until that account logs back in.
- A sync stops if the account changes mid-run.

**Safe schema evolution**
- Gym visit history exists only on the device, so every database version has a real migration covered by `MigrationTestHelper` tests.
- The fallback that wipes the database is never relied on.

**Reliable background behaviour**
- Alarms and geofences are re-registered after reboot, and a 12-hour watchdog worker re-checks them.
- All visit rules live in one repository under a mutex: no duplicate open visits, very short visits discarded, very long ones capped.

**Media handling**
- Picked photos are decoded off the main thread with EXIF orientation applied, scaled to at most 1600 px and saved as JPEG.
- Photos that are replaced, abandoned or cancelled are cleaned up.

## Testing

- About 120 instrumented tests and 29 JVM unit tests.
- **Compose UI tests** for every screen, the widget and the launcher icon. Haptics are verified with a recording fake.
- **Room tests** on an in-memory database, plus migration tests for each schema version.
- **Sync tests:** the repository against a fake backend, covering conflicts, mid-upload edits, retries and account switches. The HTTP layer is tested against MockWebServer over TLS.
- **WorkManager tests:** workers run with `TestListenableWorkerBuilder` and the scheduler under `WorkManagerTestInitHelper`.

```bash
./gradlew testDebugUnitTest            # JVM unit tests
./gradlew connectedDebugAndroidTest    # instrumented tests (device or emulator)
./gradlew lint
```

## Build and run

Requirements: a recent Android Studio, JDK 17+, and an Android 9+ device or emulator.

The app talks to a REST backend whose URL is supplied at build time, never committed. Set `NARY_API_BASE_URL` (ending in `/`) as an environment variable, or add it to the git-ignored `local.properties`:

```properties
NARY_API_BASE_URL=https://your-backend.example.com/api/
```

```bash
./gradlew installDebug
```

## Project structure

```
app/src/main/java/com/nary/gym/
├── ui/              Compose screens, components, Material 3 theme
├── viewModel/       Hilt ViewModels
├── navigation/      Type-safe routes, main shell, deep links
├── repository/      Workout sync, auth, gym visits
├── data/            Room database and DAOs, Retrofit API, secure storage
├── sync/            WorkManager sync worker and scheduler
├── checkin/         Geofencing and visit notifications
├── Notifications/   Daily reminder alarms and watchdog
├── widget/          Glance home-screen widget
└── di/              Hilt modules
```
