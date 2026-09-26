package com.nary.gym.data.room_db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymVisitDaoTest {

    private lateinit var db: WorkoutDatabase
    private lateinit var dao: GymVisitDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorkoutDatabase::class.java
        ).build()
        dao = db.gymVisitDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getOpenVisit_ignoresClosedVisits() = runBlocking {
        dao.insert(visit(id = "closed", checkInAt = 3_000, checkOutAt = 4_000))
        dao.insert(visit(id = "open", checkInAt = 1_000))

        assertEquals("open", dao.getOpenVisit()?.id)
        assertEquals("open", dao.observeOpenVisit().first()?.id)
    }

    @Test
    fun getOpenVisit_withSeveralOpen_returnsMostRecent() = runBlocking {
        dao.insert(visit(id = "older", checkInAt = 1_000))
        dao.insert(visit(id = "newer", checkInAt = 2_000))

        assertEquals("newer", dao.getOpenVisit()?.id)
    }

    @Test
    fun getOpenVisit_whenAllClosed_returnsNull() = runBlocking {
        dao.insert(visit(id = "closed", checkInAt = 1_000, checkOutAt = 2_000))

        assertNull(dao.getOpenVisit())
        assertNull(dao.observeOpenVisit().first())
    }

    @Test
    fun observeAll_ordersNewestFirst() = runBlocking {
        dao.insert(visit(id = "middle", checkInAt = 2_000, checkOutAt = 2_500))
        dao.insert(visit(id = "oldest", checkInAt = 1_000, checkOutAt = 1_500))
        dao.insert(visit(id = "newest", checkInAt = 3_000))

        assertEquals(listOf("newest", "middle", "oldest"), dao.observeAll().first().map { it.id })
    }

    @Test
    fun insert_roundTripsAllFields() = runBlocking {
        val original = GymVisit(
            id = "v1",
            checkInAt = 1_000,
            checkOutAt = 2_000,
            source = VisitSource.MANUAL,
            autoClosed = true,
        )

        dao.insert(original)

        // Covers the VisitSource converter and Boolean <-> INTEGER mapping.
        assertEquals(original, dao.observeAll().first().single())
    }

    @Test
    fun update_closesVisit() = runBlocking {
        val open = visit(id = "v1", checkInAt = 1_000)
        dao.insert(open)

        dao.update(open.copy(checkOutAt = 5_000))

        assertNull(dao.getOpenVisit())
        assertEquals(5_000L, dao.observeAll().first().single().checkOutAt)
    }

    @Test
    fun deleteById_removesOnlyThatVisit() = runBlocking {
        dao.insert(visit(id = "keep", checkInAt = 1_000, checkOutAt = 2_000))
        dao.insert(visit(id = "remove", checkInAt = 3_000, checkOutAt = 4_000))

        dao.deleteById("remove")

        assertEquals(listOf("keep"), dao.observeAll().first().map { it.id })
    }

    private fun visit(id: String, checkInAt: Long, checkOutAt: Long? = null) = GymVisit(
        id = id,
        checkInAt = checkInAt,
        checkOutAt = checkOutAt,
        source = VisitSource.AUTO,
    )
}
