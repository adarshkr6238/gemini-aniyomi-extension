package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    val settings = viewModel.settingsManager

    var customDomain by remember { mutableStateOf(settings.animePaheDomain) }
    var adBlockActive by remember { mutableStateOf(settings.isAdBlockEnabled) }
    var selectedQuality by remember { mutableStateOf(settings.preferredQuality) }
    var selectedLanguage by remember { mutableStateOf(settings.audioLanguage) }
    var selectedSubSize by remember { mutableStateOf(settings.subtitleSize) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Configurations", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // AnimePahe Domain Config
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Extensions Base Domains",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AnimePahe Custom Root Domain",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Update the active URL if the site changes mirror names.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customDomain,
                            onValueChange = {
                                customDomain = it
                                settings.animePaheDomain = it
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            placeholder = { Text("https://animepahe.ru") }
                        )
                    }
                }
            }

            // Media Quality Options
            item {
                Text(
                    text = "Playback & Preferences",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Quality preferences
                        Text(text = "Preferred Video Quality", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1080p", "720p", "360p").forEach { quality ->
                                FilterChip(
                                    selected = selectedQuality == quality,
                                    onClick = {
                                        selectedQuality = quality
                                        settings.preferredQuality = quality
                                    },
                                    label = { Text(quality, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Audio tracks preferences
                        Text(text = "Default Audio Language Mode", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Subbed", "Dubbed").forEach { pref ->
                                FilterChip(
                                    selected = selectedLanguage == pref,
                                    onClick = {
                                        selectedLanguage = pref
                                        settings.audioLanguage = pref
                                    },
                                    label = { Text(pref, fontSize = 12.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subtitle Sizing
                        Text(text = "Subtitle Text Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Small", "Medium", "Large").forEach { size ->
                                FilterChip(
                                    selected = selectedSubSize == size,
                                    onClick = {
                                        selectedSubSize = size
                                        settings.subtitleSize = size
                                    },
                                    label = { Text(size, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Blocker & Interceptor Settings
            item {
                Text(
                    text = "Security & Blocker Filters",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Inbuilt Ad Blocker",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Stops redirect chains, invisible popup overlays, and domains lists.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = adBlockActive,
                                onCheckedChange = {
                                    adBlockActive = it
                                    settings.isAdBlockEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                        var blockPopupActive by remember { mutableStateOf(settings.isBlockPopupEnabled) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Block All Redirects & Clicks",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "When ON, touching the screen won't navigate to new pages. Use when streaming video.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Switch(
                                checked = blockPopupActive,
                                onCheckedChange = {
                                    blockPopupActive = it
                                    settings.isBlockPopupEnabled = it
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Aniyomi Stream v1.0 • Client Extensor",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
