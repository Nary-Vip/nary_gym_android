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
