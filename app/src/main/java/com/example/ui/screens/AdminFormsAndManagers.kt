package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun SmartUrlImportSection(
    url: String,
    onUrlChange: (String) -> Unit,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspect: (String) -> Unit,
    onApplyMetadata: (SmartUrlMetadata) -> Unit
) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = DskNeonViolet, modifier = Modifier.size(18.dp))
                Text("Smart URL Import (MP4 / HLS M3U8 / YouTube)", style = MaterialTheme.typography.labelLarge, color = DskNeonViolet)
            }
            OutlinedTextField(
                value = url,
                onValueChange = onUrlChange,
                label = { Text("Authorized Video / Stream URL") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_url_input")
            )
            Button(
                onClick = { onInspect(url) },
                enabled = url.isNotBlank() && !isInspecting,
                colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("smart_url_inspect_button")
            ) {
                if (isInspecting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DskWhite, strokeWidth = 2.dp)
                } else {
                    Text("Inspect & Retrieve Metadata")
                }
            }
            if (smartMeta != null) {
                Text(
                    text = smartMeta.statusNote,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (smartMeta.isPlayable) DskSuccess else DskWarning
                )
                if (smartMeta.title.isNotBlank() || smartMeta.thumbnailUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onApplyMetadata(smartMeta) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply Retrieved Metadata to Form", color = DskWhite)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminMoviesList(
    movies: List<MovieItem>,
    onAdd: () -> Unit,
    onEdit: (MovieItem) -> Unit,
    onTogglePublish: (MovieItem) -> Unit,
    onDelete: (MovieItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("admin_add_movie_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add / Import Movie")
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies, key = { it.id }) { m ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${m.releaseYear} • ${m.language} • ${m.genre}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                            Text(if (m.isPublished) "Published" else "Unpublished", style = MaterialTheme.typography.labelSmall, color = if (m.isPublished) DskSuccess else DskWarning)
                        }
                        IconButton(onClick = { onTogglePublish(m) }) {
                            Icon(if (m.isPublished) Icons.Filled.Visibility else Icons.Filled.VisibilityOff, contentDescription = "Toggle Publish", tint = DskNeonViolet)
                        }
                        IconButton(onClick = { onEdit(m) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Movie", tint = DskWhite)
                        }
                        IconButton(onClick = { onDelete(m) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Movie", tint = DskError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSeriesList(
    seriesList: List<WebSeriesItem>,
    seasons: List<SeasonItem>,
    episodes: List<EpisodeItem>,
    onAddSeries: () -> Unit,
    onEditSeries: (WebSeriesItem) -> Unit,
    onDeleteSeries: (WebSeriesItem) -> Unit,
    onManageSeasonsAndEpisodes: (WebSeriesItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddSeries,
            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("admin_add_series_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Web Series")
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(seriesList, key = { it.id }) { s ->
                val sSeasons = seasons.count { it.seriesId == s.id }
                val sEps = episodes.count { it.seriesId == s.id }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("$sSeasons Seasons • $sEps Episodes • ${s.language}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                            }
                            IconButton(onClick = { onEditSeries(s) }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Series", tint = DskWhite)
                            }
                            IconButton(onClick = { onDeleteSeries(s) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Series", tint = DskError)
                            }
                        }
                        OutlinedButton(
                            onClick = { onManageSeasonsAndEpisodes(s) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = DskNeonViolet)
                            Spacer(Modifier.width(6.dp))
                            Text("Manage Seasons & Episodes", color = DskWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLiveTvList(
    channels: List<LiveChannelItem>,
    onAdd: () -> Unit,
    onEdit: (LiveChannelItem) -> Unit,
    onDelete: (LiveChannelItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("admin_add_channel_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Live TV Channel")
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(channels, key = { it.id }) { ch ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.name, style = MaterialTheme.typography.titleMedium)
                            Text("${ch.category} • ${ch.language}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                        }
                        IconButton(onClick = { onEdit(ch) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = DskWhite) }
                        IconButton(onClick = { onDelete(ch) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DskError) }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSportsList(
    items: List<SportsItem>,
    onAdd: () -> Unit,
    onEdit: (SportsItem) -> Unit,
    onDelete: (SportsItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("admin_add_sports_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Sports Content")
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { sp ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sp.title, style = MaterialTheme.typography.titleMedium)
                            Text("${sp.sportType} • ${sp.category}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                        }
                        IconButton(onClick = { onEdit(sp) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = DskWhite) }
                        IconButton(onClick = { onDelete(sp) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DskError) }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminKidsList(
    items: List<KidsItem>,
    onAdd: () -> Unit,
    onEdit: (KidsItem) -> Unit,
    onDelete: (KidsItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("admin_add_kids_button")
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Add Kids Content")
        }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { kd ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(kd.title, style = MaterialTheme.typography.titleMedium)
                            Text("${kd.category} • ${kd.ageRating} • ${kd.language}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                        }
                        IconButton(onClick = { onEdit(kd) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = DskWhite) }
                        IconButton(onClick = { onDelete(kd) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = DskError) }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieEditorDialog(
    initial: MovieItem,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspectUrl: (String) -> Unit,
    onSearchTmdb: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (MovieItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var description by remember { mutableStateOf(initial.description) }
    var videoUrl by remember { mutableStateOf(initial.videoUrl) }
    var posterUrl by remember { mutableStateOf(initial.posterUrl) }
    var bannerUrl by remember { mutableStateOf(initial.bannerUrl) }
    var trailerUrl by remember { mutableStateOf(initial.trailerUrl) }
    var subtitleUrl by remember { mutableStateOf(initial.subtitleUrl) }
    var language by remember { mutableStateOf(initial.language) }
    var genre by remember { mutableStateOf(initial.genre) }
    var category by remember { mutableStateOf(initial.category) }
    var year by remember { mutableStateOf(initial.releaseYear.toString()) }
    var duration by remember { mutableStateOf(initial.durationMinutes.toString()) }
    var rating by remember { mutableStateOf(initial.imdbRating.toString()) }
    var cast by remember { mutableStateOf(initial.castMembers) }
    var director by remember { mutableStateOf(initial.director) }
    var isFeatured by remember { mutableStateOf(initial.isFeatured) }
    var isTrending by remember { mutableStateOf(initial.isTrending) }
    var isPublished by remember { mutableStateOf(initial.isPublished) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text(if (initial.id.isBlank()) "Add / Import Movie" else "Edit Movie") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmartUrlImportSection(
                    url = videoUrl,
                    onUrlChange = { videoUrl = it },
                    smartMeta = smartMeta,
                    isInspecting = isInspecting,
                    onInspect = onInspectUrl,
                    onApplyMetadata = { meta ->
                        if (meta.title.isNotBlank()) title = meta.title
                        if (meta.description.isNotBlank()) description = meta.description
                        if (meta.thumbnailUrl.isNotBlank()) {
                            posterUrl = meta.thumbnailUrl
                            bannerUrl = meta.thumbnailUrl
                        }
                        if (meta.durationMinutes > 0) duration = meta.durationMinutes.toString()
                        if (meta.language.isNotBlank()) language = meta.language
                    }
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Movie Title*") }, singleLine = true)
                OutlinedButton(
                    onClick = { onSearchTmdb(title) },
                    enabled = title.isNotBlank() && !isInspecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Movie, contentDescription = null, tint = DskNeonViolet)
                    Spacer(Modifier.width(6.dp))
                    Text("Fetch Title & Poster from TMDB", color = DskWhite)
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, singleLine = true)
                OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, label = { Text("Banner URL") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language (Hindi, English, Punjabi...)") }, singleLine = true)
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre (Action, Comedy, Thriller...)") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Bollywood, Hollywood, South Indian)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = year, onValueChange = { year = it }, label = { Text("Year") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Min") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = rating, onValueChange = { rating = it }, label = { Text("IMDb") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = cast, onValueChange = { cast = it }, label = { Text("Cast") }, singleLine = true)
                OutlinedTextField(value = director, onValueChange = { director = it }, label = { Text("Director") }, singleLine = true)
                OutlinedTextField(value = trailerUrl, onValueChange = { trailerUrl = it }, label = { Text("Trailer URL") }, singleLine = true)
                OutlinedTextField(value = subtitleUrl, onValueChange = { subtitleUrl = it }, label = { Text("Subtitle VTT/SRT URL") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isFeatured, onCheckedChange = { isFeatured = it })
                    Text("Featured Hero")
                    Spacer(Modifier.width(8.dp))
                    Checkbox(checked = isTrending, onCheckedChange = { isTrending = it })
                    Text("Trending")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublished, onCheckedChange = { isPublished = it })
                    Text("Publish Immediately")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            description = description.trim(),
                            videoUrl = videoUrl.trim(),
                            posterUrl = posterUrl.trim(),
                            bannerUrl = bannerUrl.trim(),
                            trailerUrl = trailerUrl.trim(),
                            subtitleUrl = subtitleUrl.trim(),
                            language = language.trim(),
                            genre = genre.trim(),
                            category = category.trim(),
                            releaseYear = year.toIntOrNull() ?: 2026,
                            durationMinutes = duration.toIntOrNull() ?: 120,
                            imdbRating = rating.toDoubleOrNull() ?: 8.0,
                            castMembers = cast.trim(),
                            director = director.trim(),
                            isFeatured = isFeatured,
                            isTrending = isTrending,
                            isPublished = isPublished
                        )
                    )
                },
                enabled = title.isNotBlank() && videoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
            ) { Text("Save Movie") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SeriesEditorDialog(
    initial: WebSeriesItem,
    smartMeta: SmartUrlMetadata? = null,
    isInspecting: Boolean = false,
    onSearchTmdb: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (WebSeriesItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var description by remember { mutableStateOf(initial.description) }
    var posterUrl by remember { mutableStateOf(initial.posterUrl) }
    var bannerUrl by remember { mutableStateOf(initial.bannerUrl) }
    var language by remember { mutableStateOf(initial.language) }
    var genre by remember { mutableStateOf(initial.genre) }
    var isPublished by remember { mutableStateOf(initial.isPublished) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text(if (initial.id.isBlank()) "Add Web Series" else "Edit Web Series") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Series Title*") }, singleLine = true)
                OutlinedButton(
                    onClick = { onSearchTmdb(title) },
                    enabled = title.isNotBlank() && !isInspecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Tv, contentDescription = null, tint = DskNeonViolet)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isInspecting) "Searching TMDB..." else "Fetch Series Info from TMDB", color = DskWhite)
                }
                if (smartMeta != null && smartMeta.statusNote.isNotBlank()) {
                    Text(
                        text = smartMeta.statusNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (smartMeta.isPlayable) DskSuccess else DskWarning
                    )
                    if (smartMeta.title.isNotBlank() || smartMeta.thumbnailUrl.isNotBlank()) {
                        TextButton(
                            onClick = {
                                if (smartMeta.title.isNotBlank()) title = smartMeta.title
                                if (smartMeta.description.isNotBlank()) description = smartMeta.description
                                if (smartMeta.thumbnailUrl.isNotBlank()) {
                                    posterUrl = smartMeta.thumbnailUrl
                                    bannerUrl = smartMeta.thumbnailUrl
                                }
                                if (smartMeta.language.isNotBlank()) language = smartMeta.language
                            }
                        ) {
                            Text("Apply TMDB Metadata", color = DskNeonViolet)
                        }
                    }
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, maxLines = 3)
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, singleLine = true)
                OutlinedTextField(value = bannerUrl, onValueChange = { bannerUrl = it }, label = { Text("Banner URL") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
                OutlinedTextField(value = genre, onValueChange = { genre = it }, label = { Text("Genre") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublished, onCheckedChange = { isPublished = it })
                    Text("Published")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        initial.copy(
                            title = title.trim(),
                            description = description.trim(),
                            posterUrl = posterUrl.trim(),
                            bannerUrl = bannerUrl.trim(),
                            language = language.trim(),
                            genre = genre.trim(),
                            isPublished = isPublished
                        )
                    )
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
            ) { Text("Save Series") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SeasonsAndEpisodesManagerDialog(
    series: WebSeriesItem,
    seasons: List<SeasonItem>,
    episodes: List<EpisodeItem>,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspectUrl: (String) -> Unit,
    onAddSeason: (String, Int, String) -> Unit,
    onDeleteSeason: (String) -> Unit,
    onSaveEpisode: (EpisodeItem, () -> Unit) -> Unit,
    onDeleteEpisode: (EpisodeItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sortedSeasons = seasons.sortedBy { it.seasonNumber }
    var selectedSeasonId by remember(sortedSeasons) { mutableStateOf(sortedSeasons.firstOrNull()?.id.orEmpty()) }
    var showEpisodeForm by remember { mutableStateOf(false) }
    var epTitle by remember { mutableStateOf("") }
    var epNumber by remember { mutableStateOf("1") }
    var epUrl by remember { mutableStateOf("") }
    var epThumb by remember { mutableStateOf("") }
    var epDesc by remember { mutableStateOf("") }
    var epDuration by remember { mutableStateOf("45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text("${series.title} • Seasons & Episodes") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val nextNum = (sortedSeasons.maxOfOrNull { it.seasonNumber } ?: 0) + 1
                        onAddSeason(series.id, nextNum, "Season $nextNum")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("+ Add Season ${(sortedSeasons.maxOfOrNull { it.seasonNumber } ?: 0) + 1}")
                }

                sortedSeasons.forEach { season ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = season.id == selectedSeasonId,
                            onClick = { selectedSeasonId = season.id },
                            label = { Text(season.title.ifBlank { "Season ${season.seasonNumber}" }) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDeleteSeason(season.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete Season", tint = DskError)
                        }
                    }
                }

                if (selectedSeasonId.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            val nextEp = (episodes.filter { it.seasonId == selectedSeasonId }.maxOfOrNull { it.episodeNumber } ?: 0) + 1
                            epNumber = nextEp.toString()
                            showEpisodeForm = !showEpisodeForm
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (showEpisodeForm) "Hide Episode Form" else "+ Add Episode")
                    }
                }

                if (showEpisodeForm && selectedSeasonId.isNotBlank()) {
                    SmartUrlImportSection(
                        url = epUrl,
                        onUrlChange = { epUrl = it },
                        smartMeta = smartMeta,
                        isInspecting = isInspecting,
                        onInspect = onInspectUrl,
                        onApplyMetadata = { meta ->
                            if (meta.title.isNotBlank()) epTitle = meta.title
                            if (meta.thumbnailUrl.isNotBlank()) epThumb = meta.thumbnailUrl
                            if (meta.description.isNotBlank()) epDesc = meta.description
                            if (meta.durationMinutes > 0) epDuration = meta.durationMinutes.toString()
                        }
                    )
                    OutlinedTextField(value = epNumber, onValueChange = { epNumber = it }, label = { Text("Episode #") }, singleLine = true)
                    OutlinedTextField(value = epTitle, onValueChange = { epTitle = it }, label = { Text("Episode Title*") }, singleLine = true)
                    OutlinedTextField(value = epThumb, onValueChange = { epThumb = it }, label = { Text("Thumbnail URL") }, singleLine = true)
                    OutlinedTextField(value = epDesc, onValueChange = { epDesc = it }, label = { Text("Description") }, maxLines = 2)
                    OutlinedTextField(value = epDuration, onValueChange = { epDuration = it }, label = { Text("Duration (min)") }, singleLine = true)
                    Button(
                        onClick = {
                            onSaveEpisode(
                                EpisodeItem(
                                    seriesId = series.id,
                                    seasonId = selectedSeasonId,
                                    episodeNumber = epNumber.toIntOrNull() ?: 1,
                                    title = epTitle.trim(),
                                    description = epDesc.trim(),
                                    thumbnailUrl = epThumb.trim(),
                                    durationMinutes = epDuration.toIntOrNull() ?: 45,
                                    videoUrl = epUrl.trim()
                                )
                            ) {
                                epTitle = ""
                                epUrl = ""
                                showEpisodeForm = false
                            }
                        },
                        enabled = epTitle.isNotBlank() && epUrl.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Episode") }
                }

                episodes.filter { it.seasonId == selectedSeasonId }.sortedBy { it.episodeNumber }.forEach { ep ->
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("E${ep.episodeNumber}: ${ep.title}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onDeleteEpisode(ep) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Episode", tint = DskError)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun LiveChannelEditorDialog(
    initial: LiveChannelItem,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspectUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (LiveChannelItem) -> Unit
) {
    var name by remember { mutableStateOf(initial.name) }
    var streamUrl by remember { mutableStateOf(initial.streamUrl) }
    var logoUrl by remember { mutableStateOf(initial.logoUrl) }
    var category by remember { mutableStateOf(initial.category) }
    var language by remember { mutableStateOf(initial.language) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text(if (initial.id.isBlank()) "Add Live TV Channel" else "Edit Channel") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartUrlImportSection(
                    url = streamUrl,
                    onUrlChange = { streamUrl = it },
                    smartMeta = smartMeta,
                    isInspecting = isInspecting,
                    onInspect = onInspectUrl,
                    onApplyMetadata = { meta ->
                        if (meta.title.isNotBlank()) name = meta.title
                        if (meta.thumbnailUrl.isNotBlank()) logoUrl = meta.thumbnailUrl
                    }
                )
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Channel Name*") }, singleLine = true)
                OutlinedTextField(value = logoUrl, onValueChange = { logoUrl = it }, label = { Text("Logo URL") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (News, Entertainment, Sports, Music, Regional)") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(name = name.trim(), streamUrl = streamUrl.trim(), logoUrl = logoUrl.trim(), category = category.trim(), language = language.trim()))
                },
                enabled = name.isNotBlank() && streamUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
            ) { Text("Save Channel") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SportsEditorDialog(
    initial: SportsItem,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspectUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (SportsItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var videoUrl by remember { mutableStateOf(initial.videoUrl) }
    var thumbUrl by remember { mutableStateOf(initial.thumbnailUrl) }
    var sportType by remember { mutableStateOf(initial.sportType) }
    var category by remember { mutableStateOf(initial.category) }
    var isLive by remember { mutableStateOf(initial.isLive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text(if (initial.id.isBlank()) "Add Sports Content" else "Edit Sports Content") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartUrlImportSection(
                    url = videoUrl,
                    onUrlChange = { videoUrl = it },
                    smartMeta = smartMeta,
                    isInspecting = isInspecting,
                    onInspect = onInspectUrl,
                    onApplyMetadata = { meta ->
                        if (meta.title.isNotBlank()) title = meta.title
                        if (meta.thumbnailUrl.isNotBlank()) thumbUrl = meta.thumbnailUrl
                    }
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, singleLine = true)
                OutlinedTextField(value = thumbUrl, onValueChange = { thumbUrl = it }, label = { Text("Thumbnail URL") }, singleLine = true)
                OutlinedTextField(value = sportType, onValueChange = { sportType = it }, label = { Text("Sport (Cricket, Football, Kabaddi, Other Sports)") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Highlights, Match Replays)") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isLive, onCheckedChange = { isLive = it })
                    Text("Live Broadcast")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(title = title.trim(), videoUrl = videoUrl.trim(), thumbnailUrl = thumbUrl.trim(), sportType = sportType.trim(), category = category.trim(), isLive = isLive))
                },
                enabled = title.isNotBlank() && videoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
            ) { Text("Save Sports") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun KidsEditorDialog(
    initial: KidsItem,
    smartMeta: SmartUrlMetadata?,
    isInspecting: Boolean,
    onInspectUrl: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: (KidsItem) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var videoUrl by remember { mutableStateOf(initial.videoUrl) }
    var posterUrl by remember { mutableStateOf(initial.posterUrl) }
    var category by remember { mutableStateOf(initial.category) }
    var ageRating by remember { mutableStateOf(initial.ageRating) }
    var language by remember { mutableStateOf(initial.language) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        title = { Text(if (initial.id.isBlank()) "Add Kids Content" else "Edit Kids Content") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SmartUrlImportSection(
                    url = videoUrl,
                    onUrlChange = { videoUrl = it },
                    smartMeta = smartMeta,
                    isInspecting = isInspecting,
                    onInspect = onInspectUrl,
                    onApplyMetadata = { meta ->
                        if (meta.title.isNotBlank()) title = meta.title
                        if (meta.thumbnailUrl.isNotBlank()) posterUrl = meta.thumbnailUrl
                    }
                )
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title*") }, singleLine = true)
                OutlinedTextField(value = posterUrl, onValueChange = { posterUrl = it }, label = { Text("Poster URL") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Cartoons, Animated Movies, Kids Series, Educational Videos)") }, singleLine = true)
                OutlinedTextField(value = ageRating, onValueChange = { ageRating = it }, label = { Text("Age Rating (U, U/A 7+)") }, singleLine = true)
                OutlinedTextField(value = language, onValueChange = { language = it }, label = { Text("Language") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initial.copy(title = title.trim(), videoUrl = videoUrl.trim(), posterUrl = posterUrl.trim(), category = category.trim(), ageRating = ageRating.trim(), language = language.trim()))
                },
                enabled = title.isNotBlank() && videoUrl.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
            ) { Text("Save Kids Item") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
