package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.ui.components.DskBottomNavigationBar
import com.example.ui.components.DskNavigationRail
import com.example.ui.components.PinEntryDialog
import com.example.ui.navigation.BottomNavTab
import com.example.ui.navigation.DskRoute
import com.example.ui.screens.*
import com.example.ui.theme.DskCrimson
import com.example.ui.theme.DskVideoTheme
import com.example.ui.viewmodel.AuthState
import com.example.ui.viewmodel.CatalogState
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MainViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()

            DskVideoTheme(amoledBlack = settings.amoledDarkTheme) {
                DskVideoApp(vm = vm)
            }
        }
    }
}

@Composable
fun DskVideoApp(vm: MainViewModel) {
    val navController = rememberNavController()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val catalogState by vm.catalogState.collectAsStateWithLifecycle()
    val adminState by vm.adminState.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val searchQuery by vm.searchQuery.collectAsStateWithLifecycle()
    val searchResults by vm.searchResults.collectAsStateWithLifecycle()
    val recentSearches by vm.recentSearches.collectAsStateWithLifecycle()
    val activeMedia by vm.activePlayerMedia.collectAsStateWithLifecycle()
    val authBusy by vm.authBusy.collectAsStateWithLifecycle()
    val authError by vm.authError.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var kidsExitPendingTab by remember { mutableStateOf<BottomNavTab?>(null) }
    var kidsPinError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        vm.uiMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    val currentTabRoute: DskRoute = remember(currentDest) {
        when {
            currentDest?.hasRoute<DskRoute.Movies>() == true -> DskRoute.Movies
            currentDest?.hasRoute<DskRoute.WebSeries>() == true -> DskRoute.WebSeries
            currentDest?.hasRoute<DskRoute.LiveTv>() == true -> DskRoute.LiveTv
            currentDest?.hasRoute<DskRoute.Sports>() == true -> DskRoute.Sports
            currentDest?.hasRoute<DskRoute.Kids>() == true -> DskRoute.Kids
            currentDest?.hasRoute<DskRoute.MyList>() == true -> DskRoute.MyList
            currentDest?.hasRoute<DskRoute.Profile>() == true -> DskRoute.Profile
            else -> DskRoute.Home
        }
    }

    val showBottomChrome = remember(currentDest) {
        BottomNavTab.entries.any { tab -> currentDest?.hasRoute(tab.route::class) == true }
    }

    // Auth-based root routing
    LaunchedEffect(authState) {
        val currentIsSplash = currentDest?.hasRoute<DskRoute.Splash>() == true
        val currentIsAuth = currentDest?.hasRoute<DskRoute.Auth>() == true
        val currentIsAdminLogin = currentDest?.hasRoute<DskRoute.AdminLogin>() == true
        when (authState) {
            AuthState.Checking -> Unit
            is AuthState.SignedOut, is AuthState.VerificationPending -> {
                if (!currentIsAuth && !currentIsAdminLogin) {
                    navController.navigate(DskRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            is AuthState.SignedIn -> {
                if (currentIsSplash || currentIsAuth) {
                    navController.navigate(DskRoute.Home) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    fun navigateToTab(tab: BottomNavTab) {
        val leavingKids = currentTabRoute == DskRoute.Kids && tab != BottomNavTab.KIDS
        if (leavingKids && settings.parentalLockEnabled) {
            kidsPinError = null
            kidsExitPendingTab = tab
            return
        }
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun launchPlayer(contentId: String, contentType: String) {
        if (authState !is AuthState.SignedIn) {
            navController.navigate(DskRoute.Auth)
            return
        }
        val prepared = vm.preparePlayback(contentId, contentType)
        if (prepared != null) {
            navController.navigate(DskRoute.Player(contentId, contentType))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(snackbarData = data, containerColor = DskCrimson)
            }
        },
        bottomBar = {
            if (showBottomChrome) {
                BoxWithConstraints {
                    if (maxWidth < 720.dp) {
                        DskBottomNavigationBar(
                            currentRoute = currentTabRoute,
                            onTabSelected = ::navigateToTab
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val useRail = showBottomChrome && maxWidth >= 720.dp
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    DskNavigationRail(
                        currentRoute = currentTabRoute,
                        onTabSelected = ::navigateToTab
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(bottom = if (showBottomChrome && !useRail) innerPadding.calculateBottomPadding() else 0.dp)
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = DskRoute.Splash
                    ) {
                        composable<DskRoute.Splash> {
                            SplashScreen()
                        }

                        composable<DskRoute.Auth> {
                            AuthScreen(
                                authState = authState,
                                isBusy = authBusy,
                                errorMessage = authError,
                                onSignIn = { email, pass, remember -> vm.signIn(email, pass, remember) },
                                onSignUp = { email, pass, user -> vm.signUp(email, pass, user) },
                                onSendReset = { email -> vm.sendPasswordReset(email) },
                                onResendVerification = { email -> vm.resendVerification(email) },
                                onBackToSignIn = { vm.backToSignIn() },
                                onOpenAdminLogin = {
                                    vm.clearAuthError()
                                    navController.navigate(DskRoute.AdminLogin)
                                },
                                onClearError = { vm.clearAuthError() }
                            )
                        }

                        composable<DskRoute.AdminLogin> {
                            AdminLoginScreen(
                                isBusy = authBusy,
                                errorMessage = authError,
                                onAdminSignIn = { email, pass ->
                                    vm.signIn(email, pass, rememberSession = true, requireAdmin = true) {
                                        navController.navigate(DskRoute.AdminDashboard) {
                                            popUpTo(DskRoute.AdminLogin) { inclusive = true }
                                        }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<DskRoute.Home> {
                            HomeScreen(
                                catalogState = catalogState,
                                isAdmin = vm.isAdmin(),
                                onRefresh = { vm.refreshCatalog() },
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onAdminClick = {
                                    vm.loadAdminOverview()
                                    navController.navigate(DskRoute.AdminDashboard)
                                },
                                onMovieClick = { id -> navController.navigate(DskRoute.MovieDetail(id)) },
                                onSeriesClick = { id -> navController.navigate(DskRoute.SeriesDetail(id)) },
                                onPlayMovie = { id -> launchPlayer(id, "movie") },
                                onPlayLiveChannel = { id -> launchPlayer(id, "live") },
                                onPlaySports = { id -> launchPlayer(id, "sports") },
                                onPlayKids = { id -> launchPlayer(id, "kids") },
                                onResumeProgress = { id -> launchPlayer(id, "progress") },
                                onToggleWatchlist = { id, type, title, poster ->
                                    vm.toggleWatchlist(id, type, title, poster)
                                },
                                onExploreSection = { target ->
                                    when (target) {
                                        "movies" -> navigateToTab(BottomNavTab.MOVIES)
                                        "series" -> navigateToTab(BottomNavTab.WEB_SERIES)
                                        "live" -> navigateToTab(BottomNavTab.LIVE_TV)
                                        "sports" -> navigateToTab(BottomNavTab.SPORTS)
                                        "kids" -> navigateToTab(BottomNavTab.KIDS)
                                    }
                                }
                            )
                        }

                        composable<DskRoute.Movies> {
                            MoviesScreen(
                                catalogState = catalogState,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onMovieClick = { id -> navController.navigate(DskRoute.MovieDetail(id)) }
                            )
                        }

                        composable<DskRoute.WebSeries> {
                            WebSeriesCatalogScreen(
                                catalogState = catalogState,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onSeriesClick = { id -> navController.navigate(DskRoute.SeriesDetail(id)) }
                            )
                        }

                        composable<DskRoute.LiveTv> {
                            LiveTvScreen(
                                catalogState = catalogState,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onPlayChannel = { id -> launchPlayer(id, "live") }
                            )
                        }

                        composable<DskRoute.Sports> {
                            SportsScreen(
                                catalogState = catalogState,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onPlaySports = { id -> launchPlayer(id, "sports") }
                            )
                        }

                        composable<DskRoute.Kids> {
                            KidsScreen(
                                catalogState = catalogState,
                                settings = settings,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onPlayKids = { id -> launchPlayer(id, "kids") },
                                onSetParentalPin = { pin -> vm.setParentalPin(pin) },
                                onVerifyParentalPin = { pin -> vm.verifyParentalPin(pin) }
                            )
                        }

                        composable<DskRoute.MyList> {
                            MyListScreen(
                                catalogState = catalogState,
                                onSearchClick = { navController.navigate(DskRoute.Search) },
                                onRefresh = { vm.refreshCatalog() },
                                onOpenMovie = { id -> navController.navigate(DskRoute.MovieDetail(id)) },
                                onOpenSeries = { id -> navController.navigate(DskRoute.SeriesDetail(id)) },
                                onResumeProgress = { id -> launchPlayer(id, "progress") },
                                onRemoveWatchlist = { id, type, title, poster ->
                                    vm.toggleWatchlist(id, type, title, poster)
                                },
                                onOpenHistory = { navController.navigate(DskRoute.WatchHistory) }
                            )
                        }

                        composable<DskRoute.Profile> {
                            val signedIn = authState as? AuthState.SignedIn
                            ProfileScreen(
                                email = signedIn?.session?.email.orEmpty(),
                                profile = signedIn?.profile,
                                isAdmin = vm.isAdmin(),
                                settings = settings,
                                catalogState = catalogState,
                                onUpdateProfile = { name, lang, uri -> vm.updateProfile(name, lang, uri) },
                                onChangePassword = { pass -> vm.changePassword(pass) },
                                onSetProfilePin = { pin -> vm.setProfilePin(pin) },
                                onVerifyProfilePin = { pin -> vm.verifyProfilePin(pin) },
                                onOpenWatchlist = { navigateToTab(BottomNavTab.MY_LIST) },
                                onOpenWatchHistory = { navController.navigate(DskRoute.WatchHistory) },
                                onOpenSettings = { navController.navigate(DskRoute.Settings) },
                                onOpenAdminPanel = {
                                    vm.loadAdminOverview()
                                    navController.navigate(DskRoute.AdminDashboard)
                                },
                                onOpenAdminLogin = {
                                    vm.clearAuthError()
                                    navController.navigate(DskRoute.AdminLogin)
                                },
                                onLogout = {
                                    vm.logout {
                                        navController.navigate(DskRoute.Auth) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    }
                                }
                            )
                        }

                        composable<DskRoute.MovieDetail> { entry ->
                            val route = entry.toRoute<DskRoute.MovieDetail>()
                            val snap = (catalogState as? CatalogState.Ready)?.snapshot ?: vm.currentCatalog()
                            val movie = snap.movies.firstOrNull { it.id == route.movieId }
                            val related = snap.movies.filter {
                                it.id != route.movieId && it.isPublished &&
                                    (it.genre == movie?.genre || it.language == movie?.language)
                            }.take(10)
                            val wp = snap.watchProgress.firstOrNull { it.contentId == route.movieId }
                            val inList = snap.watchlist.any { it.contentId == route.movieId }

                            MovieDetailScreen(
                                movie = movie,
                                relatedMovies = related,
                                watchProgress = wp,
                                inWatchlist = inList,
                                onBack = { navController.popBackStack() },
                                onPlayMovie = { id -> launchPlayer(id, "movie") },
                                onPlayTrailer = { id -> launchPlayer(id, "trailer") },
                                onToggleWatchlist = { m -> vm.toggleWatchlist(m.id, "movie", m.title, m.posterUrl) },
                                onRelatedClick = { id -> navController.navigate(DskRoute.MovieDetail(id)) }
                            )
                        }

                        composable<DskRoute.SeriesDetail> { entry ->
                            val route = entry.toRoute<DskRoute.SeriesDetail>()
                            val snap = (catalogState as? CatalogState.Ready)?.snapshot ?: vm.currentCatalog()
                            val series = snap.webSeries.firstOrNull { it.id == route.seriesId }
                            val seasons = snap.seasons.filter { it.seriesId == route.seriesId }
                            val episodes = snap.episodes.filter { it.seriesId == route.seriesId }
                            val inList = snap.watchlist.any { it.contentId == route.seriesId }

                            WebSeriesDetailScreen(
                                series = series,
                                seasons = seasons,
                                episodes = episodes,
                                watchProgress = snap.watchProgress,
                                inWatchlist = inList,
                                onBack = { navController.popBackStack() },
                                onPlayEpisode = { epId -> launchPlayer(epId, "episode") },
                                onToggleWatchlist = { s -> vm.toggleWatchlist(s.id, "series", s.title, s.posterUrl) }
                            )
                        }

                        composable<DskRoute.Search> {
                            SearchScreen(
                                query = searchQuery,
                                results = searchResults,
                                recentSearches = recentSearches,
                                onQueryChange = vm::updateSearchQuery,
                                onCommitSearch = vm::commitSearch,
                                onClearRecent = vm::clearRecentSearches,
                                onBack = { navController.popBackStack() },
                                onMovieClick = { id -> navController.navigate(DskRoute.MovieDetail(id)) },
                                onSeriesClick = { id -> navController.navigate(DskRoute.SeriesDetail(id)) },
                                onPlayEpisode = { id -> launchPlayer(id, "episode") },
                                onPlayLive = { id -> launchPlayer(id, "live") },
                                onPlaySports = { id -> launchPlayer(id, "sports") },
                                onPlayKids = { id -> launchPlayer(id, "kids") }
                            )
                        }

                        composable<DskRoute.WatchHistory> {
                            WatchHistoryScreen(
                                catalogState = catalogState,
                                onBack = { navController.popBackStack() },
                                onPlayItem = { id, type -> launchPlayer(id, type) }
                            )
                        }

                        composable<DskRoute.Settings> {
                            SettingsScreen(
                                settings = settings,
                                onUpdateSettings = vm::updateSettings,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable<DskRoute.AdminDashboard> {
                            AdminDashboardScreen(
                                isAdminAuthorized = vm.isAdmin(),
                                adminState = adminState,
                                catalog = vm.currentCatalog(),
                                onBack = { navController.popBackStack() },
                                onRefresh = {
                                    vm.refreshCatalog()
                                    vm.loadAdminOverview()
                                },
                                onInspectSmartUrl = vm::inspectSmartUrl,
                                onClearSmartUrl = vm::clearSmartUrlMetadata,
                                onSaveMovie = vm::saveMovie,
                                onToggleMoviePublish = vm::toggleMoviePublish,
                                onDeleteMovie = vm::deleteMovie,
                                onSaveSeries = vm::saveWebSeries,
                                onDeleteSeries = vm::deleteWebSeries,
                                onAddSeason = vm::addSeason,
                                onDeleteSeason = vm::deleteSeason,
                                onSaveEpisode = vm::saveEpisode,
                                onDeleteEpisode = vm::deleteEpisode,
                                onSaveChannel = vm::saveLiveChannel,
                                onDeleteChannel = vm::deleteLiveChannel,
                                onSaveSports = vm::saveSportsItem,
                                onDeleteSports = vm::deleteSportsItem,
                                onSaveKids = vm::saveKidsItem,
                                onDeleteKids = vm::deleteKidsItem
                            )
                        }

                        composable<DskRoute.Player> { entry ->
                            val route = entry.toRoute<DskRoute.Player>()
                            val media = activeMedia?.takeIf { it.contentId == route.contentId }
                                ?: remember(route.contentId, route.contentType) {
                                    vm.preparePlayback(route.contentId, route.contentType)
                                }

                            PlayerScreen(
                                media = media,
                                settings = settings,
                                onSaveProgress = vm::savePlaybackProgress,
                                onPlayEpisode = { epId -> launchPlayer(epId, "episode") },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (kidsExitPendingTab != null) {
        PinEntryDialog(
            title = "Parental Lock Active",
            subtitle = "Enter your Parental PIN to leave Kids Mode.",
            confirmLabel = "Verify",
            errorMessage = kidsPinError,
            onDismiss = { kidsExitPendingTab = null },
            onConfirm = { pin ->
                scope.launch {
                    if (vm.verifyParentalPin(pin)) {
                        val target = kidsExitPendingTab!!
                        kidsExitPendingTab = null
                        navController.navigate(target.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        kidsPinError = "Incorrect Parental PIN."
                    }
                }
            }
        )
    }
}
