package com.dss.absensiKoas.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "absensi_secure_prefs")

/**
 * Penyimpanan token JWT, refresh token, dan device ID.
 * Menggunakan Jetpack DataStore (lebih aman & async dibanding SharedPreferences).
 */
@Singleton
class TokenManager @Inject constructor(
    private val context: Context
) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val USER_ID = longPreferencesKey("user_id")
        val USERNAME = stringPreferencesKey("username")
        val ROLE = stringPreferencesKey("role")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.REFRESH_TOKEN] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }
    val roleFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ROLE] }

    suspend fun getAccessToken(): String? = accessTokenFlow.first()
    suspend fun getRefreshToken(): String? = refreshTokenFlow.first()

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String?,
        userId: Long,
        username: String,
        role: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            refreshToken?.let { prefs[Keys.REFRESH_TOKEN] = it }
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USERNAME] = username
            prefs[Keys.ROLE] = role
        }
    }

    suspend fun updateAccessToken(accessToken: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
        }
    }

    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.map { it[Keys.DEVICE_ID] }.first()
        if (existing != null) return existing

        // Generate device ID unik berdasarkan Android ID
        val newId = "android-${android.os.Build.MODEL}-${java.util.UUID.randomUUID().toString().take(8)}"
            .replace(" ", "_")
        context.dataStore.edit { it[Keys.DEVICE_ID] = newId }
        return newId
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun isLoggedIn(): Boolean = getAccessToken() != null
}