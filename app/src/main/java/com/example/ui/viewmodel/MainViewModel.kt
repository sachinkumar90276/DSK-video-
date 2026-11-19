package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.github.ApkBuilderRepository
import com.example.data.github.ApkBuilderUiState
import com.example.data.github.GitHubConnectionStatus
import com.example.data.local.SessionPreferences
import com.example.data.local.StoredSession
import com.example.data.model.*
import com.example.data.repository.CatalogSnapshot
import com.example.data.repository.OttRepository
import com.example.data.supabase.SupabaseConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Checking : AuthState
    data object SignedOut : AuthState
    data class VerificationPending(val email: String, val message: String) : AuthState
    data class SignedIn(
        val session: StoredSession,
        val profile: UserProfile?
    ) : AuthState
}

sealed interface CatalogState {
    data object Loading : CatalogState
    data class Ready(val snapshot: CatalogSnapshot) : CatalogState
    data class Error(val message: String, val cached: CatalogSnapshot? = null) : CatalogState
}

data class SearchResults(
    val movies: List<MovieItem> = emptyList(),
    val series: List<WebSeriesItem> = emptyList(),
    val episodes: List<EpisodeItem> = emptyList(),
    val channels: List<LiveChannelItem> = emptyList(),
    val sports: List<SportsItem> = emptyList(),
    val kids: List<KidsItem> = emptyList(),
    val suggestions: List<String> = emptyList()
)

