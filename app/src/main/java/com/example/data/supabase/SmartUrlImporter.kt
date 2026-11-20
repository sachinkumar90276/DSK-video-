package com.example.data.supabase

import android.net.Uri
import com.example.data.local.RuntimeApiKeyStore
import com.example.data.model.SmartUrlMetadata
import com.example.data.model.StreamSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Smart URL Import & API Key Diagnostic Engine.
 * Inspects only public/authorized metadata APIs and configured admin API keys.
 */
class SmartUrlImporter {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun detectSourceType(url: String): StreamSourceType {
        val lower = url.trim().lowercase()
        return when {
            lower.contains("youtube.com/") || lower.contains("youtu.be/") -> StreamSourceType.YOUTUBE
            lower.contains(".m3u8") -> StreamSourceType.HLS
            lower.contains(".mp4") -> StreamSourceType.MP4
            lower.startsWith("https://") || lower.startsWith("http://") -> StreamSourceType.AUTHORIZED_STREAM
            else -> StreamSourceType.UNKNOWN
        }
    }

    suspend fun inspectUrl(rawUrl: String): SmartUrlMetadata = withContext(Dispatchers.IO) {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) {
            return@withContext SmartUrlMetadata(
                sourceUrl = trimmed,
                sourceType = StreamSourceType.UNKNOWN,
                isPlayable = false,
                statusNote = "Enter a valid HTTP/HTTPS stream or embed URL."
            )
        }

        val type = detectSourceType(trimmed)
        if (type == StreamSourceType.YOUTUBE) {
            val ytKey = RuntimeApiKeyStore.youtubeApiKey
            if (ytKey.isNotBlank()) {
                val apiMeta = fetchYoutubeDataApi(trimmed, ytKey)
                if (apiMeta != null) return@withContext apiMeta
            }
            return@withContext fetchYoutubeOEmbed(trimmed)
        }

