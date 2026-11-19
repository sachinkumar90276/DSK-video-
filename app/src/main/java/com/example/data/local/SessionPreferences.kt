package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppSettingsState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest

private val Context.dskDataStore: DataStore<Preferences> by preferencesDataStore(name = "dsk_video_prefs")

data class StoredSession(
    val accessToken: String = "",
    val refreshToken: String = "",
    val userId: String = "",
    val email: String = "",
    val rememberSession: Boolean = true,
    val isAdmin: Boolean = false
)

class SessionPreferences(private val context: Context) {

    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val REMEMBER_SESSION = booleanPreferencesKey("remember_session")
        val IS_ADMIN = booleanPreferencesKey("is_admin")

        val AMOLED_DARK = booleanPreferencesKey("amoled_dark")
        val PREFERRED_LANGUAGE = stringPreferencesKey("preferred_language")
        val AUTO_PLAY_NEXT = booleanPreferencesKey("auto_play_next")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
        val SUBTITLE_SIZE_SP = intPreferencesKey("subtitle_size_sp")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PARENTAL_LOCK_ENABLED = booleanPreferencesKey("parental_lock_enabled")
        val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
        val PROFILE_PIN_HASH = stringPreferencesKey("profile_pin_hash")
        val RECENT_SEARCHES_JSON = stringPreferencesKey("recent_searches_json")
    }

    val sessionFlow: Flow<StoredSession> = context.dskDataStore.data.map { prefs ->
        StoredSession(
            accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty(),
            refreshToken = prefs[Keys.REFRESH_TOKEN].orEmpty(),
            userId = prefs[Keys.USER_ID].orEmpty(),
            email = prefs[Keys.USER_EMAIL].orEmpty(),
            rememberSession = prefs[Keys.REMEMBER_SESSION] ?: true,
            isAdmin = prefs[Keys.IS_ADMIN] ?: false
        )
    }

    val settingsFlow: Flow<AppSettingsState> = context.dskDataStore.data.map { prefs ->
        AppSettingsState(
            amoledDarkTheme = prefs[Keys.AMOLED_DARK] ?: true,
            preferredLanguage = prefs[Keys.PREFERRED_LANGUAGE] ?: "English",
            autoPlayNext = prefs[Keys.AUTO_PLAY_NEXT] ?: true,
            defaultQuality = prefs[Keys.DEFAULT_QUALITY] ?: "Auto",
            subtitlesEnabled = prefs[Keys.SUBTITLES_ENABLED] ?: true,
            subtitleSizeSp = prefs[Keys.SUBTITLE_SIZE_SP] ?: 16,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            parentalLockEnabled = prefs[Keys.PARENTAL_LOCK_ENABLED] ?: false,
            hasParentalPin = !prefs[Keys.PARENTAL_PIN_HASH].isNullOrBlank(),
            hasProfilePin = !prefs[Keys.PROFILE_PIN_HASH].isNullOrBlank()
        )
    }

    val recentSearchesFlow: Flow<List<String>> = context.dskDataStore.data.map { prefs ->
        val raw = prefs[Keys.RECENT_SEARCHES_JSON].orEmpty()
        if (raw.isBlank()) emptyList() else runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        }.getOrDefault(emptyList())
    }

    suspend fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String,
        email: String,
        rememberSession: Boolean,
        isAdmin: Boolean
    ) {
        context.dskDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN] = accessToken
            prefs[Keys.REFRESH_TOKEN] = refreshToken
            prefs[Keys.USER_ID] = userId
            prefs[Keys.USER_EMAIL] = email
            prefs[Keys.REMEMBER_SESSION] = rememberSession
            prefs[Keys.IS_ADMIN] = isAdmin
        }
    }

    suspend fun clearSession() {
        context.dskDataStore.edit { prefs ->
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.USER_EMAIL)
            prefs[Keys.IS_ADMIN] = false
        }
    }

    suspend fun getStoredSession(): StoredSession = sessionFlow.first()

    suspend fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        val current = settingsFlow.first()
        val updated = transform(current)
        context.dskDataStore.edit { prefs ->
            prefs[Keys.AMOLED_DARK] = updated.amoledDarkTheme
            prefs[Keys.PREFERRED_LANGUAGE] = updated.preferredLanguage
            prefs[Keys.AUTO_PLAY_NEXT] = updated.autoPlayNext
            prefs[Keys.DEFAULT_QUALITY] = updated.defaultQuality
            prefs[Keys.SUBTITLES_ENABLED] = updated.subtitlesEnabled
            prefs[Keys.SUBTITLE_SIZE_SP] = updated.subtitleSizeSp
            prefs[Keys.NOTIFICATIONS_ENABLED] = updated.notificationsEnabled
            prefs[Keys.PARENTAL_LOCK_ENABLED] = updated.parentalLockEnabled
        }
    }

    suspend fun setParentalPin(pin: String?) {
        context.dskDataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.PARENTAL_PIN_HASH)
                prefs[Keys.PARENTAL_LOCK_ENABLED] = false
            } else {
                prefs[Keys.PARENTAL_PIN_HASH] = sha256(pin)
                prefs[Keys.PARENTAL_LOCK_ENABLED] = true
            }
        }
    }

    suspend fun verifyParentalPin(pin: String): Boolean {
        val stored = context.dskDataStore.data.first()[Keys.PARENTAL_PIN_HASH].orEmpty()
        return stored.isNotBlank() && stored == sha256(pin)
    }

    suspend fun setProfilePin(pin: String?) {
        context.dskDataStore.edit { prefs ->
            if (pin.isNullOrBlank()) {
                prefs.remove(Keys.PROFILE_PIN_HASH)
            } else {
                prefs[Keys.PROFILE_PIN_HASH] = sha256(pin)
            }
        }
    }

    suspend fun verifyProfilePin(pin: String): Boolean {
        val stored = context.dskDataStore.data.first()[Keys.PROFILE_PIN_HASH].orEmpty()
        return stored.isBlank() || stored == sha256(pin)
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val current = recentSearchesFlow.first().toMutableList()
        current.removeAll { it.equals(clean, ignoreCase = true) }
        current.add(0, clean)
        val trimmed = current.take(10)
        val arr = JSONArray(trimmed)
        context.dskDataStore.edit { prefs ->
            prefs[Keys.RECENT_SEARCHES_JSON] = arr.toString()
        }
    }

    suspend fun clearRecentSearches() {
        context.dskDataStore.edit { prefs ->
            prefs.remove(Keys.RECENT_SEARCHES_JSON)
        }
    }

    fun saveOfflineSnapshot(key: String, json: String) {
        runCatching {
            val cacheDir = File(context.filesDir, "offline_cache").apply { mkdirs() }
            File(cacheDir, "$key.json").writeText(json)
        }
    }

    fun readOfflineSnapshot(key: String): String? {
        return runCatching {
            val file = File(File(context.filesDir, "offline_cache"), "$key.json")
            if (file.exists()) file.readText() else null
        }.getOrNull()
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
