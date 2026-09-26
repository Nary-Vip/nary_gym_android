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
