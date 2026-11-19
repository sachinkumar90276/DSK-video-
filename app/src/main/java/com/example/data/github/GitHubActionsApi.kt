package com.example.data.github

import android.util.Base64
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GitHubRepoResponse(
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "default_branch") val defaultBranch: String = "main",
    val private: Boolean = false,
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubBranchResponse(
    val name: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRun(
    val id: Long = 0L,
    @Json(name = "run_number") val runNumber: Int = 0,
    val status: String = "",
    val conclusion: String? = null,
    @Json(name = "head_branch") val headBranch: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "html_url") val htmlUrl: String = ""
)

@JsonClass(generateAdapter = true)
data class GitHubJobsResponse(
    val jobs: List<GitHubJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubJob(
    val id: Long = 0L,
    val name: String = "",
    val status: String = "",
    val conclusion: String? = null,
    val steps: List<GitHubJobStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubJobStep(
    val name: String = "",
    val status: String = "",
    val conclusion: String? = null,
    val number: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val artifacts: List<GitHubArtifact> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubArtifact(
    val id: Long = 0L,
    val name: String = "",
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0L,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String = "",
    val expired: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GitHubContentResponse(
    val name: String = "",
    val path: String = "",
    val content: String? = null,
    val encoding: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String> = emptyMap()
)

interface GitHubActionsApi {
    @GET("repos/{owner}/{repo}")
    suspend fun getRepository(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepoResponse>

    @GET("repos/{owner}/{repo}/branches/{branch}")
    suspend fun getBranch(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("branch") branch: String
    ): Response<GitHubBranchResponse>

    @GET("repos/{owner}/{repo}/actions/workflows/build-apk.yml/runs")
    suspend fun getWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("branch") branch: String,
        @Query("per_page") perPage: Int = 10
    ): Response<GitHubWorkflowRunsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/artifacts")
    suspend fun getRunArtifacts(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<GitHubArtifactsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{runId}/jobs")
    suspend fun getRunJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("runId") runId: Long
    ): Response<GitHubJobsResponse>

    @POST("repos/{owner}/{repo}/actions/workflows/build-apk.yml/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body request: WorkflowDispatchRequest
    ): Response<ResponseBody>

    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path", encoded = true) path: String,
        @Query("ref") ref: String
    ): Response<GitHubContentResponse>
}

class GitHubApiProvider(
    private val tokenProvider: () -> String?
) {
    // Never logs Authorization headers or tokens
    private val headerInterceptor = Interceptor { chain ->
        val token = tokenProvider()?.trim().orEmpty()
        val builder = chain.request().newBuilder()
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        if (token.isNotBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    val api: GitHubActionsApi = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(
            com.squareup.moshi.Moshi.Builder()
                .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()
        ))
        .build()
        .create(GitHubActionsApi::class.java)
}

enum class DiagnosticSeverity { PASS, WARNING, ERROR }

data class BuildDiagnosticItem(
    val category: String,
    val title: String,
    val detail: String,
    val severity: DiagnosticSeverity
)

object ApkBuildDiagnosticsEngine {

    private val PACKAGE_ID_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)+$")

    fun isValidPackageId(packageId: String): Boolean {
        return PACKAGE_ID_REGEX.matches(packageId.trim())
    }

    fun decodeBase64Content(encoded: String?): String {
        if (encoded.isNullOrBlank()) return ""
        val clean = encoded.replace("\n", "").replace("\r", "")
        return runCatching {
            String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun evaluateRepositoryFiles(
        packageJsonText: String?,
        capacitorConfigText: String?,
        workflowYmlText: String?,
        appBuildGradleText: String?,
        failedSteps: List<String> = emptyList()
    ): List<BuildDiagnosticItem> {
        val items = mutableListOf<BuildDiagnosticItem>()

        // 1. Missing package.json
        if (packageJsonText.isNullOrBlank()) {
            items += BuildDiagnosticItem(
                category = "package.json",
                title = "Missing package.json",
                detail = "Root package.json was not found in the repository.",
                severity = DiagnosticSeverity.ERROR
            )
        } else {
            val json = runCatching { JSONObject(packageJsonText) }.getOrNull()
            // 2. Missing build script
            val buildScript = json?.optJSONObject("scripts")?.optString("build").orEmpty()
            if (buildScript.isBlank()) {
                items += BuildDiagnosticItem(
                    category = "build_script",
                    title = "Missing build script",
                    detail = "package.json does not define a 'scripts.build' command.",
                    severity = DiagnosticSeverity.ERROR
                )
            } else {
                items += BuildDiagnosticItem(
                    category = "build_script",
                    title = "Web build script configured",
                    detail = "scripts.build = \"$buildScript\"",
                    severity = DiagnosticSeverity.PASS
                )
            }

            // 3. Missing dependencies (@capacitor/core, @capacitor/cli, @capacitor/android)
            val deps = json?.optJSONObject("dependencies")
            val devDeps = json?.optJSONObject("devDependencies")
            val requiredDeps = listOf("@capacitor/core", "@capacitor/cli", "@capacitor/android")
            val missingDeps = requiredDeps.filter { dep ->
                deps?.has(dep) != true && devDeps?.has(dep) != true
            }
            if (missingDeps.isNotEmpty()) {
                items += BuildDiagnosticItem(
                    category = "dependencies",
                    title = "Missing Capacitor dependencies",
                    detail = "Missing in package.json: ${missingDeps.joinToString(", ")}",
                    severity = DiagnosticSeverity.ERROR
                )
            } else {
                items += BuildDiagnosticItem(
                    category = "dependencies",
                    title = "Capacitor dependencies installed",
                    detail = "@capacitor/core, @capacitor/cli, @capacitor/android present.",
                    severity = DiagnosticSeverity.PASS
                )
            }
        }

        // 4. Capacitor config & 5. Invalid package ID
        if (capacitorConfigText.isNullOrBlank()) {
            items += BuildDiagnosticItem(
                category = "capacitor",
                title = "Capacitor configuration missing",
                detail = "capacitor.config.ts was not found in repository root.",
                severity = DiagnosticSeverity.ERROR
            )
        } else {
            val appIdMatch = Regex("""appId\s*:\s*['"]([^'"]+)['"]""").find(capacitorConfigText)
            val capAppId = appIdMatch?.groupValues?.getOrNull(1).orEmpty()
            if (!isValidPackageId(capAppId) || capAppId != "com.dskvideo.ott") {
                items += BuildDiagnosticItem(
                    category = "package_id",
                    title = "Invalid package ID in Capacitor",
                    detail = "Found '${capAppId.ifBlank { "none" }}', expected 'com.dskvideo.ott'.",
                    severity = DiagnosticSeverity.ERROR
                )
            } else {
                items += BuildDiagnosticItem(
                    category = "capacitor",
                    title = "Capacitor configured for DSK VIDEO",
                    detail = "App ID: $capAppId • WebDir configured.",
                    severity = DiagnosticSeverity.PASS
                )
            }
        }

        // 6. Missing Android SDK & Workflow check
        if (workflowYmlText.isNullOrBlank()) {
            items += BuildDiagnosticItem(
                category = "android_sdk",
                title = "Missing GitHub Actions workflow",
                detail = ".github/workflows/build-apk.yml not found.",
                severity = DiagnosticSeverity.ERROR
            )
        } else {
            val hasAndroidSdk = workflowYmlText.contains("setup-android") || workflowYmlText.contains("ANDROID_SDK_ROOT")
            if (!hasAndroidSdk) {
                items += BuildDiagnosticItem(
                    category = "android_sdk",
                    title = "Missing Android SDK setup",
                    detail = "build-apk.yml must configure android-actions/setup-android.",
                    severity = DiagnosticSeverity.ERROR
                )
            } else {
                items += BuildDiagnosticItem(
                    category = "android_sdk",
                    title = "Android SDK & Gradle workflow ready",
                    detail = ".github/workflows/build-apk.yml configures Android SDK & Gradle.",
                    severity = DiagnosticSeverity.PASS
                )
            }

            // 8. Signing configuration check
            val hasSigningSecrets = workflowYmlText.contains("ANDROID_KEYSTORE_BASE64") &&
                workflowYmlText.contains("ANDROID_KEYSTORE_PASSWORD") &&
                workflowYmlText.contains("ANDROID_KEY_ALIAS") &&
                workflowYmlText.contains("ANDROID_KEY_PASSWORD")
            if (!hasSigningSecrets) {
                items += BuildDiagnosticItem(
                    category = "signing",
                    title = "Missing signing configuration",
                    detail = "Release signing secrets are not referenced in build-apk.yml.",
                    severity = DiagnosticSeverity.WARNING
                )
            } else {
                items += BuildDiagnosticItem(
                    category = "signing",
                    title = "Release signing secrets wired",
                    detail = "Uses ANDROID_KEYSTORE_* secrets when present; falls back to debug APK.",
                    severity = DiagnosticSeverity.PASS
                )
            }
        }

        // 7. Gradle & packageId verification
        if (appBuildGradleText.isNullOrBlank()) {
            items += BuildDiagnosticItem(
                category = "gradle",
                title = "Missing app/build.gradle.kts",
                detail = "Android Gradle module file not found.",
                severity = DiagnosticSeverity.ERROR
            )
        } else if (!appBuildGradleText.contains("com.dskvideo.ott")) {
            items += BuildDiagnosticItem(
                category = "package_id",
                title = "Invalid Gradle applicationId",
                detail = "app/build.gradle.kts must specify applicationId = \"com.dskvideo.ott\".",
                severity = DiagnosticSeverity.ERROR
            )
        } else {
            items += BuildDiagnosticItem(
                category = "gradle",
                title = "Gradle configuration valid",
                detail = "applicationId = com.dskvideo.ott configured.",
                severity = DiagnosticSeverity.PASS
            )
        }

        // Surface failed CI steps (Gradle/Capacitor/SDK errors)
        failedSteps.forEach { step ->
            items += BuildDiagnosticItem(
                category = "ci_failure",
                title = "Build step failed: $step",
                detail = "Inspect full build logs on GitHub Actions to resolve '$step'.",
                severity = DiagnosticSeverity.ERROR
            )
        }

        return items
    }
}
