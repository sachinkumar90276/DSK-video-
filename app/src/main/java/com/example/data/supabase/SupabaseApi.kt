package com.example.data.supabase

import com.example.BuildConfig
import com.example.data.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

object SupabaseConfig {
    val baseUrl: String
        get() = BuildConfig.SUPABASE_URL
            .takeIf { it.isNotBlank() && it.startsWith("https://") }
            ?.trimEnd('/')
            ?: "https://ottlzprymsdfgbgsrmxn.supabase.co"

    val anonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean
        get() = anonKey.isNotBlank() &&
            anonKey != "YOUR_SUPABASE_ANON_KEY" &&
            anonKey != "MY_SUPABASE_ANON_KEY"
}

interface SupabaseAuthApi {
    @POST("auth/v1/signup")
    suspend fun signUp(@Body request: AuthSignUpRequest): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signInWithPassword(@Body request: AuthCredentials): Response<SupabaseAuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<SupabaseAuthResponse>

    @POST("auth/v1/recover")
    suspend fun recoverPassword(@Body request: RecoverRequest): Response<ResponseBody>

    @POST("auth/v1/resend")
    suspend fun resendVerification(@Body request: ResendRequest): Response<ResponseBody>

    @GET("auth/v1/user")
    suspend fun getCurrentUser(): Response<SupabaseUser>

    @PUT("auth/v1/user")
    suspend fun updateUserPassword(@Body request: UpdateUserPasswordRequest): Response<SupabaseUser>

    @POST("auth/v1/logout")
    suspend fun logout(): Response<ResponseBody>
}

