package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.MovieItem
import com.example.data.model.WatchProgressItem
import com.example.ui.components.GlassCard
import com.example.ui.components.PosterCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.*

@Composable
fun MovieDetailScreen(
    movie: MovieItem?,
    relatedMovies: List<MovieItem>,
    watchProgress: WatchProgressItem?,
    inWatchlist: Boolean,
    onBack: () -> Unit,
    onPlayMovie: (String) -> Unit,
    onPlayTrailer: (String) -> Unit,
    onToggleWatchlist: (MovieItem) -> Unit,
    onRelatedClick: (String) -> Unit
) {
    val context = LocalContext.current

    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DskPitchBlack),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Movie not found", style = MaterialTheme.typography.titleLarge)
                Button(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val resumeFraction = if (watchProgress != null && watchProgress.durationMs > 0) {
        (watchProgress.positionMs.toFloat() / watchProgress.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

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
                    .height(340.dp)
            ) {
                AsyncImage(
                    model = movie.bannerUrl.ifBlank { movie.posterUrl },
                    contentDescription = movie.title,
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
                        .testTag("movie_detail_back")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = DskWhite
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = movie.posterUrl.ifBlank { R.drawable.img_hero_fallback },
                        contentDescription = "${movie.title} poster",
                        placeholder = painterResource(R.drawable.img_hero_fallback),
                        error = painterResource(R.drawable.img_hero_fallback),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(96.dp)
                            .height(140.dp)
                            .clip(MaterialTheme.shapes.medium)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = movie.title,
                            style = MaterialTheme.typography.headlineMedium,
                            color = DskWhite
                        )
                        Text(
                            text = "${movie.releaseYear} • ${movie.language} • ${movie.durationMinutes} min",
                            style = MaterialTheme.typography.labelMedium,
                            color = DskSilver
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                color = DskGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "IMDb ${movie.imdbRating}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DskGold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Surface(
                                color = DskRoyalPurple.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = movie.genre,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = DskNeonViolet,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (resumeFraction > 0.01f) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Continue Watching (${(resumeFraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = DskRubyGlow
                        )
                        LinearProgressIndicator(
                            progress = { resumeFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = DskCrimson,
                            trackColor = DskSlateSurface
                        )
                    }
                }

                Button(
                    onClick = { onPlayMovie(movie.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("play_movie_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (resumeFraction > 0.02f) "Resume Movie" else "Play Movie",
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (movie.trailerUrl.isNotBlank()) {
                        OutlinedButton(
                            onClick = { onPlayTrailer(movie.id) },
                            border = DskGradients.GlassBorderStroke,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("watch_trailer_button")
                        ) {
                            Icon(Icons.Filled.Theaters, contentDescription = null, tint = DskNeonViolet)
                            Spacer(Modifier.width(6.dp))
                            Text("Trailer", color = DskWhite)
                        }
                    }
                    OutlinedButton(
                        onClick = { onToggleWatchlist(movie) },
                        border = DskGradients.GlassBorderStroke,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("toggle_watchlist_button")
                    ) {
                        Icon(
                            if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = null,
                            tint = DskWhite
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (inWatchlist) "In My List" else "My List", color = DskWhite)
                    }
                    OutlinedButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch '${movie.title}' (${movie.releaseYear}) on DSK VIDEO - Entertainment Without Limits!"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Movie"))
                        },
                        border = DskGradients.GlassBorderStroke,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("share_movie_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share Movie", tint = DskWhite)
                    }
                }

                if (movie.description.isNotBlank()) {
                    Text(
                        text = movie.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = DskSilver
                    )
                }

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (movie.castMembers.isNotBlank()) {
                            DetailRow("Cast", movie.castMembers)
                        }
                        if (movie.director.isNotBlank()) {
                            DetailRow("Director", movie.director)
                        }
                        DetailRow("Category", movie.category)
                        DetailRow(
                            "Subtitles",
                            if (movie.subtitleUrl.isNotBlank()) "Available (CC)" else "Standard Audio"
                        )
                    }
                }
            }
        }

        if (relatedMovies.isNotEmpty()) {
            item {
                SectionHeader("Related Movies")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(relatedMovies, key = { it.id }) { related ->
                        PosterCard(
                            title = related.title,
                            posterUrl = related.posterUrl,
                            badgeText = "★ ${related.imdbRating}",
                            subtitle = related.language,
                            onClick = { onRelatedClick(related.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelLarge,
            color = DskMuted,
            modifier = Modifier.width(86.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = DskWhite
        )
    }
}
