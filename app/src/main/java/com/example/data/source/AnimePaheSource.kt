package com.example.data.source

import android.util.Log
import com.example.data.model.AnimeInfo
import com.example.data.model.EpisodeInfo
import com.example.data.model.VideoSource
import com.example.data.store.SettingsManager
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

class AnimePaheSource(private val settingsManager: SettingsManager) : ExtensionSource {

    override val name: String = "AnimePahe"
    
    override val baseUrl: String
        get() = settingsManager.animePaheDomain.trimEnd('/')

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    override suspend fun search(query: String): List<AnimeInfo> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "$baseUrl/api?m=search&q=$encodedQuery"
        Log.d("AnimePaheSource", "Searching: $url")
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val body = response.body?.string() ?: return emptyList()
                
                val json = JSONObject(body)
                val data = json.optJSONArray("data") ?: return emptyList()
                val list = mutableListOf<AnimeInfo>()
                
                for (i in 0 until data.length()) {
                    val item = data.getJSONObject(i)
                    list.add(
                        AnimeInfo(
                            id = item.optString("id", ""),
                            title = item.optString("title", ""),
                            type = item.optString("type", ""),
                            episodes = item.optInt("episodes", 0),
                            status = item.optString("status", ""),
                            season = item.optString("season", ""),
                            session = item.optString("session", ""),
                            poster = item.optString("poster", "")
                        )
                    )
                }
                return list
            }
        } catch (e: Exception) {
            Log.e("AnimePaheSource", "Search error", e)
            return emptyList()
        }
    }

    override suspend fun getEpisodes(animeSession: String): List<EpisodeInfo> {
        val list = mutableListOf<EpisodeInfo>()
        var page = 1
        var hasNextPage = true

        while (hasNextPage && page <= 5) { // Limit to 5 pages (150 eps) for speed
            val url = "$baseUrl/api?m=release&id=$animeSession&sort=asc&page=$page"
            Log.d("AnimePaheSource", "Fetching episodes: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        hasNextPage = false
                        return@use
                    }
                    val body = response.body?.string()
                    if (body == null) {
                        hasNextPage = false
                        return@use
                    }
                    val json = JSONObject(body)
                    val data = json.optJSONArray("data")
                    if (data == null) {
                        hasNextPage = false
                        return@use
                    }
                    
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        list.add(
                            EpisodeInfo(
                                id = item.optString("id", ""),
                                episodeNum = item.optDouble("episode", 0.0).toFloat(),
                                title = item.optString("title", null),
                                session = item.optString("session", ""),
                                snapshot = item.optString("snapshot", ""),
                                duration = item.optString("duration", "")
                            )
                        )
                    }
                    
                    val lastPage = json.optInt("last_page", 1)
                    val currentPage = json.optInt("current_page", 1)
                    hasNextPage = currentPage < lastPage
                    page++
                }
            } catch (e: Exception) {
                Log.e("AnimePaheSource", "GetEpisodes error", e)
                hasNextPage = false
            }
        }
        return list
    }

    override suspend fun getVideoLinks(animeSession: String, episodeSession: String): List<VideoSource> {
        // Step 1: Scrape the play page: https://animepahe.ru/play/<animeSession>/<episodeSession>
        val playUrl = "$baseUrl/play/$animeSession/$episodeSession"
        Log.d("AnimePaheSource", "Loading play page: $playUrl")
        
        val request = Request.Builder()
            .url(playUrl)
            .header("User-Agent", userAgent)
            .build()
            
        val sources = mutableListOf<VideoSource>()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val html = response.body?.string() ?: return emptyList()
                
                // Parse kwik links from elements like data-src or embed anchors
                val doc = Jsoup.parse(html)
                
                // Try looking in resolution menu or script tag containing resolutions JSON
                // Resolutions often look like: $('#resolutionMenu').append("<a href='...' ...>720p</a>")
                // Or standard dropdown/buttons
                val menuItems = doc.select(".dropdown-menu a, button[data-src], a[href*=\"kwik.cx\"]")
                for (item in menuItems) {
                    val kwikUrl = item.attr("data-src").ifEmpty { item.attr("href") }
                    if (kwikUrl.contains("kwik.cx")) {
                        val label = item.text().ifEmpty { "Stream" }
                        sources.add(
                            VideoSource(
                                quality = label,
                                url = kwikUrl,
                                isDirect = false,
                                headers = mapOf("Referer" to "$baseUrl/")
                            )
                        )
                    }
                }
                
                // Backup Regex match for Kwik embed URLs anywhere in the page
                if (sources.isEmpty()) {
                    val kwikRegex = Regex("https?://(?:www\\.)?kwik\\.cx/e/\\w+")
                    val matches = kwikRegex.findAll(html)
                    var index = 1
                    for (match in matches) {
                        sources.add(
                            VideoSource(
                                quality = "Stream $index (Kwik)",
                                url = match.value,
                                isDirect = false,
                                headers = mapOf("Referer" to "$baseUrl/")
                            )
                        )
                        index++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AnimePaheSource", "GetVideoLinks scrape error", e)
        }
        
        return sources
    }

    // Resolves a Kwik embed URL to its underlying play url (.m3u8 or .mp4) using P.A.C.K.E.R. decrypter
    fun resolveKwikUrl(kwikUrl: String): String? {
        Log.d("AnimePaheSource", "Resolving Kwik Embed: $kwikUrl")
        val request = Request.Builder()
            .url(kwikUrl.replace("/e/", "/f/").replace("/p/", "/e/")) // Try direct pages if needed
            .header("User-Agent", userAgent)
            .header("Referer", "$baseUrl/")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val html = response.body?.string() ?: return null
                
                // Look for packed JS blocks: eval(function(p,a,c,k,e,d)...)
                val packerRegex = Regex("eval\\s*\\(\\s*function\\(p,a,c,k,e,d\\).*?\\.split\\('\\|'\\).*?\\)\\s*\\)")
                val packerMatch = packerRegex.find(html) ?: return extractDirectSrcFromHtml(html)
                
                val unpacked = unpack(packerMatch.value)
                if (unpacked != null) {
                    Log.d("AnimePaheSource", "Unpacked JS: $unpacked")
                    // Look for const source = '...' or similar
                    val sourceRegex = Regex("source\\s*=\\s*'([^']+)'")
                    val sourceMatch = sourceRegex.find(unpacked)
                    if (sourceMatch != null) {
                        return sourceMatch.groupValues[1]
                    }
                    
                    // Try alternative regex matches
                    val urlRegex = Regex("https?://[a-zA-Z0-9_.-]+/hls/[a-zA-Z0-9_/.-]+\\.m3u8")
                    val urlMatch = urlRegex.find(unpacked)
                    if (urlMatch != null) {
                        return urlMatch.value
                    }
                    
                    val mp4Regex = Regex("https?://[a-zA-Z0-9_.-]+/videos/[a-zA-Z0-9_/.-]+\\.mp4")
                    val mp4Match = mp4Regex.find(unpacked)
                    if (mp4Match != null) {
                        return mp4Match.value
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AnimePaheSource", "Error parsing Kwik embed", e)
        }
        return null
    }

    private fun extractDirectSrcFromHtml(html: String): String? {
        // Fallback checks for direct source or embed tags
        val videoRegex = Regex("<source[^>]+src=\"([^\"]+)\"")
        val match = videoRegex.find(html)
        return match?.groupValues?.get(1)
    }

    // Modern base-A decrypter for packer JS script
    private fun unpack(packedJS: String): String? {
        try {
            // Find parameters wrapped in the outer parenthesis: }('packed_text', 10, 20, 'replacement|list'.split('|'), 0, {}))
            val paramsStartIndex = packedJS.lastIndexOf("}(")
            if (paramsStartIndex == -1) return null
            
            // Extract core arguments block
            val argsBlock = packedJS.substring(paramsStartIndex + 2, packedJS.length - 1)
            
            // Parse arguments. Since parameters can contain commas, let's extract them securely.
            // Argument 1: Packed string (wrapped in single or double quotes)
            val quotesChar = if (argsBlock.startsWith("'")) '\'' else '"'
            var endQuoteIndex = argsBlock.indexOf(quotesChar, 1)
            while (endQuoteIndex != -1 && argsBlock[endQuoteIndex - 1] == '\\') {
                endQuoteIndex = argsBlock.indexOf(quotesChar, endQuoteIndex + 1)
            }
            if (endQuoteIndex == -1) return null
            val p = argsBlock.substring(1, endQuoteIndex).replace("\\'", "'").replace("\\\\", "\\")
            
            // The rest is like: ,62,71,'a|b|c'.split('|')
            val trailingPart = argsBlock.substring(endQuoteIndex + 2)
            val parts = trailingPart.split(",")
            if (parts.size < 3) return null
            
            val a = parts[0].trim().toInt()
            val c = parts[1].trim().toInt()
            
            // Parse the keys array string
            val keysStringRaw = trailingPart.substringAfter("'").substringBefore("'")
            val k = keysStringRaw.split("|")
            
            // Reconstruct replacement table
            return decode(p, a, c, k)
        } catch (e: Exception) {
            Log.e("AnimePaheSource", "Unpack error", e)
        }
        return null
    }

    private fun decode(p: String, a: Int, c: Int, k: List<String>): String {
        var unpacked = p
        
        // Base-62 encoder table
        val chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        
        fun toBase(num: Int, base: Int): String {
            if (num == 0) return "0"
            var temp = num
            val sb = StringBuilder()
            while (temp > 0) {
                sb.append(chars[temp % base])
                temp /= base
            }
            return sb.reverse().toString()
        }

        for (i in c - 1 downTo 0) {
            val rep = k.getOrNull(i)
            if (!rep.isNullOrEmpty()) {
                val baseVal = toBase(i, a)
                // Escape regex characters
                val regex = Regex("\\b$baseVal\\b")
                unpacked = unpacked.replace(regex, rep)
            }
        }
        return unpacked
    }
}
