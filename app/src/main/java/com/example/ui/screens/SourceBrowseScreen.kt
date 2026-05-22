package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnimeInfo
import com.example.ui.viewmodel.LceState
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StreamViewModel

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceBrowseScreen(
    viewModel: StreamViewModel,
    sourceName: String,
    modifier: Modifier = Modifier
) {
    val searchState by viewModel.searchState.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current

    var showSettingsDialog by remember { mutableStateOf(false) }
    
    var sortOrder by remember { mutableStateOf("Title") } // "Title" or "Episodes"
    var filterType by remember { mutableStateOf("All") } // "All", "Anime"
    
    val pullRefreshState = rememberPullToRefreshState()
    val isRefreshing = searchState is LceState.Loading

    LaunchedEffect(sourceName) {
        viewModel.setActiveSource(sourceName)
        if (viewModel.searchQuery.isEmpty()) {
            viewModel.loadLatestReleases()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(sourceName, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val url = if (sourceName == "AnimePahe") {
                            viewModel.settingsManager.animePaheDomain
                        } else {
                            viewModel.settingsManager.getCustomSites().find { it.first == sourceName }?.second ?: viewModel.settingsManager.animePaheDomain
                        }
                        viewModel.setBrowserUrlState(url)
                        viewModel.navigateTo(Screen.UniversalBrowser(url)) 
                    }) {
                        Icon(Icons.Default.Public, contentDescription = "Open in WebView")
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { 
                if (viewModel.searchQuery.isNotEmpty()) {
                    viewModel.performSearch()
                } else {
                    // Force refresh by setting state back to Idle
                    viewModel.loadLatestReleases(force = true)
                }
            },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            
            // Search Bar
            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search AnimePahe...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (viewModel.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { 
                                viewModel.updateSearchQuery("")
                                viewModel.loadLatestReleases()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        viewModel.performSearch()
                        keyboardController?.hide()
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    var showSortMenu by remember { mutableStateOf(false) }

                    Box {
                        FilterChip(
                            selected = filterType != "All",
                            onClick = { showFilterMenu = true },
                            label = { Text(if (filterType == "All") "Filter" else filterType) },
                            leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Filter", modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(expanded = showFilterMenu, onDismissRequest = { showFilterMenu = false }) {
                            DropdownMenuItem(text = { Text("All") }, onClick = { filterType = "All"; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Anime") }, onClick = { filterType = "Anime"; showFilterMenu = false })
                            DropdownMenuItem(text = { Text("Latest") }, onClick = { filterType = "Latest"; showFilterMenu = false })
                        }
                    }
                    
                    Box {
                        FilterChip(
                            selected = sortOrder != "Title",
                            onClick = { showSortMenu = true },
                            label = { Text(if (sortOrder == "Title") "Sort" else sortOrder) },
                            leadingIcon = { Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Title") }, onClick = { sortOrder = "Title"; showSortMenu = false })
                            DropdownMenuItem(text = { Text("Episodes") }, onClick = { sortOrder = "Episodes"; showSortMenu = false })
                        }
                    }
                }
            }

            // Display Search Results or Regular Hub View
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = if (viewModel.searchQuery.trim().isNotEmpty()) "Search Results" else "Latest Releases",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            when (val state = searchState) {
                is LceState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                is LceState.Error -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
                is LceState.Success -> {
                    val filteredData = state.data.filter {
                        if (filterType == "All") true else it.type.equals(filterType, ignoreCase = true)
                    }
                    val sortedData = when (sortOrder) {
                        "Episodes" -> filteredData.sortedByDescending { it.episodes }
                        else -> filteredData.sortedBy { it.title }
                    }
                    
                    if (sortedData.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No results found. Try adjusting domains in Settings.",
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        items(sortedData.size) { index ->
                            val anime = sortedData[index]
                            SearchAnimeItem(
                                anime = anime,
                                onClick = {
                                    viewModel.loadEpisodes(anime)
                                    viewModel.navigateTo(Screen.Details(anime))
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                is LceState.Idle -> {
                    // Empty state (only seen right upon load before latest releases trigger)
                }
            }
        }
        }
        
        if (showSettingsDialog) {
            SiteSettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun SearchAnimeItem(
    anime: AnimeInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(115.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                if (!anime.poster.isNullOrEmpty()) {
                    AsyncImage(
                        model = anime.poster,
                        contentDescription = anime.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(115.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = anime.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        anime.type?.let {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(it, fontSize = 10.sp) },
                                modifier = Modifier.height(20.dp)
                            )
                        }
                        anime.status?.let {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(it, fontSize = 10.sp) },
                                modifier = Modifier.height(20.dp)
                            )
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Tv,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${anime.episodes ?: "?"} Episodes",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "GO",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BookmarkAnimeCard(
    anime: AnimeInfo,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                if (!anime.poster.isNullOrEmpty()) {
                    AsyncImage(
                        model = anime.poster,
                        contentDescription = anime.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Movie, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }

            // Dark fading gradient at the bottom for title contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.3f), Color.Black),
                            startY = 180f
                        )
                    )
            )

            // Title labels
            Text(
                text = anime.title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun SiteSettingsDialog(
    viewModel: StreamViewModel,
    onDismiss: () -> Unit
) {
    var domain by remember { mutableStateOf(viewModel.settingsManager.animePaheDomain) }
    var quality by remember { mutableStateOf(viewModel.settingsManager.preferredQuality) }
    var audio by remember { mutableStateOf(viewModel.settingsManager.audioLanguage) }
    var subtitleSize by remember { mutableStateOf(viewModel.settingsManager.subtitleSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Extension Settings")
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Site Domain") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Column {
                    Text("Quality", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("1080p", "720p", "480p").forEach { res ->
                            FilterChip(
                                selected = (quality == res),
                                onClick = { quality = res },
                                label = { Text(res) }
                            )
                        }
                    }
                }
                
                Column {
                    Text("Audio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Subbed", "Dubbed").forEach { type ->
                            FilterChip(
                                selected = (audio == type),
                                onClick = { audio = type },
                                label = { Text(type) }
                            )
                        }
                    }
                }
                
                Column {
                    Text("Subtitle Size", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Small", "Medium", "Large").forEach { size ->
                            FilterChip(
                                selected = (subtitleSize == size),
                                onClick = { subtitleSize = size },
                                label = { Text(size) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.settingsManager.animePaheDomain = domain
                    viewModel.settingsManager.preferredQuality = quality
                    viewModel.settingsManager.audioLanguage = audio
                    viewModel.settingsManager.subtitleSize = subtitleSize
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
