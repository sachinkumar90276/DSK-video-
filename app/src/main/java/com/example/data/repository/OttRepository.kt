package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.model.*
import com.example.data.supabase.SmartUrlImporter
import com.example.data.supabase.SupabaseClientProvider
import com.example.data.supabase.SupabaseConfig
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Response

data class CatalogSnapshot(
    val movies: List<MovieItem> = emptyList(),
    val webSeries: List<WebSeriesItem> = emptyList(),
    val seasons: List<SeasonItem> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
    val liveChannels: List<LiveChannelItem> = emptyList(),
    val sports: List<SportsItem> = emptyList(),
    val kids: List<KidsItem> = emptyList(),
    val categories: List<CategoryItem> = emptyList(),
    val watchProgress: List<WatchProgressItem> = emptyList(),
    val watchHistory: List<WatchHistoryItem> = emptyList(),
    val watchlist: List<WatchlistItem> = emptyList(),
    val isFromCache: Boolean = false
)

class OttRepository(
    private val context: Context,
    val preferences: SessionPreferences
) {
    @Volatile
    private var currentAccessToken: String = ""

    private val clientProvider = SupabaseClientProvider { currentAccessToken }
    private val authApi = clientProvider.authApi
    private val restApi = clientProvider.restApi
    private val urlImporter = SmartUrlImporter()

    val sessionFlow: Flow<StoredSession> = preferences.sessionFlow
    val settingsFlow: Flow<AppSettingsState> = preferences.settingsFlow
    val recentSearchesFlow: Flow<List<String>> = preferences.recentSearchesFlow

    suspend fun restoreSession(): Result<StoredSession?> = withContext(Dispatchers.IO) {
        val stored = preferences.getStoredSession()
        if (!stored.rememberSession || stored.accessToken.isBlank()) {
            return@withContext Result.success(null)
        }
        currentAccessToken = stored.accessToken
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.success(stored)
        }

        // Validate token or refresh if expired
        val userRes = runCatching { authApi.getCurrentUser() }.getOrNull()
        if (userRes != null && userRes.isSuccessful) {
            val isAdmin = checkIsAdmin(stored.userId)
            val updated = stored.copy(isAdmin = isAdmin)
            preferences.saveSession(
                updated.accessToken, updated.refreshToken, updated.userId,
                updated.email, updated.rememberSession, isAdmin
            )
            return@withContext Result.success(updated)
        }

        if (stored.refreshToken.isNotBlank()) {
            val refreshRes = runCatching {
                authApi.refreshToken(RefreshTokenRequest(stored.refreshToken))
            }.getOrNull()
            val body = refreshRes?.body()
            if (refreshRes != null && refreshRes.isSuccessful && body?.accessToken != null) {
                currentAccessToken = body.accessToken
                val uid = body.user?.id ?: stored.userId
                val email = body.user?.email ?: stored.email
                val isAdmin = checkIsAdmin(uid)
                val updated = StoredSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken ?: stored.refreshToken,
                    userId = uid,
                    email = email,
                    rememberSession = true,
                    isAdmin = isAdmin
                )
                preferences.saveSession(
                    updated.accessToken, updated.refreshToken, updated.userId,
                    updated.email, true, isAdmin
                )
                return@withContext Result.success(updated)
            }
        }
        Result.success(stored)
    }

    suspend fun signUp(email: String, password: String, username: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("SUPABASE_ANON_KEY is not configured in AI Studio Secrets."))
        }
        runCatching {
            val res = authApi.signUp(
                AuthSignUpRequest(
                    email = email.trim(),
                    password = password,
                    data = mapOf("username" to username.trim())
                )
            )
            if (!res.isSuccessful) {
                throw IllegalStateException(parseSupabaseError(res))
            }
            val body = res.body()
            val uid = body?.user?.id.orEmpty()
            val token = body?.accessToken
            if (!token.isNullOrBlank() && uid.isNotBlank()) {
                currentAccessToken = token
                preferences.saveSession(token, body.refreshToken.orEmpty(), uid, email.trim(), true, false)
                ensureProfileRow(uid, email.trim(), username.trim())
                "Account created and signed in!"
            } else {
                "Verification email sent to ${email.trim()}. Please verify your email before signing in."
            }
        }
    }

    suspend fun signIn(
        email: String,
        password: String,
        rememberSession: Boolean,
        requireAdmin: Boolean = false
    ): Result<StoredSession> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            return@withContext Result.failure(IllegalStateException("SUPABASE_ANON_KEY is missing. Add it in the AI Studio Secrets panel."))
        }
        runCatching {
            val res = authApi.signInWithPassword(AuthCredentials(email.trim(), password))
            if (!res.isSuccessful) {
                throw IllegalStateException(parseSupabaseError(res))
            }
            val body = res.body() ?: throw IllegalStateException("Empty auth response.")
            val token = body.accessToken ?: throw IllegalStateException("Missing access token.")
            val user = body.user ?: throw IllegalStateException("Missing user info.")
            currentAccessToken = token

            val isAdmin = checkIsAdmin(user.id)
            if (requireAdmin && !isAdmin) {
                currentAccessToken = ""
                throw IllegalStateException("Access denied: This account does not have an 'admin' role in user_roles.")
            }

            ensureProfileRow(
                userId = user.id,
                email = user.email ?: email.trim(),
                username = (user.userMetadata?.get("username") as? String) ?: email.substringBefore("@")
            )

            val session = StoredSession(
                accessToken = token,
                refreshToken = body.refreshToken.orEmpty(),
                userId = user.id,
                email = user.email ?: email.trim(),
                rememberSession = rememberSession,
                isAdmin = isAdmin
            )
            preferences.saveSession(
                session.accessToken, session.refreshToken, session.userId,
                session.email, rememberSession, isAdmin
            )
            session
        }
    }

    suspend fun resendVerification(email: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val res = authApi.resendVerification(ResendRequest(email = email.trim()))
            if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
            "Verification email resent to ${email.trim()}."
        }
    }

    suspend fun sendPasswordReset(email: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val res = authApi.recoverPassword(RecoverRequest(email = email.trim()))
            if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
            "Password reset instructions sent to ${email.trim()}."
        }
    }

    suspend fun updatePassword(newPassword: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val res = authApi.updateUserPassword(UpdateUserPasswordRequest(newPassword))
            if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
            "Password updated successfully."
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching { authApi.logout() }
        currentAccessToken = ""
        preferences.clearSession()
    }

    private suspend fun checkIsAdmin(userId: String): Boolean {
        if (userId.isBlank()) return false
        return runCatching {
            val res = restApi.getUserRoles("eq.$userId")
            res.isSuccessful && (res.body()?.any { it.role.equals("admin", ignoreCase = true) } == true)
        }.getOrDefault(false)
    }

    private suspend fun ensureProfileRow(userId: String, email: String, username: String) {
        runCatching {
            val existing = restApi.getProfile("eq.$userId")
            if (existing.isSuccessful && existing.body().isNullOrEmpty()) {
                restApi.upsertProfile(
                    mapOf(
                        "id" to userId,
                        "email" to email,
                        "username" to username
                    )
                )
            }
        }
    }

    suspend fun fetchProfile(userId: String): UserProfile? = withContext(Dispatchers.IO) {
        runCatching {
            val res = restApi.getProfile("eq.$userId")
            res.body()?.firstOrNull()
        }.getOrNull()
    }

    suspend fun updateProfile(
        userId: String,
        email: String,
        username: String,
        preferredLanguage: String,
        avatarUrl: String? = null
    ): Result<UserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = mutableMapOf<String, Any>(
                "id" to userId,
                "email" to email,
                "username" to username.trim(),
                "preferred_language" to preferredLanguage
            )
            if (avatarUrl != null) payload["avatar_url"] = avatarUrl
            val res = restApi.upsertProfile(payload)
            if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
            res.body()?.firstOrNull() ?: UserProfile(id = userId, email = email, username = username)
        }
    }

    suspend fun uploadProfileAvatar(userId: String, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Unable to read selected image.")
            val mime = context.contentResolver.getType(imageUri) ?: "image/jpeg"
            val ext = if (mime.contains("png")) "png" else "jpg"
            val fileName = "${userId}_avatar.$ext"
            val body = bytes.toRequestBody(mime.toMediaTypeOrNull())
            val uploadRes = restApi.uploadAvatar(fileName = fileName, fileBody = body)
            if (!uploadRes.isSuccessful) {
                throw IllegalStateException("Storage upload failed (${uploadRes.code()}). Ensure 'avatars' bucket exists in Supabase Storage.")
            }
            "${SupabaseConfig.baseUrl}/storage/v1/object/public/avatars/$fileName?t=${System.currentTimeMillis()}"
        }
    }

    suspend fun loadCatalog(userId: String): Result<CatalogSnapshot> = withContext(Dispatchers.IO) {
        if (!SupabaseConfig.isConfigured) {
            val cached = readCachedCatalog()
            return@withContext Result.success(
                cached ?: CatalogSnapshot(isFromCache = true)
            )
        }

        return@withContext runCatching {
            val moviesRes = restApi.getMovies()
            val seriesRes = restApi.getWebSeries()
            val seasonsRes = restApi.getSeasons()
            val episodesRes = restApi.getEpisodes()
            val liveRes = restApi.getLiveChannels()
            val sportsRes = restApi.getSportsContent()
            val kidsRes = restApi.getKidsContent()
            val categoriesRes = restApi.getCategories()

            val progress = if (userId.isNotBlank()) restApi.getWatchProgress("eq.$userId").body().orEmpty() else emptyList()
            val history = if (userId.isNotBlank()) restApi.getWatchHistory("eq.$userId").body().orEmpty() else emptyList()
            val watchlist = if (userId.isNotBlank()) restApi.getWatchlist("eq.$userId").body().orEmpty() else emptyList()

            val snapshot = CatalogSnapshot(
                movies = moviesRes.body().orEmpty(),
                webSeries = seriesRes.body().orEmpty(),
                seasons = seasonsRes.body().orEmpty(),
                episodes = episodesRes.body().orEmpty(),
                liveChannels = liveRes.body().orEmpty(),
                sports = sportsRes.body().orEmpty(),
                kids = kidsRes.body().orEmpty(),
                categories = categoriesRes.body().orEmpty(),
                watchProgress = progress,
                watchHistory = history,
                watchlist = watchlist,
                isFromCache = false
            )
            cacheCatalog(snapshot)
            snapshot
        }.recoverCatching { err ->
            readCachedCatalog() ?: throw err
        }
    }

    suspend fun toggleWatchlist(
        userId: String,
        contentId: String,
        contentType: String,
        title: String,
        posterUrl: String,
        currentlyInList: Boolean
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            if (currentlyInList) {
                val res = restApi.removeFromWatchlist("eq.$userId", "eq.$contentId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                false
            } else {
                val res = restApi.addToWatchlist(
                    mapOf(
                        "user_id" to userId,
                        "content_id" to contentId,
                        "content_type" to contentType,
                        "title" to title,
                        "poster_url" to posterUrl
                    )
                )
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                true
            }
        }
    }

    suspend fun recordWatchProgress(
        userId: String,
        media: PlayableMedia,
        positionMs: Long,
        durationMs: Long
    ) = withContext(Dispatchers.IO) {
        if (userId.isBlank() || media.isLive) return@withContext
        runCatching {
            restApi.upsertWatchProgress(
                mapOf(
                    "user_id" to userId,
                    "content_id" to media.contentId,
                    "content_type" to media.contentType,
                    "title" to media.title,
                    "poster_url" to media.posterUrl,
                    "video_url" to media.videoUrl,
                    "subtitle_url" to media.subtitleUrl,
                    "position_ms" to positionMs,
                    "duration_ms" to durationMs,
                    "episode_id" to media.episodeId,
                    "season_id" to media.seasonId,
                    "series_id" to media.seriesId
                )
            )
        }
    }

    suspend fun recordWatchHistory(userId: String, media: PlayableMedia) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        runCatching {
            restApi.upsertWatchHistory(
                mapOf(
                    "user_id" to userId,
                    "content_id" to media.contentId,
                    "content_type" to media.contentType,
                    "title" to media.title,
                    "poster_url" to media.posterUrl,
                    "video_url" to media.videoUrl
                )
            )
        }
    }

    // Smart URL Import
    suspend fun inspectSmartUrl(url: String): SmartUrlMetadata = urlImporter.inspectUrl(url)

    // Admin Analytics & Logs
    suspend fun loadAdminOverview(catalog: CatalogSnapshot): Pair<AdminDashboardStats, List<AdminActivityLog>> =
        withContext(Dispatchers.IO) {
            val profilesCount = runCatching { restApi.getAllProfiles().body()?.size ?: 0 }.getOrDefault(0)
            val logs = runCatching { restApi.getAdminLogs().body().orEmpty() }.getOrDefault(emptyList())
            val stats = AdminDashboardStats(
                totalUsers = profilesCount,
                totalMovies = catalog.movies.size,
                totalWebSeries = catalog.webSeries.size,
                totalEpisodes = catalog.episodes.size,
                totalLiveChannels = catalog.liveChannels.size,
                totalSports = catalog.sports.size,
                totalKids = catalog.kids.size
            )
            stats to logs
        }

    // Admin CRUD
    suspend fun saveMovie(adminId: String, adminEmail: String, movie: MovieItem): Result<MovieItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "title" to movie.title,
                    "description" to movie.description,
                    "poster_url" to movie.posterUrl,
                    "banner_url" to movie.bannerUrl,
                    "release_year" to movie.releaseYear,
                    "language" to movie.language,
                    "genre" to movie.genre,
                    "category" to movie.category,
                    "duration_minutes" to movie.durationMinutes,
                    "imdb_rating" to movie.imdbRating,
                    "cast_members" to movie.castMembers,
                    "director" to movie.director,
                    "trailer_url" to movie.trailerUrl,
                    "video_url" to movie.videoUrl,
                    "subtitle_url" to movie.subtitleUrl,
                    "is_featured" to movie.isFeatured,
                    "is_trending" to movie.isTrending,
                    "is_popular" to movie.isPopular,
                    "is_latest" to movie.isLatest,
                    "is_published" to movie.isPublished
                )
                val res = if (movie.id.isBlank()) {
                    restApi.createMovie(payload)
                } else {
                    restApi.updateMovie("eq.${movie.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: movie
                logAdminAction(adminId, adminEmail, if (movie.id.isBlank()) "CREATE" else "UPDATE", "movie", saved.id, saved.title)
                saved
            }
        }

    suspend fun toggleMoviePublish(adminId: String, adminEmail: String, movie: MovieItem): Result<MovieItem> =
        saveMovie(adminId, adminEmail, movie.copy(isPublished = !movie.isPublished))

    suspend fun deleteMovie(adminId: String, adminEmail: String, movieId: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteMovie("eq.$movieId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "movie", movieId, title)
            }
        }

    suspend fun saveWebSeries(adminId: String, adminEmail: String, series: WebSeriesItem): Result<WebSeriesItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "title" to series.title,
                    "description" to series.description,
                    "poster_url" to series.posterUrl,
                    "banner_url" to series.bannerUrl,
                    "release_year" to series.releaseYear,
                    "language" to series.language,
                    "genre" to series.genre,
                    "imdb_rating" to series.imdbRating,
                    "cast_members" to series.castMembers,
                    "director" to series.director,
                    "trailer_url" to series.trailerUrl,
                    "is_popular" to series.isPopular,
                    "is_published" to series.isPublished
                )
                val res = if (series.id.isBlank()) {
                    restApi.createWebSeries(payload)
                } else {
                    restApi.updateWebSeries("eq.${series.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: series
                logAdminAction(adminId, adminEmail, if (series.id.isBlank()) "CREATE" else "UPDATE", "web_series", saved.id, saved.title)
                saved
            }
        }

    suspend fun deleteWebSeries(adminId: String, adminEmail: String, seriesId: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteWebSeries("eq.$seriesId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "web_series", seriesId, title)
            }
        }

    suspend fun createSeason(adminId: String, adminEmail: String, seriesId: String, seasonNumber: Int, title: String): Result<SeasonItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.createSeason(
                    mapOf(
                        "series_id" to seriesId,
                        "season_number" to seasonNumber,
                        "title" to title.ifBlank { "Season $seasonNumber" }
                    )
                )
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: throw IllegalStateException("Failed to create season")
                logAdminAction(adminId, adminEmail, "CREATE", "season", saved.id, saved.title)
                saved
            }
        }

    suspend fun deleteSeason(adminId: String, adminEmail: String, seasonId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteSeason("eq.$seasonId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "season", seasonId, "Season deleted")
            }
        }

    suspend fun saveEpisode(adminId: String, adminEmail: String, episode: EpisodeItem): Result<EpisodeItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "series_id" to episode.seriesId,
                    "season_id" to episode.seasonId,
                    "episode_number" to episode.episodeNumber,
                    "title" to episode.title,
                    "description" to episode.description,
                    "thumbnail_url" to episode.thumbnailUrl,
                    "duration_minutes" to episode.durationMinutes,
                    "video_url" to episode.videoUrl,
                    "subtitle_url" to episode.subtitleUrl
                )
                val res = if (episode.id.isBlank()) {
                    restApi.createEpisode(payload)
                } else {
                    restApi.updateEpisode("eq.${episode.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: episode
                logAdminAction(adminId, adminEmail, if (episode.id.isBlank()) "CREATE" else "UPDATE", "episode", saved.id, saved.title)
                saved
            }
        }

    suspend fun deleteEpisode(adminId: String, adminEmail: String, episodeId: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteEpisode("eq.$episodeId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "episode", episodeId, title)
            }
        }

    suspend fun saveLiveChannel(adminId: String, adminEmail: String, channel: LiveChannelItem): Result<LiveChannelItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "name" to channel.name,
                    "logo_url" to channel.logoUrl,
                    "category" to channel.category,
                    "stream_url" to channel.streamUrl,
                    "language" to channel.language,
                    "is_live" to channel.isLive,
                    "is_published" to channel.isPublished
                )
                val res = if (channel.id.isBlank()) {
                    restApi.createLiveChannel(payload)
                } else {
                    restApi.updateLiveChannel("eq.${channel.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: channel
                logAdminAction(adminId, adminEmail, if (channel.id.isBlank()) "CREATE" else "UPDATE", "live_channel", saved.id, saved.name)
                saved
            }
        }

    suspend fun deleteLiveChannel(adminId: String, adminEmail: String, channelId: String, name: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteLiveChannel("eq.$channelId")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "live_channel", channelId, name)
            }
        }

    suspend fun saveSportsItem(adminId: String, adminEmail: String, item: SportsItem): Result<SportsItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "title" to item.title,
                    "description" to item.description,
                    "thumbnail_url" to item.thumbnailUrl,
                    "banner_url" to item.bannerUrl,
                    "category" to item.category,
                    "sport_type" to item.sportType,
                    "duration_minutes" to item.durationMinutes,
                    "video_url" to item.videoUrl,
                    "subtitle_url" to item.subtitleUrl,
                    "is_live" to item.isLive,
                    "is_published" to item.isPublished
                )
                val res = if (item.id.isBlank()) {
                    restApi.createSportsContent(payload)
                } else {
                    restApi.updateSportsContent("eq.${item.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: item
                logAdminAction(adminId, adminEmail, if (item.id.isBlank()) "CREATE" else "UPDATE", "sports", saved.id, saved.title)
                saved
            }
        }

    suspend fun deleteSportsItem(adminId: String, adminEmail: String, id: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteSportsContent("eq.$id")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "sports", id, title)
            }
        }

    suspend fun saveKidsItem(adminId: String, adminEmail: String, item: KidsItem): Result<KidsItem> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = mapOf(
                    "title" to item.title,
                    "description" to item.description,
                    "poster_url" to item.posterUrl,
                    "banner_url" to item.bannerUrl,
                    "category" to item.category,
                    "age_rating" to item.ageRating,
                    "language" to item.language,
                    "duration_minutes" to item.durationMinutes,
                    "video_url" to item.videoUrl,
                    "subtitle_url" to item.subtitleUrl,
                    "is_published" to item.isPublished
                )
                val res = if (item.id.isBlank()) {
                    restApi.createKidsContent(payload)
                } else {
                    restApi.updateKidsContent("eq.${item.id}", payload)
                }
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                val saved = res.body()?.firstOrNull() ?: item
                logAdminAction(adminId, adminEmail, if (item.id.isBlank()) "CREATE" else "UPDATE", "kids", saved.id, saved.title)
                saved
            }
        }

    suspend fun deleteKidsItem(adminId: String, adminEmail: String, id: String, title: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val res = restApi.deleteKidsContent("eq.$id")
                if (!res.isSuccessful) throw IllegalStateException(parseSupabaseError(res))
                logAdminAction(adminId, adminEmail, "DELETE", "kids", id, title)
            }
        }

    private suspend fun logAdminAction(
        adminId: String,
        adminEmail: String,
        action: String,
        entityType: String,
        entityId: String,
        details: String
    ) {
        if (adminId.isBlank()) return
        runCatching {
            restApi.createAdminLog(
                mapOf(
                    "admin_id" to adminId,
                    "admin_email" to adminEmail,
                    "action" to action,
                    "entity_type" to entityType,
                    "entity_id" to entityId,
                    "details" to details
                )
            )
        }
    }

    private fun cacheCatalog(snapshot: CatalogSnapshot) {
        val moshi = clientProvider.moshi
        runCatching {
            preferences.saveOfflineSnapshot(
                "movies",
                moshi.adapter<List<MovieItem>>(Types.newParameterizedType(List::class.java, MovieItem::class.java)).toJson(snapshot.movies)
            )
            preferences.saveOfflineSnapshot(
                "web_series",
                moshi.adapter<List<WebSeriesItem>>(Types.newParameterizedType(List::class.java, WebSeriesItem::class.java)).toJson(snapshot.webSeries)
            )
            preferences.saveOfflineSnapshot(
                "live_channels",
                moshi.adapter<List<LiveChannelItem>>(Types.newParameterizedType(List::class.java, LiveChannelItem::class.java)).toJson(snapshot.liveChannels)
            )
            preferences.saveOfflineSnapshot(
                "sports",
                moshi.adapter<List<SportsItem>>(Types.newParameterizedType(List::class.java, SportsItem::class.java)).toJson(snapshot.sports)
            )
            preferences.saveOfflineSnapshot(
                "kids",
                moshi.adapter<List<KidsItem>>(Types.newParameterizedType(List::class.java, KidsItem::class.java)).toJson(snapshot.kids)
            )
        }
    }

    private fun readCachedCatalog(): CatalogSnapshot? {
        val moshi = clientProvider.moshi
        return runCatching {
            val moviesJson = preferences.readOfflineSnapshot("movies") ?: return null
            val movies = moshi.adapter<List<MovieItem>>(Types.newParameterizedType(List::class.java, MovieItem::class.java)).fromJson(moviesJson).orEmpty()
            val series = preferences.readOfflineSnapshot("web_series")?.let {
                moshi.adapter<List<WebSeriesItem>>(Types.newParameterizedType(List::class.java, WebSeriesItem::class.java)).fromJson(it)
            }.orEmpty()
            val channels = preferences.readOfflineSnapshot("live_channels")?.let {
                moshi.adapter<List<LiveChannelItem>>(Types.newParameterizedType(List::class.java, LiveChannelItem::class.java)).fromJson(it)
            }.orEmpty()
            val sports = preferences.readOfflineSnapshot("sports")?.let {
                moshi.adapter<List<SportsItem>>(Types.newParameterizedType(List::class.java, SportsItem::class.java)).fromJson(it)
            }.orEmpty()
            val kids = preferences.readOfflineSnapshot("kids")?.let {
                moshi.adapter<List<KidsItem>>(Types.newParameterizedType(List::class.java, KidsItem::class.java)).fromJson(it)
            }.orEmpty()
            CatalogSnapshot(
                movies = movies,
                webSeries = series,
                liveChannels = channels,
                sports = sports,
                kids = kids,
                isFromCache = true
            )
        }.getOrNull()
    }

    private fun parseSupabaseError(res: Response<*>): String {
        val raw = runCatching { res.errorBody()?.string() }.getOrNull().orEmpty()
        return runCatching {
            val json = JSONObject(raw)
            json.optString("error_description")
                .ifBlank { json.optString("msg") }
                .ifBlank { json.optString("message") }
                .ifBlank { "Request failed (${res.code()})" }
        }.getOrDefault("Request failed (${res.code()})")
    }
}
