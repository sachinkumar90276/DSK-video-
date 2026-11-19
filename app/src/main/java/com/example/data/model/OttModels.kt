package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    @Json(name = "avatar_url") val avatarUrl: String = "",
    @Json(name = "preferred_language") val preferredLanguage: String = "English",
    @Json(name = "pin_hash") val pinHash: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class UserRole(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    val role: String = "user",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AdminRole(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val email: String = "",
    val role: String = "admin",
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class RegisterAdminRpcRequest(
    @Json(name = "p_full_name") val fullName: String,
    @Json(name = "p_secret_code") val secretCode: String
)

object AdminRegistrationFormValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun validate(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        secretCode: String
    ): String? {
        if (fullName.trim().length < 2) return "Full Name is required (at least 2 characters)."
        if (!EMAIL_REGEX.matches(email.trim())) return "Please enter a valid Email Address."
        if (password.length < 8) return "Password must be at least 8 characters."
        if (password != confirmPassword) return "Password and Confirm Password do not match."
        if (secretCode.trim().length < 6) return "Authorized Admin Registration Secret Code is required."
        return null
    }
}

@JsonClass(generateAdapter = true)
data class MovieItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "banner_url") val bannerUrl: String = "",
    @Json(name = "release_year") val releaseYear: Int = 2026,
    val language: String = "Hindi",
    val genre: String = "Drama",
    val category: String = "Bollywood",
    @Json(name = "duration_minutes") val durationMinutes: Int = 120,
    @Json(name = "imdb_rating") val imdbRating: Double = 8.0,
    @Json(name = "cast_members") val castMembers: String = "",
    val director: String = "",
    @Json(name = "trailer_url") val trailerUrl: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "subtitle_url") val subtitleUrl: String = "",
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "is_trending") val isTrending: Boolean = false,
    @Json(name = "is_popular") val isPopular: Boolean = false,
    @Json(name = "is_latest") val isLatest: Boolean = true,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class WebSeriesItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "banner_url") val bannerUrl: String = "",
    @Json(name = "release_year") val releaseYear: Int = 2026,
    val language: String = "Hindi",
    val genre: String = "Thriller",
    @Json(name = "imdb_rating") val imdbRating: Double = 8.2,
    @Json(name = "cast_members") val castMembers: String = "",
    val director: String = "",
    @Json(name = "trailer_url") val trailerUrl: String = "",
    @Json(name = "is_popular") val isPopular: Boolean = true,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SeasonItem(
    val id: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "season_number") val seasonNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class EpisodeItem(
    val id: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "season_id") val seasonId: String = "",
    @Json(name = "episode_number") val episodeNumber: Int = 1,
    val title: String = "",
    val description: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "duration_minutes") val durationMinutes: Int = 45,
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "subtitle_url") val subtitleUrl: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class LiveChannelItem(
    val id: String = "",
    val name: String = "",
    @Json(name = "logo_url") val logoUrl: String = "",
    val category: String = "Entertainment",
    @Json(name = "stream_url") val streamUrl: String = "",
    val language: String = "Hindi",
    @Json(name = "is_live") val isLive: Boolean = true,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class SportsItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "thumbnail_url") val thumbnailUrl: String = "",
    @Json(name = "banner_url") val bannerUrl: String = "",
    val category: String = "Highlights",
    @Json(name = "sport_type") val sportType: String = "Cricket",
    @Json(name = "duration_minutes") val durationMinutes: Int = 15,
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "subtitle_url") val subtitleUrl: String = "",
    @Json(name = "is_live") val isLive: Boolean = false,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class KidsItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "banner_url") val bannerUrl: String = "",
    val category: String = "Cartoons",
    @Json(name = "age_rating") val ageRating: String = "U",
    val language: String = "English",
    @Json(name = "duration_minutes") val durationMinutes: Int = 20,
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "subtitle_url") val subtitleUrl: String = "",
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class WatchHistoryItem(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "content_id") val contentId: String = "",
    @Json(name = "content_type") val contentType: String = "movie",
    val title: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "watched_at") val watchedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class WatchProgressItem(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "content_id") val contentId: String = "",
    @Json(name = "content_type") val contentType: String = "movie",
    val title: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "subtitle_url") val subtitleUrl: String = "",
    @Json(name = "position_ms") val positionMs: Long = 0L,
    @Json(name = "duration_ms") val durationMs: Long = 0L,
    @Json(name = "episode_id") val episodeId: String = "",
    @Json(name = "season_id") val seasonId: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "updated_at") val updatedAt: String = ""
)

