package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.data.repository.CatalogSnapshot
import com.example.data.supabase.SupabaseSqlSchema
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminState

@Composable
fun AdminLoginScreen(
    isBusy: Boolean,
    errorMessage: String?,
    onAdminSignIn: (email: String, password: String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DskPitchBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.TopStart)
                .minimumInteractiveComponentSize()
                .testTag("admin_login_back")
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
        }

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .widthIn(max = 480.dp)
                .align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = DskNeonViolet, modifier = Modifier.size(32.dp))
                    Column {
                        Text("Admin Portal", style = MaterialTheme.typography.headlineMedium)
                        Text("Server-verified admin role access only", style = MaterialTheme.typography.labelMedium, color = DskMuted)
                    }
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Admin Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_email_input")
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Admin Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(errorMessage, color = DskError, style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = { onAdminSignIn(email, password) },
                    enabled = !isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("admin_login_submit")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DskWhite, strokeWidth = 2.dp)
                    } else {
                        Text("Sign In as Administrator", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private enum class AdminSectionTab(val label: String) {
    OVERVIEW("Dashboard"),
    MOVIES("Movies"),
    SERIES("Web Series"),
    LIVE_TV("Live TV"),
    SPORTS("Sports"),
    KIDS("Kids")
}

@Composable
fun AdminDashboardScreen(
    isAdminAuthorized: Boolean,
    adminState: AdminState,
    catalog: CatalogSnapshot,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onInspectSmartUrl: (String) -> Unit,
    onClearSmartUrl: () -> Unit,
    onSaveMovie: (MovieItem, () -> Unit) -> Unit,
    onToggleMoviePublish: (MovieItem) -> Unit,
    onDeleteMovie: (MovieItem) -> Unit,
    onSaveSeries: (WebSeriesItem, () -> Unit) -> Unit,
    onDeleteSeries: (WebSeriesItem) -> Unit,
    onAddSeason: (String, Int, String) -> Unit,
    onDeleteSeason: (String) -> Unit,
    onSaveEpisode: (EpisodeItem, () -> Unit) -> Unit,
    onDeleteEpisode: (EpisodeItem) -> Unit,
    onSaveChannel: (LiveChannelItem, () -> Unit) -> Unit,
    onDeleteChannel: (LiveChannelItem) -> Unit,
    onSaveSports: (SportsItem, () -> Unit) -> Unit,
    onDeleteSports: (SportsItem) -> Unit,
    onSaveKids: (KidsItem, () -> Unit) -> Unit,
    onDeleteKids: (KidsItem) -> Unit
) {
    if (!isAdminAuthorized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DskPitchBlack)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(modifier = Modifier.padding(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.GppBad, contentDescription = "Unauthorized", tint = DskError, modifier = Modifier.size(48.dp))
                    Text("Admin Access Required", style = MaterialTheme.typography.headlineMedium)
                    Text("Standard user accounts cannot access the DSK VIDEO Admin Panel.", style = MaterialTheme.typography.bodyMedium, color = DskSilver)
                    Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)) {
                        Text("Return")
                    }
                }
            }
        }
        return
    }

    var activeTab by remember { mutableStateOf(AdminSectionTab.OVERVIEW) }
    var showSchemaDialog by remember { mutableStateOf(false) }

    var editingMovie by remember { mutableStateOf<MovieItem?>(null) }
    var showMovieEditor by remember { mutableStateOf(false) }

    var editingSeries by remember { mutableStateOf<WebSeriesItem?>(null) }
    var showSeriesEditor by remember { mutableStateOf(false) }
    var managingSeries by remember { mutableStateOf<WebSeriesItem?>(null) }

    var editingChannel by remember { mutableStateOf<LiveChannelItem?>(null) }
    var showChannelEditor by remember { mutableStateOf(false) }

    var editingSports by remember { mutableStateOf<SportsItem?>(null) }
    var showSportsEditor by remember { mutableStateOf(false) }

    var editingKids by remember { mutableStateOf<KidsItem?>(null) }
    var showKidsEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("DSK Admin Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Content & Smart URL Management", style = MaterialTheme.typography.labelSmall, color = DskNeonViolet)
                }
            }
            Row {
                IconButton(onClick = { showSchemaDialog = true }, modifier = Modifier.testTag("admin_schema_button")) {
                    Icon(Icons.Filled.Storage, contentDescription = "Supabase Schema", tint = DskNeonViolet)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("admin_refresh_button")) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Admin Data")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdminSectionTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskRoyalPurple,
                        selectedLabelColor = DskWhite
                    ),
                    modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                )
            }
        }

        when (activeTab) {
            AdminSectionTab.OVERVIEW -> AdminOverviewContent(adminState, catalog)
            AdminSectionTab.MOVIES -> AdminMoviesList(
                movies = catalog.movies,
                onAdd = {
                    editingMovie = MovieItem()
                    onClearSmartUrl()
                    showMovieEditor = true
                },
                onEdit = {
                    editingMovie = it
                    onClearSmartUrl()
                    showMovieEditor = true
                },
                onTogglePublish = onToggleMoviePublish,
                onDelete = onDeleteMovie
            )
            AdminSectionTab.SERIES -> AdminSeriesList(
                seriesList = catalog.webSeries,
                seasons = catalog.seasons,
                episodes = catalog.episodes,
                onAddSeries = {
                    editingSeries = WebSeriesItem()
                    showSeriesEditor = true
                },
                onEditSeries = {
                    editingSeries = it
                    showSeriesEditor = true
                },
                onDeleteSeries = onDeleteSeries,
                onManageSeasonsAndEpisodes = { managingSeries = it }
            )
            AdminSectionTab.LIVE_TV -> AdminLiveTvList(
                channels = catalog.liveChannels,
                onAdd = {
                    editingChannel = LiveChannelItem()
                    onClearSmartUrl()
                    showChannelEditor = true
                },
                onEdit = {
                    editingChannel = it
                    onClearSmartUrl()
                    showChannelEditor = true
                },
                onDelete = onDeleteChannel
            )
            AdminSectionTab.SPORTS -> AdminSportsList(
                items = catalog.sports,
                onAdd = {
                    editingSports = SportsItem()
                    onClearSmartUrl()
                    showSportsEditor = true
                },
                onEdit = {
                    editingSports = it
                    onClearSmartUrl()
                    showSportsEditor = true
                },
                onDelete = onDeleteSports
            )
            AdminSectionTab.KIDS -> AdminKidsList(
                items = catalog.kids,
                onAdd = {
                    editingKids = KidsItem()
                    onClearSmartUrl()
                    showKidsEditor = true
                },
                onEdit = {
                    editingKids = it
                    onClearSmartUrl()
                    showKidsEditor = true
                },
                onDelete = onDeleteKids
            )
        }
    }

    if (showMovieEditor && editingMovie != null) {
        MovieEditorDialog(
            initial = editingMovie!!,
            smartMeta = adminState.smartUrlMetadata,
            isInspecting = adminState.isInspectingUrl,
            onInspectUrl = onInspectSmartUrl,
            onDismiss = { showMovieEditor = false },
            onSave = { movie -> onSaveMovie(movie) { showMovieEditor = false } }
        )
    }

    if (showSeriesEditor && editingSeries != null) {
        SeriesEditorDialog(
            initial = editingSeries!!,
            onDismiss = { showSeriesEditor = false },
            onSave = { series -> onSaveSeries(series) { showSeriesEditor = false } }
        )
    }

    if (managingSeries != null) {
        val currentSeries = catalog.webSeries.firstOrNull { it.id == managingSeries!!.id } ?: managingSeries!!
        SeasonsAndEpisodesManagerDialog(
            series = currentSeries,
            seasons = catalog.seasons.filter { it.seriesId == currentSeries.id },
            episodes = catalog.episodes.filter { it.seriesId == currentSeries.id },
            smartMeta = adminState.smartUrlMetadata,
            isInspecting = adminState.isInspectingUrl,
            onInspectUrl = onInspectSmartUrl,
            onAddSeason = onAddSeason,
            onDeleteSeason = onDeleteSeason,
            onSaveEpisode = onSaveEpisode,
            onDeleteEpisode = onDeleteEpisode,
            onDismiss = { managingSeries = null }
        )
    }

    if (showChannelEditor && editingChannel != null) {
        LiveChannelEditorDialog(
            initial = editingChannel!!,
            smartMeta = adminState.smartUrlMetadata,
            isInspecting = adminState.isInspectingUrl,
            onInspectUrl = onInspectSmartUrl,
            onDismiss = { showChannelEditor = false },
            onSave = { ch -> onSaveChannel(ch) { showChannelEditor = false } }
        )
    }

    if (showSportsEditor && editingSports != null) {
        SportsEditorDialog(
            initial = editingSports!!,
            smartMeta = adminState.smartUrlMetadata,
            isInspecting = adminState.isInspectingUrl,
            onInspectUrl = onInspectSmartUrl,
            onDismiss = { showSportsEditor = false },
            onSave = { sp -> onSaveSports(sp) { showSportsEditor = false } }
        )
    }

    if (showKidsEditor && editingKids != null) {
        KidsEditorDialog(
            initial = editingKids!!,
            smartMeta = adminState.smartUrlMetadata,
            isInspecting = adminState.isInspectingUrl,
            onInspectUrl = onInspectSmartUrl,
            onDismiss = { showKidsEditor = false },
            onSave = { kd -> onSaveKids(kd) { showKidsEditor = false } }
        )
    }

    if (showSchemaDialog) {
        val clipboard = LocalClipboardManager.current
        AlertDialog(
            onDismissRequest = { showSchemaDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("14-Table Supabase SQL Schema") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Run this idempotent SQL migration in Supabase SQL Editor if any tables are not yet created. Existing data is preserved.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DskSilver
                    )
                    Surface(
                        color = DskPitchBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(
                                    SupabaseSqlSchema.MIGRATION_SQL,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DskSilver
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(SupabaseSqlSchema.MIGRATION_SQL))
                        showSchemaDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
                ) { Text("Copy SQL") }
            },
            dismissButton = {
                TextButton(onClick = { showSchemaDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun AdminOverviewContent(
    adminState: AdminState,
    catalog: CatalogSnapshot
) {
    val stats = adminState.stats
    val totalCatalog = (stats.totalMovies + stats.totalWebSeries + stats.totalLiveChannels + stats.totalSports + stats.totalKids).coerceAtLeast(1)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Total Users", "${stats.totalUsers}", Modifier.weight(1f))
                AdminKpiCard("Total Videos", "${stats.totalVideos}", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Movies", "${stats.totalMovies}", Modifier.weight(1f))
                AdminKpiCard("Web Series", "${stats.totalWebSeries}", Modifier.weight(1f))
                AdminKpiCard("Episodes", "${stats.totalEpisodes}", Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdminKpiCard("Live TV", "${stats.totalLiveChannels}", Modifier.weight(1f))
                AdminKpiCard("Sports", "${stats.totalSports}", Modifier.weight(1f))
                AdminKpiCard("Kids", "${stats.totalKids}", Modifier.weight(1f))
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Content Analytics Distribution", style = MaterialTheme.typography.titleMedium)
                    AnalyticsBar("Movies", stats.totalMovies, totalCatalog, DskCrimson)
                    AnalyticsBar("Web Series", stats.totalWebSeries, totalCatalog, DskRoyalPurple)
                    AnalyticsBar("Live TV", stats.totalLiveChannels, totalCatalog, DskLivePulse)
                    AnalyticsBar("Sports", stats.totalSports, totalCatalog, DskSuccess)
                    AnalyticsBar("Kids", stats.totalKids, totalCatalog, DskGold)
                }
            }
        }

        item {
            Text("Recently Added Movies", style = MaterialTheme.typography.titleLarge)
        }

        if (catalog.movies.isEmpty()) {
            item {
                Text("No movies added yet. Switch to the Movies tab to add or Smart-Import a movie.", color = DskMuted)
            }
        } else {
            items(catalog.movies.take(5), key = { "rec_${it.id}" }) { m ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(m.title, style = MaterialTheme.typography.titleMedium)
                            Text("${m.language} • ${m.genre}", style = MaterialTheme.typography.labelSmall, color = DskMuted)
                        }
                        Text(if (m.isPublished) "Published" else "Draft", color = if (m.isPublished) DskSuccess else DskWarning, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        item {
            Text("Admin Activity Logs", style = MaterialTheme.typography.titleLarge)
        }

        if (adminState.logs.isEmpty()) {
            item {
                Text("No recent admin activity logs recorded yet.", color = DskMuted)
            }
        } else {
            items(adminState.logs.take(10), key = { it.id }) { log ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${log.action} • ${log.entityType.uppercase()}", style = MaterialTheme.typography.labelMedium, color = DskNeonViolet)
                        Text(log.details, style = MaterialTheme.typography.bodyMedium)
                        Text(log.adminEmail, style = MaterialTheme.typography.labelSmall, color = DskMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(label: String, value: String, modifier: Modifier) {
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = DskRubyGlow)
            Text(label, style = MaterialTheme.typography.labelSmall, color = DskSilver)
        }
    }
}

@Composable
private fun AnalyticsBar(label: String, count: Int, total: Int, color: androidx.compose.ui.graphics.Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$count", style = MaterialTheme.typography.labelMedium, color = color)
        }
        LinearProgressIndicator(
            progress = { (count.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = DskSlateSurface
        )
    }
}
