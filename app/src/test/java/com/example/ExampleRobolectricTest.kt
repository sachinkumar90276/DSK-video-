package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AdminDashboardStats
import com.example.data.model.StreamSourceType
import com.example.data.repository.CatalogSnapshot
import com.example.data.supabase.SmartUrlImporter
import com.example.data.supabase.SupabaseConfig
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.DskVideoTheme
import com.example.ui.viewmodel.AdminState
import com.example.ui.viewmodel.AuthState
import org.junit.Assert.assertEquals
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
    fun `non admin users are blocked from admin dashboard`() {
        composeRule.setContent {
            DskVideoTheme {
                AdminDashboardScreen(
                    isAdminAuthorized = false,
                    adminState = AdminState(),
                    catalog = CatalogSnapshot(),
                    onBack = {},
                    onRefresh = {},
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
    fun `auth screen displays sign in and separate admin portal option`() {
        composeRule.setContent {
            DskVideoTheme {
                AuthScreen(
                    authState = AuthState.SignedOut,
                    isBusy = false,
                    errorMessage = null,
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
        composeRule.onNodeWithText("Sign In to Watch").assertIsDisplayed()
        composeRule.onNodeWithTag("open_admin_login_button").performScrollTo().assertIsDisplayed()
    }
}
