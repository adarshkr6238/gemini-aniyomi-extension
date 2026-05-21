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
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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

                // Overrides physical and gesture back button navigation
                BackHandler(enabled = currentScreen !is Screen.Explore) {
                    viewModel.navigateBack()
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                if (currentScreen is Screen.Player) {
                                    // Full-fade transitions when entering the theater player to be smooth
                                    fadeIn() with fadeOut()
                                } else {
                                    slideInHorizontally(initialOffsetX = { it }) + fadeIn() with
                                            slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
                                }
                            },
                            label = "ScreenTransition"
                        ) { screen ->
                            when (screen) {
                                is Screen.Explore -> ExploreScreen(viewModel = viewModel)
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
