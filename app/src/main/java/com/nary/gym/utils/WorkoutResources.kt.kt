package com.nary.gym.utils
import android.content.Context
import com.nary.gym.R
import com.nary.gym.core.WorkOutType
import androidx.core.content.edit

class WorkoutCycle(context: Context){
    private var pref = context.getSharedPreferences("workout_cycles", Context.MODE_PRIVATE)

    private val entries = WorkOutType.entries

    fun getCurrentDayWorkout() : WorkOutType{
       val index = pref.getInt("workout_index", 0)
        return entries[index]
    }

    fun moveToNextDayWorkOut(): WorkOutType {
        val index = pref.getInt("workout_index", 0)
        val newIndex = (index + 1)% (entries.size)
        pref.edit { putInt("workout_index", newIndex) }
        return entries[newIndex]
    }
}