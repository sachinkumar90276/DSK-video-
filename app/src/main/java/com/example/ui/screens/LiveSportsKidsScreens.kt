package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.example.data.model.AppSettingsState
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState
import kotlinx.coroutines.launch

@Composable
fun LiveTvScreen(
    catalogState: CatalogState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onPlayChannel: (String) -> Unit
) {
    val categories = listOf("All", "News", "Entertainment", "Sports", "Music", "Regional")
    var selectedCategory by remember { mutableStateOf("All") }

    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    val channels = remember(snapshot, selectedCategory) {
        snapshot?.liveChannels
            ?.filter { it.isPublished && (selectedCategory == "All" || it.category.equals(selectedCategory, true)) }
            .orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(title = "Live TV", onSearchClick = onSearchClick, onRefreshClick = onRefresh)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskCrimson,
                        selectedLabelColor = DskWhite
                    ),
                    modifier = Modifier.testTag("live_category_$cat")
                )
            }
        }

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
        } else if (channels.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.LiveTv,
                title = "No Live Channels in $selectedCategory",
                message = "Admin can add authorized live HLS (.m3u8) or stream URLs in the Admin Panel.",
                actionLabel = "Refresh Channels",
                onAction = onRefresh
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(channels, key = { it.id }) { channel ->
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("live_channel_card_${channel.id}"),
                        onClick = { onPlayChannel(channel.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = channel.logoUrl.ifBlank { R.drawable.img_hero_fallback },
                                contentDescription = channel.name,
                                placeholder = painterResource(R.drawable.img_hero_fallback),
                                error = painterResource(R.drawable.img_hero_fallback),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DskWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (channel.isLive) {
                                        Surface(
                                            color = DskLivePulse,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "LIVE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DskWhite,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${channel.category} • ${channel.language}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = DskMuted
                                )
                            }
                            IconButton(onClick = { onPlayChannel(channel.id) }) {
                                Icon(
                                    Icons.Filled.PlayCircleFilled,
                                    contentDescription = "Watch ${channel.name}",
                                    tint = DskCrimson,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SportsScreen(
    catalogState: CatalogState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onPlaySports: (String) -> Unit
) {
    val categories = listOf("All", "Cricket", "Football", "Kabaddi", "Other Sports", "Highlights", "Match Replays")
    var selectedFilter by remember { mutableStateOf("All") }

    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    val sportsList = remember(snapshot, selectedFilter) {
        snapshot?.sports
            ?.filter { sp ->
                sp.isPublished && (
                    selectedFilter == "All" ||
                        sp.sportType.equals(selectedFilter, true) ||
                        sp.category.equals(selectedFilter, true)
                    )
            }
            .orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(title = "Sports", onSearchClick = onSearchClick, onRefreshClick = onRefresh)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_sports_banner),
                contentDescription = "DSK Sports Arena",
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
                    .padding(14.dp)
            ) {
                Text("DSK SPORTS ARENA", style = MaterialTheme.typography.labelMedium, color = DskRubyGlow)
                Text("Cricket, Football, Kabaddi & Match Replays", style = MaterialTheme.typography.titleLarge, color = DskWhite)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedFilter == cat,
                    onClick = { selectedFilter = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskRoyalPurple,
                        selectedLabelColor = DskWhite
                    ),
                    modifier = Modifier.testTag("sports_filter_$cat")
                )
            }
        }

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
        } else if (sportsList.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.SportsCricket,
                title = "No Sports Content in $selectedFilter",
                message = "Authorized sports streams, highlights, and match replays managed in the Admin Panel appear here.",
                actionLabel = "Refresh Sports",
                onAction = onRefresh
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(sportsList, key = { it.id }) { item ->
                    PosterCard(
                        title = item.title,
                        posterUrl = item.thumbnailUrl.ifBlank { item.bannerUrl },
                        badgeText = if (item.isLive) "LIVE" else item.sportType.uppercase(),
                        subtitle = "${item.category} • ${item.durationMinutes}m",
                        width = 190.dp,
                        height = 120.dp,
                        testTag = "sports_card_${item.id}",
                        onClick = { onPlaySports(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun KidsScreen(
    catalogState: CatalogState,
    settings: AppSettingsState,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onPlayKids: (String) -> Unit,
    onSetParentalPin: (String?) -> Unit,
    onVerifyParentalPin: suspend (String) -> Boolean
) {
    val categories = listOf("All", "Cartoons", "Animated Movies", "Kids Series", "Educational Videos")
    var selectedCategory by remember { mutableStateOf("All") }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showUnlockDialog by remember { mutableStateOf(false) }
    var pinError by remember { mutableStateOf<String?>(null) }

    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
        ?: (catalogState as? CatalogState.Error)?.cached

    val kidsItems = remember(snapshot, selectedCategory) {
        snapshot?.kids
            ?.filter { it.isPublished && (selectedCategory == "All" || it.category.equals(selectedCategory, true)) }
            .orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        DskTopBar(title = "DSK Kids", onSearchClick = onSearchClick, onRefreshClick = onRefresh)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(144.dp)
                .padding(horizontal = 16.dp)
                .clip(MaterialTheme.shapes.large)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_kids_banner),
                contentDescription = "DSK Kids Wonderland",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DskGradients.HeroOverlay)
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("SAFE KIDS WORLD", style = MaterialTheme.typography.labelMedium, color = DskGold)
                    Text("Cartoons, Learning & Animated Fun", style = MaterialTheme.typography.titleLarge, color = DskWhite)
                }
                FilledTonalButton(
                    onClick = {
                        pinError = null
                        if (settings.parentalLockEnabled) {
                            showUnlockDialog = true
                        } else {
                            showPinSetupDialog = true
                        }
                    },
                    modifier = Modifier.testTag("parental_lock_button")
                ) {
                    Icon(
                        if (settings.parentalLockEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen,
                        contentDescription = "Parental Lock"
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(if (settings.parentalLockEnabled) "Locked" else "Set Lock")
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DskRoyalPurple,
                        selectedLabelColor = DskWhite
                    ),
                    modifier = Modifier.testTag("kids_filter_$cat")
                )
            }
        }

        if (catalogState is CatalogState.Loading && snapshot == null) {
            LoadingSkeletonSection()
        } else if (kidsItems.isEmpty()) {
            CinemaEmptyState(
                icon = Icons.Filled.ChildCare,
                title = "No Kids Content in $selectedCategory",
                message = "Cartoons, Animated Movies, Kids Series, and Educational Videos added by Admin appear here.",
                actionLabel = "Refresh Kids Hub",
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
                items(kidsItems, key = { it.id }) { item ->
                    PosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl.ifBlank { item.bannerUrl },
                        badgeText = item.ageRating,
                        subtitle = "${item.category} • ${item.durationMinutes}m",
                        width = 160.dp,
                        height = 200.dp,
                        testTag = "kids_card_${item.id}",
                        onClick = { onPlayKids(item.id) }
                    )
                }
            }
        }
    }

    if (showPinSetupDialog) {
        PinEntryDialog(
            title = "Enable Kids Parental Lock",
            subtitle = "Set a 4-6 digit PIN to lock parental controls and keep Kids Mode protected.",
            confirmLabel = "Enable Lock",
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                onSetParentalPin(pin)
                showPinSetupDialog = false
            }
        )
    }

    if (showUnlockDialog) {
        val scope = rememberCoroutineScope()
        PinEntryDialog(
            title = "Disable Parental Lock",
            subtitle = "Enter your Parental PIN to unlock.",
            confirmLabel = "Unlock",
            errorMessage = pinError,
            onDismiss = { showUnlockDialog = false },
            onConfirm = { pin ->
                scope.launch {
                    if (onVerifyParentalPin(pin)) {
                        onSetParentalPin(null)
                        showUnlockDialog = false
                    } else {
                        pinError = "Incorrect PIN. Try again."
                    }
                }
            }
        )
    }
}
