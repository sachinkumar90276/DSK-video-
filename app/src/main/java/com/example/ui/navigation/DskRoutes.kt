package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface DskRoute {
    @Serializable data object Splash : DskRoute
    @Serializable data object Auth : DskRoute
    @Serializable data object AdminLogin : DskRoute
    @Serializable data object AdminRegister : DskRoute
    @Serializable data object Home : DskRoute
    @Serializable data object Movies : DskRoute
    @Serializable data object WebSeries : DskRoute
    @Serializable data object LiveTv : DskRoute
    @Serializable data object Sports : DskRoute
    @Serializable data object Kids : DskRoute
    @Serializable data object MyList : DskRoute
    @Serializable data object Profile : DskRoute
    @Serializable data object Search : DskRoute
    @Serializable data object Settings : DskRoute
    @Serializable data object WatchHistory : DskRoute
    @Serializable data object AdminDashboard : DskRoute
    @Serializable data object ApkBuilder : DskRoute
    @Serializable data class MovieDetail(val movieId: String) : DskRoute
    @Serializable data class SeriesDetail(val seriesId: String) : DskRoute
    @Serializable data class Player(
        val contentId: String,
        val contentType: String
    ) : DskRoute
}

enum class BottomNavTab(
    val label: String,
    val route: DskRoute,
    val testTag: String
) {
    HOME("Home", DskRoute.Home, "nav_home"),
    MOVIES("Movies", DskRoute.Movies, "nav_movies"),
    WEB_SERIES("Series", DskRoute.WebSeries, "nav_web_series"),
    LIVE_TV("Live TV", DskRoute.LiveTv, "nav_live_tv"),
    SPORTS("Sports", DskRoute.Sports, "nav_sports"),
    KIDS("Kids", DskRoute.Kids, "nav_kids"),
    MY_LIST("My List", DskRoute.MyList, "nav_my_list"),
    PROFILE("Profile", DskRoute.Profile, "nav_profile")
}
