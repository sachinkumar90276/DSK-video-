package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.navigation.BottomNavTab
import com.example.ui.navigation.DskRoute
import com.example.ui.theme.*

@Composable
fun DskBrandMark(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (compact) 30.dp else 38.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(DskGradients.BrandAccent),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(if (compact) 16.dp else 20.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.14f)
                    lineTo(size.width * 0.88f, size.height * 0.5f)
                    lineTo(size.width * 0.22f, size.height * 0.86f)
                    close()
                }
                drawPath(path = path, color = DskWhite)
            }
        }
        Column {
            Text(
                text = "DSK VIDEO",
                style = if (compact) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.2.sp
            )
            if (!compact) {
                Text(
                    text = "A DSK OFFICIAL PRODUCTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = DskNeonViolet,
                    letterSpacing = 1.6.sp
                )
            }
        }
    }
}

@Composable
fun DskTopBar(
    title: String? = null,
    onSearchClick: () -> Unit,
    onAdminClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (title == null) {
            DskBrandMark(compact = true)
        } else {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (onRefreshClick != null) {
                IconButton(
                    onClick = onRefreshClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("refresh_catalog_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refresh Catalog",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onAdminClick != null) {
                IconButton(
                    onClick = onAdminClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("admin_portal_shortcut")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = DskNeonViolet
                    )
                }
            }
            IconButton(
                onClick = onSearchClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("global_search_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search DSK VIDEO",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = MaterialTheme.shapes.medium,
        color = DskGlassFill,
        border = DskGradients.GlassBorderStroke,
        tonalElevation = 4.dp
    ) {
        Column(content = content)
    }
}

@Composable
fun PosterCard(
    title: String,
    posterUrl: String,
    badgeText: String? = null,
    subtitle: String? = null,
    width: Dp = 128.dp,
    height: Dp = 186.dp,
    testTag: String = "poster_card",
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(width)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .clip(MaterialTheme.shapes.medium)
                .background(DskSurfaceElevated)
        ) {
            AsyncImage(
                model = posterUrl.ifBlank { R.drawable.img_hero_fallback },
                contentDescription = title,
                placeholder = painterResource(R.drawable.img_hero_fallback),
                error = painterResource(R.drawable.img_hero_fallback),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC07070B))
                        )
                    )
            )
            if (!badgeText.isNullOrBlank()) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = DskCrimson.copy(alpha = 0.92f)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = DskWhite,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DskMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContinueWatchingCard(
    title: String,
    posterUrl: String,
    progressFraction: Float,
    subtitle: String,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .width(210.dp)
            .testTag("continue_watching_card"),
        onClick = onClick
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(118.dp)
        ) {
            AsyncImage(
                model = posterUrl.ifBlank { R.drawable.img_hero_fallback },
                contentDescription = title,
                placeholder = painterResource(R.drawable.img_hero_fallback),
                error = painterResource(R.drawable.img_hero_fallback),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x55000000)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = DskCrimson.copy(alpha = 0.9f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Resume $title",
                        tint = DskWhite,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.BottomCenter),
                color = DskCrimson,
                trackColor = Color(0x55FFFFFF)
            )
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = DskMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(DskGradients.BrandAccent)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = DskNeonViolet
                )
            }
        }
    }
}

@Composable
fun LoadingSkeletonSection() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(MaterialTheme.shapes.large)
                .background(DskSlateSurface.copy(alpha = alpha))
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(5) {
                Box(
                    modifier = Modifier
                        .width(128.dp)
                        .height(186.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(DskSlateSurface.copy(alpha = alpha))
                )
            }
        }
    }
}

@Composable
fun CinemaEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = DskRoyalPurple.copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = DskNeonViolet,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = DskMuted
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun tabIcons(tab: BottomNavTab): Pair<ImageVector, ImageVector> = when (tab) {
    BottomNavTab.HOME -> Icons.Filled.Home to Icons.Outlined.Home
    BottomNavTab.MOVIES -> Icons.Filled.Movie to Icons.Outlined.Movie
    BottomNavTab.WEB_SERIES -> Icons.Filled.Tv to Icons.Outlined.Tv
    BottomNavTab.LIVE_TV -> Icons.Filled.LiveTv to Icons.Outlined.LiveTv
    BottomNavTab.SPORTS -> Icons.Filled.SportsCricket to Icons.Outlined.SportsCricket
    BottomNavTab.KIDS -> Icons.Filled.ChildCare to Icons.Outlined.ChildCare
    BottomNavTab.MY_LIST -> Icons.Filled.Bookmark to Icons.Outlined.BookmarkBorder
    BottomNavTab.PROFILE -> Icons.Filled.Person to Icons.Outlined.PersonOutline
}

@Composable
fun DskBottomNavigationBar(
    currentRoute: DskRoute,
    onTabSelected: (BottomNavTab) -> Unit
) {
    Surface(
        color = Color(0xEB0B0B12),
        border = BorderStroke(0.8.dp, DskGlassBorder),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                val (activeIcon, inactiveIcon) = tabIcons(tab)
                val tint by animateColorAsState(
                    if (selected) DskWhite else DskMuted,
                    label = "tab_tint"
                )
                Column(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected) {
                                Brush.horizontalGradient(
                                    listOf(DskCrimson.copy(alpha = 0.28f), DskRoyalPurple.copy(alpha = 0.28f))
                                )
                            } else {
                                Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                        )
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag(tab.testTag),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) activeIcon else inactiveIcon,
                        contentDescription = tab.label,
                        tint = if (selected) DskRubyGlow else tint,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun DskNavigationRail(
    currentRoute: DskRoute,
    onTabSelected: (BottomNavTab) -> Unit
) {
    NavigationRail(
        containerColor = Color(0xFF0A0A10),
        contentColor = DskWhite,
        modifier = Modifier.navigationBarsPadding()
    ) {
        BottomNavTab.entries.forEach { tab ->
            val selected = currentRoute == tab.route
            val (activeIcon, inactiveIcon) = tabIcons(tab)
            NavigationRailItem(
                selected = selected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (selected) activeIcon else inactiveIcon,
                        contentDescription = tab.label
                    )
                },
                label = { Text(tab.label, maxLines = 1) },
                modifier = Modifier.testTag(tab.testTag),
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = DskWhite,
                    selectedTextColor = DskWhite,
                    indicatorColor = DskCrimson.copy(alpha = 0.35f),
                    unselectedIconColor = DskMuted,
                    unselectedTextColor = DskMuted
                )
            )
        }
    }
}

@Composable
fun PinEntryDialog(
    title: String,
    subtitle: String,
    confirmLabel: String = "Confirm",
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DskSurfaceElevated,
        titleContentColor = DskWhite,
        textContentColor = DskSilver,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) pin = it },
                    label = { Text("4-6 Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field")
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(errorMessage, color = DskError, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                modifier = Modifier.testTag("pin_confirm_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DskSilver)
            }
        }
    )
}
