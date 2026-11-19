package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.MovieItem
import com.example.ui.components.CinemaEmptyState
import com.example.ui.components.DskTopBar
import com.example.ui.components.LoadingSkeletonSection
import com.example.ui.components.PosterCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState

@Composable
fun MoviesScreen(
    catalogState: CatalogState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onMovieClick: (String) -> Unit
) {
    val filters = listOf(
        "All", "Hindi", "Bollywood", "Hollywood", "South Indian", "Punjabi",
        "Action", "Comedy", "Thriller", "Romance", "Horror", "Documentary"
    )
    var selectedFilter by remember { mutableStateOf("All") }
    var inlineSearch by remember { mutableStateOf("") }

    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    val allMovies = remember(snapshot) {
        snapshot?.movies?.filter { it.isPublished }.orEmpty()
    }

    val filteredMovies = remember(allMovies, selectedFilter, inlineSearch) {
        allMovies.filter { m ->
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Hindi", "Punjabi" -> m.language.equals(selectedFilter, true) || m.category.equals(selectedFilter, true)
                "Bollywood", "Hollywood" -> m.category.equals(selectedFilter, true)
                "South Indian" -> m.category.contains("South", true) || m.language in listOf("Tamil", "Telugu", "Malayalam", "Kannada")
                else -> m.genre.contains(selectedFilter, true)
            }
            val matchesQuery = inlineSearch.isBlank() ||
                m.title.contains(inlineSearch, true) ||
                m.castMembers.contains(inlineSearch, true) ||
                m.director.contains(inlineSearch, true)
            matchesFilter && matchesQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(
            title = "Movies",
            onSearchClick = onSearchClick,
            onRefreshClick = onRefresh
        )

        OutlinedTextField(
            value = inlineSearch,
            onValueChange = { inlineSearch = it },
            placeholder = { Text("Filter movies by title, actor, director...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("movies_inline_search")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskCrimson,
                        selectedLabelColor = DskWhite
                    ),
                    modifier = Modifier.testTag("movie_filter_${filter.lowercase().replace(" ", "_")}")
                )
            }
        }

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
        } else if (filteredMovies.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.Movie,
                title = "No Movies Found",
                message = "No published movies match '$selectedFilter'. Refresh catalog or add movies from the Admin Panel.",
                actionLabel = "Reset Filters",
                onAction = {
                    selectedFilter = "All"
                    inlineSearch = ""
                }
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 136.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredMovies, key = { it.id }) { movie: MovieItem ->
                    PosterCard(
                        title = movie.title,
                        posterUrl = movie.posterUrl,
                        badgeText = "★ ${movie.imdbRating}",
                        subtitle = "${movie.releaseYear} • ${movie.language}",
                        width = 160.dp,
                        height = 210.dp,
                        testTag = "movie_grid_card_${movie.id}",
                        onClick = { onMovieClick(movie.id) }
                    )
                }
            }
        }
    }
}
