package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonItem
import com.example.data.model.WatchProgressItem
import com.example.data.model.WebSeriesItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState

@Composable
fun WebSeriesCatalogScreen(
    catalogState: CatalogState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onSeriesClick: (String) -> Unit
) {
    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    val seriesList = remember(snapshot) {
        snapshot?.webSeries?.filter { it.isPublished }.orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(
            title = "Web Series",
            onSearchClick = onSearchClick,
            onRefreshClick = onRefresh
        )

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
        } else if (seriesList.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.Tv,
                title = "No Web Series Published",
                message = "Web series with unlimited seasons and episodes added in the Admin Panel will appear here.",
                actionLabel = "Refresh",
                onAction = onRefresh
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 140.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(seriesList, key = { it.id }) { series ->
                    PosterCard(
                        title = series.title,
                        posterUrl = series.posterUrl,
                        badgeText = "SERIES",
                        subtitle = "${series.releaseYear} • ${series.language}",
                        width = 160.dp,
                        height = 210.dp,
                        testTag = "series_card_${series.id}",
                        onClick = { onSeriesClick(series.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun WebSeriesDetailScreen(
    series: WebSeriesItem?,
    seasons: List<SeasonItem>,
    episodes: List<EpisodeItem>,
    watchProgress: List<WatchProgressItem>,
    inWatchlist: Boolean,
    onBack: () -> Unit,
    onPlayEpisode: (String) -> Unit,
    onToggleWatchlist: (WebSeriesItem) -> Unit
) {
    if (series == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DskPitchBlack),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = onBack) { Text("Series Not Found • Back") }
        }
        return
    }

    val sortedSeasons = remember(seasons) { seasons.sortedBy { it.seasonNumber } }
    var selectedSeasonId by remember(sortedSeasons) {
        mutableStateOf(sortedSeasons.firstOrNull()?.id.orEmpty())
    }

    val seasonEpisodes = remember(episodes, selectedSeasonId) {
        if (selectedSeasonId.isBlank()) {
            episodes.sortedBy { it.episodeNumber }
        } else {
            episodes.filter { it.seasonId == selectedSeasonId }.sortedBy { it.episodeNumber }
        }
    }

    val lastWatchedEpisode = remember(episodes, watchProgress) {
        val epIds = episodes.map { it.id }.toSet()
        watchProgress.firstOrNull { it.contentId in epIds }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                AsyncImage(
                    model = series.bannerUrl.ifBlank { series.posterUrl },
                    contentDescription = series.title,
                    placeholder = painterResource(R.drawable.img_hero_fallback),
                    error = painterResource(R.drawable.img_hero_fallback),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DskGradients.HeroOverlay)
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .clip(CircleShape)
                        .background(DskGlassFill)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = DskWhite)
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(series.title, style = MaterialTheme.typography.headlineLarge, color = DskWhite)
                    Text(
                        text = "${series.releaseYear} • ${series.language} • ${series.genre} • IMDb ${series.imdbRating}",
                        style = MaterialTheme.typography.labelMedium,
                        color = DskSilver
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val primaryEpisode = lastWatchedEpisode?.contentId
                    ?: seasonEpisodes.firstOrNull()?.id
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { primaryEpisode?.let(onPlayEpisode) },
                        enabled = primaryEpisode != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("series_primary_play_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (lastWatchedEpisode != null) "Resume Episode" else "Watch S1 E1")
                    }
                    OutlinedButton(
                        onClick = { onToggleWatchlist(series) },
                        border = DskGradients.GlassBorderStroke,
                        modifier = Modifier.height(50.dp)
                    ) {
                        Icon(
                            if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = "My List",
                            tint = DskWhite
                        )
                    }
                }

                if (series.description.isNotBlank()) {
                    Text(series.description, style = MaterialTheme.typography.bodyLarge, color = DskSilver)
                }

                if (sortedSeasons.isNotEmpty()) {
                    SectionHeader("Seasons")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sortedSeasons.forEach { season ->
                            FilterChip(
                                selected = season.id == selectedSeasonId,
                                onClick = { selectedSeasonId = season.id },
                                label = { Text(season.title.ifBlank { "Season ${season.seasonNumber}" }) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DskRoyalPurple,
                                    selectedLabelColor = DskWhite
                                ),
                                modifier = Modifier.testTag("season_chip_${season.seasonNumber}")
                            )
                        }
                    }
                }
            }
        }

        if (seasonEpisodes.isEmpty()) {
            item {
                CinemaEmptyState(
                    icon = Icons.Filled.VideoLibrary,
                    title = "No Episodes Yet",
                    message = "Admin can add unlimited seasons and episodes for this series from the Admin Panel."
                )
            }
        } else {
            items(seasonEpisodes, key = { it.id }) { ep ->
                val progress = watchProgress.firstOrNull { it.contentId == ep.id }
                val progressRatio = if (progress != null && progress.durationMs > 0) {
                    (progress.positionMs.toFloat() / progress.durationMs.toFloat()).coerceIn(0f, 1f)
                } else 0f

                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("episode_card_${ep.episodeNumber}"),
                    onClick = { onPlayEpisode(ep.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(126.dp)
                                .height(74.dp)
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            AsyncImage(
                                model = ep.thumbnailUrl.ifBlank { series.bannerUrl.ifBlank { series.posterUrl } },
                                contentDescription = ep.title,
                                placeholder = painterResource(R.drawable.img_hero_fallback),
                                error = painterResource(R.drawable.img_hero_fallback),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Icon(
                                Icons.Filled.PlayCircleFilled,
                                contentDescription = "Play Episode",
                                tint = DskWhite.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                            if (progressRatio > 0.02f) {
                                LinearProgressIndicator(
                                    progress = { progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .align(Alignment.BottomCenter),
                                    color = DskCrimson
                                )
                            }
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "E${ep.episodeNumber} • ${ep.title}",
                                style = MaterialTheme.typography.titleMedium,
                                color = DskWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${ep.durationMinutes} min",
                                style = MaterialTheme.typography.labelMedium,
                                color = DskNeonViolet
                            )
                            if (ep.description.isNotBlank()) {
                                Text(
                                    text = ep.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DskMuted,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
