package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.github.ApkBuildDiagnosticsEngine
import com.example.data.github.ApkBuilderUiState
import com.example.data.github.DiagnosticSeverity
import com.example.data.model.AdminDashboardStats
import com.example.data.model.AdminRegistrationFormValidator
import com.example.data.model.StreamSourceType
import com.example.data.repository.CatalogSnapshot
import com.example.data.supabase.SmartUrlImporter
import com.example.data.supabase.SupabaseConfig
import com.example.ui.screens.AdminAccessDeniedScreen
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AdminLoginScreen
import com.example.ui.screens.AdminRegisterScreen
import com.example.ui.screens.ApkBuilderScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.DskVideoTheme
import com.example.ui.viewmodel.AdminState
import com.example.ui.viewmodel.AuthState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp")
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `app branding and package configuration match DSK VIDEO`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("DSK VIDEO", context.getString(R.string.app_name))
        assertEquals("com.dskvideo.ott", context.packageName)
        assertEquals("https://ottlzprymsdfgbgsrmxn.supabase.co", SupabaseConfig.baseUrl)
    }

    @Test
    fun `smart url importer detects stream types and youtube ids`() {
        val importer = SmartUrlImporter()
        assertEquals(StreamSourceType.MP4, importer.detectSourceType("https://cdn.example.com/movie.mp4"))
        assertEquals(StreamSourceType.HLS, importer.detectSourceType("https://live.example.com/stream.m3u8"))
        assertEquals(StreamSourceType.YOUTUBE, importer.detectSourceType("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertEquals("dQw4w9WgXcQ", SmartUrlImporter.extractYoutubeVideoId("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun `admin stats aggregate total videos accurately`() {
        val stats = AdminDashboardStats(
            totalUsers = 50,
            totalMovies = 10,
            totalWebSeries = 4,
            totalEpisodes = 24,
            totalLiveChannels = 8,
            totalSports = 6,
            totalKids = 5
        )
        assertEquals(45, stats.totalVideos)
    }

    @Test
    fun `apk diagnostics engine detects missing config and passes valid setup`() {
        val missingResults = ApkBuildDiagnosticsEngine.evaluateRepositoryFiles(
            packageJsonText = null,
            capacitorConfigText = "appId: 'bad'",
            workflowYmlText = "name: Empty",
            appBuildGradleText = null,
            failedSteps = listOf("Build Debug APK")
        )
        assertTrue(missingResults.any { it.title.contains("Missing package.json") && it.severity == DiagnosticSeverity.ERROR })
        assertTrue(missingResults.any { it.title.contains("Invalid package ID") && it.severity == DiagnosticSeverity.ERROR })
        assertTrue(missingResults.any { it.title.contains("Missing Android SDK") && it.severity == DiagnosticSeverity.ERROR })
        assertTrue(missingResults.any { it.title.contains("Build step failed: Build Debug APK") && it.severity == DiagnosticSeverity.ERROR })

        val validResults = ApkBuildDiagnosticsEngine.evaluateRepositoryFiles(
            packageJsonText = """
                {
                  "scripts": {"build": "node scripts/prepare-web-dist.mjs"},
                  "dependencies": {"@capacitor/core": "^6.2.0", "@capacitor/cli": "^6.2.0", "@capacitor/android": "^6.2.0"}
                }
            """.trimIndent(),
            capacitorConfigText = "const config = { appId: 'com.dskvideo.ott', webDir: 'dist' };",
            workflowYmlText = "uses: android-actions/setup-android@v3\nANDROID_KEYSTORE_BASE64\nANDROID_KEYSTORE_PASSWORD\nANDROID_KEY_ALIAS\nANDROID_KEY_PASSWORD",
            appBuildGradleText = "applicationId = \"com.dskvideo.ott\""
        )
        assertTrue(validResults.all { it.severity == DiagnosticSeverity.PASS })
    }

    @Test
    fun `non admin users are blocked from admin dashboard`() {
        composeRule.setContent {
            DskVideoTheme {
                AdminDashboardScreen(
                    isAdminAuthorized = false,
                    adminState = AdminState(),
                    catalog = CatalogSnapshot(),
                    onBack = {},
                    onRefresh = {},
                    onOpenApkBuilder = {},
                    onInspectSmartUrl = {},
                    onClearSmartUrl = {},
                    onSaveMovie = { _, _ -> },
                    onToggleMoviePublish = {},
                    onDeleteMovie = {},
                    onSaveSeries = { _, _ -> },
                    onDeleteSeries = {},
                    onAddSeason = { _, _, _ -> },
                    onDeleteSeason = {},
                    onSaveEpisode = { _, _ -> },
                    onDeleteEpisode = {},
                    onSaveChannel = { _, _ -> },
                    onDeleteChannel = {},
                    onSaveSports = { _, _ -> },
                    onDeleteSports = {},
                    onSaveKids = { _, _ -> },
                    onDeleteKids = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Admin Access Required").assertIsDisplayed()
    }

    @Test
    fun `auth screen displays backend setup box when key missing and sign in controls`() {
        composeRule.setContent {
            DskVideoTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    isBusy = false,
                    errorMessage = null,
                    isBackendConfigured = false,
                    onSignIn = { _, _, _ -> },
                    onSignUp = { _, _, _ -> },
                    onSendReset = {},
                    onResendVerification = {},
                    onBackToSignIn = {},
                    onOpenAdminLogin = {},
                    onClearError = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("backend_anon_key_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("backend_key_save_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Sign In to Watch").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("open_admin_login_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `apk builder screen shows real disconnected status and controls`() {
        composeRule.setContent {
            DskVideoTheme {
                ApkBuilderScreen(
                    uiState = ApkBuilderUiState(),
                    onConnectAndInspect = { _, _, _, _ -> },
                    onRebuildApk = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("DSK VIDEO APK BUILDER").assertIsDisplayed()
        composeRule.onNodeWithText("Disconnected").assertIsDisplayed()
        composeRule.onNodeWithTag("connect_github_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin registration form validator validates fields without requiring secret code`() {
        assertNotNull(AdminRegistrationFormValidator.validate("", "admin@dsk.com", "StrongPass1", "StrongPass1"))
        assertNotNull(AdminRegistrationFormValidator.validate("Admin User", "invalid-email", "StrongPass1", "StrongPass1"))
        assertNotNull(AdminRegistrationFormValidator.validate("Admin User", "admin@dsk.com", "short", "short"))
        assertNotNull(AdminRegistrationFormValidator.validate("Admin User", "admin@dsk.com", "StrongPass1", "Mismatch1"))
        assertNull(AdminRegistrationFormValidator.validate("Admin User", "admin@dsk.com", "StrongPass1", "StrongPass1"))
    }

    @Test
    fun `backend key guard rejects service role keys`() {
        assertTrue(SupabaseConfig.isLikelyServiceRoleKey("sb_secret_abcdef"))
        assertFalse(SupabaseConfig.isLikelyServiceRoleKey("sb_publishable_abcdef"))
    }

    @Test
    fun `admin register screen renders four fields with no secret code required`() {
        composeRule.setContent {
            DskVideoTheme {
                AdminRegisterScreen(
                    isBusy = false,
                    errorMessage = null,
                    isBackendConfigured = true,
                    onRegisterAdmin = { _, _, _, _, _ -> },
                    onOpenAdminLogin = {},
                    onClearError = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_reg_full_name_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_reg_email_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_reg_password_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_reg_confirm_password_input").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("admin_reg_secret_code_input").assertDoesNotExist()
        composeRule.onNodeWithTag("admin_register_submit").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin login screen exposes forgot password and admin registration entry`() {
        composeRule.setContent {
            DskVideoTheme {
                AdminLoginScreen(
                    isBusy = false,
                    errorMessage = null,
                    isBackendConfigured = true,
                    onAdminSignIn = { _, _ -> },
                    onSendReset = {},
                    onActivateAdmin = { _, _, _ -> },
                    onOpenAdminRegister = {},
                    onClearError = {},
                    onBack = {}
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("admin_forgot_password_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("open_admin_register_button").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `admin access denied screen blocks unauthorized route access`() {
        composeRule.setContent {
            DskVideoTheme {
                AdminAccessDeniedScreen(onBack = {})
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Admin Access Required").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_access_denied_return").assertIsDisplayed()
    }
}
