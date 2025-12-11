package com.spotifyplayer.app.playback

import android.content.Context
import android.util.Log
import com.spotifyplayer.app.data.api.SpotifyApiClient
import com.spotifyplayer.app.data.model.CurrentlyPlayingTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Monitors Spotify playback state by polling the API
 */
class PlaybackStateMonitor(private val context: Context) {
    
    private val apiClient = SpotifyApiClient.getInstance(context)
    
    // Current playback state
    private val _currentState = MutableStateFlow<PlaybackStateInfo?>(null)
    val currentState: StateFlow<PlaybackStateInfo?> = _currentState.asStateFlow()
    
    // External control detection
    private val _isExternallyControlled = MutableStateFlow(false)
    val isExternallyControlled: StateFlow<Boolean> = _isExternallyControlled.asStateFlow()
    
    private var pollingJob: Job? = null
    private var previousTrackId: String? = null
    
    /**
     * Start monitoring playback state
     */
    fun startMonitoring(pollingIntervalMs: Long = 3000) {
        stopMonitoring()
        
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    pollPlaybackState()
                    delay(pollingIntervalMs)
                } catch (e: CancellationException) {
                    break
                } catch (e: Exception) {
                    Log.e(TAG, "Error polling playback state", e)
                    delay(pollingIntervalMs)
                }
            }
        }
        
        Log.d(TAG, "Started playback monitoring")
    }
    
    /**
     * Stop monitoring playback state
     */
    fun stopMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d(TAG, "Stopped playback monitoring")
    }
    
    /**
     * Poll current playback state
     */
    private suspend fun pollPlaybackState() {
        try {
            val response = apiClient.apiService.getCurrentlyPlayingTrack()
            
            if (response.isSuccessful) {
                val currentlyPlaying = response.body()
                
                if (currentlyPlaying != null) {
                    val trackId = currentlyPlaying.item?.id
                    
                    // Update state
                    val stateInfo = PlaybackStateInfo(
                        trackId = trackId,
                        isPlaying = currentlyPlaying.isPlaying,
                        progressMs = currentlyPlaying.progressMs ?: 0,
                        timestamp = currentlyPlaying.timestamp
                    )
                    
                    _currentState.value = stateInfo
                    
                    // Detect track change
                    if (trackId != null && trackId != previousTrackId) {
                        Log.d(TAG, "Track changed: $previousTrackId -> $trackId")
                        onTrackChanged(previousTrackId, trackId)
                        previousTrackId = trackId
                    }
                } else {
                    // No active playback
                    _currentState.value = null
                }
            } else if (response.code() == 204) {
                // No content - nothing playing
                _currentState.value = null
            } else {
                Log.w(TAG, "Failed to get playback state: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception polling playback state", e)
        }
    }
    
    /**
     * Called when track changes
     */
    private fun onTrackChanged(oldTrackId: String?, newTrackId: String) {
        Log.d(TAG, "Track transition detected: $oldTrackId -> $newTrackId")
        // This will be used in Phase 5 for queue advancement
    }
    
    /**
     * Detect if playback is controlled externally (by user, not app)
     */
    fun detectExternalControl(playbackController: PlaybackController) {
        val state = _currentState.value ?: return
        val currentTime = System.currentTimeMillis()
        
        // If playing but app didn't send play command recently (within 5 seconds)
        if (state.isPlaying) {
            val timeSincePlayCommand = currentTime - playbackController.getLastPlayCommandTime()
            if (timeSincePlayCommand > 5000) {
                // User controlled playback externally
                if (!_isExternallyControlled.value) {
                    Log.d(TAG, "External control detected - playback started externally")
                    _isExternallyControlled.value = true
                }
            }
        }
    }
    
    /**
     * Clear external control flag (when user resumes from app)
     */
    fun clearExternalControl() {
        if (_isExternallyControlled.value) {
            Log.d(TAG, "Clearing external control flag")
            _isExternallyControlled.value = false
        }
    }
    
    companion object {
        private const val TAG = "PlaybackStateMonitor"
    }
}

/**
 * Playback state information
 */
data class PlaybackStateInfo(
    val trackId: String?,
    val isPlaying: Boolean,
    val progressMs: Int,
    val timestamp: Long
)

