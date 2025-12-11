package com.spotifyplayer.app.playback

import android.content.Context
import android.util.Log
import com.spotifyplayer.app.database.AppDatabase
import com.spotifyplayer.app.spotify.SpotifyRemoteManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Controls Spotify playback via Spotify Android SDK
 */
class PlaybackController(private val context: Context) {
    
    private val spotifyRemote = SpotifyRemoteManager.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    private val trackDao = database.trackDao()
    
    // Track app-initiated actions for external control detection
    private var lastPlayCommandTime: Long = 0
    private var lastPauseCommandTime: Long = 0
    
    /**
     * Connect to Spotify App Remote
     */
    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.connect(
                    onSuccess = {
                        Log.d(TAG, "✅ Connected to Spotify")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to connect: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception connecting", e)
            Result.failure(e)
        }
    }
    
    /**
     * Play a specific track by ID using Spotify SDK
     */
    suspend fun playTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Check connection
            if (!spotifyRemote.isConnected()) {
                Log.e(TAG, "Not connected to Spotify")
                return@withContext Result.failure(Exception("Not connected. Please connect to Spotify first."))
            }
            
            // Get track URI from database
            val track = trackDao.getTrackById(trackId)
            if (track == null) {
                Log.e(TAG, "Track not found in database: $trackId")
                return@withContext Result.failure(Exception("Track not found"))
            }
            
            val uri = track.uri
            Log.d(TAG, "Playing track: ${track.name} by ${track.artists}")
            Log.d(TAG, "Track URI: $uri")
            
            // Play track using SDK
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.playTrack(
                    uri = uri,
                    onSuccess = {
                        lastPlayCommandTime = System.currentTimeMillis()
                        Log.d(TAG, "✅ Track playback started")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to play track: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception playing track", e)
            Result.failure(e)
        }
    }
    
    /**
     * Pause playback
     */
    suspend fun pausePlayback(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!spotifyRemote.isConnected()) {
                return@withContext Result.failure(Exception("Not connected to Spotify"))
            }
            
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.pause(
                    onSuccess = {
                        lastPauseCommandTime = System.currentTimeMillis()
                        Log.d(TAG, "✅ Playback paused")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to pause: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception pausing playback", e)
            Result.failure(e)
        }
    }
    
    /**
     * Resume playback (continues current track)
     */
    suspend fun resumePlayback(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!spotifyRemote.isConnected()) {
                return@withContext Result.failure(Exception("Not connected to Spotify"))
            }
            
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.resume(
                    onSuccess = {
                        lastPlayCommandTime = System.currentTimeMillis()
                        Log.d(TAG, "✅ Playback resumed")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to resume: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception resuming playback", e)
            Result.failure(e)
        }
    }
    
    /**
     * Skip to next track
     */
    suspend fun skipNext(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!spotifyRemote.isConnected()) {
                return@withContext Result.failure(Exception("Not connected to Spotify"))
            }
            
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.skipNext(
                    onSuccess = {
                        Log.d(TAG, "✅ Skipped to next")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to skip: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception skipping", e)
            Result.failure(e)
        }
    }
    
    /**
     * Skip to previous track
     */
    suspend fun skipPrevious(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!spotifyRemote.isConnected()) {
                return@withContext Result.failure(Exception("Not connected to Spotify"))
            }
            
            suspendCancellableCoroutine<Unit> { continuation ->
                spotifyRemote.skipPrevious(
                    onSuccess = {
                        Log.d(TAG, "✅ Skipped to previous")
                        continuation.resume(Unit)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Failed to skip previous: ${error.message}", error)
                        continuation.resumeWithException(error)
                    }
                )
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Exception skipping previous", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = spotifyRemote.isConnected()
    
    /**
     * Get last play command time (for external control detection)
     */
    fun getLastPlayCommandTime(): Long = lastPlayCommandTime
    
    /**
     * Get last pause command time (for external control detection)
     */
    fun getLastPauseCommandTime(): Long = lastPauseCommandTime
    
    /**
     * Disconnect from Spotify
     */
    fun disconnect() {
        spotifyRemote.disconnect()
    }
    
    companion object {
        private const val TAG = "PlaybackController"
    }
}
