package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.example.data.model.AnimeInfo
import com.example.data.model.EpisodeInfo
import com.example.data.model.VideoSource
import com.example.data.source.AnimePaheSource
import com.example.data.source.ExtensionSource
import com.example.data.source.GenericHeuristicSource
import com.example.data.source.WebViewFetcher
import com.example.data.store.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    object Library : Screen
    object Browse : Screen
    data class SourceBrowse(val sourceName: String) : Screen
    object More : Screen
    data class Details(val anime: AnimeInfo) : Screen
    data class Player(val anime: AnimeInfo, val episode: EpisodeInfo) : Screen
    data class UniversalBrowser(val initialUrl: String) : Screen
    object Settings : Screen
}

sealed interface LceState<out T> {
    object Idle : LceState<Nothing>
    object Loading : LceState<Nothing>
    data class Success<out T>(val data: T) : LceState<T>
    data class Error(val message: String) : LceState<Nothing>
}

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    val settingsManager = SettingsManager(application)
    private var activeSource: ExtensionSource = AnimePaheSource(settingsManager)

    fun setActiveSource(name: String) {
        if (name == "AnimePahe") {
            activeSource = AnimePaheSource(settingsManager)
        } else {
            val url = settingsManager.getCustomSites().find { it.first == name }?.second ?: "https://example.com"
            activeSource = GenericHeuristicSource(url, name, settingsManager.context)
        }
    }

    // Navigation and UX state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Library)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>()

    // Library & bookmarks State
    private val _bookmarks = MutableStateFlow<List<AnimeInfo>>(emptyList())
    val bookmarks: StateFlow<List<AnimeInfo>> = _bookmarks.asStateFlow()

    // Browse AnimePahe state
    private val _searchState = MutableStateFlow<LceState<List<AnimeInfo>>>(LceState.Idle)
    val searchState: StateFlow<LceState<List<AnimeInfo>>> = _searchState.asStateFlow()

    var searchQuery by mutableStateOf("")
        private set

    // Selected Anime Details
    private val _episodesState = MutableStateFlow<LceState<List<EpisodeInfo>>>(LceState.Idle)
    val episodesState: StateFlow<LceState<List<EpisodeInfo>>> = _episodesState.asStateFlow()

    // Selected Player Videos
    private val _videoSourcesState = MutableStateFlow<LceState<List<VideoSource>>>(LceState.Idle)
    val videoSourcesState: StateFlow<LceState<List<VideoSource>>> = _videoSourcesState.asStateFlow()

    // Real video play url (fully resolved link)
    private val _resolvedVideoUrl = MutableStateFlow<String?>(null)
    val resolvedVideoUrl: StateFlow<String?> = _resolvedVideoUrl.asStateFlow()

    // Universal Browser state
    var browserUrl by mutableStateOf("https://google.com")
        private set
    var detectedStreamUrl by mutableStateOf<String?>(null)
        private set

    init {
        loadBookmarks()
    }

    fun navigateTo(screen: Screen) {
        screenStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateToBottomTab(screen: Screen) {
        // Don't add to backstack for root tabs
        screenStack.clear()
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        if (screenStack.isNotEmpty()) {
            _currentScreen.value = screenStack.removeAt(screenStack.size - 1)
            return true
        }
        return false
    }

    fun loadBookmarks() {
        _bookmarks.value = settingsManager.getBookmarks()
    }

    fun toggleBookmark(anime: AnimeInfo) {
        if (settingsManager.isBookmarked(anime)) {
            settingsManager.removeBookmark(anime)
        } else {
            settingsManager.addBookmark(anime)
        }
        loadBookmarks()
    }

    fun isBookmarked(anime: AnimeInfo): Boolean {
        return settingsManager.isBookmarked(anime)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
    }

    fun loadLatestReleases(force: Boolean = false) {
        if (!force && _searchState.value is LceState.Success && searchQuery.trim().isEmpty()) return // already loaded
        
        _searchState.value = LceState.Loading
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    activeSource.getLatestReleases()
                }
                _searchState.value = LceState.Success(results)
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Latest releases failure", e)
                _searchState.value = LceState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun performSearch() {
        if (searchQuery.trim().isEmpty()) return
        
        _searchState.value = LceState.Loading
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    activeSource.search(searchQuery)
                }
                _searchState.value = LceState.Success(results)
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Search failure", e)
                _searchState.value = LceState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun loadEpisodes(anime: AnimeInfo) {
        _episodesState.value = LceState.Loading
        viewModelScope.launch {
            try {
                val results = withContext(Dispatchers.IO) {
                    activeSource.getEpisodes(anime.session)
                }
                _episodesState.value = LceState.Success(results)
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Load episodes failure", e)
                _episodesState.value = LceState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun loadVideoSourcesAndPlay(anime: AnimeInfo, episode: EpisodeInfo, context: Context? = null) {
        _videoSourcesState.value = LceState.Loading
        _resolvedVideoUrl.value = null
        
        viewModelScope.launch {
            try {
                val sources = withContext(Dispatchers.IO) {
                    activeSource.getVideoLinks(anime.session, episode.session)
                }
                _videoSourcesState.value = LceState.Success(sources)
                
                if (sources.isNotEmpty()) {
                    // Try to pick selected / preferred quality
                    val preferred = settingsManager.preferredQuality
                    val matchedSource = sources.find { it.quality.contains(preferred) } ?: sources.first()
                    
                    resolveStreamLink(matchedSource, context)
                } else {
                    _videoSourcesState.value = LceState.Error("No stream sources found for this episode.")
                }
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Load video failure", e)
                _videoSourcesState.value = LceState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }

    fun resolveStreamLink(source: VideoSource, context: Context? = null) {
        viewModelScope.launch {
            _resolvedVideoUrl.value = null
            try {
                val resolved = withContext(Dispatchers.IO) {
                    if (source.isDirect) {
                        source.url
                    } else {
                        val kwik = (activeSource as? AnimePaheSource)?.resolveKwikUrl(source.url)
                        if (kwik != null) {
                            kwik
                        } else {
                            val activeContext = context ?: settingsManager.context
                            val extracted = WebViewFetcher.extractStreamUrl(activeContext, source.url)
                            extracted ?: ""
                        }
                    }
                }
                Log.d("StreamViewModel", "Stream successfully resolved: $resolved")
                _resolvedVideoUrl.value = resolved
            } catch (e: Exception) {
                Log.e("StreamViewModel", "Decryption error", e)
            }
        }
    }

    fun setResolvedVideoUrlOverride(url: String) {
        _resolvedVideoUrl.value = url
    }

    fun setBrowserUrlState(url: String) {
        browserUrl = url
        detectedStreamUrl = null
    }

    fun setDetectedBrowserStream(url: String) {
        Log.d("StreamViewModel", "Captured raw web stream url: $url")
        detectedStreamUrl = url
    }

    fun playDetectedBrowserStream() {
        val stream = detectedStreamUrl ?: return
        
        // Build a mock anime info and episode info for playback
        val mockAnime = AnimeInfo(
            id = "-1",
            title = "Extracted Stream",
            type = "Web",
            episodes = 1,
            status = "Playing",
            season = "Now",
            session = "extracted",
            poster = null
        )
        val mockEpisode = EpisodeInfo(
            id = "-1",
            episodeNum = 1.0f,
            title = "Web Video Stream",
            session = "webstream",
            snapshot = null,
            duration = "Unknown"
        )
        
        _videoSourcesState.value = LceState.Success(
            listOf(VideoSource(quality = "Direct Source", url = stream, isDirect = true))
        )
        _resolvedVideoUrl.value = stream
        navigateTo(Screen.Player(mockAnime, mockEpisode))
    }
}
