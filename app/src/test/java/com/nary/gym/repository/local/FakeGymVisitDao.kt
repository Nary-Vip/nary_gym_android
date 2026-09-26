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
        // Read, then suspend before returning, like a real query whose result arrives later.
        // This lets a concurrent caller read the same (now stale) state - the race the
        // repository's Mutex must prevent. Yielding *before* the read would hide the race.
        val result = visits.value.openVisit()
        yield()
        return result
    }

    override fun observeAll(): Flow<List<GymVisit>> =
        visits.map { list -> list.sortedByDescending { it.checkInAt } }

    override fun observeOpenVisit(): Flow<GymVisit?> = visits.map { it.openVisit() }

    private fun List<GymVisit>.openVisit() =
        filter { it.checkOutAt == null }.maxByOrNull { it.checkInAt }
}
