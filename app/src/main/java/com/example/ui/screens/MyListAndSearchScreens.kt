package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState
import com.example.ui.viewmodel.SearchResults

@Composable
fun MyListScreen(
    catalogState: CatalogState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onOpenMovie: (String) -> Unit,
    onOpenSeries: (String) -> Unit,
    onResumeProgress: (String) -> Unit,
    onRemoveWatchlist: (String, String, String, String) -> Unit,
    onOpenHistory: () -> Unit
) {
    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(title = "My List", onSearchClick = onSearchClick, onRefreshClick = onRefresh)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (snapshot != null && snapshot.watchProgress.isNotEmpty()) {
                item {
                    SectionHeader("Continue Watching", "History", onOpenHistory)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(snapshot.watchProgress, key = { it.contentId }) { wp ->
                            val ratio = if (wp.durationMs > 0) wp.positionMs.toFloat() / wp.durationMs else 0.1f
                            ContinueWatchingCard(
                                title = wp.title,
                                posterUrl = wp.posterUrl,
                                progressFraction = ratio,
                                subtitle = wp.contentType.uppercase(),
                                onClick = { onResumeProgress(wp.contentId) }
                            )
                        }
                    }
                }
            }

            item {
                SectionHeader("Saved Watchlist", "Watch History", onOpenHistory)
            }

            val items = snapshot?.watchlist.orEmpty()
            if (items.isEmpty()) {
                item {
                    CinemaEmptyState(
                        icon = Icons.Filled.BookmarkBorder,
                        title = "Your Watchlist Is Empty",
                        message = "Tap '+ My List' on any Movie or Web Series to save it here for instant access."
                    )
                }
            } else {
                items(items, key = { it.contentId }) { item ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("watchlist_item_${item.contentId}"),
                        onClick = {
                            if (item.contentType == "series") onOpenSeries(item.contentId)
                            else onOpenMovie(item.contentId)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.posterUrl.ifBlank { R.drawable.img_hero_fallback },
                                contentDescription = item.title,
                                placeholder = painterResource(R.drawable.img_hero_fallback),
                                error = painterResource(R.drawable.img_hero_fallback),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(72.dp)
                                    .height(96.dp)
                                    .clip(MaterialTheme.shapes.small)
                            )
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.contentType.uppercase(), style = MaterialTheme.typography.labelMedium, color = DskNeonViolet)
                            }
                            IconButton(
                                onClick = {
                                    onRemoveWatchlist(item.contentId, item.contentType, item.title, item.posterUrl)
                                },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove from My List", tint = DskSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WatchHistoryScreen(
    catalogState: CatalogState,
    onBack: () -> Unit,
    onPlayItem: (contentId: String, contentType: String) -> Unit
) {
    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached
    val history = snapshot?.watchHistory.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Watch History", style = MaterialTheme.typography.headlineMedium)
        }

        if (history.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.History,
                title = "No Watch History Yet",
                message = "Movies, episodes, live channels, sports, and kids videos you watch will be logged here."
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(history, key = { "${it.contentId}_${it.watchedAt}" }) { item ->
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onPlayItem(item.contentId, item.contentType) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = item.posterUrl.ifBlank { R.drawable.img_hero_fallback },
                                contentDescription = item.title,
                                placeholder = painterResource(R.drawable.img_hero_fallback),
                                error = painterResource(R.drawable.img_hero_fallback),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .width(96.dp)
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(item.contentType.uppercase(), style = MaterialTheme.typography.labelSmall, color = DskNeonViolet)
                            }
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Watch Again", tint = DskCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    query: String,
    results: SearchResults,
    recentSearches: List<String>,
    onQueryChange: (String) -> Unit,
    onCommitSearch: (String) -> Unit,
    onClearRecent: () -> Unit,
    onBack: () -> Unit,
    onMovieClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onPlayEpisode: (String) -> Unit,
    onPlayLive: (String) -> Unit,
    onPlaySports: (String) -> Unit,
    onPlayKids: (String) -> Unit
) {
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("search_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Movies, actors, directors, language, series...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onCommitSearch(query) }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("global_search_input")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (recentSearches.isNotEmpty() && query.isBlank()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Searches", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = onClearRecent) {
                            Text("Clear", color = DskCrimson)
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentSearches.forEach { term ->
                            AssistChip(
                                onClick = { onQueryChange(term) },
                                label = { Text(term) },
                                leadingIcon = { Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }

            if (results.suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) "Trending Suggestions" else "Suggestions",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        results.suggestions.forEach { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    onQueryChange(suggestion)
                                    onCommitSearch(suggestion)
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }
            }

            if (query.isNotBlank()) {
                val totalMatches = results.movies.size + results.series.size + results.episodes.size +
                    results.channels.size + results.sports.size + results.kids.size

                if (totalMatches == 0) {
                    item {
                        CinemaEmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = "No Results for \"$query\"",
                            message = "Try searching by movie title, actor, director, language, genre, web series, or episode name."
                        )
                    }
                }

                if (results.movies.isNotEmpty()) {
                    item { SectionHeader("Movies (${results.movies.size})") }
                    items(results.movies, key = { "sm_${it.id}" }) { m ->
                        SearchHitRow(m.title, "${m.releaseYear} • ${m.language} • ${m.genre}", m.posterUrl) {
                            onCommitSearch(query)
                            onMovieClick(m.id)
                        }
                    }
                }

                if (results.series.isNotEmpty()) {
                    item { SectionHeader("Web Series (${results.series.size})") }
                    items(results.series, key = { "ss_${it.id}" }) { s ->
                        SearchHitRow(s.title, "${s.language} • ${s.genre}", s.posterUrl) {
                            onCommitSearch(query)
                            onSeriesClick(s.id)
                        }
                    }
                }

                if (results.episodes.isNotEmpty()) {
                    item { SectionHeader("Episodes (${results.episodes.size})") }
                    items(results.episodes, key = { "se_${it.id}" }) { ep ->
                        SearchHitRow("E${ep.episodeNumber}: ${ep.title}", "${ep.durationMinutes} min", ep.thumbnailUrl) {
                            onCommitSearch(query)
                            onPlayEpisode(ep.id)
                        }
                    }
                }

                if (results.channels.isNotEmpty()) {
                    item { SectionHeader("Live TV (${results.channels.size})") }
                    items(results.channels, key = { "sc_${it.id}" }) { ch ->
                        SearchHitRow(ch.name, "${ch.category} • LIVE", ch.logoUrl) {
                            onCommitSearch(query)
                            onPlayLive(ch.id)
                        }
                    }
                }

                if (results.sports.isNotEmpty()) {
                    item { SectionHeader("Sports (${results.sports.size})") }
                    items(results.sports, key = { "ssp_${it.id}" }) { sp ->
                        SearchHitRow(sp.title, "${sp.sportType} • ${sp.category}", sp.thumbnailUrl) {
                            onCommitSearch(query)
                            onPlaySports(sp.id)
                        }
                    }
                }

                if (results.kids.isNotEmpty()) {
                    item { SectionHeader("Kids (${results.kids.size})") }
                    items(results.kids, key = { "sk_${it.id}" }) { kd ->
                        SearchHitRow(kd.title, "${kd.category} • ${kd.ageRating}", kd.posterUrl) {
                            onCommitSearch(query)
                            onPlayKids(kd.id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchHitRow(
    title: String,
    subtitle: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = imageUrl.ifBlank { R.drawable.img_hero_fallback },
                contentDescription = title,
                placeholder = painterResource(R.drawable.img_hero_fallback),
                error = painterResource(R.drawable.img_hero_fallback),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(72.dp)
                    .height(54.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DskMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DskNeonViolet)
        }
    }
}
