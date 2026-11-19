package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.AppSettingsState
import com.example.data.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.PinEntryDialog
import com.example.ui.theme.*
import com.example.ui.viewmodel.CatalogState
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    email: String,
    profile: UserProfile?,
    isAdmin: Boolean,
    settings: AppSettingsState,
    catalogState: CatalogState,
    onUpdateProfile: (username: String, language: String, avatarUri: Uri?) -> Unit,
    onChangePassword: (String) -> Unit,
    onSetProfilePin: (String?) -> Unit,
    onVerifyProfilePin: suspend (String) -> Boolean,
    onOpenWatchlist: () -> Unit,
    onOpenWatchHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdminPanel: () -> Unit,
    onOpenAdminLogin: () -> Unit,
    onLogout: () -> Unit
) {
    val snapshot = (catalogState as? CatalogState.Ready)?.snapshot
    var showEditDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showPinUnlockDialog by remember { mutableStateOf(false) }
    var pinLocked by remember(settings.hasProfilePin) { mutableStateOf(settings.hasProfilePin) }
    var pinError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onUpdateProfile(
                profile?.username?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@"),
                profile?.preferredLanguage ?: settings.preferredLanguage,
                uri
            )
        }
    }

    if (pinLocked && settings.hasProfilePin) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DskPitchBlack)
                .statusBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Lock, contentDescription = "Profile Locked", tint = DskNeonViolet, modifier = Modifier.size(44.dp))
                    Text("Profile PIN Protected", style = MaterialTheme.typography.headlineMedium)
                    Text("Unlock your DSK VIDEO profile with your PIN.", style = MaterialTheme.typography.bodyMedium, color = DskMuted)
                    Button(
                        onClick = { pinError = null; showPinUnlockDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                        modifier = Modifier.testTag("unlock_profile_button")
                    ) {
                        Text("Enter Profile PIN")
                    }
                }
            }
        }

        if (showPinUnlockDialog) {
            PinEntryDialog(
                title = "Unlock Profile",
                subtitle = "Enter your Profile PIN.",
                errorMessage = pinError,
                onDismiss = { showPinUnlockDialog = false },
                onConfirm = { pin ->
                    scope.launch {
                        if (onVerifyProfilePin(pin)) {
                            pinLocked = false
                            showPinUnlockDialog = false
                        } else {
                            pinError = "Invalid PIN"
                        }
                    }
                }
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Profile", style = MaterialTheme.typography.headlineLarge)
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(DskSlateSurface)
                            .clickable {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                            .testTag("upload_avatar_button")
                    ) {
                        AsyncImage(
                            model = profile?.avatarUrl?.takeIf { it.isNotBlank() } ?: R.drawable.dsk_icon_foreground,
                            contentDescription = "Profile Picture",
                            placeholder = painterResource(R.drawable.dsk_icon_foreground),
                            error = painterResource(R.drawable.dsk_icon_foreground),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Surface(
                            color = DskCrimson,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "Change photo", tint = DskWhite, modifier = Modifier.padding(4.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = profile?.username?.ifBlank { email.substringBefore("@") } ?: email.substringBefore("@"),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(email, style = MaterialTheme.typography.bodyMedium, color = DskMuted)
                        if (isAdmin) {
                            Surface(color = DskRoyalPurple.copy(alpha = 0.25f), shape = MaterialTheme.shapes.small) {
                                Text(
                                    text = "ADMIN AUTHORIZED",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DskNeonViolet,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_profile_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = DskWhite)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatChipCard("Watchlist", "${snapshot?.watchlist?.size ?: 0}", Modifier.weight(1f), onOpenWatchlist)
                StatChipCard("Continue", "${snapshot?.watchProgress?.size ?: 0}", Modifier.weight(1f), onOpenWatchlist)
                StatChipCard("History", "${snapshot?.watchHistory?.size ?: 0}", Modifier.weight(1f), onOpenWatchHistory)
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileActionRow(Icons.Filled.Bookmark, "My Watchlist", "Saved movies & web series", onOpenWatchlist, "profile_watchlist_row")
                    HorizontalDivider(color = DskGlassBorder)
                    ProfileActionRow(Icons.Filled.History, "Watch History", "Recently streamed titles", onOpenWatchHistory, "profile_history_row")
                    HorizontalDivider(color = DskGlassBorder)
                    ProfileActionRow(Icons.Filled.Password, "Change Password", "Update your account password", { showPasswordDialog = true }, "profile_change_password_row")
                    HorizontalDivider(color = DskGlassBorder)
                    ProfileActionRow(
                        Icons.Filled.Pin,
                        if (settings.hasProfilePin) "Profile PIN Lock (Active)" else "Enable Profile PIN Lock",
                        if (settings.hasProfilePin) "Tap to remove PIN" else "Protect profile with a 4-6 digit PIN",
                        {
                            if (settings.hasProfilePin) onSetProfilePin(null) else showPinSetupDialog = true
                        },
                        "profile_pin_row"
                    )
                    HorizontalDivider(color = DskGlassBorder)
                    ProfileActionRow(Icons.Filled.Settings, "App Settings", "Theme, Playback, Subtitles, Legal & Support", onOpenSettings, "profile_settings_row")
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                ProfileActionRow(
                    icon = Icons.Filled.AdminPanelSettings,
                    title = if (isAdmin) "Open Admin Panel" else "Admin Portal Login",
                    subtitle = if (isAdmin) "Manage movies, series, live TV, sports & kids" else "Separate administrator sign-in",
                    onClick = if (isAdmin) onOpenAdminPanel else onOpenAdminLogin,
                    testTag = "profile_admin_row"
                )
            }
        }

        item {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = DskSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("logout_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DskCrimson)
                Spacer(Modifier.width(8.dp))
                Text("Sign Out", color = DskCrimson, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditDialog) {
        var username by remember { mutableStateOf(profile?.username.orEmpty()) }
        var language by remember { mutableStateOf(profile?.preferredLanguage ?: "English") }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("Edit Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = language,
                        onValueChange = { language = it },
                        label = { Text("Preferred Language") },
                        singleLine = true
                    )
                    OutlinedButton(
                        onClick = {
                            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pick Profile Photo")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(username, language, null)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            containerColor = DskSurfaceElevated,
            title = { Text("Change Password") },
            text = {
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = { Text("New Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onChangePassword(newPass)
                        showPasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)
                ) { Text("Update") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showPinSetupDialog) {
        PinEntryDialog(
            title = "Set Profile PIN Lock",
            subtitle = "Protect your profile access with a 4-6 digit PIN.",
            confirmLabel = "Save PIN",
            onDismiss = { showPinSetupDialog = false },
            onConfirm = { pin ->
                onSetProfilePin(pin)
                showPinSetupDialog = false
            }
        )
    }
}

@Composable
private fun StatChipCard(label: String, count: String, modifier: Modifier, onClick: () -> Unit) {
    GlassCard(modifier = modifier, onClick = onClick) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, style = MaterialTheme.typography.headlineMedium, color = DskRubyGlow)
            Text(label, style = MaterialTheme.typography.labelSmall, color = DskMuted)
        }
    }
}

@Composable
private fun ProfileActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = null, tint = DskNeonViolet)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = DskMuted)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = DskMuted)
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettingsState,
    onUpdateSettings: ((AppSettingsState) -> AppSettingsState) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var infoModalTitle by remember { mutableStateOf<String?>(null) }
    var infoModalBody by remember { mutableStateOf("") }

    val languages = listOf("English", "Hindi", "Punjabi", "Tamil", "Telugu")
    val qualities = listOf("Auto", "1080p", "720p", "480p")

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
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Appearance & Language", style = MaterialTheme.typography.titleMedium, color = DskNeonViolet)
                        SettingSwitchRow(
                            title = "AMOLED Pitch Black Dark Theme",
                            checked = settings.amoledDarkTheme,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(amoledDarkTheme = checked) } }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Content Language", style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = {
                                val nextIdx = (languages.indexOf(settings.preferredLanguage) + 1) % languages.size
                                onUpdateSettings { it.copy(preferredLanguage = languages[nextIdx]) }
                            }) {
                                Text(settings.preferredLanguage, color = DskRubyGlow)
                            }
                        }
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Playback & Subtitles", style = MaterialTheme.typography.titleMedium, color = DskNeonViolet)
                        SettingSwitchRow(
                            title = "Auto-Play Next Episode",
                            checked = settings.autoPlayNext,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(autoPlayNext = checked) } }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Default Stream Quality", style = MaterialTheme.typography.bodyLarge)
                            TextButton(onClick = {
                                val nextIdx = (qualities.indexOf(settings.defaultQuality) + 1) % qualities.size
                                onUpdateSettings { it.copy(defaultQuality = qualities[nextIdx]) }
                            }) {
                                Text(settings.defaultQuality, color = DskRubyGlow)
                            }
                        }
                        SettingSwitchRow(
                            title = "Enable Subtitles by Default",
                            checked = settings.subtitlesEnabled,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(subtitlesEnabled = checked) } }
                        )
                        SettingSwitchRow(
                            title = "Notifications & New Releases",
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { checked -> onUpdateSettings { it.copy(notificationsEnabled = checked) } }
                        )
                    }
                }
            }

            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ProfileActionRow(Icons.Filled.PrivacyTip, "Privacy Policy", "How DSK VIDEO protects your account", {
                            infoModalTitle = "Privacy Policy"
                            infoModalBody = "DSK VIDEO collects only your account email, profile details, watchlist, and playback progress on your configured Supabase backend to synchronize your viewing experience across sessions. We never sell user data or serve third-party payment trackers."
                        }, "settings_privacy")
                        HorizontalDivider(color = DskGlassBorder)
                        ProfileActionRow(Icons.Filled.Gavel, "Terms & Conditions", "Platform usage & authorized streaming", {
                            infoModalTitle = "Terms & Conditions"
                            infoModalBody = "DSK VIDEO is a 100% free OTT streaming platform by DSK OFFICIAL. All streams, live channels, sports, and series are provided strictly for personal viewing from authorized media sources."
                        }, "settings_terms")
                        HorizontalDivider(color = DskGlassBorder)
                        ProfileActionRow(Icons.Filled.Info, "About DSK VIDEO", "A DSK OFFICIAL PRODUCTION", {
                            infoModalTitle = "About DSK VIDEO"
                            infoModalBody = "DSK VIDEO\nA DSK OFFICIAL PRODUCTION\nEntertainment Without Limits.\n\nPackage: com.dskvideo.ott\nFree OTT Streaming • Movies • Web Series • Live TV • Sports • Kids"
                        }, "settings_about")
                        HorizontalDivider(color = DskGlassBorder)
                        ProfileActionRow(Icons.Filled.SupportAgent, "Contact Support", "Reach DSK OFFICIAL Support", {
                            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@dskvideo.com")
                                putExtra(Intent.EXTRA_SUBJECT, "DSK VIDEO Support Request")
                            }
                            runCatching { context.startActivity(mailIntent) }.onFailure {
                                infoModalTitle = "Contact Support"
                                infoModalBody = "Email us anytime at support@dskvideo.com for assistance with your DSK VIDEO account or playback."
                            }
                        }, "settings_support")
                    }
                }
            }
        }
    }

    if (infoModalTitle != null) {
        AlertDialog(
            onDismissRequest = { infoModalTitle = null },
            containerColor = DskSurfaceElevated,
            title = { Text(infoModalTitle.orEmpty()) },
            text = { Text(infoModalBody, style = MaterialTheme.typography.bodyMedium, color = DskSilver) },
            confirmButton = {
                Button(onClick = { infoModalTitle = null }, colors = ButtonDefaults.buttonColors(containerColor = DskCrimson)) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingSwitchRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
