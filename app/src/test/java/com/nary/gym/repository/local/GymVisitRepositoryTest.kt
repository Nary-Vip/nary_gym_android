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