interface SupabaseRestApi {
    // 1. Profiles
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idEq: String,
        @Query("select") select: String = "*"
    ): Response<List<UserProfile>>

    @GET("rest/v1/profiles")
    suspend fun getAllProfiles(
        @Query("select") select: String = "id,email,username,avatar_url,preferred_language,created_at"
    ): Response<List<UserProfile>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/profiles")
    suspend fun upsertProfile(@Body profile: Map<String, @JvmSuppressWildcards Any>): Response<List<UserProfile>>

    // 2. User Roles & Admin Roles
    @GET("rest/v1/user_roles")
    suspend fun getUserRoles(
        @Query("user_id") userIdEq: String,
        @Query("select") select: String = "*"
    ): Response<List<UserRole>>

    @GET("rest/v1/admin_roles")
    suspend fun getAdminRoles(
        @Query("user_id") userIdEq: String,
        @Query("select") select: String = "*"
    ): Response<List<AdminRole>>

    @POST("rest/v1/rpc/register_admin_with_secret")
    suspend fun registerAdminWithSecret(
        @Body request: RegisterAdminRpcRequest
    ): Response<Boolean>

    // 3. Movies
    @GET("rest/v1/movies")
    suspend fun getMovies(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<MovieItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/movies")
    suspend fun createMovie(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<MovieItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/movies")
    suspend fun updateMovie(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<MovieItem>>

    @DELETE("rest/v1/movies")
    suspend fun deleteMovie(@Query("id") idEq: String): Response<ResponseBody>

    // 4. Web Series
    @GET("rest/v1/web_series")
    suspend fun getWebSeries(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<WebSeriesItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/web_series")
    suspend fun createWebSeries(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<WebSeriesItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/web_series")
    suspend fun updateWebSeries(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<WebSeriesItem>>

    @DELETE("rest/v1/web_series")
    suspend fun deleteWebSeries(@Query("id") idEq: String): Response<ResponseBody>

    // 5. Seasons
    @GET("rest/v1/seasons")
    suspend fun getSeasons(
        @Query("select") select: String = "*",
        @Query("order") order: String = "season_number.asc"
    ): Response<List<SeasonItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/seasons")
    suspend fun createSeason(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<SeasonItem>>

    @DELETE("rest/v1/seasons")
    suspend fun deleteSeason(@Query("id") idEq: String): Response<ResponseBody>

    // 6. Episodes
    @GET("rest/v1/episodes")
    suspend fun getEpisodes(
        @Query("select") select: String = "*",
        @Query("order") order: String = "episode_number.asc"
    ): Response<List<EpisodeItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/episodes")
    suspend fun createEpisode(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<EpisodeItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/episodes")
    suspend fun updateEpisode(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<EpisodeItem>>

    @DELETE("rest/v1/episodes")
    suspend fun deleteEpisode(@Query("id") idEq: String): Response<ResponseBody>

    // 7. Live Channels
    @GET("rest/v1/live_channels")
    suspend fun getLiveChannels(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<LiveChannelItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/live_channels")
    suspend fun createLiveChannel(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<LiveChannelItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/live_channels")
    suspend fun updateLiveChannel(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<LiveChannelItem>>

    @DELETE("rest/v1/live_channels")
    suspend fun deleteLiveChannel(@Query("id") idEq: String): Response<ResponseBody>

    // 8. Sports Content
    @GET("rest/v1/sports_content")
    suspend fun getSportsContent(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SportsItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/sports_content")
    suspend fun createSportsContent(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<SportsItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/sports_content")
    suspend fun updateSportsContent(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<SportsItem>>

    @DELETE("rest/v1/sports_content")
    suspend fun deleteSportsContent(@Query("id") idEq: String): Response<ResponseBody>

    // 9. Kids Content
    @GET("rest/v1/kids_content")
    suspend fun getKidsContent(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc"
    ): Response<List<KidsItem>>

    @Headers("Prefer: return=representation")
    @POST("rest/v1/kids_content")
    suspend fun createKidsContent(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<KidsItem>>

    @Headers("Prefer: return=representation")
    @PATCH("rest/v1/kids_content")
    suspend fun updateKidsContent(
        @Query("id") idEq: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<List<KidsItem>>

    @DELETE("rest/v1/kids_content")
    suspend fun deleteKidsContent(@Query("id") idEq: String): Response<ResponseBody>

    // 10. Watch History
    @GET("rest/v1/watch_history")
    suspend fun getWatchHistory(
        @Query("user_id") userIdEq: String,
        @Query("order") order: String = "watched_at.desc"
    ): Response<List<WatchHistoryItem>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/watch_history")
    suspend fun upsertWatchHistory(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<WatchHistoryItem>>

    // 11. Watch Progress
    @GET("rest/v1/watch_progress")
    suspend fun getWatchProgress(
        @Query("user_id") userIdEq: String,
        @Query("order") order: String = "updated_at.desc"
    ): Response<List<WatchProgressItem>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/watch_progress")
    suspend fun upsertWatchProgress(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<WatchProgressItem>>

    // 12. Watchlist
    @GET("rest/v1/watchlist")
    suspend fun getWatchlist(
        @Query("user_id") userIdEq: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<WatchlistItem>>

    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    @POST("rest/v1/watchlist")
    suspend fun addToWatchlist(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<List<WatchlistItem>>

    @DELETE("rest/v1/watchlist")
    suspend fun removeFromWatchlist(
        @Query("user_id") userIdEq: String,
        @Query("content_id") contentIdEq: String
    ): Response<ResponseBody>

    // 13. Categories
    @GET("rest/v1/categories")
    suspend fun getCategories(
        @Query("select") select: String = "*",
        @Query("order") order: String = "display_order.asc"
    ): Response<List<CategoryItem>>

    // 14. Admin Activity Logs
    @GET("rest/v1/admin_activity_logs")
    suspend fun getAdminLogs(
        @Query("select") select: String = "*",
        @Query("order") order: String = "created_at.desc",
        @Query("limit") limit: Int = 50
    ): Response<List<AdminActivityLog>>

    @POST("rest/v1/admin_activity_logs")
    suspend fun createAdminLog(@Body body: Map<String, @JvmSuppressWildcards Any>): Response<ResponseBody>

    // Storage Avatar Upload
    @POST("storage/v1/object/avatars/{fileName}")
    suspend fun uploadAvatar(
        @Path("fileName") fileName: String,
        @Header("x-upsert") upsert: String = "true",
        @Body fileBody: RequestBody
    ): Response<ResponseBody>
}

class SupabaseClientProvider(
    private val tokenProvider: () -> String?
) {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val authHeaderInterceptor = Interceptor { chain ->
        val original = chain.request()
        val key = SupabaseConfig.anonKey
        val jwt = tokenProvider()?.takeIf { it.isNotBlank() } ?: key

        val builder = original.newBuilder()
            .header("apikey", key)
            .header("Content-Type", original.header("Content-Type") ?: "application/json")
        if (jwt.isNotBlank()) {
            builder.header("Authorization", "Bearer $jwt")
        }
        chain.proceed(builder.build())
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authHeaderInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("${SupabaseConfig.baseUrl}/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val authApi: SupabaseAuthApi = retrofit.create(SupabaseAuthApi::class.java)
    val restApi: SupabaseRestApi = retrofit.create(SupabaseRestApi::class.java)
}
