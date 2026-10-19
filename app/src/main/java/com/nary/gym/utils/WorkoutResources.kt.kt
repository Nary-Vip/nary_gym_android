package com.nary.gym.utils
import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.nary.gym.core.WorkOutType
import androidx.core.content.edit
import java.time.LocalDate

class WorkoutCycle(context: Context){
    private var pref = context.getSharedPreferences("workout_cycles", Context.MODE_PRIVATE)

    fun changes(): Flow<Unit> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(Unit) }
        pref.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { pref.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private val entries = WorkOutType.entries

    fun getCurrentDayWorkout() : WorkOutType{
       val index = pref.getInt("workout_index", 0)
        return entries[index]
    }

    /** Whether today's workout has already been marked complete (guards against double-advancing the cycle). */
    fun hasCompletedToday(): Boolean {
        return pref.getLong("last_completed_epoch_day", -1L) == LocalDate.now().toEpochDay()
    }

    /**
     * Advances to the next workout in the rotation, unless today's workout was already marked
     * complete - otherwise tapping "complete" more than once in a day would skip a workout type.
     */
    fun moveToNextDayWorkOut(): WorkOutType {
        val index = pref.getInt("workout_index", 0)
        if (hasCompletedToday()) {
            return entries[index]
        }

        val newIndex = (index + 1) % entries.size
        pref.edit {
            putInt("workout_index", newIndex)
            putLong("last_completed_epoch_day", LocalDate.now().toEpochDay())
        }
        return entries[newIndex]
    }
}