data class AdminState(
    val isLoading: Boolean = false,
    val stats: AdminDashboardStats = AdminDashboardStats(),
    val logs: List<AdminActivityLog> = emptyList(),
    val smartUrlMetadata: SmartUrlMetadata? = null,
    val isInspectingUrl: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val preferences = SessionPreferences(application.applicationContext)
    private val repository = OttRepository(application.applicationContext, preferences)
    private val apkBuilderRepository = ApkBuilderRepository(application.applicationContext)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Checking)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _apkBuilderState = MutableStateFlow(ApkBuilderUiState())
    val apkBuilderState: StateFlow<ApkBuilderUiState> = _apkBuilderState.asStateFlow()

    private val _catalogState = MutableStateFlow<CatalogState>(CatalogState.Loading)
    val catalogState: StateFlow<CatalogState> = _catalogState.asStateFlow()

    private val _adminState = MutableStateFlow(AdminState())
    val adminState: StateFlow<AdminState> = _adminState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activePlayerMedia = MutableStateFlow<PlayableMedia?>(null)
    val activePlayerMedia: StateFlow<PlayableMedia?> = _activePlayerMedia.asStateFlow()

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _uiMessage = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiMessage: SharedFlow<String> = _uiMessage.asSharedFlow()

    val settings: StateFlow<AppSettingsState> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettingsState())

    val recentSearches: StateFlow<List<String>> = repository.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchResults: StateFlow<SearchResults> = combine(_searchQuery, _catalogState) { query, cat ->
        val snap = (cat as? CatalogState.Ready)?.snapshot ?: (cat as? CatalogState.Error)?.cached ?: CatalogSnapshot()
        buildSearchResults(query, snap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResults())

    init {
        bootstrap()
        loadApkBuilderConfig()
    }

    private fun loadApkBuilderConfig() {
        viewModelScope.launch {
            val (cfg, _) = apkBuilderRepository.loadSavedCredentials()
            _apkBuilderState.update { it.copy(config = cfg) }
        }
    }

    fun connectAndInspectApkBuilder(owner: String, repo: String, branch: String, token: String?) {
        viewModelScope.launch {
            apkBuilderRepository.saveConfig(owner, repo, branch, token)
            _apkBuilderState.update {
                it.copy(
                    connectionStatus = GitHubConnectionStatus.CONNECTING,
                    statusMessage = "Connecting to GitHub Actions..."
                )
            }
            val result = apkBuilderRepository.inspectAndFetchStatus(owner, repo, branch, token)
            _apkBuilderState.value = result
        }
    }

    fun triggerApkRebuild(buildType: String) {
        val current = _apkBuilderState.value
        viewModelScope.launch {
            _apkBuilderState.update { it.copy(isTriggeringBuild = true) }
            apkBuilderRepository.triggerRebuild(
                owner = current.config.owner,
                repo = current.config.repo,
                branch = current.config.branch,
                buildType = buildType
            ).onSuccess { msg ->
                _uiMessage.tryEmit(msg)
                delay(2000)
                val updated = apkBuilderRepository.inspectAndFetchStatus(
                    current.config.owner,
                    current.config.repo,
                    current.config.branch
                )
                _apkBuilderState.value = updated.copy(isTriggeringBuild = false)
            }.onFailure { err ->
                _uiMessage.tryEmit(err.message ?: "Failed to dispatch APK rebuild")
                _apkBuilderState.update { it.copy(isTriggeringBuild = false) }
            }
        }
    }

    private fun bootstrap() {
        viewModelScope.launch {
            delay(1500) // Show DSK VIDEO splash brand animation briefly
            val session = repository.restoreSession().getOrNull()
            if (session != null && session.accessToken.isNotBlank()) {
                val profile = repository.fetchProfile(session.userId)
                _authState.value = AuthState.SignedIn(session, profile)
                refreshCatalog()
            } else {
                _authState.value = AuthState.SignedOut
            }
        }
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun signIn(email: String, password: String, rememberSession: Boolean, requireAdmin: Boolean = false, onSuccess: () -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _authError.value = "Please enter both email and password."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repository.signIn(email, password, rememberSession, requireAdmin)
            _authBusy.value = false
            res.onSuccess { session ->
                val profile = repository.fetchProfile(session.userId)
                _authState.value = AuthState.SignedIn(session, profile)
                refreshCatalog()
                if (session.isAdmin) loadAdminOverview()
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Sign in failed."
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        if (email.isBlank() || password.length < 6 || username.isBlank()) {
            _authError.value = "Enter username, valid email, and password (min 6 characters)."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repository.signUp(email, password, username)
            _authBusy.value = false
            res.onSuccess { msg ->
                val stored = preferences.getStoredSession()
                if (stored.accessToken.isNotBlank()) {
                    val profile = repository.fetchProfile(stored.userId)
                    _authState.value = AuthState.SignedIn(stored, profile)
                    refreshCatalog()
                } else {
                    _authState.value = AuthState.VerificationPending(email.trim(), msg)
                }
            }.onFailure { err ->
                _authError.value = err.message ?: "Registration failed."
            }
        }
    }

    fun registerAdmin(
        fullName: String,
        email: String,
        password: String,
        confirmPassword: String,
        secretCode: String,
        onActivated: () -> Unit,
        onVerificationPending: (String) -> Unit
    ) {
        val validationError = AdminRegistrationFormValidator.validate(
            fullName = fullName,
            email = email,
            password = password,
            confirmPassword = confirmPassword,
            secretCode = secretCode
        )
        if (validationError != null) {
            _authError.value = validationError
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repository.registerAdmin(fullName, email, password, secretCode)
            _authBusy.value = false
            res.onSuccess { outcome ->
                when (outcome) {
                    is com.example.data.repository.AdminRegistrationOutcome.Activated -> {
                        val profile = repository.fetchProfile(outcome.session.userId)
                        _authState.value = AuthState.SignedIn(outcome.session, profile)
                        refreshCatalog()
                        loadAdminOverview()
                        _uiMessage.tryEmit("Admin account registered and verified.")
                        onActivated()
                    }
                    is com.example.data.repository.AdminRegistrationOutcome.VerificationPending -> {
                        onVerificationPending(outcome.message)
                    }
                }
            }.onFailure { err ->
                _authError.value = err.message ?: "Admin registration failed."
            }
        }
    }

    fun activateAdminWithSecret(
        fullName: String,
        email: String,
        password: String,
        secretCode: String,
        onSuccess: () -> Unit
    ) {
        if (email.isBlank() || password.isBlank() || secretCode.trim().length < 6) {
            _authError.value = "Enter email, password, and valid Admin Registration Secret Code."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            _authError.value = null
            val res = repository.activateAdminWithSecret(fullName, email, password, secretCode)
            _authBusy.value = false
            res.onSuccess { session ->
                val profile = repository.fetchProfile(session.userId)
                _authState.value = AuthState.SignedIn(session, profile)
                refreshCatalog()
                loadAdminOverview()
                _uiMessage.tryEmit("Admin privileges activated.")
                onSuccess()
            }.onFailure { err ->
                _authError.value = err.message ?: "Admin activation failed."
            }
        }
    }

    fun resendVerification(email: String) {
        viewModelScope.launch {
            _authBusy.value = true
            repository.resendVerification(email)
                .onSuccess { _uiMessage.tryEmit(it) }
                .onFailure { _authError.value = it.message }
            _authBusy.value = false
        }
    }

    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _authError.value = "Enter your email address to receive reset instructions."
            return
        }
        viewModelScope.launch {
            _authBusy.value = true
            repository.sendPasswordReset(email)
                .onSuccess { _uiMessage.tryEmit(it) }
                .onFailure { _authError.value = it.message }
            _authBusy.value = false
        }
    }

    fun backToSignIn() {
        _authState.value = AuthState.SignedOut
        _authError.value = null
    }

    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.SignedOut
            _activePlayerMedia.value = null
            onDone()
        }
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            val uid = currentUserId()
            if (_catalogState.value !is CatalogState.Ready) {
                _catalogState.value = CatalogState.Loading
            }
            repository.loadCatalog(uid)
                .onSuccess { snap ->
                    _catalogState.value = CatalogState.Ready(snap)
                    if (isAdmin()) {
                        loadAdminOverview(snap)
                    }
                }
                .onFailure { err ->
                    val existing = (_catalogState.value as? CatalogState.Ready)?.snapshot
                    _catalogState.value = CatalogState.Error(
                        message = err.message ?: "Failed to load catalog.",
                        cached = existing
                    )
                }
        }
    }

    fun currentCatalog(): CatalogSnapshot {
        return (_catalogState.value as? CatalogState.Ready)?.snapshot
            ?: (_catalogState.value as? CatalogState.Error)?.cached
            ?: CatalogSnapshot()
    }

    fun currentUserId(): String = (_authState.value as? AuthState.SignedIn)?.session?.userId.orEmpty()
    fun currentEmail(): String = (_authState.value as? AuthState.SignedIn)?.session?.email.orEmpty()
    fun isAdmin(): Boolean = (_authState.value as? AuthState.SignedIn)?.session?.isAdmin == true

    fun toggleWatchlist(contentId: String, contentType: String, title: String, posterUrl: String) {
        val uid = currentUserId()
        if (uid.isBlank()) {
            _uiMessage.tryEmit("Please sign in to manage My List.")
            return
        }
        val currentlyInList = currentCatalog().watchlist.any { it.contentId == contentId }
        viewModelScope.launch {
            repository.toggleWatchlist(uid, contentId, contentType, title, posterUrl, currentlyInList)
                .onSuccess { added ->
                    _uiMessage.tryEmit(if (added) "Added to My List" else "Removed from My List")
                    refreshCatalog()
                }
                .onFailure { _uiMessage.tryEmit(it.message ?: "Watchlist update failed") }
        }
    }

    fun preparePlayback(contentId: String, contentType: String): PlayableMedia? {
        val snap = currentCatalog()
        val progressMs = snap.watchProgress.firstOrNull { it.contentId == contentId }?.positionMs ?: 0L

        val media: PlayableMedia? = when (contentType) {
            "movie" -> snap.movies.firstOrNull { it.id == contentId }?.let { m ->
                PlayableMedia(
                    contentId = m.id,
                    contentType = "movie",
                    title = m.title,
                    subtitle = "${m.releaseYear} • ${m.language} • ${m.genre}",
                    videoUrl = m.videoUrl,
                    posterUrl = m.bannerUrl.ifBlank { m.posterUrl },
                    subtitleUrl = m.subtitleUrl,
                    resumePositionMs = progressMs
                )
            }
            "trailer" -> snap.movies.firstOrNull { it.id == contentId }?.let { m ->
                PlayableMedia(
                    contentId = "${m.id}_trailer",
                    contentType = "trailer",
                    title = "${m.title} (Official Trailer)",
                    subtitle = m.language,
                    videoUrl = m.trailerUrl,
                    posterUrl = m.bannerUrl.ifBlank { m.posterUrl },
                    resumePositionMs = 0L
                )
            }
            "episode" -> snap.episodes.firstOrNull { it.id == contentId }?.let { ep ->
                val seasonEpisodes = snap.episodes
                    .filter { it.seasonId == ep.seasonId }
                    .sortedBy { it.episodeNumber }
                val idx = seasonEpisodes.indexOfFirst { it.id == ep.id }
                val nextEp = if (idx >= 0 && idx + 1 < seasonEpisodes.size) seasonEpisodes[idx + 1] else null
                val prevEp = if (idx > 0) seasonEpisodes[idx - 1] else null
                val seriesTitle = snap.webSeries.firstOrNull { it.id == ep.seriesId }?.title ?: "Web Series"
                PlayableMedia(
                    contentId = ep.id,
                    contentType = "episode",
                    title = ep.title,
                    subtitle = "$seriesTitle • Ep ${ep.episodeNumber}",
                    videoUrl = ep.videoUrl,
                    posterUrl = ep.thumbnailUrl,
                    subtitleUrl = ep.subtitleUrl,
                    resumePositionMs = progressMs,
                    seriesId = ep.seriesId,
                    seasonId = ep.seasonId,
                    episodeId = ep.id,
                    nextEpisode = nextEp,
                    previousEpisode = prevEp
                )
            }
            "live" -> snap.liveChannels.firstOrNull { it.id == contentId }?.let { ch ->
                PlayableMedia(
                    contentId = ch.id,
                    contentType = "live",
                    title = ch.name,
                    subtitle = "LIVE • ${ch.category} • ${ch.language}",
                    videoUrl = ch.streamUrl,
                    posterUrl = ch.logoUrl,
                    isLive = true
                )
            }
            "sports" -> snap.sports.firstOrNull { it.id == contentId }?.let { sp ->
                PlayableMedia(
                    contentId = sp.id,
                    contentType = "sports",
                    title = sp.title,
                    subtitle = "${sp.sportType} • ${sp.category}",
                    videoUrl = sp.videoUrl,
                    posterUrl = sp.bannerUrl.ifBlank { sp.thumbnailUrl },
                    subtitleUrl = sp.subtitleUrl,
                    resumePositionMs = progressMs,
                    isLive = sp.isLive
                )
            }
            "kids" -> snap.kids.firstOrNull { it.id == contentId }?.let { kd ->
                PlayableMedia(
                    contentId = kd.id,
                    contentType = "kids",
                    title = kd.title,
                    subtitle = "${kd.category} • ${kd.ageRating}",
                    videoUrl = kd.videoUrl,
                    posterUrl = kd.bannerUrl.ifBlank { kd.posterUrl },
                    subtitleUrl = kd.subtitleUrl,
                    resumePositionMs = progressMs
                )
            }
            "progress" -> snap.watchProgress.firstOrNull { it.contentId == contentId }?.let { wp ->
                PlayableMedia(
                    contentId = wp.contentId,
                    contentType = wp.contentType,
                    title = wp.title,
                    videoUrl = wp.videoUrl,
                    posterUrl = wp.posterUrl,
                    subtitleUrl = wp.subtitleUrl,
                    resumePositionMs = wp.positionMs,
                    seriesId = wp.seriesId,
                    seasonId = wp.seasonId,
                    episodeId = wp.episodeId
                )
            }
            else -> null
        }

        if (media != null) {
            _activePlayerMedia.value = media
            viewModelScope.launch {
                repository.recordWatchHistory(currentUserId(), media)
            }
        }
        return media
    }

    fun savePlaybackProgress(media: PlayableMedia, positionMs: Long, durationMs: Long) {
        val uid = currentUserId()
        if (uid.isBlank() || media.isLive || durationMs <= 0L) return
        viewModelScope.launch {
            repository.recordWatchProgress(uid, media, positionMs, durationMs)
            refreshCatalog()
        }
    }

    // Search
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearch(query: String) {
        viewModelScope.launch {
            preferences.addRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferences.clearRecentSearches()
        }
    }

    private fun buildSearchResults(query: String, snap: CatalogSnapshot): SearchResults {
        val q = query.trim().lowercase()
        val suggestions = (
            snap.movies.map { it.title } +
                snap.movies.map { it.genre } +
                snap.movies.map { it.language } +
                snap.webSeries.map { it.title }
            ).filter { it.isNotBlank() }.distinct().take(12)

        if (q.isBlank()) {
            return SearchResults(suggestions = suggestions)
        }

        val matchingMovies = snap.movies.filter { m ->
            m.isPublished && (
                m.title.lowercase().contains(q) ||
                    m.castMembers.lowercase().contains(q) ||
                    m.director.lowercase().contains(q) ||
                    m.language.lowercase().contains(q) ||
                    m.genre.lowercase().contains(q) ||
                    m.category.lowercase().contains(q)
                )
        }
        val matchingSeries = snap.webSeries.filter { s ->
            s.isPublished && (
                s.title.lowercase().contains(q) ||
                    s.castMembers.lowercase().contains(q) ||
                    s.director.lowercase().contains(q) ||
                    s.language.lowercase().contains(q) ||
                    s.genre.lowercase().contains(q)
                )
        }
        val matchingEpisodes = snap.episodes.filter { e ->
            e.title.lowercase().contains(q) || e.description.lowercase().contains(q)
        }
        val matchingChannels = snap.liveChannels.filter { c ->
            c.isPublished && (c.name.lowercase().contains(q) || c.category.lowercase().contains(q) || c.language.lowercase().contains(q))
        }
        val matchingSports = snap.sports.filter { sp ->
            sp.isPublished && (sp.title.lowercase().contains(q) || sp.sportType.lowercase().contains(q) || sp.category.lowercase().contains(q))
        }
        val matchingKids = snap.kids.filter { kd ->
            kd.isPublished && (kd.title.lowercase().contains(q) || kd.category.lowercase().contains(q) || kd.language.lowercase().contains(q))
        }

        return SearchResults(
            movies = matchingMovies,
            series = matchingSeries,
            episodes = matchingEpisodes,
            channels = matchingChannels,
            sports = matchingSports,
            kids = matchingKids,
            suggestions = suggestions.filter { it.lowercase().contains(q) }
        )
    }

    // Profile & Settings
    fun updateProfile(username: String, preferredLanguage: String, avatarUri: Uri?) {
        val state = _authState.value as? AuthState.SignedIn ?: return
        viewModelScope.launch {
            var uploadedUrl = state.profile?.avatarUrl
            if (avatarUri != null) {
                repository.uploadProfileAvatar(state.session.userId, avatarUri)
                    .onSuccess { uploadedUrl = it }
                    .onFailure { _uiMessage.tryEmit(it.message ?: "Avatar upload failed") }
            }
            repository.updateProfile(
                userId = state.session.userId,
                email = state.session.email,
                username = username,
                preferredLanguage = preferredLanguage,
                avatarUrl = uploadedUrl
            ).onSuccess { updated ->
                _authState.value = state.copy(profile = updated)
                _uiMessage.tryEmit("Profile updated")
            }.onFailure { _uiMessage.tryEmit(it.message ?: "Profile update failed") }
        }
    }

    fun changePassword(newPassword: String) {
        if (newPassword.length < 6) {
            _uiMessage.tryEmit("Password must be at least 6 characters.")
            return
        }
        viewModelScope.launch {
            repository.updatePassword(newPassword)
                .onSuccess { _uiMessage.tryEmit(it) }
                .onFailure { _uiMessage.tryEmit(it.message ?: "Failed to update password") }
        }
    }

    fun updateSettings(transform: (AppSettingsState) -> AppSettingsState) {
        viewModelScope.launch { preferences.updateSettings(transform) }
    }

    fun setParentalPin(pin: String?) {
        viewModelScope.launch {
            preferences.setParentalPin(pin)
            _uiMessage.tryEmit(if (pin.isNullOrBlank()) "Parental Lock disabled" else "Parental Lock PIN saved")
        }
    }

    suspend fun verifyParentalPin(pin: String): Boolean = preferences.verifyParentalPin(pin)

    fun setProfilePin(pin: String?) {
        viewModelScope.launch {
            preferences.setProfilePin(pin)
            _uiMessage.tryEmit(if (pin.isNullOrBlank()) "Profile PIN removed" else "Profile PIN enabled")
        }
    }

    suspend fun verifyProfilePin(pin: String): Boolean = preferences.verifyProfilePin(pin)

    // Admin Actions
    fun loadAdminOverview(snapshot: CatalogSnapshot = currentCatalog()) {
        viewModelScope.launch {
            _adminState.update { it.copy(isLoading = true) }
            val (stats, logs) = repository.loadAdminOverview(snapshot)
            _adminState.update { it.copy(isLoading = false, stats = stats, logs = logs) }
        }
    }

    fun inspectSmartUrl(url: String) {
        viewModelScope.launch {
            _adminState.update { it.copy(isInspectingUrl = true, smartUrlMetadata = null) }
            val meta = repository.inspectSmartUrl(url)
            _adminState.update { it.copy(isInspectingUrl = false, smartUrlMetadata = meta) }
        }
    }

    fun clearSmartUrlMetadata() {
        _adminState.update { it.copy(smartUrlMetadata = null) }
    }

    fun saveMovie(movie: MovieItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveMovie(currentUserId(), currentEmail(), movie)
                .onSuccess {
                    _uiMessage.tryEmit("Movie '${it.title}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save movie") }
        }
    }

    fun toggleMoviePublish(movie: MovieItem) {
        viewModelScope.launch {
            repository.toggleMoviePublish(currentUserId(), currentEmail(), movie)
                .onSuccess {
                    _uiMessage.tryEmit(if (it.isPublished) "Published '${it.title}'" else "Unpublished '${it.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Publish update failed") }
        }
    }

    fun deleteMovie(movie: MovieItem) {
        viewModelScope.launch {
            repository.deleteMovie(currentUserId(), currentEmail(), movie.id, movie.title)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${movie.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun saveWebSeries(series: WebSeriesItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveWebSeries(currentUserId(), currentEmail(), series)
                .onSuccess {
                    _uiMessage.tryEmit("Series '${it.title}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save series") }
        }
    }

    fun deleteWebSeries(series: WebSeriesItem) {
        viewModelScope.launch {
            repository.deleteWebSeries(currentUserId(), currentEmail(), series.id, series.title)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${series.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun addSeason(seriesId: String, seasonNumber: Int, title: String) {
        viewModelScope.launch {
            repository.createSeason(currentUserId(), currentEmail(), seriesId, seasonNumber, title)
                .onSuccess {
                    _uiMessage.tryEmit("Added ${it.title}")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to add season") }
        }
    }

    fun deleteSeason(seasonId: String) {
        viewModelScope.launch {
            repository.deleteSeason(currentUserId(), currentEmail(), seasonId)
                .onSuccess {
                    _uiMessage.tryEmit("Season deleted")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to delete season") }
        }
    }

    fun saveEpisode(episode: EpisodeItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveEpisode(currentUserId(), currentEmail(), episode)
                .onSuccess {
                    _uiMessage.tryEmit("Episode '${it.title}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save episode") }
        }
    }

    fun deleteEpisode(episode: EpisodeItem) {
        viewModelScope.launch {
            repository.deleteEpisode(currentUserId(), currentEmail(), episode.id, episode.title)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${episode.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun saveLiveChannel(channel: LiveChannelItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveLiveChannel(currentUserId(), currentEmail(), channel)
                .onSuccess {
                    _uiMessage.tryEmit("Channel '${it.name}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save channel") }
        }
    }

    fun deleteLiveChannel(channel: LiveChannelItem) {
        viewModelScope.launch {
            repository.deleteLiveChannel(currentUserId(), currentEmail(), channel.id, channel.name)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${channel.name}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun saveSportsItem(item: SportsItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveSportsItem(currentUserId(), currentEmail(), item)
                .onSuccess {
                    _uiMessage.tryEmit("Sports item '${it.title}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save sports item") }
        }
    }

    fun deleteSportsItem(item: SportsItem) {
        viewModelScope.launch {
            repository.deleteSportsItem(currentUserId(), currentEmail(), item.id, item.title)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${item.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun saveKidsItem(item: KidsItem, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.saveKidsItem(currentUserId(), currentEmail(), item)
                .onSuccess {
                    _uiMessage.tryEmit("Kids title '${it.title}' saved")
                    refreshCatalog()
                    onDone()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Failed to save kids title") }
        }
    }

    fun deleteKidsItem(item: KidsItem) {
        viewModelScope.launch {
            repository.deleteKidsItem(currentUserId(), currentEmail(), item.id, item.title)
                .onSuccess {
                    _uiMessage.tryEmit("Deleted '${item.title}'")
                    refreshCatalog()
                }.onFailure { _uiMessage.tryEmit(it.message ?: "Delete failed") }
        }
    }

    fun isSupabaseConfigured(): Boolean = SupabaseConfig.isConfigured
}
