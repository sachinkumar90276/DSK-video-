package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.data.model.*
import com.example.data.repository.CatalogSnapshot
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    catalogState: CatalogState,
    isAdmin: Boolean,
    onRefresh: () -> Unit,
    onSearchClick: () -> Unit,
    onAdminClick: () -> Unit,
    onMovieClick: (String) -> Unit,
    onSeriesClick: (String) -> Unit,
    onPlayMovie: (String) -> Unit,
    onPlayLiveChannel: (String) -> Unit,
    onPlaySports: (String) -> Unit,
    onPlayKids: (String) -> Unit,
    onResumeProgress: (String) -> Unit,
    onToggleWatchlist: (String, String, String, String) -> Unit,
    onExploreSection: (String) -> Unit
) {
    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(
            onSearchClick = onSearchClick,
            onAdminClick = if (isAdmin) onAdminClick else null,
            onRefreshClick = onRefresh
        )

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
            return
        }

        val snap = snapshot ?: CatalogSnapshot()
        val publishedMovies = remember(snap.movies) { snap.movies.filter { it.isPublished } }
        val publishedSeries = remember(snap.webSeries) { snap.webSeries.filter { it.isPublished } }
        val publishedChannels = remember(snap.liveChannels) { snap.liveChannels.filter { it.isPublished } }
        val publishedSports = remember(snap.sports) { snap.sports.filter { it.isPublished } }
        val publishedKids = remember(snap.kids) { snap.kids.filter { it.isPublished } }

        val featuredList = remember(publishedMovies) {
            publishedMovies.filter { it.isFeatured }.ifEmpty { publishedMovies.take(5) }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (snap.isFromCache) {
                item {
                    Surface(
                        color = DskRoyalPurple.copy(alpha = 0.22f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = "Offline-friendly cached view active. Tap refresh to sync latest catalog.",
                            style = MaterialTheme.typography.labelMedium,
                            color = DskSilver,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // 1. Featured Hero Banner
            item {
                FeaturedHeroBanner(
                    featuredMovies = featuredList,
                    watchlistIds = snap.watchlist.map { it.contentId }.toSet(),
                    onPlay = onPlayMovie,
                    onDetails = onMovieClick,
                    onToggleWatchlist = { m ->
                        onToggleWatchlist(m.id, "movie", m.title, m.posterUrl)
                    }
                )
            }

            if (publishedMovies.isEmpty() && publishedSeries.isEmpty() && publishedChannels.isEmpty()) {
                item {
                    CinemaEmptyState(
                        icon = Icons.Filled.MovieFilter,
                        title = "Catalog Ready for Content",
                        message = "Your DSK VIDEO backend is connected. Publish Movies, Web Series, Live TV, Sports, or Kids titles from the Admin Panel to light up every homepage row.",
                        actionLabel = if (isAdmin) "Open Admin Panel" else "Refresh Catalog",
                        onAction = if (isAdmin) onAdminClick else onRefresh
                    )
                }
            }

            // 2. Continue Watching
            if (snap.watchProgress.isNotEmpty()) {
                item {
                    SectionHeader("Continue Watching")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(snap.watchProgress, key = { it.contentId }) { wp ->
                            val fraction = if (wp.durationMs > 0) {
                                wp.positionMs.toFloat() / wp.durationMs.toFloat()
                            } else 0.15f
                            ContinueWatchingCard(
                                title = wp.title,
                                posterUrl = wp.posterUrl,
                                progressFraction = fraction,
                                subtitle = wp.contentType.uppercase(),
                                onClick = { onResumeProgress(wp.contentId) }
                            )
                        }
                    }
                }
            }

            // 3. Trending Movies
            val trending = publishedMovies.filter { it.isTrending }.ifEmpty { publishedMovies.take(10) }
            if (trending.isNotEmpty()) {
                item {
                    MovieShelfRow("Trending Movies", trending, onMovieClick) {
                        onExploreSection("movies")
                    }
                }
            }

            // 4. Popular Movies
            val popular = publishedMovies.filter { it.isPopular }.ifEmpty { publishedMovies.sortedByDescending { it.imdbRating }.take(10) }
            if (popular.isNotEmpty()) {
                item { MovieShelfRow("Popular Movies", popular, onMovieClick) }
            }

            // 5. Latest Releases
            val latest = publishedMovies.filter { it.isLatest }.ifEmpty { publishedMovies.sortedByDescending { it.releaseYear }.take(10) }
            if (latest.isNotEmpty()) {
                item { MovieShelfRow("Latest Releases", latest, onMovieClick) }
            }

            // 6. New on DSK VIDEO
            if (publishedMovies.isNotEmpty()) {
                item { MovieShelfRow("New on DSK VIDEO", publishedMovies.take(12), onMovieClick) }
            }

            // 7. Recommended for You
            val recommended = publishedMovies.sortedByDescending { it.imdbRating }.take(10)
            if (recommended.isNotEmpty()) {
                item { MovieShelfRow("Recommended for You", recommended, onMovieClick) }
            }

            // 8. Popular Web Series
            if (publishedSeries.isNotEmpty()) {
                item {
                    SectionHeader("Popular Web Series", "See All") { onExploreSection("series") }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publishedSeries, key = { it.id }) { series ->
                            PosterCard(
                                title = series.title,
                                posterUrl = series.posterUrl,
                                badgeText = "SERIES",
                                subtitle = "${series.language} • ${series.genre}",
                                onClick = { onSeriesClick(series.id) }
                            )
                        }
                    }
                }
            }

            // 9-20. Language, Industry & Genre Sections
            val languageAndGenreShelves = listOf(
                "Hindi Movies" to publishedMovies.filter { it.language.equals("Hindi", true) },
                "Bollywood" to publishedMovies.filter { it.category.equals("Bollywood", true) || it.language.equals("Hindi", true) },
                "Hollywood" to publishedMovies.filter { it.category.equals("Hollywood", true) || it.language.equals("English", true) },
                "South Indian Movies" to publishedMovies.filter {
                    it.category.contains("South", true) || it.language in listOf("Tamil", "Telugu", "Malayalam", "Kannada")
                },
                "Punjabi Movies" to publishedMovies.filter { it.language.equals("Punjabi", true) || it.category.equals("Punjabi", true) },
                "Comedy" to publishedMovies.filter { it.genre.contains("Comedy", true) },
                "Action" to publishedMovies.filter { it.genre.contains("Action", true) },
                "Thriller" to publishedMovies.filter { it.genre.contains("Thriller", true) },
                "Romance" to publishedMovies.filter { it.genre.contains("Romance", true) },
                "Horror" to publishedMovies.filter { it.genre.contains("Horror", true) },
                "Documentary" to publishedMovies.filter { it.genre.contains("Documentary", true) }
            )

            languageAndGenreShelves.forEach { (sectionTitle, items) ->
                if (items.isNotEmpty()) {
                    item(key = "shelf_$sectionTitle") {
                        MovieShelfRow(sectionTitle, items, onMovieClick)
                    }
                }
            }

            // 21. Kids Entertainment
            if (publishedKids.isNotEmpty()) {
                item {
                    SectionHeader("Kids Entertainment", "Kids Hub") { onExploreSection("kids") }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publishedKids, key = { it.id }) { kid ->
                            PosterCard(
                                title = kid.title,
                                posterUrl = kid.posterUrl,
                                badgeText = kid.ageRating,
                                subtitle = kid.category,
                                onClick = { onPlayKids(kid.id) }
                            )
                        }
                    }
                }
            }

            // 22. Live TV Channels
            if (publishedChannels.isNotEmpty()) {
                item {
                    SectionHeader("Live TV Channels", "All Channels") { onExploreSection("live") }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publishedChannels, key = { it.id }) { channel ->
                            GlassCard(
                                modifier = Modifier
                                    .width(164.dp)
                                    .testTag("home_live_card_${channel.id}"),
                                onClick = { onPlayLiveChannel(channel.id) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                ) {
                                    AsyncImage(
                                        model = channel.logoUrl.ifBlank { R.drawable.img_hero_fallback },
                                        contentDescription = channel.name,
                                        placeholder = painterResource(R.drawable.img_hero_fallback),
                                        error = painterResource(R.drawable.img_hero_fallback),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = DskLivePulse,
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(6.dp)
                                    ) {
                                        Text(
                                            text = "LIVE",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DskWhite,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(channel.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                                    Text("${channel.category} • ${channel.language}", style = MaterialTheme.typography.labelSmall, color = DskMuted, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // 23. Sports Highlights
            if (publishedSports.isNotEmpty()) {
                item {
                    SectionHeader("Sports Highlights", "Sports Arena") { onExploreSection("sports") }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(publishedSports, key = { it.id }) { sp ->
                            PosterCard(
                                title = sp.title,
                                posterUrl = sp.thumbnailUrl.ifBlank { sp.bannerUrl },
                                badgeText = if (sp.isLive) "LIVE" else sp.sportType.uppercase(),
                                subtitle = sp.category,
                                width = 185.dp,
                                height = 115.dp,
                                onClick = { onPlaySports(sp.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeaturedHeroBanner(
    featuredMovies: List<MovieItem>,
    watchlistIds: Set<String>,
    onPlay: (String) -> Unit,
    onDetails: (String) -> Unit,
    onToggleWatchlist: (MovieItem) -> Unit
) {
    var index by remember(featuredMovies) { mutableIntStateOf(0) }
    LaunchedEffect(featuredMovies) {
        if (featuredMovies.size > 1) {
            while (true) {
                delay(5500)
                index = (index + 1) % featuredMovies.size
            }
        }
    }

    val hero = featuredMovies.getOrNull(index)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(340.dp)
            .padding(horizontal = 16.dp)
            .clip(MaterialTheme.shapes.large)
            .background(DskSurfaceElevated)
            .clickable(enabled = hero != null) { hero?.let { onDetails(it.id) } }
            .testTag("featured_hero_banner")
    ) {
        AsyncImage(
            model = hero?.bannerUrl?.ifBlank { hero.posterUrl } ?: R.drawable.img_hero_fallback,
            contentDescription = hero?.title ?: "DSK VIDEO Featured Showcase",
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = DskCrimson.copy(alpha = 0.9f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = if (hero != null) "FEATURED • ${hero.genre.uppercase()}" else "DSK VIDEO ORIGINALS",
                    style = MaterialTheme.typography.labelSmall,
                    color = DskWhite,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                text = hero?.title ?: "Entertainment Without Limits",
                style = MaterialTheme.typography.headlineLarge,
                color = DskWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (hero != null) {
                Text(
                    text = "${hero.releaseYear} • ${hero.language} • ${hero.durationMinutes}m • IMDb ${hero.imdbRating}",
                    style = MaterialTheme.typography.labelMedium,
                    color = DskSilver
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onPlay(hero.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = DskWhite, contentColor = DskPitchBlack),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_play_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Play Now", fontWeight = FontWeight.Bold)
                    }
                    val inList = hero.id in watchlistIds
                    OutlinedButton(
                        onClick = { onToggleWatchlist(hero) },
                        border = DskGradients.GlassBorderStroke,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hero_watchlist_button")
                    ) {
                        Icon(
                            if (inList) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = "My List",
                            tint = DskWhite
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (inList) "In List" else "My List", color = DskWhite)
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieShelfRow(
    title: String,
    movies: List<MovieItem>,
    onMovieClick: (String) -> Unit,
    onSeeAll: (() -> Unit)? = null
) {
    Column {
        SectionHeader(title, actionLabel = if (onSeeAll != null) "See All" else null, onActionClick = onSeeAll)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies, key = { "${title}_${it.id}" }) { movie ->
                PosterCard(
                    title = movie.title,
                    posterUrl = movie.posterUrl,
                    badgeText = if (movie.imdbRating > 0) "★ ${movie.imdbRating}" else null,
                    subtitle = "${movie.releaseYear} • ${movie.language}",
                    onClick = { onMovieClick(movie.id) }
                )
            }
        }
    }
}
