package com.example.data.source

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object WebViewFetcher {
    private const val TAG = "WebViewFetcher"

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchHtml(context: Context, url: String, waitTimeMs: Long = 3000): String {
        return suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                try {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                    var isResumed = false
                    val timeoutRunnable = Runnable {
                        if (!isResumed) {
                            isResumed = true
                            try {
                                webView.evaluateJavascript(
                                    "(function() { return document.documentElement.outerHTML; })();"
                                ) { html ->
                                    val decoded = html?.removePrefix("\"")?.removeSuffix("\"")
                                        ?.replace("\\u003C", "<")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\n", "\n")
                                    if (continuation.isActive) {
                                        continuation.resume(decoded ?: "")
                                    }
                                    try {
                                        webView.destroy()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error destroying WebView", e)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Exception during JS evaluation in fetchHtml", e)
                                if (continuation.isActive) {
                                    continuation.resume("")
                                }
                                try { webView.destroy() } catch (ex: Exception) {}
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            handler.postDelayed(timeoutRunnable, waitTimeMs)
                        }

                        override fun onReceivedHttpError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            errorResponse: WebResourceResponse?
                        ) {
                            super.onReceivedHttpError(view, request, errorResponse)
                        }
                    }

                    continuation.invokeOnCancellation {
                        handler.removeCallbacks(timeoutRunnable)
                        handler.post {
                            try {
                                webView.destroy()
                            } catch (ex: Exception) {}
                        }
                    }

                    webView.loadUrl(url)
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal Exception during WebView creation in fetchHtml", e)
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun fetchJson(context: Context, htmlUrl: String, apiUrl: String, waitTimeMs: Long = 3000): String {
        return suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                try {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                    var isResumed = false
                    val timeoutRunnable = Runnable {
                        if (!isResumed) {
                            isResumed = true
                            try {
                                webView.evaluateJavascript(
                                    "(function() { " +
                                    "  var request = new XMLHttpRequest();" +
                                    "  request.open('GET', '$apiUrl', false); " +
                                    "  request.send(null); " +
                                    "  return request.responseText; " +
                                    "})();"
                                ) { stringifiedJson ->
                                    val decoded = stringifiedJson?.removePrefix("\"")?.removeSuffix("\"")
                                        ?.replace("\\\"", "\"")
                                        ?.replace("\\'", "'")
                                        ?.replace("\\n", "\n")
                                        ?.replace("\\\\", "\\")
                                    if (continuation.isActive) {
                                        continuation.resume(decoded ?: "")
                                    }
                                    try {
                                        webView.destroy()
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error destroying WebView", e)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Exception during JS evaluation in fetchJson", e)
                                if (continuation.isActive) {
                                    continuation.resume("")
                                }
                                try { webView.destroy() } catch (ex: Exception) {}
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            handler.postDelayed(timeoutRunnable, waitTimeMs)
                        }
                    }

                    continuation.invokeOnCancellation {
                        handler.removeCallbacks(timeoutRunnable)
                        handler.post {
                            try {
                                webView.destroy()
                            } catch (ex: Exception) {}
                        }
                    }

                    webView.loadUrl(htmlUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal Exception during WebView creation in fetchJson", e)
                    if (continuation.isActive) {
                        continuation.resume("")
                    }
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun extractStreamUrl(context: Context, pageUrl: String, waitTimeMs: Long = 10000): String? {
        return suspendCancellableCoroutine { continuation ->
            val handler = Handler(Looper.getMainLooper())
            handler.post {
                try {
                    val webView = WebView(context)
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

                    var isResumed = false
                    val timeoutRunnable = Runnable {
                        if (!isResumed) {
                            isResumed = true
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                            try {
                                webView.destroy()
                            } catch (e: Exception) {
                                Log.e(TAG, "Error destroying WebView", e)
                            }
                        }
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val urlStr = request?.url?.toString() ?: ""
                            if (urlStr.contains(".m3u8") || urlStr.contains(".mp4")) {
                                if (!isResumed) {
                                    isResumed = true
                                    if (continuation.isActive) {
                                        continuation.resume(urlStr)
                                    }
                                    handler.removeCallbacks(timeoutRunnable)
                                    handler.post {
                                        try {
                                            webView.destroy()
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Error destroying WebView from shouldInterceptRequest", e)
                                        }
                                    }
                                }
                            }
                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            handler.postDelayed(timeoutRunnable, waitTimeMs)
                        }
                    }

                    continuation.invokeOnCancellation {
                        handler.removeCallbacks(timeoutRunnable)
                        handler.post {
                            try {
                                webView.destroy()
                            } catch (ex: Exception) {}
                        }
                    }

                    webView.loadUrl(pageUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Fatal Exception during WebView creation in extractStreamUrl", e)
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            }
        }
    }
}
