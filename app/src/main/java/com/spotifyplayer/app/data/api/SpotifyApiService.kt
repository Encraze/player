package com.spotifyplayer.app.data.api

import com.spotifyplayer.app.data.model.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Spotify Web API service interface
 */
interface SpotifyApiService {
    
    // ========== Library / Tracks ==========
    
    /**
     * Get user's saved tracks (liked songs)
     * Requires: user-library-read scope
     */
    @GET("me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<SavedTracksResponse>
    
    // ========== Player / Playback Control ==========
    
    /**
     * Get current playback state
     * Requires: user-read-playback-state scope
     */
    @GET("me/player")
    suspend fun getPlaybackState(): Response<PlaybackState>
    
    /**
     * Get currently playing track
     * Requires: user-read-currently-playing scope
     */
    @GET("me/player/currently-playing")
    suspend fun getCurrentlyPlayingTrack(): Response<CurrentlyPlayingTrack>
    
    /**
     * Start/Resume playback
     * Requires: user-modify-playback-state scope
     * 
     * @param deviceId Optional device ID to target
     * @param body Optional body with context_uri, uris, offset, position_ms
     */
    @PUT("me/player/play")
    suspend fun startPlayback(
        @Body body: PlaybackRequest? = null,
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>
    
    /**
     * Pause playback
     * Requires: user-modify-playback-state scope
     * 
     * @param deviceId Optional device ID to target
     */
    @PUT("me/player/pause")
    suspend fun pausePlayback(
        @Query("device_id") deviceId: String? = null
    ): Response<Unit>
    
    /**
     * Skip to next track
     * Requires: user-modify-playback-state scope
     */
    @POST("me/player/next")
    suspend fun skipToNext(): Response<Unit>
    
    /**
     * Skip to previous track
     * Requires: user-modify-playback-state scope
     */
    @POST("me/player/previous")
    suspend fun skipToPrevious(): Response<Unit>
    
    /**
     * Seek to position in currently playing track
     * Requires: user-modify-playback-state scope
     */
    @PUT("me/player/seek")
    suspend fun seekToPosition(
        @Query("position_ms") positionMs: Int
    ): Response<Unit>
    
    /**
     * Set repeat mode
     * Requires: user-modify-playback-state scope
     * 
     * @param state track, context, or off
     */
    @PUT("me/player/repeat")
    suspend fun setRepeatMode(
        @Query("state") state: String
    ): Response<Unit>
    
    /**
     * Set playback volume
     * Requires: user-modify-playback-state scope
     */
    @PUT("me/player/volume")
    suspend fun setVolume(
        @Query("volume_percent") volumePercent: Int
    ): Response<Unit>
    
    /**
     * Toggle playback shuffle
     * Requires: user-modify-playback-state scope
     */
    @PUT("me/player/shuffle")
    suspend fun toggleShuffle(
        @Query("state") state: Boolean
    ): Response<Unit>
    
    /**
     * Add item to playback queue
     * Requires: user-modify-playback-state scope
     */
    @POST("me/player/queue")
    suspend fun addToQueue(
        @Query("uri") uri: String
    ): Response<Unit>
    
    // ========== Devices ==========
    
    /**
     * Get available devices
     * Requires: user-read-playback-state scope
     */
    @GET("me/player/devices")
    suspend fun getAvailableDevices(): Response<DevicesResponse>
    
    /**
     * Transfer playback to a new device
     * Requires: user-modify-playback-state scope
     */
    @PUT("me/player")
    suspend fun transferPlayback(
        @Body body: TransferPlaybackRequest
    ): Response<Unit>
}

/**
 * Request body for starting playback
 */
data class PlaybackRequest(
    val context_uri: String? = null,
    val uris: List<String>? = null,
    val offset: PlaybackOffset? = null,
    val position_ms: Int? = null
)

data class PlaybackOffset(
    val position: Int? = null,
    val uri: String? = null
)

/**
 * Request body for transferring playback
 */
data class TransferPlaybackRequest(
    val device_ids: List<String>,
    val play: Boolean = false
)


