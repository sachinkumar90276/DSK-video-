package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ApiKeysConfig
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
        val SUPABASE_URL = stringPreferencesKey("runtime_supabase_url")
        val SUPABASE_ANON_KEY = stringPreferencesKey("runtime_supabase_anon_key")
        val TMDB_API_KEY = stringPreferencesKey("runtime_tmdb_api_key")
        val YOUTUBE_API_KEY = stringPreferencesKey("runtime_youtube_api_key")
        val FIREBASE_PROJECT_ID = stringPreferencesKey("runtime_firebase_project_id")
        val FIREBASE_API_KEY = stringPreferencesKey("runtime_firebase_api_key")
        val FIREBASE_APP_ID = stringPreferencesKey("runtime_firebase_app_id")
        val MEDIA_CDN_API_KEY = stringPreferencesKey("runtime_media_cdn_api_key")
        val MEDIA_CDN_HEADER_NAME = stringPreferencesKey("runtime_media_cdn_header_name")
        val OWNER_ADMIN_PASSWORD_HASH = stringPreferencesKey("owner_admin_password_hash")
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

    suspend fun saveSupabaseCredentials(url: String, anonKey: String) {
        context.dskDataStore.edit { prefs ->
            prefs[Keys.SUPABASE_URL] = url.trim().trimEnd('/')
            prefs[Keys.SUPABASE_ANON_KEY] = anonKey.trim()
        }
    }

    suspend fun getSupabaseCredentials(): Pair<String, String> {
        val prefs = context.dskDataStore.data.first()
        return prefs[Keys.SUPABASE_URL].orEmpty() to prefs[Keys.SUPABASE_ANON_KEY].orEmpty()
    }

    suspend fun saveExtraApiKeys(config: ApiKeysConfig) {
        context.dskDataStore.edit { prefs ->
            prefs[Keys.TMDB_API_KEY] = config.tmdbApiKey.trim()
            prefs[Keys.YOUTUBE_API_KEY] = config.youtubeApiKey.trim()
            prefs[Keys.FIREBASE_PROJECT_ID] = config.firebaseProjectId.trim()
            prefs[Keys.FIREBASE_API_KEY] = config.firebaseApiKey.trim()
            prefs[Keys.FIREBASE_APP_ID] = config.firebaseAppId.trim()
            prefs[Keys.MEDIA_CDN_API_KEY] = config.mediaCdnApiKey.trim()
            prefs[Keys.MEDIA_CDN_HEADER_NAME] = config.mediaCdnHeaderName.trim().ifBlank { "X-API-Key" }
        }
    }

    suspend fun getExtraApiKeys(): ApiKeysConfig {
        val prefs = context.dskDataStore.data.first()
        return ApiKeysConfig(
            supabaseUrl = prefs[Keys.SUPABASE_URL].orEmpty(),
            supabaseAnonKey = prefs[Keys.SUPABASE_ANON_KEY].orEmpty(),
            tmdbApiKey = prefs[Keys.TMDB_API_KEY].orEmpty(),
            youtubeApiKey = prefs[Keys.YOUTUBE_API_KEY].orEmpty(),
            firebaseProjectId = prefs[Keys.FIREBASE_PROJECT_ID].orEmpty(),
            firebaseApiKey = prefs[Keys.FIREBASE_API_KEY].orEmpty(),
            firebaseAppId = prefs[Keys.FIREBASE_APP_ID].orEmpty(),
            mediaCdnApiKey = prefs[Keys.MEDIA_CDN_API_KEY].orEmpty(),
            mediaCdnHeaderName = prefs[Keys.MEDIA_CDN_HEADER_NAME]?.ifBlank { "X-API-Key" } ?: "X-API-Key"
        )
    }

    suspend fun verifyOwnerAdminPassword(email: String, password: String): Boolean {
        if (!email.trim().equals(OWNER_ADMIN_EMAIL, ignoreCase = true)) return false
        val prefs = context.dskDataStore.data.first()
        val storedHash = prefs[Keys.OWNER_ADMIN_PASSWORD_HASH].orEmpty()
        val expectedHash = storedHash.ifBlank { defaultOwnerPasswordHash() }
        return sha256(OWNER_PASSWORD_SALT + password) == expectedHash
    }

    suspend fun updateOwnerAdminPassword(currentPassword: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters."))
        }
        val prefs = context.dskDataStore.data.first()
        val storedHash = prefs[Keys.OWNER_ADMIN_PASSWORD_HASH].orEmpty()
        val expectedHash = storedHash.ifBlank { defaultOwnerPasswordHash() }
        if (sha256(OWNER_PASSWORD_SALT + currentPassword) != expectedHash) {
            return Result.failure(IllegalArgumentException("Current Admin password is incorrect."))
        }
        context.dskDataStore.edit { editPrefs ->
            editPrefs[Keys.OWNER_ADMIN_PASSWORD_HASH] = sha256(OWNER_PASSWORD_SALT + newPassword)
        }
        return Result.success(Unit)
    }

    suspend fun forceSyncOwnerAdminPassword(newPassword: String) {
        if (newPassword.length < 6) return
        context.dskDataStore.edit { editPrefs ->
            editPrefs[Keys.OWNER_ADMIN_PASSWORD_HASH] = sha256(OWNER_PASSWORD_SALT + newPassword)
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
        const val OWNER_ADMIN_EMAIL = "s7165349@gmail.com"
        private const val OWNER_PASSWORD_SALT = "dsk_video_owner_v1:"
        private val INITIAL_OWNER_PASS_CHARS = charArrayOf('S', 'a', 'c', 'h', 'i', 'n', '@', '2', '0', '0', '3')

        private fun defaultOwnerPasswordHash(): String =
            sha256(OWNER_PASSWORD_SALT + String(INITIAL_OWNER_PASS_CHARS))

        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
