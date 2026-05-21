package com.example.data.store

import android.content.Context
import android.util.Log
import com.example.data.model.AnimeInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("aniyomi_stream_prefs", Context.MODE_PRIVATE)
    
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val animeAdapter = moshi.adapter<List<AnimeInfo>>(
        Types.newParameterizedType(List::class.java, AnimeInfo::class.java)
    )

    companion object {
        private const val KEY_ANIMEPAHE_DOMAIN = "animepahe_domain"
        private const val DEFAULT_ANIMEPAHE_DOMAIN = "https://animepahe.ru"
        
        private const val KEY_PREFERRED_QUALITY = "preferred_quality"
        private const val DEFAULT_QUALITY = "720p"
        
        private const val KEY_AUDIO_LANGUAGE = "audio_language"
        private const val DEFAULT_AUDIO_LANGUAGE = "Subbed"
        
        private const val KEY_SUBTITLE_SIZE = "subtitle_size"
        private const val DEFAULT_SUBTITLE_SIZE = "Medium"
        
        private const val KEY_ADBLOCK_ENABLED = "adblock_enabled"
        private const val KEY_BLOCK_POPUP_ENABLED = "block_popup_enabled"
        
        private const val KEY_BOOKMARKS = "bookmarked_animes"
        
        private const val KEY_CUSTOM_SITES = "custom_streaming_sites"
    }

    var animePaheDomain: String
        get() = prefs.getString(KEY_ANIMEPAHE_DOMAIN, DEFAULT_ANIMEPAHE_DOMAIN) ?: DEFAULT_ANIMEPAHE_DOMAIN
        set(value) = prefs.edit().putString(KEY_ANIMEPAHE_DOMAIN, value).apply()

    var preferredQuality: String
        get() = prefs.getString(KEY_PREFERRED_QUALITY, DEFAULT_QUALITY) ?: DEFAULT_QUALITY
        set(value) = prefs.edit().putString(KEY_PREFERRED_QUALITY, value).apply()

    var audioLanguage: String
        get() = prefs.getString(KEY_AUDIO_LANGUAGE, DEFAULT_AUDIO_LANGUAGE) ?: DEFAULT_AUDIO_LANGUAGE
        set(value) = prefs.edit().putString(KEY_AUDIO_LANGUAGE, value).apply()

    var subtitleSize: String
        get() = prefs.getString(KEY_SUBTITLE_SIZE, DEFAULT_SUBTITLE_SIZE) ?: DEFAULT_SUBTITLE_SIZE
        set(value) = prefs.edit().putString(KEY_SUBTITLE_SIZE, value).apply()

    var isAdBlockEnabled: Boolean
        get() = prefs.getBoolean(KEY_ADBLOCK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_ADBLOCK_ENABLED, value).apply()

    var isBlockPopupEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_POPUP_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_POPUP_ENABLED, value).apply()

    fun getBookmarks(): List<AnimeInfo> {
        val json = prefs.getString(KEY_BOOKMARKS, null) ?: return emptyList()
        return try {
            animeAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error parsing bookmarks", e)
            emptyList()
        }
    }

    fun addBookmark(anime: AnimeInfo) {
        val list = getBookmarks().toMutableList()
        if (list.none { it.session == anime.session }) {
            list.add(anime)
            saveBookmarks(list)
        }
    }

    fun removeBookmark(anime: AnimeInfo) {
        val list = getBookmarks().filterNot { it.session == anime.session }
        saveBookmarks(list)
    }

    fun isBookmarked(anime: AnimeInfo): Boolean {
        return getBookmarks().any { it.session == anime.session }
    }

    private fun saveBookmarks(list: List<AnimeInfo>) {
        try {
            val json = animeAdapter.toJson(list)
            prefs.edit().putString(KEY_BOOKMARKS, json).apply()
        } catch (e: Exception) {
            Log.e("SettingsManager", "Error saving bookmarks", e)
        }
    }

    fun getCustomSites(): List<Pair<String, String>> {
        val json = prefs.getString(KEY_CUSTOM_SITES, null) ?: return listOf(
            "KickassAnime" to "https://kickassanime.am",
            "Gogoanime" to "https://gogoanime3.co",
            "AnimeSuge" to "https://animesuge.to"
        )
        return try {
            json.split(";;;").filter { it.isNotEmpty() }.map {
                val split = it.split("|||")
                split[0] to split[1]
            }
        } catch (e: Exception) {
            listOf("Gogoanime" to "https://gogoanime3.co")
        }
    }

    fun addCustomSite(name: String, url: String) {
        val current = getCustomSites().toMutableList()
        if (current.none { it.second == url }) {
            current.add(name to url)
            saveCustomSites(current)
        }
    }

    fun removeCustomSite(name: String) {
        val current = getCustomSites().filterNot { it.first == name }
        saveCustomSites(current)
    }

    private fun saveCustomSites(list: List<Pair<String, String>>) {
        val serialized = list.joinToString(";;;") { "${it.first}|||${it.second}" }
        prefs.edit().putString(KEY_CUSTOM_SITES, serialized).apply()
    }
}