@JsonClass(generateAdapter = true)
data class WatchlistItem(
    val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "content_id") val contentId: String = "",
    @Json(name = "content_type") val contentType: String = "movie",
    val title: String = "",
    @Json(name = "poster_url") val posterUrl: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    @Json(name = "section_type") val sectionType: String = "movie",
    @Json(name = "display_order") val displayOrder: Int = 0
)

@JsonClass(generateAdapter = true)
data class AdminActivityLog(
    val id: String = "",
    @Json(name = "admin_id") val adminId: String = "",
    @Json(name = "admin_email") val adminEmail: String = "",
    val action: String = "",
    @Json(name = "entity_type") val entityType: String = "",
    @Json(name = "entity_id") val entityId: String = "",
    val details: String = "",
    @Json(name = "created_at") val createdAt: String = ""
)

// Auth Models
@JsonClass(generateAdapter = true)
data class AuthCredentials(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class AuthSignUpRequest(
    val email: String,
    val password: String,
    val data: Map<String, String> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    @Json(name = "refresh_token") val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RecoverRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class ResendRequest(
    val type: String = "signup",
    val email: String
)

@JsonClass(generateAdapter = true)
data class UpdateUserPasswordRequest(
    val password: String
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    val id: String = "",
    val email: String? = null,
    @Json(name = "email_confirmed_at") val emailConfirmedAt: String? = null,
    @Json(name = "user_metadata") val userMetadata: Map<String, Any?>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseAuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    val user: SupabaseUser? = null,
    val msg: String? = null,
    val error: String? = null,
    @Json(name = "error_description") val errorDescription: String? = null
)

// Player & Smart URL Import Models
enum class StreamSourceType {
    MP4, HLS, YOUTUBE, AUTHORIZED_STREAM, UNKNOWN
}

data class SmartUrlMetadata(
    val sourceUrl: String,
    val sourceType: StreamSourceType,
    val title: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val language: String = "Hindi",
    val durationMinutes: Int = 0,
    val category: String = "",
    val isPlayable: Boolean = false,
    val statusNote: String = ""
)

data class PlayableMedia(
    val contentId: String,
    val contentType: String, // "movie", "episode", "live", "sports", "kids", "trailer"
    val title: String,
    val subtitle: String = "",
    val videoUrl: String,
    val posterUrl: String = "",
    val subtitleUrl: String = "",
    val resumePositionMs: Long = 0L,
    val isLive: Boolean = false,
    val seriesId: String = "",
    val seasonId: String = "",
    val episodeId: String = "",
    val nextEpisode: EpisodeItem? = null,
    val previousEpisode: EpisodeItem? = null
)

data class AppSettingsState(
    val amoledDarkTheme: Boolean = true,
    val preferredLanguage: String = "English",
    val autoPlayNext: Boolean = true,
    val defaultQuality: String = "Auto",
    val subtitlesEnabled: Boolean = true,
    val subtitleSizeSp: Int = 16,
    val notificationsEnabled: Boolean = true,
    val parentalLockEnabled: Boolean = false,
    val hasParentalPin: Boolean = false,
    val hasProfilePin: Boolean = false
)

data class AdminDashboardStats(
    val totalUsers: Int = 0,
    val totalMovies: Int = 0,
    val totalWebSeries: Int = 0,
    val totalEpisodes: Int = 0,
    val totalLiveChannels: Int = 0,
    val totalSports: Int = 0,
    val totalKids: Int = 0
) {
    val totalVideos: Int get() = totalMovies + totalEpisodes + totalSports + totalKids
}
