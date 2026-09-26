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
