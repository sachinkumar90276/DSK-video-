package com.example.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.AppSettingsState
import com.example.data.model.PlayableMedia
import com.example.data.supabase.SmartUrlImporter
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    media: PlayableMedia?,
    settings: AppSettingsState,
    onSaveProgress: (PlayableMedia, Long, Long) -> Unit,
    onPlayEpisode: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }

    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var selectedQuality by remember { mutableStateOf(settings.defaultQuality) }
    var subtitlesEnabled by remember { mutableStateOf(settings.subtitlesEnabled) }
    var volume by remember { mutableFloatStateOf(1.0f) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var playerError by remember { mutableStateOf<String?>(null) }

    fun exitPlayer() {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onBack()
    }

    BackHandler { exitPlayer() }

    DisposableEffect(isFullscreen) {
        activity?.requestedOrientation = if (isFullscreen) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    if (media == null || media.videoUrl.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DskPitchBlack)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = DskCrimson, modifier = Modifier.size(48.dp))
                Text("Stream URL unavailable", style = MaterialTheme.typography.titleLarge)
                Button(onClick = ::exitPlayer) { Text("Back") }
            }
        }
        return
    }

    val youtubeId = remember(media.videoUrl) { SmartUrlImporter.extractYoutubeVideoId(media.videoUrl) }

    if (youtubeId != null) {
        YoutubeOfficialEmbedPlayer(
            media = media,
            videoId = youtubeId,
            isFullscreen = isFullscreen,
            onToggleFullscreen = { isFullscreen = !isFullscreen },
            onBack = ::exitPlayer
        )
        return
    }

    val exoPlayer = remember(media.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val itemBuilder = MediaItem.Builder()
                .setUri(media.videoUrl.toUri())
            if (media.videoUrl.lowercase().contains(".m3u8")) {
                itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
            }
            if (media.subtitleUrl.isNotBlank()) {
                val subConfig = MediaItem.SubtitleConfiguration.Builder(media.subtitleUrl.toUri())
                    .setMimeType(
                        if (media.subtitleUrl.lowercase().endsWith(".srt")) MimeTypes.APPLICATION_SUBRIP
                        else MimeTypes.TEXT_VTT
                    )
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                itemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
            setMediaItem(itemBuilder.build())
            if (media.resumePositionMs > 2000L && !media.isLive) {
                seekTo(media.resumePositionMs)
            }
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer, media.contentId) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED && settings.autoPlayNext && media.nextEpisode != null) {
                    onSaveProgress(media, exoPlayer.duration, exoPlayer.duration)
                    onPlayEpisode(media.nextEpisode.id)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                playerError = error.localizedMessage ?: "Unable to play this video stream."
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration.coerceAtLeast(0L)
            onSaveProgress(media, pos, dur)
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    LaunchedEffect(exoPlayer, media.contentId) {
        var tick = 0
        while (true) {
            currentPosMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            durationMs = exoPlayer.duration.coerceAtLeast(0L)
            tick++
            if (tick % 5 == 0 && durationMs > 0L) {
                onSaveProgress(media, currentPosMs, durationMs)
            }
            delay(1000)
        }
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
    }

    LaunchedEffect(volume) {
        exoPlayer.volume = volume
    }

    LaunchedEffect(selectedQuality, subtitlesEnabled) {
        val maxHeight = when (selectedQuality) {
            "1080p" -> 1080
            "720p" -> 720
            "480p" -> 480
            "360p" -> 360
            else -> Int.MAX_VALUE
        }
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, maxHeight)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            .build()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { controlsVisible = !controlsVisible }
            .testTag("player_root")
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        if (isBuffering) {
            CircularProgressIndicator(
                color = DskCrimson,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (playerError != null) {
            Surface(
                color = DskSurfaceElevated.copy(alpha = 0.94f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.WifiOff, contentDescription = null, tint = DskError)
                    Text("Playback Error", style = MaterialTheme.typography.titleLarge)
                    Text(playerError.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = DskSilver)
                    Button(onClick = {
                        playerError = null
                        exoPlayer.prepare()
                        exoPlayer.play()
                    }, colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)) {
                        Text("Retry")
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x73000000))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(14.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = ::exitPlayer,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
                        }
                        Column {
                            Text(media.title, style = MaterialTheme.typography.titleMedium, color = DskWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (media.subtitle.isNotBlank()) {
                                Text(media.subtitle, style = MaterialTheme.typography.labelSmall, color = DskSilver, maxLines = 1)
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { showSpeedDialog = true }, modifier = Modifier.testTag("player_speed_button")) {
                            Text("${playbackSpeed}x", color = DskWhite)
                        }
                        TextButton(onClick = { showQualityDialog = true }, modifier = Modifier.testTag("player_quality_button")) {
                            Text(selectedQuality, color = DskNeonViolet)
                        }
                        IconButton(onClick = { showSubtitleDialog = true }, modifier = Modifier.testTag("player_subtitle_button")) {
                            Icon(
                                if (subtitlesEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                                contentDescription = "Subtitles",
                                tint = DskWhite
                            )
                        }
                    }
                }

                // Center Transport Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (media.previousEpisode != null) {
                        IconButton(onClick = { onPlayEpisode(media.previousEpisode.id) }) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous Episode", tint = DskWhite, modifier = Modifier.size(34.dp))
                        }
                    }
                    if (!media.isLive) {
                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10_000L).coerceAtLeast(0L)) },
                            modifier = Modifier.testTag("player_seek_back")
                        ) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Rewind 10s", tint = DskWhite, modifier = Modifier.size(38.dp))
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = DskCrimson.copy(alpha = 0.9f),
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .clickable {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            }
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = DskWhite,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    if (!media.isLive) {
                        IconButton(
                            onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 10_000L) },
                            modifier = Modifier.testTag("player_seek_forward")
                        ) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Forward 10s", tint = DskWhite, modifier = Modifier.size(38.dp))
                        }
                    }
                    if (media.nextEpisode != null) {
                        IconButton(
                            onClick = { onPlayEpisode(media.nextEpisode.id) },
                            modifier = Modifier.testTag("player_next_episode")
                        ) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Next Episode", tint = DskWhite, modifier = Modifier.size(34.dp))
                        }
                    }
                }

                // Bottom Scrub Bar, Volume, Fullscreen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (media.isLive) {
                        Surface(color = DskLivePulse, shape = RoundedCornerShape(6.dp)) {
                            Text("● LIVE STREAM", style = MaterialTheme.typography.labelMedium, color = DskWhite, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    } else if (durationMs > 0L) {
                        Slider(
                            value = currentPosMs.toFloat(),
                            onValueChange = { exoPlayer.seekTo(it.toLong()) },
                            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(thumbColor = DskCrimson, activeTrackColor = DskCrimson)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatDuration(currentPosMs), style = MaterialTheme.typography.labelMedium, color = DskWhite)
                            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = DskSilver)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Volume", tint = DskWhite, modifier = Modifier.size(20.dp))
                            Slider(
                                value = volume,
                                onValueChange = { volume = it },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = DskWhite, activeTrackColor = DskNeonViolet)
                            )
                        }
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("player_fullscreen_button")
                        ) {
                            Icon(
                                if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = DskWhite
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSpeedDialog) {
        val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("Playback Speed") },
            text = {
                Column {
                    speeds.forEach { spd ->
                        ListItem(
                            headlineContent = { Text("${spd}x") },
                            trailingContent = { if (playbackSpeed == spd) Icon(Icons.Filled.Check, null, tint = DskCrimson) },
                            modifier = Modifier.clickable {
                                playbackSpeed = spd
                                showSpeedDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showQualityDialog) {
        val qualities = listOf("Auto", "1080p", "720p", "480p", "360p")
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("Video Quality") },
            text = {
                Column {
                    qualities.forEach { q ->
                        ListItem(
                            headlineContent = { Text(q) },
                            trailingContent = { if (selectedQuality == q) Icon(Icons.Filled.Check, null, tint = DskCrimson) },
                            modifier = Modifier.clickable {
                                selectedQuality = q
                                showQualityDialog = false
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }

    if (showSubtitleDialog) {
        AlertDialog(
            onDismissRequest = { showSubtitleDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("Subtitles / Captions") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Enabled") },
                        trailingContent = { if (subtitlesEnabled) Icon(Icons.Filled.Check, null, tint = DskCrimson) },
                        modifier = Modifier.clickable {
                            subtitlesEnabled = true
                            showSubtitleDialog = false
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Off") },
                        trailingContent = { if (!subtitlesEnabled) Icon(Icons.Filled.Check, null, tint = DskCrimson) },
                        modifier = Modifier.clickable {
                            subtitlesEnabled = false
                            showSubtitleDialog = false
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun YoutubeOfficialEmbedPlayer(
    media: PlayableMedia,
    videoId: String,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    onBack: () -> Unit
) {
    val embedHtml = """
        <!DOCTYPE html>
        <html>
        <body style="margin:0;padding:0;background:#000;overflow:hidden;">
        <iframe width="100%" height="100%" style="position:fixed;top:0;left:0;width:100%;height:100%;border:0;"
        src="https://www.youtube.com/embed/$videoId?autoplay=1&playsinline=1&rel=0"
        allow="autoplay; encrypted-media; picture-in-picture" allowfullscreen></iframe>
        </body>
        </html>
    """.trimIndent()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL("https://www.youtube.com", embedHtml, "text/html", "UTF-8", null)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color(0x88000000))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
                }
                Text(media.title, style = MaterialTheme.typography.titleMedium, color = DskWhite, maxLines = 1)
            }
            IconButton(onClick = onToggleFullscreen) {
                Icon(
                    if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                    contentDescription = "Fullscreen",
                    tint = DskWhite
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format(Locale.US, "%02d:%02d", mins, secs)
    }
}
