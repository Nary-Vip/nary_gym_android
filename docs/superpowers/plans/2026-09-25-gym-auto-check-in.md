# Gym Auto Check-in Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Automatically check the user in when they arrive at their gym and check them out when they leave, using a geofence, and show those visits on a new "Check-ins" page. Everything is stored locally in Room for now.

**Architecture:** The user saves their gym's location with "Use current location" while standing at the gym. `GymGeofenceManager` registers one Play-services geofence around that point. `GeofenceBroadcastReceiver` turns DWELL/EXIT events into `GymVisitRepository.checkIn()`/`checkOut()`. That repository holds all the check-in rules (no duplicate visits, discard very short visits, cap forgotten check-outs) and writes to a new `gym_visits` table in the existing `WorkoutDatabase`. A new `CheckInScreen` tab shows the current status, gym setup, the permission flow and the visit history. Manual check-in/out buttons use the same repository.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Hilt, Room 2.8 (explicit migration + schema export), Google Play services Location 21.x (Geofencing + Fused Location), kotlinx-coroutines-play-services, JUnit4 + kotlinx-coroutines-test.

**Spec:** There is no separate spec document. The "Design decisions" section below is the spec and is what the reviewer approves.

## Design decisions (review these first)

| # | Decision | Why | Alternative if you disagree |
|---|---|---|---|
| D1 | The gym location is set with **"Use current location" while standing at the gym**. There is no map picker in v1. | This needs no Maps API key or Google Cloud billing, and the Fused Location API is the learning target. | Add a Maps Compose picker as a follow-up plan. |
| D2 | **One gym** only, with a radius of 100 / **150 (default)** / 250 m. | This is YAGNI. Google recommends at least 100–150 m for reliable geofences. | Use a list of gyms, which needs a `gyms` table. |
| D3 | Check-in triggers on **DWELL (3 min loitering)**, not ENTER. Check-out triggers on **EXIT**. | DWELL avoids checking you in when you drive past the gym. | Use ENTER for faster check-in with more false positives. |
| D4 | Visits **shorter than 5 min are discarded** when checking out. | Handles GPS jitter at the edge of the fence and accidental manual check-ins. | Keep them and flag them. |
| D5 | Visits are **capped at 4 h**. A forgotten or missed check-out is closed at check-in + 4 h and flagged `autoClosed`. | EXIT events can be lost when location is off, the phone reboots, or Doze delays them. | A different cap, or prompting the user. |
| D6 | Visit ids are **client-generated UUID strings**. | When we move to the API later, rows created offline can be uploaded without id clashes. This matches the `String` ids used for workouts. | Auto-increment `Long`. |
| D7 | Uses a **new table in the existing `WorkoutDatabase`** (v5 → v6) with an **explicit migration and a migration test**. | Visits are local-only. The destructive fallback would silently wipe them on any future version bump. | A separate Room database for visits. |
| D8 | **On logout:** auto check-in is switched off and the geofence removed. **Visits are kept.** | This mirrors how reminders are cancelled on logout, and there is no server copy of visits yet. | Also delete visits on logout (for multiple accounts on one device). |
| D9 | The gym location is stored in a plain private `SharedPreferences` file (`gym_checkin`), not the encrypted `TokenManager` prefs. | `TokenManager` is for auth. `security-crypto` is deprecated upstream. | Put it in `TokenManager`. |
| D10 | Notifications: "Checked in · Today: Chest day" with an **Undo** action, and "Checked out · 1h 12m". Both use a new channel at default importance. | Gives feedback on each automatic action, and Undo is the escape hatch for a false positive. | Silent (no notifications). |
| D11 | No notification-permission prompt on this screen. | The Profile screen already requests `POST_NOTIFICATIONS`. Visits are still recorded without it. | Request it here too. |

**Out of scope:** backend/API sync, map picker, multiple gyms, editing visit times, Play Store background-location declaration form (needed only before publishing, see Task 9).

## Global Constraints

- minSdk 28, targetSdk/compileSdk 37. `ACCESS_BACKGROUND_LOCATION` only exists at API 29+. At API 28, fine location implies background location.
- Room `WorkoutDatabase` goes from version **5 → 6** only through `MIGRATION_5_6`. The destructive fallback must not fire for this bump.
- There are no backend or `ApiService` changes.
- Follow the existing layout: Room code in `data/room_db`, prefs in `data/local`, repositories in `repository/`, ViewModels in `viewModel/`, screens in `ui/screens/<Feature>/`, and platform receivers in a feature package (like `Notifications/`, `widget/`). The new package is `checkin/`.
- Hilt modules declare package `com.nary.gym.di` (as `RoomModule.kt` already does).
- `PendingIntent` for geofence events **must be `FLAG_MUTABLE`**, because Play services writes the event into it. All other PendingIntents stay `FLAG_IMMUTABLE`.
- Unit tests go in `app/src/test/java/com/nary/gym/...` and run with `./gradlew testDebugUnitTest`.

## Review Focus

These are the failure modes most likely to bite a real user, and the test that pins each one:

1. **Duplicate DWELL events.** Re-registering the geofence on every app start re-fires the initial DWELL while you're inside. Expected: still one open visit. *Test:* `checkIn_whenAlreadyCheckedIn_doesNotCreateSecondVisit` (Task 3).
2. **The EXIT event never arrives** (location off, reboot, Doze). Expected: the next check-in closes the old visit at +4 h and flags it, instead of leaving it open forever. *Tests:* `checkIn_whenOpenVisitIsStale_autoClosesItAndStartsNew` and `checkOut_afterMaxDuration_capsVisitAndFlagsIt` (Task 3).
3. **A geofence event and a manual button tap at the same moment.** Expected: exactly one visit. *Test:* `concurrentCheckIns_createExactlyOneVisit` (Task 3).
4. **The user grants only "Approximate" location** (Android 12+). Geofencing needs fine location. Expected: the screen asks for precise location instead of claiming auto check-in is on. *Test:* `coarseOnly_needsPrecise` (Task 5).
5. **An upgrade from DB v5 with existing cached workouts.** Expected: the workouts survive and the `gym_visits` table exists with the exact schema Room expects. *Test:* `migrate5To6_keepsWorkoutsAndCreatesGymVisits` (Task 2).

---

## File Structure

**Create**
| File | Responsibility |
|---|---|
| `app/src/main/java/com/nary/gym/data/room_db/GymVisit.kt` | `GymVisit` entity and the `VisitSource` enum |
| `app/src/main/java/com/nary/gym/data/room_db/GymVisitDao.kt` | Room DAO for `gym_visits` |
| `app/src/main/java/com/nary/gym/data/room_db/Migrations.kt` | `MIGRATION_5_6` |
| `app/src/main/java/com/nary/gym/di/TimeModule.kt` | Provides `java.time.Clock` so the check-in rules can be tested |
| `app/src/main/java/com/nary/gym/repository/local/GymVisitRepository.kt` | Check-in/out rules; the only writer of `gym_visits` |
| `app/src/main/java/com/nary/gym/utils/VisitStats.kt` | `formatDuration`, `weeklySummary` (pure functions) |
| `app/src/main/java/com/nary/gym/checkin/LocationPermissions.kt` | Pure `nextLocationPermissionStep` plus a `Context` helper |
| `app/src/main/java/com/nary/gym/data/local/GymLocationStore.kt` | Saved gym location, radius and auto-check-in flag |
| `app/src/main/java/com/nary/gym/checkin/CurrentLocationProvider.kt` | Wraps the fused `getCurrentLocation` |
| `app/src/main/java/com/nary/gym/checkin/GymGeofenceManager.kt` | Registers/removes the gym geofence (`syncRegistration()`) |
| `app/src/main/java/com/nary/gym/checkin/GeofenceBroadcastReceiver.kt` | Geofence event → repository → notification |
| `app/src/main/java/com/nary/gym/checkin/CheckInNotificationHelper.kt` | Channel and check-in/out notifications |
| `app/src/main/java/com/nary/gym/checkin/CheckInActionReceiver.kt` | Handles the "Undo" notification action |
| `app/src/main/java/com/nary/gym/viewModel/CheckInViewModel.kt` | Screen state and actions |
| `app/src/main/java/com/nary/gym/ui/screens/CheckIn/CheckInScreen.kt` | The page |
| `app/src/main/java/com/nary/gym/ui/screens/CheckIn/AutoCheckInToggle.kt` | Switch plus the foreground → background permission flow |
| `app/src/test/java/com/nary/gym/repository/local/GymVisitRepositoryTest.kt` | Rules tests |
| `app/src/test/java/com/nary/gym/repository/local/FakeGymVisitDao.kt` | In-memory DAO for tests |
| `app/src/test/java/com/nary/gym/testutil/TestClock.kt` | Controllable `Clock` |
| `app/src/test/java/com/nary/gym/utils/VisitStatsTest.kt` | Stats/format tests |
| `app/src/test/java/com/nary/gym/checkin/LocationPermissionsTest.kt` | Permission-step tests |
| `app/src/androidTest/java/com/nary/gym/data/room_db/WorkoutDatabaseMigrationTest.kt` | Migration test |
| `app/schemas/...` | Room-generated schema JSON (v5, v6) |

