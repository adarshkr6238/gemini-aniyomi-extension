package com.example.ui.screens

import android.graphics.Bitmap
import android.os.Message
import android.util.Log
import android.webkit.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.blocker.AdBlocker
import com.example.ui.viewmodel.StreamViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UniversalBrowserScreen(
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }

    val adBlockEnabled = viewModel.settingsManager.isAdBlockEnabled

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Universal Web Browser",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.browserUrl,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { webView?.reload() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            // Display extractor dynamic FAB when streaming links are detected!
            AnimatedVisibility(visible = viewModel.detectedStreamUrl != null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        webView?.stopLoading()
                        viewModel.playDetectedBrowserStream()
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                    text = { Text("Stream Detected: Play Now", fontWeight = FontWeight.Bold) }
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Web Loading Progress indicator
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { webProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }

            // Secure web view container
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportMultipleWindows(false) // Important: Prevents popunder window creation!
                            javaScriptCanOpenWindowsAutomatically = false // Blocks prompt loops
                        }
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                url?.let { viewModel.setBrowserUrlState(it) }
                                // Inject ad block JS initially
                                view?.evaluateJavascript(AdBlocker.AD_BLOCK_JS, null)
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Reinforce ad/popup blocking JS injections once fully loaded
                                view?.evaluateJavascript(AdBlocker.AD_BLOCK_JS, null)
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                val blockPopupEnabled = viewModel.settingsManager.isBlockPopupEnabled
                                if (blockPopupEnabled) {
                                    Log.d("UniversalBrowser", "Popup/Redirect Blocked due to Strict Block mode: $url")
                                    return true // Intercept & block ALL navigation
                                }

                                // Direct safety checking
                                if (adBlockEnabled && AdBlocker.isAd(url)) {
                                    Log.d("UniversalBrowser", "AdBlock blocked redirect navigation: $url")
                                    return true // Intercept & block navigation to ad domains!
                                }
                                return false
                            }

                            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                val url = request?.url?.toString() ?: return null
                                
                                // 1. Scan request URLs for dynamic stream extracts (.m3u8, .mp4 HLS playlists!)
                                if (url.contains(".m3u8") || url.contains(".mp4") || url.contains("/hls/") || url.contains("/stream/")) {
                                    // Make sure it is not from an ad source
                                    if (!AdBlocker.isAd(url)) {
                                        view?.post {
                                            viewModel.setDetectedBrowserStream(url)
                                        }
                                    }
                                }

                                // 2. Ad blocking on network level
                                if (adBlockEnabled && AdBlocker.isAd(url)) {
                                    Log.d("AdBlockNetwork", "Blocked target request: $url")
                                    // Intercept & return an empty response
                                    return AdBlocker.createEmptyResponse()
                                }
                                
                                return super.shouldInterceptRequest(view, request)
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                webProgress = newProgress
                                // Inject adblock code throughout loading phase to prevent sneaky early banners
                                if (newProgress > 30) {
                                    view?.evaluateJavascript(AdBlocker.AD_BLOCK_JS, null)
                                }
                            }

                            override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                Log.d("AdBlocker", "Blocked popunder/window creation attempt")
                                return false // Reject new window popups completely!
                            }
                        }
                        
                        loadUrl(viewModel.browserUrl)
                        webView = this
                    }
                },
                update = { view ->
                    webView = view
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}
