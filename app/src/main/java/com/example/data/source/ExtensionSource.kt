package com.example.data.source

import com.example.data.model.AnimeInfo
import com.example.data.model.EpisodeInfo
import com.example.data.model.VideoSource

interface ExtensionSource {
    val name: String
    val baseUrl: String
    
    suspend fun search(query: String): List<AnimeInfo>
    suspend fun getEpisodes(animeSession: String): List<EpisodeInfo>
    suspend fun getVideoLinks(animeSession: String, episodeSession: String): List<VideoSource>
}