**Modify:** `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build.gradle.kts` (root), `data/room_db/WorkoutDatabase.kt`, `data/room_db/Converters.kt`, `data/room_db/RoomModule.kt`, `AndroidManifest.xml`, `NaryApplication.kt`, `ui/screens/Home/HomeScreen.kt`, `CLAUDE.md`.

---

### Task 1: Build setup and exporting the v5 schema

To test a migration from v5, Room needs the v5 schema JSON. This task exports it **before** anything changes.

**Files:**
- Modify: `gradle/libs.versions.toml`, `build.gradle.kts` (root), `app/build.gradle.kts`, `app/src/main/java/com/nary/gym/data/room_db/WorkoutDatabase.kt`

**Interfaces:**
- Produces: dependencies `libs.play.services.location`, `libs.kotlinx.coroutines.play.services`, `libs.androidx.lifecycle.runtime.compose`, `libs.kotlinx.coroutines.test`, `libs.androidx.room.testing`, and `app/schemas/com.nary.gym.data.room_db.WorkoutDatabase/5.json`.

- [ ] **Step 1: Add versions and libraries to `gradle/libs.versions.toml`**

Under `[versions]` add (check [Maven](https://maven.google.com/web/index.html#com.google.android.gms:play-services-location) for a newer `playServicesLocation`):
```toml
playServicesLocation = "21.3.0"
```
Under `[libraries]` add:
```toml
play-services-location = { group = "com.google.android.gms", name = "play-services-location", version.ref = "playServicesLocation" }
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
```
(The `room` plugin entry already exists under `[plugins]`.)

- [ ] **Step 2: Apply the Room plugin**

Root `build.gradle.kts`, add inside `plugins { }`:
```kotlin
    alias(libs.plugins.room) apply false
```
`app/build.gradle.kts`, add to `plugins { }`:
```kotlin
    alias(libs.plugins.room)
```
Add a top-level block after `android { }`:
```kotlin
room {
    schemaDirectory("$projectDir/schemas")
}
```
Inside `android { }`, add so `MigrationTestHelper` can read the schemas:
```kotlin
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
```
(If the build fails with a duplicate-asset error, remove this `sourceSets` block. Some Room plugin versions add the folder themselves.)

- [ ] **Step 3: Add the dependencies in `app/build.gradle.kts`**

```kotlin
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.room.testing)
```

- [ ] **Step 4: Turn on schema export at the current version (still 5)**

In `WorkoutDatabase.kt` change `exportSchema = false` to `exportSchema = true`. Do **not** change the version yet.

- [ ] **Step 5: Build and check that the schema was generated**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL, and `app/schemas/com.nary.gym.data.room_db.WorkoutDatabase/5.json` exists.
Run: `ls app/schemas/com.nary.gym.data.room_db.WorkoutDatabase/`
Expected: `5.json`

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts app/src/main/java/com/nary/gym/data/room_db/WorkoutDatabase.kt app/schemas
git commit -m "build: add location/test deps and export Room schema v5"
```

---

### Task 2: `GymVisit` entity, DAO and migration from v5 to v6

**Files:**
- Create: `data/room_db/GymVisit.kt`, `data/room_db/GymVisitDao.kt`, `data/room_db/Migrations.kt`
- Modify: `data/room_db/WorkoutDatabase.kt`, `data/room_db/Converters.kt`, `data/room_db/RoomModule.kt`
- Test: `app/src/androidTest/java/com/nary/gym/data/room_db/WorkoutDatabaseMigrationTest.kt`

(All `main` paths are under `app/src/main/java/com/nary/gym/`.)

**Interfaces:**
- Produces:
  - `data class GymVisit(id: String = UUID, checkInAt: Long, checkOutAt: Long? = null, source: VisitSource, autoClosed: Boolean = false)`
  - `enum class VisitSource { AUTO, MANUAL }`
  - `interface GymVisitDao { suspend fun insert(visit: GymVisit); suspend fun update(visit: GymVisit); suspend fun deleteById(id: String); suspend fun getOpenVisit(): GymVisit?; fun observeAll(): Flow<List<GymVisit>>; fun observeOpenVisit(): Flow<GymVisit?> }`
  - `WorkoutDatabase.gymVisitDao()`, the Hilt-provided `GymVisitDao`, and `val MIGRATION_5_6: Migration`

- [ ] **Step 1: Write the failing migration test**

`app/src/androidTest/java/com/nary/gym/data/room_db/WorkoutDatabaseMigrationTest.kt`:
```kotlin
package com.nary.gym.data.room_db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WorkoutDatabase::class.java
    )

    @Test
    fun migrate5To6_keepsWorkoutsAndCreatesGymVisits() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL(
                "INSERT INTO workouts (id, workOutName, workOutType, workOutImage) " +
                    "VALUES ('w1', 'Squat', 'LEG', '')"
            )
            close()
        }

        // validateDroppedTables = true also checks gym_visits matches the entity exactly.
        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, MIGRATION_5_6)

        db.query("SELECT workOutName FROM workouts WHERE id = 'w1'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Squat", cursor.getString(0))
        }
        db.execSQL(
            "INSERT INTO gym_visits (id, checkInAt, checkOutAt, source, autoClosed) " +
                "VALUES ('v1', 1000, NULL, 'AUTO', 0)"
        )
        db.query("SELECT COUNT(*) FROM gym_visits").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals(1, cursor.getInt(0))
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./gradlew connectedDebugAndroidTest --tests "com.nary.gym.data.room_db.WorkoutDatabaseMigrationTest"` (needs an emulator/device)
Expected: compilation FAIL, `Unresolved reference: MIGRATION_5_6`.

- [ ] **Step 3: Create the entity**

`data/room_db/GymVisit.kt`:
```kotlin
package com.nary.gym.data.room_db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class VisitSource { AUTO, MANUAL }

/**
 * One gym visit. [checkOutAt] is null while the user is still checked in.
 * [autoClosed] marks visits whose check-out was never observed and was capped by the app.
 * Ids are client-generated so offline-created visits can later be uploaded without clashes.
 */
@Entity(tableName = "gym_visits", indices = [Index("checkInAt")])
data class GymVisit(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val checkInAt: Long,
    val checkOutAt: Long? = null,
    val source: VisitSource,
    val autoClosed: Boolean = false,
)
```

- [ ] **Step 4: Create the DAO**

`data/room_db/GymVisitDao.kt`:
```kotlin
package com.nary.gym.data.room_db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GymVisitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(visit: GymVisit)

    @Update
    suspend fun update(visit: GymVisit)

    @Query("DELETE FROM gym_visits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM gym_visits WHERE checkOutAt IS NULL ORDER BY checkInAt DESC LIMIT 1")
    suspend fun getOpenVisit(): GymVisit?

    @Query("SELECT * FROM gym_visits ORDER BY checkInAt DESC")
    fun observeAll(): Flow<List<GymVisit>>

    @Query("SELECT * FROM gym_visits WHERE checkOutAt IS NULL ORDER BY checkInAt DESC LIMIT 1")
    fun observeOpenVisit(): Flow<GymVisit?>
}
```

- [ ] **Step 5: Add a `VisitSource` converter**

Append to the `Converters` class in `data/room_db/Converters.kt` (and add `import` if needed; `VisitSource` is in the same package):
```kotlin
    @TypeConverter
    fun fromVisitSource(value: VisitSource): String = value.name

    @TypeConverter
    fun toVisitSource(value: String): VisitSource = VisitSource.valueOf(value)
