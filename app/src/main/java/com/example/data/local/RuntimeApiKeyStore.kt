package com.example.data.local

import com.example.data.model.ApiKeysConfig

/**
 * Thread-safe runtime holder for admin-configured API keys loaded from DataStore.
 */
object RuntimeApiKeyStore {
    @Volatile
    var tmdbApiKey: String = ""

    @Volatile
    var youtubeApiKey: String = ""

    @Volatile
    var firebaseProjectId: String = ""

    @Volatile
    var firebaseApiKey: String = ""

    @Volatile
    var firebaseAppId: String = ""

    @Volatile
    var mediaCdnApiKey: String = ""

    @Volatile
    var mediaCdnHeaderName: String = "X-API-Key"

    fun applyExtraKeys(config: ApiKeysConfig) {
        tmdbApiKey = config.tmdbApiKey.trim()
        youtubeApiKey = config.youtubeApiKey.trim()
        firebaseProjectId = config.firebaseProjectId.trim()
        firebaseApiKey = config.firebaseApiKey.trim()
        firebaseAppId = config.firebaseAppId.trim()
        mediaCdnApiKey = config.mediaCdnApiKey.trim()
        mediaCdnHeaderName = config.mediaCdnHeaderName.trim().ifBlank { "X-API-Key" }
    }
}
