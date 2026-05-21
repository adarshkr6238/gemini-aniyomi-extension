package com.example.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Subtitles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.AnimeInfo
import com.example.data.model.EpisodeInfo
import com.example.data.model.VideoSource
import com.example.ui.viewmodel.LceState
import com.example.ui.viewmodel.StreamViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    anime: AnimeInfo,
    episode: EpisodeInfo,
    viewModel: StreamViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val videoSourcesState by viewModel.videoSourcesState.collectAsState()
    val resolvedVideoUrl by viewModel.resolvedVideoUrl.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var isBuffering by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }

    // Dropdown/sheet visibility states
    var showQualitySelector by remember { mutableStateOf(false) }
    var showAudioSelector by remember { mutableStateOf(false) }
    var showSubtitleSelector by remember { mutableStateOf(false) }

    // Setup ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }

                override fun onPlaybackStateChanged(state: Int) {
                    isBuffering = state == Player.STATE_BUFFERING
                    duration = this@apply.duration.coerceAtLeast(0L)
                }
            })
        }
    }

    // Capture standard loop to update seeker bar positions
    val scope = rememberCoroutineScope()
    DisposableEffect(exoPlayer) {
        val job = scope.launch {
            while (true) {
                playbackPosition = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    duration = exoPlayer.duration
                }
                delay(1000)
            }
        }
        onDispose {
            job.cancel()
            exoPlayer.release()
        }
    }

    // Auto-hide controls effect
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Load stream link in ExoPlayer when successfully decrypted/selected
    LaunchedEffect(resolvedVideoUrl) {
        if (!resolvedVideoUrl.isNullOrEmpty()) {
            exoPlayer.stop()
            val mediaItem = MediaItem.fromUri(resolvedVideoUrl!!)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedVideoUrl.isNullOrEmpty()) {
            // Sources Loading or parsing state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                when (val state = videoSourcesState) {
                    is LceState.Loading -> {
                        Text(
                            text = "Extracting stream, ad & popup blocker active...",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                    is LceState.Error -> {
                        Text(
                            text = "Extraction Error:\n${state.message}",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.navigateBack() }) {
                            Text("Go Back")
                        }
                    }
                    else -> {}
                }
            }
        } else {
            // Actual ExoPlayer Viewport
            AndroidView<PlayerView>(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showControls = !showControls }
                    )
            )

            // Buffering overlay spinner
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            // Beautiful custom material overlay panels
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    // Header Bar (Title, quality button, sub toggles, settings config)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.TopCenter),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.navigateBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = anime.title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Episode ${episode.episodeNum}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        // Playback Settings menu options (Quality, audio language, subtitles!)
                        IconButton(onClick = { showQualitySelector = true }) {
                            Icon(Icons.Default.HighQuality, contentDescription = "Quality Select", tint = Color.White)
                        }
                        IconButton(onClick = { showAudioSelector = true }) {
                            Icon(Icons.Default.Audiotrack, contentDescription = "Audio Track", tint = Color.White)
                        }
                        IconButton(onClick = { showSubtitleSelector = true }) {
                            Icon(Icons.Outlined.Subtitles, contentDescription = "Subtitle Controls", tint = Color.White)
                        }
                    }

                    // Centered scrubbing controllers (Seek rewind 10s, PlayPause, Seek Fast-forward 10s)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000L).coerceAtLeast(0)) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay10,
                                contentDescription = "Rewind 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "PlayPause",
                                tint = Color.White,
                                modifier = Modifier.size(42.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(32.dp))

                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forward10,
                                contentDescription = "Fast Forward 10s",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Bottom progress timeline slider bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(playbackPosition),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatTime(duration),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                        
                        Slider(
                            value = if (duration > 0) playbackPosition.toFloat() / duration else 0f,
                            onValueChange = { percent ->
                                if (duration > 0) {
                                    val position = (percent * duration).toLong()
                                    exoPlayer.seekTo(position)
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Modal Drawer / dialog sheets for stream configurations
    
    // Quality Selection Modal
    if (showQualitySelector) {
        AlertDialog(
            onDismissRequest = { showQualitySelector = false },
            title = { Text("Select Playback Quality") },
            text = {
                Column {
                    when (val state = videoSourcesState) {
                        is LceState.Success -> {
                            state.data.forEach { source ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            showQualitySelector = false
                                            viewModel.resolveStreamLink(source)
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = if (resolvedVideoUrl == source.url) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(text = source.quality, fontSize = 15.sp)
                                }
                            }
                        }
                        else -> {
                            Text("Loading stream sources...")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualitySelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Audio Language Selector
    if (showAudioSelector) {
        AlertDialog(
            onDismissRequest = { showAudioSelector = false },
            title = { Text("Select Audio Language") },
            text = {
                val audioTracks = remember { getExoPlayerTracks(exoPlayer, C.TRACK_TYPE_AUDIO) }
                Column {
                    if (audioTracks.isEmpty()) {
                        Text(
                            text = "Only standard stereo feed is embedded in this server.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        audioTracks.forEach { (trackGroup, trackIdx, label, isSelected) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAudioSelector = false
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setOverrideForType(TrackSelectionOverride(trackGroup, trackIdx))
                                            .build()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = label, fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAudioSelector = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Subtitle Control Sheet
    if (showSubtitleSelector) {
        val subtitleTracks = remember { getExoPlayerTracks(exoPlayer, C.TRACK_TYPE_TEXT) }
        
        AlertDialog(
            onDismissRequest = { showSubtitleSelector = false },
            title = { Text("Subtitle Controls") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Subtitle Tracks", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    
                    // Enable/Disable toggler
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val isCurrentlyDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                                exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                    .buildUpon()
                                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !isCurrentlyDisabled)
                                    .build()
                                showSubtitleSelector = false
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isDisabled = exoPlayer.trackSelectionParameters.disabledTrackTypes.contains(C.TRACK_TYPE_TEXT)
                        Icon(
                            imageVector = if (isDisabled) Icons.Default.ClosedCaptionDisabled else Icons.Default.ClosedCaption,
                            contentDescription = null,
                            tint = if (!isDisabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = if (isDisabled) "Subtitles Disabled (Turn On)" else "Subtitles Active (Turn Off)")
                    }

                    if (subtitleTracks.isNotEmpty()) {
                        Divider()
                        subtitleTracks.forEach { (trackGroup, trackIdx, label, isSelected) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showSubtitleSelector = false
                                        // Enable track type if disabled
                                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                                            .buildUpon()
                                            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                            .setOverrideForType(TrackSelectionOverride(trackGroup, trackIdx))
                                            .build()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(text = label, fontSize = 15.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSubtitleSelector = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// Model for ExoPlayer tracks
data class ExoTrackInfo(
    val mediaTrackGroup: androidx.media3.common.TrackGroup,
    val trackIndex: Int,
    val label: String,
    val isSelected: Boolean
)

@OptIn(UnstableApi::class)
private fun getExoPlayerTracks(exoPlayer: ExoPlayer, trackType: Int): List<ExoTrackInfo> {
    val options = mutableListOf<ExoTrackInfo>()
    val currentTracks = exoPlayer.currentTracks
    
    for (group in currentTracks.groups) {
        if (group.type == trackType) {
            for (i in 0 until group.length) {
                if (group.isTrackSupported(i)) {
                    val format = group.getTrackFormat(i)
                    val lang = format.language ?: "unknown"
                    val label = format.label ?: lang.uppercase()
                    options.add(
                        ExoTrackInfo(
                            mediaTrackGroup = group.mediaTrackGroup,
                            trackIndex = i,
                            label = label,
                            isSelected = group.isTrackSelected(i)
                        )
                    )
                }
            }
        }
    }
    return options
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}
