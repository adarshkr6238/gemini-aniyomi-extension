package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.Screen
import com.example.ui.viewmodel.StreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    var showAddSiteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sources", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Extensions",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Default extension
            item {
                CustomSiteItem(
                    name = "AnimePahe",
                    url = viewModel.settingsManager.animePaheDomain,
                    // Open Native UI instead of browser!
                    onClick = { viewModel.navigateTo(Screen.SourceBrowse("AnimePahe")) },
                    onWebClick = { 
                        viewModel.setBrowserUrlState(viewModel.settingsManager.animePaheDomain)
                        viewModel.navigateTo(Screen.UniversalBrowser(viewModel.settingsManager.animePaheDomain)) 
                    },
                    onDelete = null
                )
            }
            
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Text(
                        text = "Custom Sites",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { showAddSiteDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add custom site", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            val customSites = viewModel.settingsManager.getCustomSites()
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    customSites.forEach { (name, url) ->
                        CustomSiteItem(
                            name = name,
                            url = url,
                            onClick = { 
                                // Open Native App Interface for Custom Sites
                                viewModel.navigateTo(Screen.SourceBrowse(name))
                            },
                            onWebClick = {
                                viewModel.setBrowserUrlState(url)
                                viewModel.navigateTo(Screen.UniversalBrowser(url))
                            },
                            onDelete = {
                                viewModel.settingsManager.removeCustomSite(name)
                                viewModel.loadBookmarks() // Force trigger state reset/recomposites
                            }
                        )
                    }
                }
            }
        }

        if (showAddSiteDialog) {
            AddSiteDialog(
                onDismiss = { showAddSiteDialog = false },
                onConfirm = { name, url ->
                    viewModel.settingsManager.addCustomSite(name, url)
                    showAddSiteDialog = false
                    viewModel.loadBookmarks()
                }
            )
        }
    }
}

@Composable
fun CustomSiteItem(
    name: String,
    url: String,
    onClick: () -> Unit,
    onWebClick: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(32.dp).padding(4.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onWebClick) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = "Browse WebView",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun AddSiteDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("https://") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Universal Site") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Add any video streaming site. You can browse it inside our ad-blocking browser and play streams in the native player!",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Site Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Website Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty() && url.trim().isNotEmpty() && url.startsWith("http")) {
                        onConfirm(name, url)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Add Site")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
