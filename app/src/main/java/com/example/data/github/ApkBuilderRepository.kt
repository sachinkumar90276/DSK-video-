package com.example.data.github

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.apkBuilderDataStore by preferencesDataStore(name = "dsk_apk_builder_prefs")

data class GitHubRepoConfig(
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val hasToken: Boolean = false
)

enum class GitHubConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

data class ApkBuilderUiState(
    val config: GitHubRepoConfig = GitHubRepoConfig(),
    val connectionStatus: GitHubConnectionStatus = GitHubConnectionStatus.DISCONNECTED,
    val statusMessage: String = "Enter your GitHub Owner and Repository to connect.",
    val latestBuildNumber: Int? = null,
    val buildStatus: String = "Not Run",
    val buildConclusion: String? = null,
    val buildDate: String = "—",
    val apkVersion: String = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})",
    val artifactName: String? = null,
    val artifactSizeBytes: Long = 0L,
    val downloadPageUrl: String? = null,
    val logsUrl: String? = null,
    val diagnostics: List<BuildDiagnosticItem> = emptyList(),
    val isTriggeringBuild: Boolean = false
)

class ApkBuilderRepository(private val context: Context) {

    @Volatile
    private var activeToken: String = ""

    private val apiProvider = GitHubApiProvider { activeToken }
    private val api = apiProvider.api

    private object Keys {
        val OWNER = stringPreferencesKey("gh_owner")
        val REPO = stringPreferencesKey("gh_repo")
        val BRANCH = stringPreferencesKey("gh_branch")
        val TOKEN = stringPreferencesKey("gh_pat")
    }

    val configFlow: Flow<GitHubRepoConfig> = context.apkBuilderDataStore.data.map { prefs ->
        GitHubRepoConfig(
            owner = prefs[Keys.OWNER].orEmpty(),
            repo = prefs[Keys.REPO].orEmpty(),
            branch = prefs[Keys.BRANCH]?.ifBlank { "main" } ?: "main",
            hasToken = !prefs[Keys.TOKEN].isNullOrBlank()
        )
    }

    suspend fun loadSavedCredentials(): Pair<GitHubRepoConfig, String> {
        val prefs = context.apkBuilderDataStore.data.first()
        val token = prefs[Keys.TOKEN].orEmpty()
        activeToken = token
        return GitHubRepoConfig(
            owner = prefs[Keys.OWNER].orEmpty(),
            repo = prefs[Keys.REPO].orEmpty(),
            branch = prefs[Keys.BRANCH]?.ifBlank { "main" } ?: "main",
            hasToken = token.isNotBlank()
        ) to token
    }

    suspend fun saveConfig(owner: String, repo: String, branch: String, token: String?) {
        val cleanToken = token?.trim()
        context.apkBuilderDataStore.edit { prefs ->
            prefs[Keys.OWNER] = owner.trim()
            prefs[Keys.REPO] = repo.trim()
            prefs[Keys.BRANCH] = branch.trim().ifBlank { "main" }
            if (cleanToken != null) {
                if (cleanToken.isBlank()) prefs.remove(Keys.TOKEN)
                else prefs[Keys.TOKEN] = cleanToken
            }
        }
        if (cleanToken != null) {
            activeToken = cleanToken
        }
    }

