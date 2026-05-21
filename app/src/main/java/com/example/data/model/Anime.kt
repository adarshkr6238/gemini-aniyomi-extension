package com.example.data.model

import java.io.Serializable

data class AnimeInfo(
    val id: String,
    val title: String,
    val type: String?,
    val episodes: Int?,
    val status: String?,
    val season: String?,
    val session: String,
    val poster: String?
) : Serializable

data class EpisodeInfo(
    val id: String,
    val episodeNum: Float,
    val title: String?,
    val session: String,
    val snapshot: String?,
    val duration: String?
) : Serializable

data class VideoSource(
    val quality: String,
    val url: String,
    val isDirect: Boolean = false,
    val headers: Map<String, String> = emptyMap()
) : Serializable
