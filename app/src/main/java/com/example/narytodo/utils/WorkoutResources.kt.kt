package com.example.narytodo.utils
import android.content.Context
import com.example.narytodo.R
import com.example.narytodo.core.WorkOutType
import androidx.core.content.edit

object WorkoutResources {
    val drawableMap = mapOf(
        "machine_shoulder_press"      to R.drawable.machine_shoulder_press,
        "lateral_raise"               to R.drawable.lateral_raise,
        "front_raise"                 to R.drawable.front_raise,
        "machine_rear_delt_fly"       to R.drawable.machine_rear_delt_fly,
        "leg_extension"               to R.drawable.leg_extension,
        "leg_press"                   to R.drawable.leg_press,
        "lying_curls"                 to R.drawable.lying_curls,
        "squats"                      to R.drawable.squats,
        "machine_incline_chest_press" to R.drawable.machine_incline_chest_press,
        "machine_pec_fly"             to R.drawable.machine_pec_fly,
        "machine_flat_press"          to R.drawable.machine_flat_press,
        "push_down"                   to R.drawable.push_down,
        "seated_dips"                 to R.drawable.seated_dips,
        "lat_pulldown"                to R.drawable.lat_pulldown,
        "seated_rowing"               to R.drawable.seated_rowing,
        "one_arm_rowing"              to R.drawable.one_arm_rowing,
        "dumbbell_curls"              to R.drawable.dumbbell_curls,
        "preacher_curls"              to R.drawable.preacher_curls,
    )
}

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