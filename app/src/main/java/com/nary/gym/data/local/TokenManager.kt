package com.nary.gym.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class TokenManager @Inject constructor(@ApplicationContext context: Context) : CurrentAccount {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _accountId = MutableStateFlow(prefs.getString("account_id", null))
    override val accountId: StateFlow<String?> = _accountId.asStateFlow()

    fun saveAccountId(id: String) {
        prefs.edit { putString("account_id", id) }
        _accountId.value = id
    }

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString("access_token", accessToken)
            putString("refresh_token", refreshToken)
        }
    }

    fun getAccessToken(): String? = prefs.getString("access_token", null)

    fun getRefreshToken(): String? = prefs.getString("refresh_token", null)

    fun saveNotificationPreference(enabled: Boolean) {
        prefs.edit {
            putBoolean("notifications_enabled", enabled)
        }
    }

    fun getNotificationPreference(): Boolean = prefs.getBoolean("notifications_enabled", false)

    fun saveReminderTime(hour: Int, minute: Int) {
        prefs.edit {
            putInt("reminder_hour", hour)
            putInt("reminder_minute", minute)
        }
    }

    fun getReminderHour(): Int = prefs.getInt("reminder_hour", 6)
    fun getReminderMinute(): Int = prefs.getInt("reminder_minute", 0)

    fun clearTokens() {
        prefs.edit {
            remove("access_token")
            remove("refresh_token")
            remove("account_id")
        }
        _accountId.value = null
    }
}
