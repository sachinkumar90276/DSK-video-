package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.github.ApkBuilderUiState
import com.example.data.github.DiagnosticSeverity
import com.example.data.github.GitHubConnectionStatus
import com.example.ui.components.GlassCard
import com.example.ui.theme.*

@Composable
fun ApkBuilderScreen(
    uiState: ApkBuilderUiState,
    onConnectAndInspect: (owner: String, repo: String, branch: String, token: String?) -> Unit,
    onRebuildApk: (buildType: String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var owner by remember(uiState.config.owner) { mutableStateOf(uiState.config.owner) }
    var repo by remember(uiState.config.repo) { mutableStateOf(uiState.config.repo) }
    var branch by remember(uiState.config.branch) { mutableStateOf(uiState.config.branch.ifBlank { "main" }) }
    var tokenInput by remember { mutableStateOf("") }
    var selectedBuildType by remember { mutableStateOf("both") }

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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("apk_builder_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("DSK VIDEO APK BUILDER", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Automated GitHub Actions & Capacitor Pipeline", style = MaterialTheme.typography.labelSmall, color = DskNeonViolet)
                }
            }
            IconButton(
                onClick = { onConnectAndInspect(owner, repo, branch, tokenInput.takeIf { it.isNotBlank() }) },
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh Build Status")
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Connection & Status Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("GitHub Connection Status", style = MaterialTheme.typography.titleMedium)
                            val (statusText, statusColor) = when (uiState.connectionStatus) {
                                GitHubConnectionStatus.CONNECTED -> "Connected" to DskSuccess
                                GitHubConnectionStatus.CONNECTING -> "Connecting..." to DskWarning
                                GitHubConnectionStatus.ERROR -> "Error" to DskError
                                GitHubConnectionStatus.DISCONNECTED -> "Disconnected" to DskMuted
                            }
                            Surface(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                        .testTag("github_connection_badge")
                                )
                            }
                        }

                        Text(uiState.statusMessage, style = MaterialTheme.typography.bodyMedium, color = DskSilver)

                        OutlinedTextField(
                            value = owner,
                            onValueChange = { owner = it },
                            label = { Text("GitHub Owner / Username") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gh_owner_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = repo,
                                onValueChange = { repo = it },
                                label = { Text("Repository Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("gh_repo_input")
                            )
                            OutlinedTextField(
                                value = branch,
                                onValueChange = { branch = it },
                                label = { Text("Branch") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("gh_branch_input")
                            )
                        }
                        OutlinedTextField(
                            value = tokenInput,
                            onValueChange = { tokenInput = it },
                            label = {
                                Text(
                                    if (uiState.config.hasToken) "GitHub Token (Saved • enter to update)"
                                    else "GitHub Token (Required for Rebuild / Private Repo)"
                                )
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gh_token_input")
                        )

                        Button(
                            onClick = { onConnectAndInspect(owner, repo, branch, tokenInput.takeIf { it.isNotBlank() }) },
                            enabled = owner.isNotBlank() && repo.isNotBlank() && uiState.connectionStatus != GitHubConnectionStatus.CONNECTING,
                            colors = ButtonDefaults.buttonColors(containerColor = DskRoyalPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("connect_github_button")
                        ) {
                            Icon(Icons.Filled.Sync, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Connect & Inspect Repository")
                        }
                    }
                }
            }

            // Build Telemetry Card
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Build Telemetry", style = MaterialTheme.typography.titleLarge)
                        TelemetryRow("Repository Name", if (owner.isNotBlank() && repo.isNotBlank()) "$owner/$repo" else "—")
                        TelemetryRow("Current Branch", branch.ifBlank { "main" })
                        TelemetryRow("Latest Build Number", uiState.latestBuildNumber?.let { "#$it" } ?: "—")
                        TelemetryRow("Build Status", uiState.buildStatus)
                        TelemetryRow("Build Date", uiState.buildDate)
                        TelemetryRow("APK Version", uiState.apkVersion)
                        if (uiState.artifactName != null) {
                            val mb = String.format(java.util.Locale.US, "%.2f MB", uiState.artifactSizeBytes / (1024.0 * 1024.0))
                            TelemetryRow("Artifact", "${uiState.artifactName} ($mb)")
                        }
                    }
                }
            }

            // Action Buttons: Download APK, View Build Logs, Rebuild APK
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("APK Actions", style = MaterialTheme.typography.titleMedium)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("both" to "Debug + Release", "debug" to "Debug Only", "release" to "Release Only").forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedBuildType == key,
                                    onClick = { selectedBuildType = key },
                                    label = { Text(label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DskRoyalPurple,
                                        selectedLabelColor = DskWhite
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = { onRebuildApk(selectedBuildType) },
                            enabled = uiState.connectionStatus == GitHubConnectionStatus.CONNECTED && !uiState.isTriggeringBuild,
                            colors = ButtonDefaults.buttonColors(containerColor = DskCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("rebuild_apk_button")
                        ) {
                            Icon(Icons.Filled.Build, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (uiState.isTriggeringBuild) "Dispatching Build..." else "Rebuild APK")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    uiState.downloadPageUrl?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                enabled = uiState.buildConclusion == "success" && !uiState.downloadPageUrl.isNullOrBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = DskSuccess),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("download_apk_button")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Download APK")
                            }

                            OutlinedButton(
                                onClick = {
                                    uiState.logsUrl?.let { url ->
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                enabled = !uiState.logsUrl.isNullOrBlank(),
                                border = DskGradients.GlassBorderStroke,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("view_build_logs_button")
                            ) {
                                Icon(Icons.Filled.Terminal, contentDescription = null, tint = DskNeonViolet)
                                Spacer(Modifier.width(6.dp))
                                Text("View Build Logs", color = DskWhite)
                            }
                        }
                    }
                }
            }

            // Pre-Flight Diagnostics
            item {
                Text("Pre-Flight & Error Diagnostics", style = MaterialTheme.typography.titleLarge)
            }

            if (uiState.diagnostics.isEmpty()) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Connect to your GitHub repository to validate package.json, build scripts, Capacitor dependencies, Android SDK, Gradle config, package ID (com.dskvideo.ott), and release signing secrets.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DskMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(uiState.diagnostics, key = { "${it.category}_${it.title}" }) { item ->
                    val (icon, tint) = when (item.severity) {
                        DiagnosticSeverity.PASS -> Icons.Filled.CheckCircle to DskSuccess
                        DiagnosticSeverity.WARNING -> Icons.Filled.Warning to DskWarning
                        DiagnosticSeverity.ERROR -> Icons.Filled.Error to DskError
                    }
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = item.severity.name, tint = tint)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, color = DskWhite)
                                Text(item.detail, style = MaterialTheme.typography.bodyMedium, color = DskSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = DskMuted)
        Text(value, style = MaterialTheme.typography.labelLarge, color = DskWhite)
    }
}
