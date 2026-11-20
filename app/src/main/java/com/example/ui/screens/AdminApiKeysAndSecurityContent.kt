package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.ApiKeysConfig
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.AdminPasswordUiState
import com.example.ui.viewmodel.ApiKeysUiState

@Composable
fun AdminApiKeysHubContent(
    apiKeysState: ApiKeysUiState,
    onSaveAll: (ApiKeysConfig) -> Unit,
    onTestService: (service: String, config: ApiKeysConfig) -> Unit
) {
    var draft by remember(apiKeysState.config) { mutableStateOf(apiKeysState.config) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_api_keys_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.VpnKey, contentDescription = null, tint = DskNeonViolet)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("All-in-One API Key Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Configure & test Supabase, YouTube, TMDB, Firebase, GitHub APK Builder, and Media CDN keys right inside Admin Studio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DskSilver
                            )
                        }
                    }
                    Button(
                        onClick = { onSaveAll(draft) },
                        enabled = !apiKeysState.isSaving,
                        colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_all_api_keys_top")
                    ) {
                        if (apiKeysState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DskWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save All API Keys & Activate", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 1. Supabase Backend
        item {
            ApiKeySectionCard(
                title = "1. Supabase Backend (Auth & Catalog)",
                subtitle = "Public Project URL and Anon/Publishable Key (never Service Role key).",
                serviceKey = "supabase",
                apiKeysState = apiKeysState,
                onTest = { onTestService("supabase", draft) }
            ) {
                OutlinedTextField(
                    value = draft.supabaseUrl,
                    onValueChange = { draft = draft.copy(supabaseUrl = it) },
                    label = { Text("Supabase Project URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_supabase_url_input")
                )
                SecretKeyField(
                    value = draft.supabaseAnonKey,
                    onValueChange = { draft = draft.copy(supabaseAnonKey = it) },
                    label = "Supabase Public Anon Key",
                    testTag = "api_supabase_anon_input"
                )
            }
        }

        // 2. YouTube Data API v3
        item {
            ApiKeySectionCard(
                title = "2. YouTube Data API v3",
                subtitle = "Enables full HD thumbnails, descriptions & video duration lookup in Smart URL Import.",
                serviceKey = "youtube",
                apiKeysState = apiKeysState,
                onTest = { onTestService("youtube", draft) }
            ) {
                SecretKeyField(
                    value = draft.youtubeApiKey,
                    onValueChange = { draft = draft.copy(youtubeApiKey = it) },
                    label = "YouTube Data API v3 Key",
                    testTag = "api_youtube_key_input"
                )
            }
        }

        // 3. TMDB API
        item {
            ApiKeySectionCard(
                title = "3. TMDB Metadata API",
                subtitle = "Powers one-tap Movie & Web Series poster, banner & synopsis search inside Admin Editors.",
                serviceKey = "tmdb",
                apiKeysState = apiKeysState,
                onTest = { onTestService("tmdb", draft) }
            ) {
                SecretKeyField(
                    value = draft.tmdbApiKey,
                    onValueChange = { draft = draft.copy(tmdbApiKey = it) },
                    label = "TMDB API Key (v3) or Bearer Token (v4)",
                    testTag = "api_tmdb_key_input"
                )
            }
        }

        // 4. Firebase Integration
        item {
            ApiKeySectionCard(
                title = "4. Firebase / Google Cloud Config",
                subtitle = "Store & verify your Firebase Project ID, Web API Key, and App ID.",
                serviceKey = "firebase",
                apiKeysState = apiKeysState,
                onTest = { onTestService("firebase", draft) }
            ) {
                OutlinedTextField(
                    value = draft.firebaseProjectId,
                    onValueChange = { draft = draft.copy(firebaseProjectId = it) },
                    label = { Text("Firebase / GCP Project ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_firebase_project_input")
                )
                SecretKeyField(
                    value = draft.firebaseApiKey,
                    onValueChange = { draft = draft.copy(firebaseApiKey = it) },
                    label = "Firebase Web API Key",
                    testTag = "api_firebase_key_input"
                )
                OutlinedTextField(
                    value = draft.firebaseAppId,
                    onValueChange = { draft = draft.copy(firebaseAppId = it) },
                    label = { Text("Firebase App ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_firebase_app_id_input")
                )
            }
        }

        // 5. GitHub Actions APK Builder API
        item {
            ApiKeySectionCard(
                title = "5. GitHub Actions APK Builder API",
                subtitle = "Connects Admin APK Builder to dispatch workflows & inspect builds.",
                serviceKey = "github",
                apiKeysState = apiKeysState,
                onTest = { onTestService("github", draft) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = draft.githubOwner,
                        onValueChange = { draft = draft.copy(githubOwner = it) },
                        label = { Text("Owner") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("api_gh_owner_input")
                    )
                    OutlinedTextField(
                        value = draft.githubRepo,
                        onValueChange = { draft = draft.copy(githubRepo = it) },
                        label = { Text("Repo") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("api_gh_repo_input")
                    )
                }
                OutlinedTextField(
                    value = draft.githubBranch,
                    onValueChange = { draft = draft.copy(githubBranch = it) },
                    label = { Text("Branch") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_gh_branch_input")
                )
                SecretKeyField(
                    value = draft.githubToken,
                    onValueChange = { draft = draft.copy(githubToken = it) },
                    label = "GitHub Personal Access Token",
                    testTag = "api_gh_token_input"
                )
            }
        }

        // 6. Media CDN / Protected Stream Header
        item {
            ApiKeySectionCard(
                title = "6. Media CDN / Stream API Header",
                subtitle = "Automatically sent by ExoPlayer & Smart Importer for private CDN video streams.",
                serviceKey = "cdn",
                apiKeysState = apiKeysState,
                onTest = { onTestService("cdn", draft) }
            ) {
                OutlinedTextField(
                    value = draft.mediaCdnHeaderName,
                    onValueChange = { draft = draft.copy(mediaCdnHeaderName = it) },
                    label = { Text("HTTP Header Name (e.g. X-API-Key)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("api_cdn_header_input")
                )
                SecretKeyField(
                    value = draft.mediaCdnApiKey,
                    onValueChange = { draft = draft.copy(mediaCdnApiKey = it) },
                    label = "Media CDN / Stream API Key",
                    testTag = "api_cdn_key_input"
                )
            }
        }

        item {
            Button(
                onClick = { onSaveAll(draft) },
                enabled = !apiKeysState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_all_api_keys_bottom")
            ) {
                Text("Save All API Keys", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ApiKeySectionCard(
    title: String,
    subtitle: String,
    serviceKey: String,
    apiKeysState: ApiKeysUiState,
    onTest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val status = apiKeysState.statusByService[serviceKey]
    val isTesting = apiKeysState.testingService == serviceKey

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = DskWhite)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DskMuted)
            content()
            if (status != null) {
                val (ok, msg) = status
                Surface(
                    color = (if (ok) DskSuccess else DskError).copy(alpha = 0.16f),
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = if (ok) DskSuccess else DskError,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
            OutlinedButton(
                onClick = onTest,
                enabled = apiKeysState.testingService == null,
                border = DskGradients.GlassBorderStroke,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("test_api_${serviceKey}_button")
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = DskNeonViolet, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.NetworkCheck, contentDescription = null, tint = DskNeonViolet)
                    Spacer(Modifier.width(8.dp))
                    Text("Save & Test Connection", color = DskWhite)
                }
            }
        }
    }
}

@Composable
private fun SecretKeyField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    testTag: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = "Toggle $label Visibility"
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun AdminSecuritySettingsContent(
    adminEmail: String,
    passwordState: AdminPasswordUiState,
    onChangeAdminPassword: (currentPassword: String, newPassword: String, confirmPassword: String) -> Unit,
    onClearFeedback: () -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var showCurrent by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.AdminPanelSettings, contentDescription = null, tint = DskRubyGlow, modifier = Modifier.size(36.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Active Admin Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(adminEmail.ifBlank { "Signed In Admin" }, style = MaterialTheme.typography.bodyMedium, color = DskSilver)
                        Text("Password changes sync both in-app vault and Supabase Auth.", style = MaterialTheme.typography.labelSmall, color = DskNeonViolet)
                    }
                }
            }
        }

        item {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Change Admin Password", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Update your Admin Portal login password directly from inside the app.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DskSilver
                    )

                    OutlinedTextField(
                        value = currentPass,
                        onValueChange = { currentPass = it; localError = null; onClearFeedback() },
                        label = { Text("Current Admin Password") },
                        leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showCurrent = !showCurrent }) {
                                Icon(if (showCurrent) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle Current Password")
                            }
                        },
                        visualTransformation = if (showCurrent) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_current_password_input")
                    )

                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it; localError = null; onClearFeedback() },
                        label = { Text("New Admin Password (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showNew = !showNew }) {
                                Icon(if (showNew) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle New Password")
                            }
                        },
                        visualTransformation = if (showNew) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_new_password_input")
                    )

                    OutlinedTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it; localError = null; onClearFeedback() },
                        label = { Text("Confirm New Password") },
                        leadingIcon = { Icon(Icons.Filled.LockReset, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirm = !showConfirm }) {
                                Icon(if (showConfirm) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = "Toggle Confirm Password")
                            }
                        },
                        visualTransformation = if (showConfirm) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_confirm_new_password_input")
                    )

                    val err = localError ?: passwordState.errorMessage
                    if (!err.isNullOrBlank()) {
                        Surface(color = DskError.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
                            Text(err, color = DskError, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                        }
                    }

                    if (!passwordState.successMessage.isNullOrBlank()) {
                        Surface(color = DskSuccess.copy(alpha = 0.16f), shape = MaterialTheme.shapes.small) {
                            Text(passwordState.successMessage, color = DskSuccess, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(10.dp))
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                currentPass.isBlank() -> localError = "Enter your current password."
                                newPass.length < 6 -> localError = "New password must be at least 6 characters."
                                newPass != confirmPass -> localError = "New password and confirmation do not match."
                                else -> onChangeAdminPassword(currentPass, newPass, confirmPass)
                            }
                        },
                        enabled = !passwordState.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("change_admin_password_submit")
                    ) {
                        if (passwordState.isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DskWhite, strokeWidth = 2.dp)
                        } else {
                            Text("Update Admin Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