        runCatching {
            val headBuilder = Request.Builder().url(trimmed).head()
            applyCdnHeaderIfConfigured(headBuilder)
            val headRes = client.newCall(headBuilder.build()).execute()
            val code = headRes.code
            val contentType = headRes.header("Content-Type").orEmpty().lowercase()
            headRes.close()

            if (code == 401 || code == 403) {
                return@withContext SmartUrlMetadata(
                    sourceUrl = trimmed,
                    sourceType = type,
                    title = inferTitleFromUrl(trimmed),
                    isPlayable = false,
                    statusNote = "Access restricted (HTTP $code). Configure Media CDN API Key in Admin Panel if required."
                )
            }

            if (code >= 400 && code != 405) {
                return@withContext SmartUrlMetadata(
                    sourceUrl = trimmed,
                    sourceType = type,
                    title = inferTitleFromUrl(trimmed),
                    isPlayable = false,
                    statusNote = "URL returned HTTP $code. Verify stream availability."
                )
            }

            val resolvedType = when {
                contentType.contains("mpegurl") || trimmed.lowercase().contains(".m3u8") -> StreamSourceType.HLS
                contentType.contains("mp4") || trimmed.lowercase().contains(".mp4") -> StreamSourceType.MP4
                else -> type
            }

            var durationMinutes = 0
            if (resolvedType == StreamSourceType.HLS) {
                durationMinutes = estimateHlsDurationMinutes(trimmed)
            }

            val inferredTitle = inferTitleFromUrl(trimmed)
            SmartUrlMetadata(
                sourceUrl = trimmed,
                sourceType = resolvedType,
                title = inferredTitle,
                description = "Imported via DSK Smart URL Importer (${resolvedType.name}). Review and edit before publishing.",
                language = "Hindi",
                durationMinutes = durationMinutes,
                category = if (resolvedType == StreamSourceType.HLS) "Live / Adaptive Stream" else "Direct Video",
                isPlayable = true,
                statusNote = "Verified ${resolvedType.name} stream (${contentType.ifBlank { "media stream" }})."
            )
        }.getOrElse { err ->
            SmartUrlMetadata(
                sourceUrl = trimmed,
                sourceType = type,
                title = inferTitleFromUrl(trimmed),
                isPlayable = false,
                statusNote = "Could not inspect URL: ${err.message ?: "Network error"}. You can still fill details manually."
            )
        }
    }

    suspend fun searchTmdb(query: String, isSeries: Boolean): SmartUrlMetadata = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return@withContext SmartUrlMetadata(
                sourceUrl = "",
                sourceType = StreamSourceType.UNKNOWN,
                statusNote = "Enter a title first to search TMDB."
            )
        }
        val key = RuntimeApiKeyStore.tmdbApiKey.trim()
        if (key.isBlank()) {
            return@withContext SmartUrlMetadata(
                sourceUrl = "",
                sourceType = StreamSourceType.UNKNOWN,
                statusNote = "TMDB API Key is not configured yet. Add it in Admin Studio -> API Keys."
            )
        }
        runCatching {
            val encoded = URLEncoder.encode(cleanQuery, "UTF-8")
            val endpoint = if (isSeries) "tv" else "movie"
            val isBearer = key.contains(".") && key.length > 40
            val url = if (isBearer) {
                "https://api.themoviedb.org/3/search/$endpoint?query=$encoded"
            } else {
                "https://api.themoviedb.org/3/search/$endpoint?api_key=${URLEncoder.encode(key, "UTF-8")}&query=$encoded"
            }
            val reqBuilder = Request.Builder().url(url).get()
            if (isBearer) {
                reqBuilder.header("Authorization", "Bearer $key")
            }
            client.newCall(reqBuilder.build()).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (!res.isSuccessful) {
                    return@withContext SmartUrlMetadata(
                        sourceUrl = "",
                        sourceType = StreamSourceType.UNKNOWN,
                        statusNote = "TMDB API returned HTTP ${res.code}. Check your TMDB API Key."
                    )
                }
                val json = JSONObject(raw)
                val results = json.optJSONArray("results")
                if (results == null || results.length() == 0) {
                    return@withContext SmartUrlMetadata(
                        sourceUrl = "",
                        sourceType = StreamSourceType.UNKNOWN,
                        statusNote = "No TMDB match found for '$cleanQuery'."
                    )
                }
                val first = results.getJSONObject(0)
                val title = first.optString(if (isSeries) "name" else "title", cleanQuery)
                val overview = first.optString("overview", "")
                val posterPath = first.optString("poster_path", "")
                val backdropPath = first.optString("backdrop_path", "")
                val langCode = first.optString("original_language", "en")
                val resolvedLang = when (langCode.lowercase()) {
                    "hi" -> "Hindi"
                    "pa" -> "Punjabi"
                    "ta" -> "Tamil"
                    "te" -> "Telugu"
                    else -> "English"
                }
                val thumbUrl = when {
                    posterPath.isNotBlank() && posterPath != "null" -> "https://image.tmdb.org/t/p/w500$posterPath"
                    backdropPath.isNotBlank() && backdropPath != "null" -> "https://image.tmdb.org/t/p/w780$backdropPath"
                    else -> ""
                }
                SmartUrlMetadata(
                    sourceUrl = "",
                    sourceType = StreamSourceType.AUTHORIZED_STREAM,
                    title = title,
                    description = overview,
                    thumbnailUrl = thumbUrl,
                    language = resolvedLang,
                    category = if (isSeries) "Web Series" else "Cinema",
                    isPlayable = true,
                    statusNote = "Retrieved TMDB metadata for '$title'."
                )
            }
        }.getOrElse { err ->
            SmartUrlMetadata(
                sourceUrl = "",
                sourceType = StreamSourceType.UNKNOWN,
                statusNote = "TMDB lookup failed: ${err.message ?: "Network error"}"
            )
        }
    }

    suspend fun testSupabaseKey(url: String, anonKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = url.trim().trimEnd('/')
            val cleanKey = anonKey.trim()
            if (!cleanUrl.startsWith("https://")) throw IllegalStateException("Supabase URL must start with https://")
            if (cleanKey.isBlank()) throw IllegalStateException("Enter Supabase Public Anon Key first.")
            if (SupabaseConfig.isLikelyServiceRoleKey(cleanKey)) {
                throw IllegalStateException("Never use a Service Role key in the app. Use Public Anon Key.")
            }
            val req = Request.Builder()
                .url("$cleanUrl/rest/v1/movies?select=id&limit=1")
                .header("apikey", cleanKey)
                .header("Authorization", "Bearer $cleanKey")
                .get()
                .build()
            client.newCall(req).execute().use { res ->
                when (res.code) {
                    in 200..299 -> "Connected to Supabase REST & Auth (HTTP ${res.code})."
                    404 -> "Connected to Supabase! Run SQL Schema to create catalog tables (HTTP 404)."
                    401, 403 -> throw IllegalStateException("Invalid Supabase Anon Key (HTTP ${res.code}).")
                    else -> "Supabase responded with HTTP ${res.code}."
                }
            }
        }
    }

    suspend fun testTmdbKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) throw IllegalStateException("Enter TMDB API Key first.")
            val isBearer = cleanKey.contains(".") && cleanKey.length > 40
            val url = if (isBearer) {
                "https://api.themoviedb.org/3/configuration"
            } else {
                "https://api.themoviedb.org/3/configuration?api_key=${URLEncoder.encode(cleanKey, "UTF-8")}"
            }
            val builder = Request.Builder().url(url).get()
            if (isBearer) builder.header("Authorization", "Bearer $cleanKey")
            client.newCall(builder.build()).execute().use { res ->
                if (!res.isSuccessful) {
                    throw IllegalStateException("TMDB rejected key (HTTP ${res.code}).")
                }
                "TMDB API Key verified (HTTP ${res.code})."
            }
        }
    }

    suspend fun testYoutubeKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) throw IllegalStateException("Enter YouTube Data API v3 Key first.")
            val url = "https://www.googleapis.com/youtube/v3/videos?part=id&id=dQw4w9WgXcQ&key=${URLEncoder.encode(cleanKey, "UTF-8")}"
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    val body = res.body?.string().orEmpty()
                    val msg = runCatching {
                        JSONObject(body).optJSONObject("error")?.optString("message")
                    }.getOrNull()
                    throw IllegalStateException(msg?.takeIf { it.isNotBlank() } ?: "YouTube API rejected key (HTTP ${res.code}).")
                }
                "YouTube Data API v3 Key verified (HTTP ${res.code})."
            }
        }
    }

    suspend fun testFirebaseKey(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) throw IllegalStateException("Enter Firebase Web API Key first.")
            val url = "https://identitytoolkit.googleapis.com/v1/accounts:createAuthUri?key=${URLEncoder.encode(cleanKey, "UTF-8")}"
            val body = """{"identifier":"check@example.com","continueUri":"https://localhost"}"""
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder().url(url).post(body).build()
            client.newCall(req).execute().use { res ->
                val raw = res.body?.string().orEmpty()
                if (res.isSuccessful) {
                    return@runCatching "Firebase Web API Key verified (HTTP ${res.code})."
                }
                val errMsg = runCatching {
                    JSONObject(raw).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                if (errMsg.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true)) {
                    "Firebase API Key is recognized, enable Identity Toolkit / Firebase Auth in GCP Console."
                } else {
                    throw IllegalStateException(errMsg.ifBlank { "Firebase API Key check failed (HTTP ${res.code})." })
                }
            }
        }
    }

    private fun fetchYoutubeDataApi(url: String, apiKey: String): SmartUrlMetadata? {
        val videoId = extractYoutubeVideoId(url) ?: return null
        return runCatching {
            val apiUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet,contentDetails&id=${URLEncoder.encode(videoId, "UTF-8")}&key=${URLEncoder.encode(apiKey, "UTF-8")}"
            val req = Request.Builder().url(apiUrl).get().build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return null
                val items = JSONObject(res.body?.string().orEmpty()).optJSONArray("items") ?: return null
                if (items.length() == 0) return null
                val item = items.getJSONObject(0)
                val snippet = item.optJSONObject("snippet") ?: return null
                val details = item.optJSONObject("contentDetails")
                val thumbs = snippet.optJSONObject("thumbnails")
                val bestThumb = thumbs?.optJSONObject("maxres")?.optString("url")
                    ?: thumbs?.optJSONObject("high")?.optString("url")
                    ?: thumbs?.optJSONObject("medium")?.optString("url")
                    ?: ""
                val durationIso = details?.optString("duration").orEmpty()
                val durationMins = parseIsoDurationMinutes(durationIso)
                SmartUrlMetadata(
                    sourceUrl = url,
                    sourceType = StreamSourceType.YOUTUBE,
                    title = snippet.optString("title", "YouTube Video"),
                    description = snippet.optString("description", "").take(500),
                    thumbnailUrl = bestThumb,
                    language = "English",
                    durationMinutes = durationMins,
                    category = "YouTube Data API v3",
                    isPlayable = true,
                    statusNote = "Retrieved YouTube Data API v3 metadata (${if (durationMins > 0) "${durationMins}m" else "verified"})."
                )
            }
        }.getOrNull()
    }

    private fun fetchYoutubeOEmbed(url: String): SmartUrlMetadata {
        return runCatching {
            val encoded = URLEncoder.encode(url, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val req = Request.Builder().url(oembedUrl).get().build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    return SmartUrlMetadata(
                        sourceUrl = url,
                        sourceType = StreamSourceType.YOUTUBE,
                        isPlayable = false,
                        statusNote = "YouTube oEmbed unavailable (HTTP ${res.code}). Video may be private or embedding disabled."
                    )
                }
                val json = JSONObject(res.body?.string().orEmpty())
                val title = json.optString("title", "YouTube Video")
                val author = json.optString("author_name", "")
                val thumb = json.optString("thumbnail_url", "")
                SmartUrlMetadata(
                    sourceUrl = url,
                    sourceType = StreamSourceType.YOUTUBE,
                    title = title,
                    description = if (author.isNotBlank()) "Official YouTube stream by $author." else "Official YouTube embed.",
                    thumbnailUrl = thumb,
                    language = "English",
                    category = "Official YouTube Embed",
                    isPlayable = true,
                    statusNote = "Retrieved official YouTube oEmbed metadata."
                )
            }
        }.getOrElse { err ->
            SmartUrlMetadata(
                sourceUrl = url,
                sourceType = StreamSourceType.YOUTUBE,
                isPlayable = false,
                statusNote = "YouTube metadata lookup failed: ${err.message}"
            )
        }
    }

    private fun estimateHlsDurationMinutes(url: String): Int {
        return runCatching {
            val reqBuilder = Request.Builder().url(url).get()
            applyCdnHeaderIfConfigured(reqBuilder)
            client.newCall(reqBuilder.build()).execute().use { res ->
                if (!res.isSuccessful) return 0
                val text = res.body?.string().orEmpty()
                val totalSeconds = Regex("""#EXTINF:([0-9]+(?:\.[0-9]+)?)""")
                    .findAll(text)
                    .sumOf { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                if (totalSeconds > 0) (totalSeconds / 60.0).roundToInt().coerceAtLeast(1) else 0
            }
        }.getOrDefault(0)
    }

    private fun applyCdnHeaderIfConfigured(builder: Request.Builder) {
        val cdnKey = RuntimeApiKeyStore.mediaCdnApiKey
        val cdnHeader = RuntimeApiKeyStore.mediaCdnHeaderName
        if (cdnKey.isNotBlank() && cdnHeader.isNotBlank()) {
            builder.header(cdnHeader, cdnKey)
        }
    }

    private fun inferTitleFromUrl(url: String): String {
        val lastSegment = runCatching { Uri.parse(url).lastPathSegment }.getOrNull().orEmpty()
        val base = lastSegment.substringBeforeLast('.')
            .replace(Regex("""[-_]+"""), " ")
            .trim()
        return if (base.length >= 2) {
            base.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
        } else {
            "Imported Stream"
        }
    }

    companion object {
        fun parseIsoDurationMinutes(iso: String): Int {
            if (iso.isBlank()) return 0
            val hours = Regex("""(\d+)H""").find(iso)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val mins = Regex("""(\d+)M""").find(iso)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val secs = Regex("""(\d+)S""").find(iso)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            val totalSecs = hours * 3600 + mins * 60 + secs
            return if (totalSecs > 0) (totalSecs / 60.0).roundToInt().coerceAtLeast(1) else 0
        }

        fun extractYoutubeVideoId(url: String): String? {
            val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return null
            val host = uri.host?.lowercase().orEmpty()
            return when {
                host.contains("youtu.be") -> uri.pathSegments.firstOrNull()
                host.contains("youtube.com") -> {
                    uri.getQueryParameter("v")
                        ?: uri.pathSegments.let { segs ->
                            val idx = segs.indexOfFirst { it == "embed" || it == "shorts" || it == "live" }
                            if (idx >= 0 && idx + 1 < segs.size) segs[idx + 1] else null
                        }
                }
                else -> null
            }
        }
    }
}
