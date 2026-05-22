package com.example.data.source

import android.content.Context
import android.util.Log
import com.example.data.model.AnimeInfo
import com.example.data.model.EpisodeInfo
import com.example.data.model.VideoSource
import org.jsoup.Jsoup
import java.net.URLEncoder

class GenericHeuristicSource(
    private val customUrl: String,
    override val name: String,
    private val context: Context
) : ExtensionSource {

    override val baseUrl: String
        get() = customUrl.trimEnd('/')

    private fun extractAnimeFromHtml(html: String, searchMode: Boolean = false): List<AnimeInfo> {
        val doc = Jsoup.parse(html)
        val list = mutableListOf<AnimeInfo>()
        val seenSessions = mutableSetOf<String>()

        // Find standard links that might be anime covers
        val links = doc.select("a")
        for (link in links) {
            val href = link.attr("href")
            if (href.isEmpty() || href == "/" || href.startsWith("#") || href.startsWith("javascript")) continue
            
            // Should have an image inside or nearby
            var img = link.select("img").first()
            if (img == null && link.parent() != null) {
               img = link.parent()?.select("img")?.first()
            }
            
            val poster = img?.attr("src") ?: img?.attr("data-src") ?: ""
            if (poster.isEmpty() && !searchMode) continue // Require images for grid display unless we are desperate
            
            // Text can be in the title attribute, inside the a tag, or nearby
            var title = link.attr("title").trim()
            if (title.isEmpty()) title = img?.attr("alt")?.trim() ?: ""
            if (title.isEmpty()) title = link.text().trim()
            if (title.isEmpty() && link.parent() != null) {
                title = link.parent()?.text()?.trim() ?: ""
            }

            // Clean up href to be relative if needed, but we'll store the full path as session
            val session = if (href.startsWith("http")) href else {
                if (href.startsWith("/")) baseUrl + href else "$baseUrl/$href"
            }

            // Exclude common non-anime links
            val lowerHref = href.lowercase()
            if (lowerHref.contains("login") || lowerHref.contains("register") || lowerHref.contains("contact") || lowerHref.contains("discord") || title.length < 2) continue

            if (!seenSessions.contains(session) && title.isNotEmpty()) {
                seenSessions.add(session)
                list.add(
                    AnimeInfo(
                        id = session,
                        title = title,
                        type = "Heuristic",
                        episodes = 0,
                        status = "",
                        season = "",
                        session = session,
                        poster = poster
                    )
                )
            }
        }
        
        // Filter out obvious noise
        return list.filter { it.title.length > 2 && it.title.length < 100 }.take(30)
    }

    override suspend fun getLatestReleases(): List<AnimeInfo> {
        Log.d("GenericSource", "Fetching generic releases: $baseUrl")
        return try {
            val html = WebViewFetcher.fetchHtml(context, baseUrl)
            extractAnimeFromHtml(html)
        } catch (e: Exception) {
            Log.e("GenericSource", "Releases error", e)
            emptyList()
        }
    }

    override suspend fun search(query: String): List<AnimeInfo> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        
        // Try common search paths
        val possibleUrls = listOf(
            "$baseUrl/search.html?keyword=$encodedQuery",
            "$baseUrl/search?keyword=$encodedQuery",
            "$baseUrl/?s=$encodedQuery"
        )

        for (url in possibleUrls) {
            Log.d("GenericSource", "Searching generic: $url")
            try {
                val html = WebViewFetcher.fetchHtml(context, url)
                val results = extractAnimeFromHtml(html, searchMode = true)
                if (results.isNotEmpty()) {
                    return results
                }
            } catch (e: Exception) {
                Log.e("GenericSource", "Search error on $url", e)
            }
        }
        return emptyList()
    }

    override suspend fun getEpisodes(animeSession: String): List<EpisodeInfo> {
        val url = if (animeSession.startsWith("http")) animeSession else "$baseUrl/$animeSession"
        val list = mutableListOf<EpisodeInfo>()
        val seenUrls = mutableSetOf<String>()

        try {
            val html = WebViewFetcher.fetchHtml(context, url)
            val doc = Jsoup.parse(html)
            val links = doc.select("a")
            
            var epCount = 1
            for (link in links) {
                val href = link.attr("href")
                if (href.isEmpty() || href == "/" || href.startsWith("#") || href.startsWith("javascript")) continue
                
                val text = link.text().lowercase()
                val titleAttr = link.attr("title").lowercase()
                
                // Look for episode indicators
                val isEpisode = text.contains("ep") || text.contains("episode") || titleAttr.contains("ep") || text.matches(Regex("^\\d+$"))
                
                if (isEpisode) {
                    val session = if (href.startsWith("http")) href else {
                        if (href.startsWith("/")) baseUrl + href else "$baseUrl/$href"
                    }
                    if (!seenUrls.contains(session)) {
                        seenUrls.add(session)
                        list.add(
                            EpisodeInfo(
                                id = session,
                                episodeNum = epCount.toFloat(),
                                title = link.text().takeIf { it.isNotEmpty() } ?: "Episode $epCount",
                                session = session,
                                snapshot = "",
                                duration = ""
                            )
                        )
                        epCount++
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("GenericSource", "Episodes error", e)
        }
        return list.reversed() // Usually sites list episodes newest first or oldest first. Reversed is a guess to standard order.
    }

    override suspend fun getVideoLinks(animeSession: String, episodeSession: String): List<VideoSource> {
        val playUrl = if (episodeSession.startsWith("http")) episodeSession else "$baseUrl/$episodeSession"
        return listOf(
            VideoSource(
                quality = "Web Extraction (Auto)",
                url = playUrl, 
                isDirect = false,
                headers = mapOf("Referer" to baseUrl)
            )
        )
    }
}
