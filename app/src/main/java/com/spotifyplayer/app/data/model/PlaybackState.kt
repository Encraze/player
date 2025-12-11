package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify Playback State model
 */
data class PlaybackState(
    @SerializedName("is_playing")
    val isPlaying: Boolean,
    
    @SerializedName("item")
    val item: Track?,
    
    @SerializedName("progress_ms")
    val progressMs: Int?,
    
    @SerializedName("device")
    val device: Device?,
    
    @SerializedName("shuffle_state")
    val shuffleState: Boolean?,
    
    @SerializedName("repeat_state")
    val repeatState: String?,
    
    @SerializedName("timestamp")
    val timestamp: Long
)

/**
 * Currently playing track response
 */
data class CurrentlyPlayingTrack(
    @SerializedName("is_playing")
    val isPlaying: Boolean,
    
    @SerializedName("item")
    val item: Track?,
    
    @SerializedName("progress_ms")
    val progressMs: Int?,
    
    @SerializedName("timestamp")
    val timestamp: Long
)