```

- [ ] **Step 6: Write the migration**

`data/room_db/Migrations.kt`:
```kotlin
package com.nary.gym.data.room_db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Adds gym_visits. The SQL must match what Room generates for [GymVisit] exactly
 * (compare with app/schemas/.../6.json) - WorkoutDatabaseMigrationTest enforces this.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `gym_visits` (" +
                "`id` TEXT NOT NULL, " +
                "`checkInAt` INTEGER NOT NULL, " +
                "`checkOutAt` INTEGER, " +
                "`source` TEXT NOT NULL, " +
                "`autoClosed` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))"
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_gym_visits_checkInAt` ON `gym_visits` (`checkInAt`)")
    }
}
```

- [ ] **Step 7: Register the entity, bump the version, and expose the DAO**

`data/room_db/WorkoutDatabase.kt` becomes:
```kotlin
@Database(
    entities = [
        WorkOuts::class,
        GymVisit::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkOutsDao

    abstract fun gymVisitDao(): GymVisitDao
}
```

- [ ] **Step 8: Wire the migration and DAO in Hilt**

In `data/room_db/RoomModule.kt`, add `.addMigrations(MIGRATION_5_6)` before `.fallbackToDestructiveMigration(true)` (import `com.nary.gym.data.room_db.MIGRATION_5_6`), and add:
```kotlin
    @Provides
    @Singleton
    fun provideGymVisitDao(
        database: WorkoutDatabase
    ): GymVisitDao {
        return database.gymVisitDao()
    }
```
(import `com.nary.gym.data.room_db.GymVisitDao`).

- [ ] **Step 9: Build, then run the migration test**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL and `app/schemas/com.nary.gym.data.room_db.WorkoutDatabase/6.json` exists. Open it and check that the `createSql` for `gym_visits` matches Step 6 (with `${TABLE_NAME}` in place of `gym_visits`).
Run: `./gradlew connectedDebugAndroidTest --tests "com.nary.gym.data.room_db.WorkoutDatabaseMigrationTest"`
Expected: PASS. If it fails with "Migration didn't properly handle gym_visits", copy the exact `createSql` from `6.json` into `MIGRATION_5_6`.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/nary/gym/data/room_db app/src/androidTest app/schemas
git commit -m "feat(checkin): add gym_visits table with v5->v6 migration"
```

---

### Task 3: `GymVisitRepository` with the check-in rules

**Files:**
- Create: `di/TimeModule.kt`, `repository/local/GymVisitRepository.kt`
- Test: `app/src/test/java/com/nary/gym/testutil/TestClock.kt`, `app/src/test/java/com/nary/gym/repository/local/FakeGymVisitDao.kt`, `app/src/test/java/com/nary/gym/repository/local/GymVisitRepositoryTest.kt`

**Interfaces:**
- Consumes: `GymVisitDao`, `GymVisit`, `VisitSource` (Task 2)
- Produces:
  - `@Singleton class GymVisitRepository @Inject constructor(dao: GymVisitDao, clock: Clock)`
    - `fun observeVisits(): Flow<List<GymVisit>>`
    - `fun observeOpenVisit(): Flow<GymVisit?>`
    - `suspend fun checkIn(source: VisitSource): CheckInResult`
    - `suspend fun checkOut(): CheckOutResult`
    - `suspend fun delete(id: String)`
    - `companion object { val MIN_VISIT_MILLIS: Long; val MAX_VISIT_MILLIS: Long }`
  - `sealed interface CheckInResult { data class CheckedIn(val visit: GymVisit, val autoClosedStale: GymVisit?); data class AlreadyCheckedIn(val visit: GymVisit) }`
  - `sealed interface CheckOutResult { data class CheckedOut(val visit: GymVisit); data object Discarded; data object NotCheckedIn }`
  - A Hilt-provided `java.time.Clock`

- [ ] **Step 1: Write the test helpers**

`app/src/test/java/com/nary/gym/testutil/TestClock.kt`:
```kotlin
package com.nary.gym.testutil

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class TestClock(var millis: Long = 0L, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = TestClock(millis, zone)
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
    fun advance(duration: Duration) { millis += duration.toMillis() }
}
```

`app/src/test/java/com/nary/gym/repository/local/FakeGymVisitDao.kt`:
```kotlin
package com.nary.gym.repository.local

import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.GymVisitDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.yield

class FakeGymVisitDao : GymVisitDao {
    val visits = MutableStateFlow<List<GymVisit>>(emptyList())

    override suspend fun insert(visit: GymVisit) {
        visits.value = visits.value.filterNot { it.id == visit.id } + visit
    }

    override suspend fun update(visit: GymVisit) {
        visits.value = visits.value.map { if (it.id == visit.id) visit else it }
    }

    override suspend fun deleteById(id: String) {
        visits.value = visits.value.filterNot { it.id == id }
    }

    override suspend fun getOpenVisit(): GymVisit? {
        // Suspend like real Room I/O would, so concurrent callers can interleave here.
        yield()
        return visits.value.openVisit()
    }

    override fun observeAll(): Flow<List<GymVisit>> =
        visits.map { list -> list.sortedByDescending { it.checkInAt } }

    override fun observeOpenVisit(): Flow<GymVisit?> = visits.map { it.openVisit() }

    private fun List<GymVisit>.openVisit() =
        filter { it.checkOutAt == null }.maxByOrNull { it.checkInAt }
}
```

- [ ] **Step 2: Write the failing tests**

`app/src/test/java/com/nary/gym/repository/local/GymVisitRepositoryTest.kt`:
```kotlin
package com.nary.gym.repository.local

import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.testutil.TestClock
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class GymVisitRepositoryTest {

    private val dao = FakeGymVisitDao()
    private val clock = TestClock(millis = 1_000_000L)
    private val repository = GymVisitRepository(dao, clock)

    @Test
    fun checkIn_createsOpenVisit() = runTest {
        val result = repository.checkIn(VisitSource.AUTO)

        assertTrue(result is CheckInResult.CheckedIn)
        val visit = dao.visits.value.single()
        assertEquals(1_000_000L, visit.checkInAt)
        assertNull(visit.checkOutAt)
        assertEquals(VisitSource.AUTO, visit.source)
    }

    @Test
    fun checkIn_whenAlreadyCheckedIn_doesNotCreateSecondVisit() = runTest {
        repository.checkIn(VisitSource.AUTO)
        clock.advance(Duration.ofMinutes(10))

        val result = repository.checkIn(VisitSource.AUTO)

        assertTrue(result is CheckInResult.AlreadyCheckedIn)
        assertEquals(1, dao.visits.value.size)
    }

    @Test
    fun checkIn_whenOpenVisitIsStale_autoClosesItAndStartsNew() = runTest {
        repository.checkIn(VisitSource.AUTO)
        val firstCheckIn = clock.millis
        clock.advance(Duration.ofHours(20)) // EXIT was never delivered

        val result = repository.checkIn(VisitSource.AUTO) as CheckInResult.CheckedIn

        val stale = result.autoClosedStale!!
        assertEquals(firstCheckIn + GymVisitRepository.MAX_VISIT_MILLIS, stale.checkOutAt)
        assertTrue(stale.autoClosed)
        assertEquals(2, dao.visits.value.size)
        assertEquals(1, dao.visits.value.count { it.checkOutAt == null })
    }

    @Test
    fun checkOut_withoutOpenVisit_isNoOp() = runTest {
        val result = repository.checkOut()

        assertEquals(CheckOutResult.NotCheckedIn, result)
        assertTrue(dao.visits.value.isEmpty())
    }

    @Test
    fun checkOut_underMinDuration_discardsVisit() = runTest {
        repository.checkIn(VisitSource.AUTO)
        clock.advance(Duration.ofMinutes(4))

        val result = repository.checkOut()

        assertEquals(CheckOutResult.Discarded, result)
        assertTrue(dao.visits.value.isEmpty())
    }

    @Test
    fun checkOut_closesVisitAtCurrentTime() = runTest {
        repository.checkIn(VisitSource.MANUAL)
        clock.advance(Duration.ofMinutes(72))

        val result = repository.checkOut() as CheckOutResult.CheckedOut

        assertEquals(clock.millis, result.visit.checkOutAt)
        assertFalse(result.visit.autoClosed)
        assertEquals(result.visit, dao.visits.value.single())
    }

    @Test
    fun checkOut_afterMaxDuration_capsVisitAndFlagsIt() = runTest {
        repository.checkIn(VisitSource.AUTO)
        val checkIn = clock.millis
        clock.advance(Duration.ofHours(9))

        val result = repository.checkOut() as CheckOutResult.CheckedOut

        assertEquals(checkIn + GymVisitRepository.MAX_VISIT_MILLIS, result.visit.checkOutAt)
        assertTrue(result.visit.autoClosed)
    }

    @Test
    fun concurrentCheckIns_createExactlyOneVisit() = runTest {
        // A geofence DWELL and a manual tap racing each other.
        launch { repository.checkIn(VisitSource.AUTO) }
        launch { repository.checkIn(VisitSource.MANUAL) }
        testScheduler.advanceUntilIdle()

        assertEquals(1, dao.visits.value.size)
    }

    @Test
    fun delete_removesVisit() = runTest {
        val visit = (repository.checkIn(VisitSource.AUTO) as CheckInResult.CheckedIn).visit

        repository.delete(visit.id)

        assertTrue(dao.visits.value.isEmpty())
    }
}
```

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.repository.local.GymVisitRepositoryTest"`
Expected: compilation FAIL, `Unresolved reference: GymVisitRepository`.

- [ ] **Step 4: Provide `Clock` through Hilt**

`app/src/main/java/com/nary/gym/di/TimeModule.kt`:
```kotlin
package com.nary.gym.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TimeModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
```

- [ ] **Step 5: Implement the repository**

`app/src/main/java/com/nary/gym/repository/local/GymVisitRepository.kt`:
```kotlin
package com.nary.gym.repository.local

import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.GymVisitDao
import com.nary.gym.data.room_db.VisitSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

sealed interface CheckInResult {
    data class CheckedIn(val visit: GymVisit, val autoClosedStale: GymVisit?) : CheckInResult
    data class AlreadyCheckedIn(val visit: GymVisit) : CheckInResult
}

sealed interface CheckOutResult {
    data class CheckedOut(val visit: GymVisit) : CheckOutResult
    data object Discarded : CheckOutResult
    data object NotCheckedIn : CheckOutResult
}

/**
 * Local-only store of gym visits and the single place check-in rules live. Geofence events,
 * notification actions and manual taps all go through here, so it is a @Singleton and every
 * read-modify-write runs under [mutex] - otherwise two near-simultaneous check-ins would both
 * see "no open visit" and insert twice.
 */
@Singleton
class GymVisitRepository @Inject constructor(
    private val dao: GymVisitDao,
    private val clock: Clock,
) {
    private val mutex = Mutex()

    fun observeVisits(): Flow<List<GymVisit>> = dao.observeAll()

    fun observeOpenVisit(): Flow<GymVisit?> = dao.observeOpenVisit()

    suspend fun checkIn(source: VisitSource): CheckInResult = mutex.withLock {
        val now = clock.millis()
        val open = dao.getOpenVisit()
        var autoClosedStale: GymVisit? = null

        if (open != null) {
            if (now - open.checkInAt < MAX_VISIT_MILLIS) {
                return@withLock CheckInResult.AlreadyCheckedIn(open)
            }
            // The EXIT for that visit was never delivered - close it at the cap.
            autoClosedStale = open.capped()
            dao.update(autoClosedStale)
        }

        val visit = GymVisit(checkInAt = now, source = source)
        dao.insert(visit)
        CheckInResult.CheckedIn(visit, autoClosedStale)
    }

    suspend fun checkOut(): CheckOutResult = mutex.withLock {
        val now = clock.millis()
        val open = dao.getOpenVisit() ?: return@withLock CheckOutResult.NotCheckedIn
        val duration = now - open.checkInAt

        when {
            duration < MIN_VISIT_MILLIS -> {
                dao.deleteById(open.id)
                CheckOutResult.Discarded
            }
            duration > MAX_VISIT_MILLIS -> {
                val closed = open.capped()
                dao.update(closed)
                CheckOutResult.CheckedOut(closed)
            }
            else -> {
                val closed = open.copy(checkOutAt = now)
                dao.update(closed)
                CheckOutResult.CheckedOut(closed)
            }
        }
    }

    suspend fun delete(id: String) = mutex.withLock { dao.deleteById(id) }

    private fun GymVisit.capped() =
        copy(checkOutAt = checkInAt + MAX_VISIT_MILLIS, autoClosed = true)

    companion object {
        /** Shorter visits are treated as geofence jitter or accidental taps and discarded. */
        val MIN_VISIT_MILLIS: Long = Duration.ofMinutes(5).toMillis()

        /** A visit with no observed check-out is closed at this length. */
        val MAX_VISIT_MILLIS: Long = Duration.ofHours(4).toMillis()
    }
}
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.repository.local.GymVisitRepositoryTest"`
Expected: 9 tests PASS.
Optional learning check: temporarily remove `mutex.withLock` from `checkIn` and confirm `concurrentCheckIns_createExactlyOneVisit` fails, then put it back.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/nary/gym/di/TimeModule.kt app/src/main/java/com/nary/gym/repository/local app/src/test
git commit -m "feat(checkin): add GymVisitRepository with check-in/out rules"
```

---

### Task 4: Visit stats and duration formatting

**Files:**
- Create: `utils/VisitStats.kt`
- Test: `app/src/test/java/com/nary/gym/utils/VisitStatsTest.kt`

**Interfaces:**
- Consumes: `GymVisit` (Task 2)
- Produces: `fun formatDuration(millis: Long): String`, `data class WeeklySummary(val visitCount: Int, val totalMillis: Long)`, `fun weeklySummary(visits: List<GymVisit>, now: Instant, zone: ZoneId): WeeklySummary`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/com/nary/gym/utils/VisitStatsTest.kt`:
```kotlin
package com.nary.gym.utils

import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset

class VisitStatsTest {

    private val zone = ZoneOffset.UTC
    private fun at(y: Int, m: Int, d: Int, h: Int) =
        LocalDateTime.of(y, m, d, h, 0).toInstant(zone).toEpochMilli()
    private fun visit(checkIn: Long, minutes: Long?) = GymVisit(
        checkInAt = checkIn,
        checkOutAt = minutes?.let { checkIn + Duration.ofMinutes(it).toMillis() },
        source = VisitSource.AUTO,
    )

    @Test
    fun formatDuration_formatsMinutesAndHours() {
        assertEquals("0m", formatDuration(0))
        assertEquals("45m", formatDuration(Duration.ofMinutes(45).toMillis()))
        assertEquals("1h", formatDuration(Duration.ofMinutes(60).toMillis()))
        assertEquals("1h 12m", formatDuration(Duration.ofMinutes(72).toMillis()))
    }

    @Test
    fun weeklySummary_countsCompletedVisitsSinceMonday() {
        // 2026-09-24 is a Thursday; the week starts Monday 2026-09-21.
        val now = java.time.Instant.ofEpochMilli(at(2026, 9, 24, 20))
        val visits = listOf(
            visit(at(2026, 9, 21, 7), 60),   // Monday - counted
            visit(at(2026, 9, 23, 18), 30),  // Wednesday - counted
            visit(at(2026, 9, 20, 18), 90),  // last Sunday - excluded
            visit(at(2026, 9, 24, 19), null) // still open - excluded
        )

        val summary = weeklySummary(visits, now, zone)

        assertEquals(2, summary.visitCount)
        assertEquals(Duration.ofMinutes(90).toMillis(), summary.totalMillis)
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.utils.VisitStatsTest"`
Expected: compilation FAIL, `Unresolved reference: formatDuration`.

- [ ] **Step 3: Implement**

`app/src/main/java/com/nary/gym/utils/VisitStats.kt`:
```kotlin
package com.nary.gym.utils

import com.nary.gym.data.room_db.GymVisit
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** "45m", "1h", "1h 12m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return when {
        hours == 0L -> "${minutes}m"
        minutes == 0L -> "${hours}h"
        else -> "${hours}h ${minutes}m"
    }
}

data class WeeklySummary(val visitCount: Int, val totalMillis: Long)

/** Completed visits whose check-in falls in the current Monday-start week. */
fun weeklySummary(visits: List<GymVisit>, now: Instant, zone: ZoneId): WeeklySummary {
    val weekStart = now.atZone(zone).toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        .atStartOfDay(zone)
        .toInstant()
        .toEpochMilli()
    val completed = visits.filter { it.checkOutAt != null && it.checkInAt >= weekStart }
    return WeeklySummary(
        visitCount = completed.size,
        totalMillis = completed.sumOf { it.checkOutAt!! - it.checkInAt }
    )
}
```

- [ ] **Step 4: Run to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.utils.VisitStatsTest"`
Expected: 2 tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nary/gym/utils/VisitStats.kt app/src/test/java/com/nary/gym/utils
git commit -m "feat(checkin): add visit duration formatting and weekly summary"
```

---

### Task 5: Location permission step logic

This keeps the multi-step permission rules in a pure function so they can be tested, separate from the Compose UI.

**Files:**
- Create: `checkin/LocationPermissions.kt`
- Test: `app/src/test/java/com/nary/gym/checkin/LocationPermissionsTest.kt`

**Interfaces:**
- Produces: `enum class LocationPermissionStep { REQUEST_FOREGROUND, NEED_PRECISE, REQUEST_BACKGROUND, GRANTED }`, `fun nextLocationPermissionStep(sdkInt: Int, hasFine: Boolean, hasCoarse: Boolean, hasBackground: Boolean): LocationPermissionStep`, `fun Context.currentLocationPermissionStep(): LocationPermissionStep`, `fun Context.hasFineLocationPermission(): Boolean`

- [ ] **Step 1: Write the failing tests**

`app/src/test/java/com/nary/gym/checkin/LocationPermissionsTest.kt`:
```kotlin
package com.nary.gym.checkin

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationPermissionsTest {

    @Test
    fun nothingGranted_requestsForeground() {
        assertEquals(
            LocationPermissionStep.REQUEST_FOREGROUND,
            nextLocationPermissionStep(sdkInt = 34, hasFine = false, hasCoarse = false, hasBackground = false)
        )
    }

    @Test
    fun coarseOnly_needsPrecise() {
        assertEquals(
            LocationPermissionStep.NEED_PRECISE,
            nextLocationPermissionStep(sdkInt = 34, hasFine = false, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineWithoutBackground_onApi29Plus_requestsBackground() {
        assertEquals(
            LocationPermissionStep.REQUEST_BACKGROUND,
            nextLocationPermissionStep(sdkInt = 29, hasFine = true, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineOnApi28_isEnough() {
        // Background location permission doesn't exist before API 29.
        assertEquals(
            LocationPermissionStep.GRANTED,
            nextLocationPermissionStep(sdkInt = 28, hasFine = true, hasCoarse = true, hasBackground = false)
        )
    }

    @Test
    fun fineAndBackground_isGranted() {
        assertEquals(
            LocationPermissionStep.GRANTED,
            nextLocationPermissionStep(sdkInt = 36, hasFine = true, hasCoarse = true, hasBackground = true)
        )
    }
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.checkin.LocationPermissionsTest"`
Expected: compilation FAIL, `Unresolved reference: nextLocationPermissionStep`.

- [ ] **Step 3: Implement**

`app/src/main/java/com/nary/gym/checkin/LocationPermissions.kt`:
```kotlin
package com.nary.gym.checkin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Where the user is in the permission ladder auto check-in needs. */
enum class LocationPermissionStep { REQUEST_FOREGROUND, NEED_PRECISE, REQUEST_BACKGROUND, GRANTED }

private const val API_Q = 29 // ACCESS_BACKGROUND_LOCATION was introduced in Android 10

/**
 * Geofencing needs precise (fine) location, and to fire while the app is closed it also needs
 * background location on API 29+. Kept pure so the ladder can be unit-tested.
 */
fun nextLocationPermissionStep(
    sdkInt: Int,
    hasFine: Boolean,
    hasCoarse: Boolean,
    hasBackground: Boolean,
): LocationPermissionStep = when {
    !hasFine && !hasCoarse -> LocationPermissionStep.REQUEST_FOREGROUND
    !hasFine -> LocationPermissionStep.NEED_PRECISE
    sdkInt >= API_Q && !hasBackground -> LocationPermissionStep.REQUEST_BACKGROUND
    else -> LocationPermissionStep.GRANTED
}

fun Context.hasFineLocationPermission(): Boolean = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)

fun Context.currentLocationPermissionStep(): LocationPermissionStep = nextLocationPermissionStep(
    sdkInt = Build.VERSION.SDK_INT,
    hasFine = hasFineLocationPermission(),
    hasCoarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION),
    hasBackground = Build.VERSION.SDK_INT < API_Q ||
        isGranted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
)

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
```

- [ ] **Step 4: Add the permissions to the manifest**

In `app/src/main/AndroidManifest.xml`, after the notification permissions:
```xml
    <!-- Gym auto check-in (geofencing) -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
```

- [ ] **Step 5: Run to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.nary.gym.checkin.LocationPermissionsTest"`
Expected: 5 tests PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nary/gym/checkin/LocationPermissions.kt app/src/test/java/com/nary/gym/checkin app/src/main/AndroidManifest.xml
git commit -m "feat(checkin): add location permission ladder"
```

---

### Task 6: Check-in notifications and the Undo action

**Files:**
- Create: `checkin/CheckInNotificationHelper.kt`, `checkin/CheckInActionReceiver.kt`
- Modify: `AndroidManifest.xml`, `NaryApplication.kt` (channel creation only)

(No JVM tests: this is notification glue. It is verified in Task 9.)

**Interfaces:**
- Consumes: `GymVisit`, `GymVisitRepository.delete` (Tasks 2–3), `WorkoutCycle`, `formatDuration` (Task 4)
- Produces: `object CheckInNotificationHelper { fun createChannel(context: Context); fun showCheckedIn(context: Context, visit: GymVisit); fun showCheckedOut(context: Context, visit: GymVisit); fun cancel(context: Context); const val SOURCE_CHECK_IN = "checkin" }`. When MainActivity is launched from these notifications, the intent carries extra `"source" = "checkin"`.

- [ ] **Step 1: `CheckInNotificationHelper`**

`app/src/main/java/com/nary/gym/checkin/CheckInNotificationHelper.kt`:
```kotlin
package com.nary.gym.checkin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.nary.gym.MainActivity
import com.nary.gym.R
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.utils.WorkoutCycle
import com.nary.gym.utils.formatDuration

object CheckInNotificationHelper {
    private const val CHANNEL_ID = "gym-checkin"
    private const val NOTIFICATION_ID = 7001
    const val ACTION_UNDO_CHECK_IN = "com.nary.gym.action.UNDO_CHECK_IN"
    const val EXTRA_VISIT_ID = "visit_id"
    const val SOURCE_CHECK_IN = "checkin"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Gym check-in",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Automatic check-in and check-out at your gym"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun showCheckedIn(context: Context, visit: GymVisit) {
        val today = WorkoutCycle(context).getCurrentDayWorkout().title
        val undoIntent = Intent(context, CheckInActionReceiver::class.java).apply {
            action = ACTION_UNDO_CHECK_IN
            putExtra(EXTRA_VISIT_ID, visit.id)
        }
        val undoPendingIntent = PendingIntent.getBroadcast(
            context, 2, undoIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        notify(
            context,
            baseBuilder(context)
                .setContentTitle("Checked in at the gym")
                .setContentText("Today: $today day")
                .addAction(0, "Undo", undoPendingIntent)
        )
    }

    fun showCheckedOut(context: Context, visit: GymVisit) {
        val duration = formatDuration((visit.checkOutAt ?: visit.checkInAt) - visit.checkInAt)
        notify(
            context,
            baseBuilder(context)
                .setContentTitle("Checked out")
                .setContentText("Workout time: $duration")
        )
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun baseBuilder(context: Context): NotificationCompat.Builder {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("source", SOURCE_CHECK_IN)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context, 124, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
    }

    private fun notify(context: Context, builder: NotificationCompat.Builder) {
        // Silently no-ops if POST_NOTIFICATIONS was denied; the visit is still recorded.
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())
    }
}
```

- [ ] **Step 2: `CheckInActionReceiver`**

`app/src/main/java/com/nary/gym/checkin/CheckInActionReceiver.kt`:
```kotlin
package com.nary.gym.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nary.gym.repository.local.GymVisitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/** "Undo" on the check-in notification: the geofence fired but the user isn't really at the gym. */
@AndroidEntryPoint
class CheckInActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: GymVisitRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CheckInNotificationHelper.ACTION_UNDO_CHECK_IN) return
        val visitId = intent.getStringExtra(CheckInNotificationHelper.EXTRA_VISIT_ID) ?: return
        CheckInNotificationHelper.cancel(context)

        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                repository.delete(visitId)
            } finally {
                pending.finish()
            }
        }
    }
}
```

- [ ] **Step 3: Manifest and channel**

`AndroidManifest.xml`, next to the other receivers:
```xml
        <receiver
            android:name=".checkin.CheckInActionReceiver"
            android:exported="false" />
```
`NaryApplication.onCreate`, right after `NotificationHelper.createNotificationChannel(this)`:
```kotlin
        CheckInNotificationHelper.createChannel(this)
```
(import `com.nary.gym.checkin.CheckInNotificationHelper`).

- [ ] **Step 4: Build and run all unit tests**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all tests PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nary/gym/checkin app/src/main/AndroidManifest.xml app/src/main/java/com/nary/gym/NaryApplication.kt
git commit -m "feat(checkin): add check-in/out notifications with undo"
```

---

### Task 7: Gym location store, current location and geofence registration

This task is platform glue with no JVM-testable logic. It is verified with a build here and on the emulator in Task 9.

**Files:**
- Create: `data/local/GymLocationStore.kt`, `checkin/CurrentLocationProvider.kt`, `checkin/GymGeofenceManager.kt`, `checkin/GeofenceBroadcastReceiver.kt`
- Modify: `AndroidManifest.xml`

**Interfaces:**
- Consumes: `GymVisitRepository`, `CheckInResult`, `CheckOutResult` (Task 3); `currentLocationPermissionStep()` (Task 5); `CheckInNotificationHelper` (Task 6)
- Produces:
  - `data class GymLocation(val latitude: Double, val longitude: Double, val radiusMeters: Float)`
  - `@Singleton class GymLocationStore { fun getGym(): GymLocation?; fun saveGym(gym: GymLocation); fun isAutoCheckInEnabled(): Boolean; fun setAutoCheckInEnabled(enabled: Boolean); companion object { const val DEFAULT_RADIUS_METERS = 150f; val RADIUS_OPTIONS_METERS = listOf(100f, 150f, 250f) } }`
  - `@Singleton class CurrentLocationProvider { suspend fun getCurrentLocation(): Location? }` (throws `SecurityException` without fine permission)
  - `@Singleton class GymGeofenceManager { suspend fun syncRegistration(): Boolean; suspend fun unregister() }`

- [ ] **Step 1: `GymLocationStore`**

`app/src/main/java/com/nary/gym/data/local/GymLocationStore.kt`:
```kotlin
package com.nary.gym.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class GymLocation(val latitude: Double, val longitude: Double, val radiusMeters: Float)

/** The user's gym and whether auto check-in is switched on. Local only. */
@Singleton
class GymLocationStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs = context.getSharedPreferences("gym_checkin", Context.MODE_PRIVATE)

    fun getGym(): GymLocation? {
        if (!prefs.contains(KEY_LAT)) return null
        return GymLocation(
            // SharedPreferences has no putDouble; store the raw bits in a Long.
            latitude = Double.fromBits(prefs.getLong(KEY_LAT, 0L)),
            longitude = Double.fromBits(prefs.getLong(KEY_LNG, 0L)),
            radiusMeters = prefs.getFloat(KEY_RADIUS, DEFAULT_RADIUS_METERS),
        )
    }

    fun saveGym(gym: GymLocation) {
        prefs.edit {
            putLong(KEY_LAT, gym.latitude.toRawBits())
            putLong(KEY_LNG, gym.longitude.toRawBits())
            putFloat(KEY_RADIUS, gym.radiusMeters)
        }
    }

    fun isAutoCheckInEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setAutoCheckInEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_ENABLED, enabled) }
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 150f
        val RADIUS_OPTIONS_METERS = listOf(100f, 150f, 250f)

        private const val KEY_LAT = "gym_lat"
        private const val KEY_LNG = "gym_lng"
        private const val KEY_RADIUS = "gym_radius"
        private const val KEY_ENABLED = "auto_check_in_enabled"
    }
}
```

- [ ] **Step 2: `CurrentLocationProvider`**

`app/src/main/java/com/nary/gym/checkin/CurrentLocationProvider.kt`:
```kotlin
package com.nary.gym.checkin

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CurrentLocationProvider @Inject constructor(@ApplicationContext context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    /**
     * A fresh high-accuracy fix, or null if device location is off / no fix was obtained.
     * Throws SecurityException if fine location isn't granted - callers check first.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? {
        val cancellation = CancellationTokenSource()
        return try {
            client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellation.token).await()
        } finally {
            cancellation.cancel()
        }
    }
}
```

- [ ] **Step 3: `GymGeofenceManager`**

`app/src/main/java/com/nary/gym/checkin/GymGeofenceManager.kt`:
```kotlin
package com.nary.gym.checkin

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.local.GymLocationStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the system geofence in step with [GymLocationStore]. Geofences are wiped by the system
 * on reboot, app-data clear and when location is turned off, so [syncRegistration] is idempotent
 * and is called on every process start (see NaryApplication).
 */
@Singleton
class GymGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: GymLocationStore,
) {
    private val client = LocationServices.getGeofencingClient(context)

    private val pendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // Must be MUTABLE: Play services writes the GeofencingEvent into this intent's extras.
        PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /** Registers the gym geofence if enabled, set and permitted; otherwise removes it. Returns true if active. */
    @SuppressLint("MissingPermission") // checked via currentLocationPermissionStep()
    suspend fun syncRegistration(): Boolean {
        val gym = store.getGym()
        if (!store.isAutoCheckInEnabled() || gym == null ||
            context.currentLocationPermissionStep() != LocationPermissionStep.GRANTED
        ) {
            unregister()
            return false
        }
        return try {
            // Adding with the same request id replaces the existing geofence.
            client.addGeofences(buildRequest(gym), pendingIntent).await()
            true
        } catch (e: Exception) {
            // Typically GEOFENCE_NOT_AVAILABLE because device location is off.
            Log.e(TAG, "Failed to register gym geofence", e)
            false
        }
    }

    suspend fun unregister() {
        try {
            client.removeGeofences(listOf(GEOFENCE_ID)).await()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove gym geofence", e)
        }
    }

    private fun buildRequest(gym: GymLocation): GeofencingRequest {
        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(gym.latitude, gym.longitude, gym.radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_EXIT)
            .setLoiteringDelay(LOITERING_DELAY_MS)
            // Let the system batch location checks to save battery; check-in isn't time-critical.
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .build()
        return GeofencingRequest.Builder()
            // If already inside when registering, deliver DWELL after the loitering delay.
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()
    }

    private companion object {
        const val TAG = "GymGeofenceManager"
        const val GEOFENCE_ID = "nary_gym"
        const val REQUEST_CODE = 4201
        val LOITERING_DELAY_MS = Duration.ofMinutes(3).toMillis().toInt()
        val RESPONSIVENESS_MS = Duration.ofMinutes(2).toMillis().toInt()
    }
}
```

- [ ] **Step 4: `GeofenceBroadcastReceiver`**

`app/src/main/java/com/nary/gym/checkin/GeofenceBroadcastReceiver.kt`:
```kotlin
package com.nary.gym.checkin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.repository.local.CheckInResult
import com.nary.gym.repository.local.CheckOutResult
import com.nary.gym.repository.local.GymVisitRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: GymVisitRepository

    @Inject
    lateinit var store: GymLocationStore

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            // GEOFENCE_NOT_AVAILABLE: location was turned off and the system dropped our geofence.
            // It is re-registered on the next process start (NaryApplication).
            Log.w(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        if (!store.isAutoCheckInEnabled()) return

        // goAsync keeps the receiver alive (~10 s budget) while Room does its I/O.
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (event.geofenceTransition) {
                    Geofence.GEOFENCE_TRANSITION_DWELL -> {
                        val result = repository.checkIn(VisitSource.AUTO)
                        if (result is CheckInResult.CheckedIn) {
                            CheckInNotificationHelper.showCheckedIn(context, result.visit)
                        }
                    }
                    Geofence.GEOFENCE_TRANSITION_EXIT -> {
                        val result = repository.checkOut()
                        if (result is CheckOutResult.CheckedOut) {
                            CheckInNotificationHelper.showCheckedOut(context, result.visit)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle geofence transition", e)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
```

- [ ] **Step 5: Register the receiver in the manifest**

In `AndroidManifest.xml`, next to the other receivers:
```xml
        <!-- Gym auto check-in -->
        <receiver
            android:name=".checkin.GeofenceBroadcastReceiver"
            android:exported="false" />
```

- [ ] **Step 6: Build**

Run: `./gradlew assembleDebug testDebugUnitTest`
Expected: BUILD SUCCESSFUL; all unit tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/nary/gym/data/local/GymLocationStore.kt app/src/main/java/com/nary/gym/checkin app/src/main/AndroidManifest.xml
git commit -m "feat(checkin): register gym geofence and handle transitions"
```

---

### Task 8: Check-ins page (ViewModel, screen, permission flow, Home tab) and app lifecycle wiring

**Files:**
- Create: `viewModel/CheckInViewModel.kt`, `ui/screens/CheckIn/CheckInScreen.kt`, `ui/screens/CheckIn/AutoCheckInToggle.kt`
- Modify: `ui/screens/Home/HomeScreen.kt`, `NaryApplication.kt`

**Interfaces:**
- Consumes: everything from Tasks 3–7 (`GymVisitRepository`, `GymLocationStore`, `GymGeofenceManager`, `CurrentLocationProvider`, `CheckInNotificationHelper`, `AutoCheckInToggle` is created here)
- Produces: `@Composable fun CheckInScreen(innerPadding: PaddingValues, viewModel: CheckInViewModel = hiltViewModel())`, reached at Home `pageIndex = 3`

- [ ] **Step 1: `CheckInViewModel`**

`app/src/main/java/com/nary/gym/viewModel/CheckInViewModel.kt`:
```kotlin
package com.nary.gym.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nary.gym.checkin.CurrentLocationProvider
import com.nary.gym.checkin.GymGeofenceManager
import com.nary.gym.data.local.GymLocation
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.repository.local.CheckInResult
import com.nary.gym.repository.local.CheckOutResult
import com.nary.gym.repository.local.GymVisitRepository
import com.nary.gym.utils.formatDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class CheckInViewModel @Inject constructor(
    private val repository: GymVisitRepository,
    private val store: GymLocationStore,
    private val geofenceManager: GymGeofenceManager,
    private val locationProvider: CurrentLocationProvider,
    val clock: Clock,
) : ViewModel() {

    val visits: StateFlow<List<GymVisit>> = repository.observeVisits()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    val openVisit: StateFlow<GymVisit?> = repository.observeOpenVisit()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue = null)

    var gym by mutableStateOf(store.getGym())
        private set

    var autoCheckInEnabled by mutableStateOf(store.isAutoCheckInEnabled())
        private set

    var isLocating by mutableStateOf(false)
        private set

    var message by mutableStateOf<String?>(null)
        private set

    /** Caller must hold fine location permission. */
    fun useCurrentLocationAsGym() {
        viewModelScope.launch {
            isLocating = true
            try {
                val location = locationProvider.getCurrentLocation()
                if (location == null) {
                    message = "Couldn't get your location. Is device location turned on?"
                    return@launch
                }
                if (location.accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
                    message = "Location too imprecise (±${location.accuracy.toInt()} m). Try again near a window or outside."
                    return@launch
                }
                val updated = GymLocation(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    radiusMeters = gym?.radiusMeters ?: GymLocationStore.DEFAULT_RADIUS_METERS,
                )
                store.saveGym(updated)
                gym = updated
                geofenceManager.syncRegistration()
                message = "Gym location saved"
            } catch (e: SecurityException) {
                message = "Location permission is needed to set your gym"
            } finally {
                isLocating = false
            }
        }
    }

    fun setRadius(radiusMeters: Float) {
        val current = gym ?: return
        val updated = current.copy(radiusMeters = radiusMeters)
        store.saveGym(updated)
        gym = updated
        viewModelScope.launch { geofenceManager.syncRegistration() }
    }

    /** Called once every location permission is granted. */
    fun enableAutoCheckIn() {
        store.setAutoCheckInEnabled(true)
        autoCheckInEnabled = true
        viewModelScope.launch {
            if (!geofenceManager.syncRegistration()) {
                message = "Couldn't start auto check-in. Make sure device location is on."
            }
        }
    }

    fun disableAutoCheckIn() {
        store.setAutoCheckInEnabled(false)
        autoCheckInEnabled = false
        viewModelScope.launch { geofenceManager.unregister() }
    }

    fun manualCheckIn() {
        viewModelScope.launch {
            message = when (repository.checkIn(VisitSource.MANUAL)) {
                is CheckInResult.CheckedIn -> "Checked in"
                is CheckInResult.AlreadyCheckedIn -> "You're already checked in"
            }
        }
    }

    fun manualCheckOut() {
        viewModelScope.launch {
            message = when (val result = repository.checkOut()) {
                is CheckOutResult.CheckedOut ->
                    "Checked out · ${formatDuration(result.visit.checkOutAt!! - result.visit.checkInAt)}"
                CheckOutResult.Discarded -> "Visit under 5 minutes discarded"
                CheckOutResult.NotCheckedIn -> "You're not checked in"
            }
        }
    }

    fun deleteVisit(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun clearMessage() {
        message = null
    }

    private companion object {
        const val MAX_ACCEPTABLE_ACCURACY_METERS = 100f
    }
}
```

- [ ] **Step 2: `AutoCheckInToggle` (the permission flow)**

`app/src/main/java/com/nary/gym/ui/screens/CheckIn/AutoCheckInToggle.kt`:
```kotlin
package com.nary.gym.ui.screens.CheckIn

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.nary.gym.checkin.LocationPermissionStep
import com.nary.gym.checkin.currentLocationPermissionStep
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.TextColor

/**
 * Auto check-in switch plus the permission ladder: fine+coarse → (API 29+) background.
 * On API 30+ the background request opens the system Settings page, so the step is
 * re-read on every resume to notice the user coming back.
 */
@Composable
fun AutoCheckInToggle(
    enabled: Boolean,
    gymIsSet: Boolean,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
) {
    val context = LocalContext.current
    var step by remember { mutableStateOf(context.currentLocationPermissionStep()) }
    var pendingEnable by remember { mutableStateOf(false) }
    var showBackgroundRationale by remember { mutableStateOf(false) }

    LifecycleResumeEffect(Unit) {
        step = context.currentLocationPermissionStep()
        onPauseOrDispose { }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { step = context.currentLocationPermissionStep() }

    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        step = context.currentLocationPermissionStep()
        if (step == LocationPermissionStep.REQUEST_BACKGROUND) showBackgroundRationale = true
    }

    // Finish enabling once every permission is in place, possibly after returning from Settings.
    LaunchedEffect(step, pendingEnable) {
        if (pendingEnable && step == LocationPermissionStep.GRANTED) {
            pendingEnable = false
            onEnable()
        }
    }

    fun advance() {
        pendingEnable = true
        when (step) {
            LocationPermissionStep.REQUEST_FOREGROUND, LocationPermissionStep.NEED_PRECISE ->
                foregroundLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            LocationPermissionStep.REQUEST_BACKGROUND -> showBackgroundRationale = true
            LocationPermissionStep.GRANTED -> Unit // LaunchedEffect above calls onEnable
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Auto check-in", color = TextColor, fontSize = 16.sp)
                Text(
                    if (gymIsSet) "Check in when you arrive, out when you leave" else "Set your gym location first",
                    color = LabelColor, fontSize = 13.sp
                )
            }
            Switch(
                checked = enabled,
                enabled = gymIsSet,
                onCheckedChange = { checked ->
                    if (checked) advance() else {
                        pendingEnable = false
                        onDisable()
                    }
                }
            )
        }

        if (enabled && step != LocationPermissionStep.GRANTED) {
            Text("Auto check-in is paused: location permission is missing.", color = Accent, fontSize = 13.sp)
            TextButton(onClick = { advance() }) { Text("Fix permissions") }
        }
        if (step == LocationPermissionStep.NEED_PRECISE) {
            Text("Auto check-in needs Precise location, not Approximate.", color = LabelColor, fontSize = 13.sp)
            TextButton(onClick = { openAppSettings(context) }) { Text("Open app settings") }
        }
    }

    if (showBackgroundRationale) {
        AlertDialog(
            onDismissRequest = {
                showBackgroundRationale = false
                pendingEnable = false
            },
            title = { Text("Allow location all the time") },
            text = {
                Text(
                    "To check you in while the app is closed, choose \"${backgroundOptionLabel(context)}\" " +
                        "on the next screen. Your location is only compared with your gym and never leaves this phone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackgroundRationale = false
                    pendingEnable = false
                }) { Text("Not now") }
            }
        )
    }
}

private fun backgroundOptionLabel(context: Context): CharSequence =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.packageManager.backgroundPermissionOptionLabel
    } else {
        "Allow all the time"
    }

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
```

- [ ] **Step 3: `CheckInScreen`**

`app/src/main/java/com/nary/gym/ui/screens/CheckIn/CheckInScreen.kt`:
```kotlin
package com.nary.gym.ui.screens.CheckIn

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.nary.gym.checkin.hasFineLocationPermission
import com.nary.gym.data.local.GymLocationStore
import com.nary.gym.data.room_db.GymVisit
import com.nary.gym.data.room_db.VisitSource
import com.nary.gym.ui.theme.Accent
import com.nary.gym.ui.theme.CardBg
import com.nary.gym.ui.theme.LabelColor
import com.nary.gym.ui.theme.Primary
import com.nary.gym.ui.theme.TextColor
import com.nary.gym.utils.formatDuration
import com.nary.gym.utils.weeklySummary
import com.nary.gym.viewModel.CheckInViewModel
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun CheckInScreen(
    innerPadding: PaddingValues,
    viewModel: CheckInViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val visits by viewModel.visits.collectAsState()
    val openVisit by viewModel.openVisit.collectAsState()
    val zone = viewModel.clock.zone
    var visitPendingDelete by remember { mutableStateOf<GymVisit?>(null) }

    // Ticks once a minute so "checked in for 38m" stays current.
    val now by produceState(viewModel.clock.millis()) {
        while (true) {
            delay(60_000)
            value = viewModel.clock.millis()
        }
    }

    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // Setting the gym only needs foreground location; the toggle handles background.
    val setGymLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
    }

    val summary = weeklySummary(visits, Instant.ofEpochMilli(now), zone)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(Modifier.height(16.dp))
            Text("Gym Check-ins", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextColor)
            Text(
                "${summary.visitCount} visits · ${formatDuration(summary.totalMillis)} this week",
                fontSize = 14.sp, color = LabelColor
            )
        }

        item {
            SectionCard {
                val open = openVisit
                if (open != null) {
                    Text("Checked in", fontWeight = FontWeight.SemiBold, color = Accent)
                    Text(
                        "Since ${formatTime(open.checkInAt, zone)} · ${formatDuration(now - open.checkInAt)}",
                        color = TextColor
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = viewModel::manualCheckOut) { Text("Check out") }
                } else {
                    Text("Not at the gym", fontWeight = FontWeight.SemiBold, color = TextColor)
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = viewModel::manualCheckIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) { Text("Check in now") }
                }
            }
        }

        item {
            SectionCard {
                Text("My gym", fontWeight = FontWeight.SemiBold, color = TextColor)
                val gym = viewModel.gym
                Text(
                    if (gym == null) "Stand inside your gym and tap below to save its location."
                    else "Saved · %.5f, %.5f".format(gym.latitude, gym.longitude),
                    color = LabelColor, fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    enabled = !viewModel.isLocating,
                    onClick = {
                        if (context.hasFineLocationPermission()) viewModel.useCurrentLocationAsGym()
                        else setGymLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        )
                    }
                ) {
                    if (viewModel.isLocating) CircularProgressIndicator(Modifier.height(18.dp))
                    else Text(if (gym == null) "Use current location" else "Update to current location")
                }
                if (gym != null) {
                    Spacer(Modifier.height(8.dp))
                    Text("Radius", color = LabelColor, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymLocationStore.RADIUS_OPTIONS_METERS.forEach { radius ->
                            FilterChip(
                                selected = gym.radiusMeters == radius,
                                onClick = { viewModel.setRadius(radius) },
                                label = { Text("${radius.toInt()} m") }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                AutoCheckInToggle(
                    enabled = viewModel.autoCheckInEnabled,
                    gymIsSet = gym != null,
                    onEnable = viewModel::enableAutoCheckIn,
                    onDisable = viewModel::disableAutoCheckIn
                )
            }
        }

        item { Text("History", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextColor) }

        if (visits.isEmpty()) {
            item { Text("No visits yet.", color = LabelColor) }
        }
        items(visits, key = { it.id }) { visit ->
            VisitRow(visit, zone, onDelete = { visitPendingDelete = visit })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    visitPendingDelete?.let { visit ->
        AlertDialog(
            onDismissRequest = { visitPendingDelete = null },
            title = { Text("Delete visit?") },
            text = { Text("This visit will be permanently deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVisit(visit.id)
                    visitPendingDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { visitPendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun VisitRow(visit: GymVisit, zone: java.time.ZoneId, onDelete: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatDate(visit.checkInAt, zone), fontWeight = FontWeight.SemiBold, color = TextColor)
                val out = visit.checkOutAt
                Text(
                    if (out == null) "${formatTime(visit.checkInAt, zone)} – now"
                    else "${formatTime(visit.checkInAt, zone)} – ${formatTime(out, zone)} · ${formatDuration(out - visit.checkInAt)}",
                    color = TextColor, fontSize = 14.sp
                )
                val tags = buildList {
                    add(if (visit.source == VisitSource.AUTO) "Auto" else "Manual")
                    if (visit.autoClosed) add("Check-out not detected")
                }
                Text(tags.joinToString(" · "), color = LabelColor, fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete visit", tint = LabelColor)
            }
        }
    }
}

private fun formatTime(millis: Long, zone: java.time.ZoneId): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(millis).atZone(zone))

private fun formatDate(millis: Long, zone: java.time.ZoneId): String =
    DateTimeFormatter.ofPattern("EEE, d MMM").format(Instant.ofEpochMilli(millis).atZone(zone))
```

- [ ] **Step 4: Add the Home tab and handle the notification tap**

In `ui/screens/Home/HomeScreen.kt`:

1. Imports: `androidx.compose.material.icons.filled.LocationOn`, `com.nary.gym.checkin.CheckInNotificationHelper`, `com.nary.gym.ui.screens.CheckIn.CheckInScreen`.
2. In the `LaunchedEffect(currentIntent)` block, add a branch after the `notification_yes` one:
```kotlin
        } else if (source == CheckInNotificationHelper.SOURCE_CHECK_IN) {
            pageIndex = 3
            activity?.intent?.removeExtra("source")
```
3. In the bottom bar `Row`, add a fourth `IconButton` **before** the Profile (`Person`) button:
```kotlin
                    IconButton(onClick = {
                        pageIndex = 3
                    }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Check-ins",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
```
4. In `when (pageIndex)`, add before `else ->`:
```kotlin
            3 -> {
                CheckInScreen(innerPadding)
            }
```
(Profile stays at index 2 through `else`, so the existing `narygym://profile` deep link is unaffected.)

- [ ] **Step 5: Re-register on process start and turn off on logout**

In `NaryApplication.kt`:
- Inject:
```kotlin
    @Inject
    lateinit var gymGeofenceManager: GymGeofenceManager

    @Inject
    lateinit var gymLocationStore: GymLocationStore
```
- In the logged-out branch of `collectLatest` (next to `AlarmScheduler.cancelDailyAlarm`):
```kotlin
                    gymLocationStore.setAutoCheckInEnabled(false)
                    gymGeofenceManager.unregister()
```
- In the logged-in branch (after the reminder `if` block):
```kotlin
                    // Geofences don't survive reboots or app-data clears. Every process start
                    // (including the one Android makes for BOOT_COMPLETED) re-syncs here.
                    gymGeofenceManager.syncRegistration()
```
(imports: `com.nary.gym.checkin.GymGeofenceManager`, `com.nary.gym.data.local.GymLocationStore`).

- [ ] **Step 6: Build, lint and test**

Run: `./gradlew assembleDebug testDebugUnitTest lint`
Expected: BUILD SUCCESSFUL, all unit tests PASS, and no new lint *errors*. If lint flags `MissingPermission` in `CurrentLocationProvider`/`GymGeofenceManager`, check that the `@SuppressLint` annotations are there. Callers check permission first.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/nary/gym
git commit -m "feat(checkin): add Check-ins page, permission flow and lifecycle wiring"
```

---

### Task 9: Manual verification on the emulator, then docs

Geofencing can't be unit-tested, so this checklist is the real test. Use an API 34+ emulator with Google Play services.

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Install and set the gym**

Run: `./gradlew installDebug`
In the emulator: Extended controls (⋯) → Location → set a point (e.g. search any address) → **Set location**.
In the app: Check-ins tab → **Use current location** → grant *Precise* → toast "Gym location saved", and the coordinates show.

- [ ] **Step 2: Permission ladder**

Toggle **Auto check-in**:
- If you pick "Approximate", the "Precise location" warning and "Open app settings" appear. The switch stays off.
- With Precise granted, the rationale dialog appears → Continue → system Settings → choose **Allow all the time** → press back. The switch turns on by itself.

- [ ] **Step 3: DWELL → check-in**

Because the emulator is already inside the fence, `INITIAL_TRIGGER_DWELL` fires after about 3 min. Keep the app in the background, and open Google Maps once in the emulator to get location updates flowing.
Expected: a "Checked in at the gym · Today: X day" notification; the status card shows "Checked in"; the history shows an "Auto" visit.
Debug: `adb logcat -s GeofenceReceiver GymGeofenceManager`

- [ ] **Step 4: EXIT → check-out**

Wait more than 5 min after check-in, then in Extended controls set a location more than 1 km away.
Expected within a few minutes: "Checked out · Workout time: Xm", and the visit shows its time range.
(If you do this before 5 min, the visit is discarded and no notification appears. That's D4.)

- [ ] **Step 5: Edge cases**

- Tap **Undo** on a check-in notification → the visit disappears.
- Tap **Check in now** twice → second toast says "You're already checked in".
- Kill the app (`adb shell am force-stop com.nary.gym`), move the location back inside → a check-in still happens (background receiver).
- Reboot the emulator **without opening the app**, then move out and back in. A transition should still arrive, because the boot broadcast starts the process and `NaryApplication` re-syncs the geofence.
- Log out → switch is off after logging back in; history still present (D8).
- Upgrade path: install the previous release APK, add a workout, install this build → workouts still there (migration).

- [ ] **Step 6: Update `CLAUDE.md`**

Add under **Architecture** (after the Notifications bullet):
```markdown
- **Gym auto check-in** (`checkin/`, `ui/screens/CheckIn/`) — The user saves their gym via fused "current location" (`GymLocationStore`, plain prefs `gym_checkin`). `GymGeofenceManager.syncRegistration()` registers one DWELL/EXIT geofence and is re-run on every process start from `NaryApplication` (geofences don't survive reboot/data clear). `GeofenceBroadcastReceiver` → `repository/local/GymVisitRepository`, which owns all rules (no duplicate open visit, <5 min discarded, >4 h capped + `autoClosed`) under a `Mutex`. Visits live in the `gym_visits` table of `WorkoutDatabase` and are **local-only** (no API yet). The geofence `PendingIntent` must stay `FLAG_MUTABLE`.
```
Replace the destructive-migration gotcha with:
```markdown
- `WorkoutDatabase` exports schemas to `app/schemas` and has explicit migrations (`data/room_db/Migrations.kt`). `gym_visits` is local-only data, so **every version bump now needs a real `Migration`** — the `fallbackToDestructiveMigration` safety net would wipe users' visit history. Add a case to `WorkoutDatabaseMigrationTest` for each bump.
- Auto check-in needs `ACCESS_BACKGROUND_LOCATION`; publishing to Play requires the background-location declaration in Play Console.
```
Also update the "only template tests exist" note: unit tests now exist in `app/src/test` (`GymVisitRepositoryTest` with `FakeGymVisitDao` + `TestClock` is the pattern to follow).

- [ ] **Step 7: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: document gym auto check-in architecture"
```

---

## Later: moving to the API (not part of this plan)

What we'd need when the backend is ready. It's listed here so the local design doesn't block it:
- Add `syncedAt: Long?` (or `isDirty`) to `GymVisit` with a `MIGRATION_6_7`.
- A `@HiltWorker` `GymVisitSyncWorker` with a `NetworkType.CONNECTED` constraint that uploads unsynced visits, using the UUID `id` as an idempotency key.
- `GymVisitRepository` stays the only writer, and marks rows dirty on insert, update and delete (soft-delete if the server needs to know about deletions).
