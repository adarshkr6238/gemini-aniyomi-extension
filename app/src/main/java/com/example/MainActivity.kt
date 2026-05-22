package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StreamViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: StreamViewModel by viewModels()

    @OptIn(ExperimentalAnimationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsState()

                // Bottom Nav Logic
                val isBottomNavScreen = currentScreen is Screen.Library || 
                                        currentScreen is Screen.Browse || 
                                        currentScreen is Screen.More
                // Overrides physical and gesture back button navigation
                BackHandler(enabled = !isBottomNavScreen) {
                    viewModel.navigateBack()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (isBottomNavScreen) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Library,
                                    onClick = { if (currentScreen !is Screen.Library) viewModel.navigateToBottomTab(Screen.Library) },
                                    icon = { Icon(Icons.Default.Book, contentDescription = "Library") },
                                    label = { Text("Library") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.Browse,
                                    onClick = { if (currentScreen !is Screen.Browse) viewModel.navigateToBottomTab(Screen.Browse) },
                                    icon = { Icon(Icons.Default.Explore, contentDescription = "Browse") },
                                    label = { Text("Browse") }
                                )
                                NavigationBarItem(
                                    selected = currentScreen is Screen.More,
                                    onClick = { if (currentScreen !is Screen.More) viewModel.navigateToBottomTab(Screen.More) },
                                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More") },
                                    label = { Text("More") }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (targetState is Screen.Player || initialState is Screen.Player) {
                                    // Full-fade transitions when entering the theater player to be smooth
                                    fadeIn() togetherWith fadeOut()
                                } else if (targetState is Screen.Library || targetState is Screen.Browse || targetState is Screen.More) {
                                    fadeIn() togetherWith fadeOut()
                                } else {
                                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() togetherWith
                                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                }
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Library -> LibraryScreen(viewModel = viewModel)
                                is Screen.Browse -> BrowseScreen(viewModel = viewModel)
                                is Screen.More -> MoreScreen(viewModel = viewModel)
                                is Screen.SourceBrowse -> SourceBrowseScreen(viewModel = viewModel, sourceName = screen.sourceName)
                                is Screen.Details -> DetailsScreen(anime = screen.anime, viewModel = viewModel)
                                is Screen.Player -> PlayerScreen(anime = screen.anime, episode = screen.episode, viewModel = viewModel)
                                is Screen.UniversalBrowser -> UniversalBrowserScreen(viewModel = viewModel)
                                is Screen.Settings -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