    suspend fun inspectAndFetchStatus(
        owner: String,
        repo: String,
        branch: String,
        tokenOverride: String? = null
    ): ApkBuilderUiState = withContext(Dispatchers.IO) {
        val cleanOwner = owner.trim()
        val cleanRepo = repo.trim()
        val cleanBranch = branch.trim().ifBlank { "main" }
        if (tokenOverride != null) {
            activeToken = tokenOverride.trim()
        }

        if (cleanOwner.isBlank() || cleanRepo.isBlank()) {
            return@withContext ApkBuilderUiState(
                config = GitHubRepoConfig(cleanOwner, cleanRepo, cleanBranch, activeToken.isNotBlank()),
                connectionStatus = GitHubConnectionStatus.DISCONNECTED,
                statusMessage = "Repository owner and name are required."
            )
        }

        val repoRes = runCatching { api.getRepository(cleanOwner, cleanRepo) }.getOrElse { err ->
            return@withContext ApkBuilderUiState(
                config = GitHubRepoConfig(cleanOwner, cleanRepo, cleanBranch, activeToken.isNotBlank()),
                connectionStatus = GitHubConnectionStatus.ERROR,
                statusMessage = "Network error connecting to GitHub: ${err.message}"
            )
        }

        if (!repoRes.isSuccessful) {
            val hint = if (repoRes.code() == 404 || repoRes.code() == 401) {
                "Repository not found or private. Add a GitHub token with Actions & Contents permissions."
            } else {
                "GitHub API returned HTTP ${repoRes.code()}"
            }
            return@withContext ApkBuilderUiState(
                config = GitHubRepoConfig(cleanOwner, cleanRepo, cleanBranch, activeToken.isNotBlank()),
                connectionStatus = GitHubConnectionStatus.ERROR,
                statusMessage = hint
            )
        }

        val branchRes = runCatching { api.getBranch(cleanOwner, cleanRepo, cleanBranch) }.getOrNull()
        val branchExists = branchRes?.isSuccessful == true

        // Fetch repository files for real pre-flight diagnostics
        val packageJson = fetchFileText(cleanOwner, cleanRepo, "package.json", cleanBranch)
        val capConfig = fetchFileText(cleanOwner, cleanRepo, "capacitor.config.ts", cleanBranch)
        val workflowYml = fetchFileText(cleanOwner, cleanRepo, ".github/workflows/build-apk.yml", cleanBranch)
        val appGradle = fetchFileText(cleanOwner, cleanRepo, "app/build.gradle.kts", cleanBranch)

        // Fetch latest workflow runs
        val runsRes = runCatching { api.getWorkflowRuns(cleanOwner, cleanRepo, cleanBranch) }.getOrNull()
        val latestRun = runsRes?.body()?.workflowRuns?.firstOrNull()

        var artifact: GitHubArtifact? = null
        val failedSteps = mutableListOf<String>()

        if (latestRun != null) {
            val artifactsRes = runCatching {
                api.getRunArtifacts(cleanOwner, cleanRepo, latestRun.id)
            }.getOrNull()
            artifact = artifactsRes?.body()?.artifacts?.firstOrNull {
                it.name.contains("DSK-VIDEO-APK", ignoreCase = true) && !it.expired
            } ?: artifactsRes?.body()?.artifacts?.firstOrNull { !it.expired }

            if (latestRun.conclusion == "failure") {
                val jobsRes = runCatching {
                    api.getRunJobs(cleanOwner, cleanRepo, latestRun.id)
                }.getOrNull()
                jobsRes?.body()?.jobs?.forEach { job ->
                    job.steps.filter { it.conclusion == "failure" }.forEach { step ->
                        failedSteps += step.name
                    }
                }
            }
        }

        val diagnostics = ApkBuildDiagnosticsEngine.evaluateRepositoryFiles(
            packageJsonText = packageJson,
            capacitorConfigText = capConfig,
            workflowYmlText = workflowYml,
            appBuildGradleText = appGradle,
            failedSteps = failedSteps
        )

        val displayStatus = when {
            !branchExists -> "Branch '$cleanBranch' Not Found"
            latestRun == null -> "No Builds Yet"
            latestRun.status != "completed" -> latestRun.status.replace("_", " ").uppercase()
            else -> (latestRun.conclusion ?: "completed").uppercase()
        }

        ApkBuilderUiState(
            config = GitHubRepoConfig(cleanOwner, cleanRepo, cleanBranch, activeToken.isNotBlank()),
            connectionStatus = GitHubConnectionStatus.CONNECTED,
            statusMessage = "Connected to ${repoRes.body()?.fullName ?: "$cleanOwner/$cleanRepo"}",
            latestBuildNumber = latestRun?.runNumber,
            buildStatus = displayStatus,
            buildConclusion = latestRun?.conclusion,
            buildDate = latestRun?.updatedAt?.take(19)?.replace("T", " ") ?: "—",
            apkVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})",
            artifactName = artifact?.name,
            artifactSizeBytes = artifact?.sizeInBytes ?: 0L,
            downloadPageUrl = latestRun?.htmlUrl,
            logsUrl = latestRun?.htmlUrl,
            diagnostics = diagnostics
        )
    }

    suspend fun triggerRebuild(
        owner: String,
        repo: String,
        branch: String,
        buildType: String
    ): Result<String> = withContext(Dispatchers.IO) {
        if (activeToken.isBlank()) {
            return@withContext Result.failure(
                IllegalStateException("A GitHub Personal Access Token (Actions: Read & Write) is required to trigger Rebuild APK.")
            )
        }
        runCatching {
            val res = api.dispatchWorkflow(
                owner = owner.trim(),
                repo = repo.trim(),
                request = WorkflowDispatchRequest(
                    ref = branch.trim().ifBlank { "main" },
                    inputs = mapOf("build_type" to buildType)
                )
            )
            if (!res.isSuccessful) {
                throw IllegalStateException("GitHub Actions dispatch failed (HTTP ${res.code()}). Ensure token has 'workflow' / 'Actions: write' scope.")
            }
            "GitHub Actions APK build dispatched ($buildType) on branch '$branch'."
        }
    }

    private suspend fun fetchFileText(owner: String, repo: String, path: String, ref: String): String? {
        return runCatching {
            val res = api.getFileContent(owner, repo, path, ref)
            if (res.isSuccessful) {
                ApkBuildDiagnosticsEngine.decodeBase64Content(res.body()?.content)
            } else null
        }.getOrNull()
    }
}
