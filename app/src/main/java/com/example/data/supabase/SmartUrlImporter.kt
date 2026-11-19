package com.example.data.supabase

import android.net.Uri
import com.example.data.model.SmartUrlMetadata
import com.example.data.model.StreamSourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Smart URL Import inspects only public/authorized metadata APIs and standard HTTP headers
 * (never bypasses DRM, paywalls, or authentication walls).
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
            return@withContext fetchYoutubeOEmbed(trimmed)
        }

        // Inspect public HTTP headers safely (HEAD, fallback Range GET for HLS/MP4 info)
        runCatching {
            val headReq = Request.Builder().url(trimmed).head().build()
            val headRes = client.newCall(headReq).execute()
            val code = headRes.code
            val contentType = headRes.header("Content-Type").orEmpty().lowercase()
            headRes.close()

            if (code == 401 || code == 403) {
                return@withContext SmartUrlMetadata(
                    sourceUrl = trimmed,
                    sourceType = type,
                    title = inferTitleFromUrl(trimmed),
                    isPlayable = false,
                    statusNote = "Access restricted (HTTP $code). Only authorized public/signed URLs are supported."
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
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) return 0
                val text = res.body?.string().orEmpty()
                val totalSeconds = Regex("""#EXTINF:([0-9]+(?:\.[0-9]+)?)""")
                    .findAll(text)
                    .sumOf { it.groupValues[1].toDoubleOrNull() ?: 0.0 }
                if (totalSeconds > 0) (totalSeconds / 60.0).roundToInt().coerceAtLeast(1) else 0
            }
        }.getOrDefault(0)
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
