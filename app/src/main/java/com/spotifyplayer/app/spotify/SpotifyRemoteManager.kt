package com.spotifyplayer.app.spotify

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.Track
import com.spotifyplayer.app.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Manages Spotify App Remote connection and playback control
 * Uses Spotify Android SDK for reliable mobile playback
 */
class SpotifyRemoteManager(private val context: Context) {
    
    private val clientId: String by lazy {
        context.getString(R.string.spotify_client_id)
    }
    
    private val redirectUri: String by lazy {
        context.getString(R.string.spotify_redirect_uri)
    }
    
    private var spotifyAppRemote: SpotifyAppRemote? = null
    
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState
    
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack
    
    /**
     * Connect to Spotify App Remote
     */
    fun connect(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        if (spotifyAppRemote?.isConnected == true) {
            Log.d(TAG, "Already connected to Spotify")
            _connectionState.value = ConnectionState.Connected
            onSuccess()
            return
        }
        
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()
        
        Log.d(TAG, "Connecting to Spotify App Remote...")
        _connectionState.value = ConnectionState.Connecting
        
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                spotifyAppRemote = appRemote
                _connectionState.value = ConnectionState.Connected
                Log.d(TAG, "✅ Connected to Spotify App Remote")
                
                // Subscribe to player state
                subscribeToPlayerState()
                
                onSuccess()
            }
            
            override fun onFailure(error: Throwable) {
                _connectionState.value = ConnectionState.Disconnected
                Log.e(TAG, "Failed to connect to Spotify: ${error.message}", error)
                onFailure(error)
            }
        })
    }
    
    /**
     * Disconnect from Spotify App Remote
     */
    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            _connectionState.value = ConnectionState.Disconnected
            _currentTrack.value = null
            Log.d(TAG, "Disconnected from Spotify")
        }
    }
    
    /**
     * Play a track by URI
     */
    fun playTrack(uri: String, onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Playing track: $uri")
        remote.playerApi.play(uri)
            .setResultCallback {
                Log.d(TAG, "✅ Track playback started")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to play track: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Pause playback
     */
    fun pause(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Pausing playback")
        remote.playerApi.pause()
            .setResultCallback {
                Log.d(TAG, "✅ Playback paused")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to pause: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Resume playback
     */
    fun resume(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Resuming playback")
        remote.playerApi.resume()
            .setResultCallback {
                Log.d(TAG, "✅ Playback resumed")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to resume: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Skip to next track
     */
    fun skipNext(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Skipping to next track")
        remote.playerApi.skipNext()
            .setResultCallback {
                Log.d(TAG, "✅ Skipped to next")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to skip: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Skip to previous track
     */
    fun skipPrevious(onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Skipping to previous track")
        remote.playerApi.skipPrevious()
            .setResultCallback {
                Log.d(TAG, "✅ Skipped to previous")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to skip previous: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Queue a track
     */
    fun queueTrack(uri: String, onSuccess: () -> Unit = {}, onFailure: (Throwable) -> Unit = {}) {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            val error = IllegalStateException("Not connected to Spotify")
            Log.e(TAG, error.message ?: "Connection error")
            onFailure(error)
            return
        }
        
        Log.d(TAG, "Adding track to queue: $uri")
        remote.playerApi.queue(uri)
            .setResultCallback {
                Log.d(TAG, "✅ Track added to queue")
                onSuccess()
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Failed to queue track: ${error.message}", error)
                onFailure(error)
            }
    }
    
    /**
     * Subscribe to player state changes
     */
    private fun subscribeToPlayerState() {
        val remote = spotifyAppRemote ?: return
        
        remote.playerApi.subscribeToPlayerState()
            .setEventCallback { playerState ->
                Log.d(TAG, "Player state: ${playerState.track.name} - Playing: ${!playerState.isPaused}")
                _currentTrack.value = playerState.track
            }
            .setErrorCallback { error ->
                Log.e(TAG, "Error subscribing to player state: ${error.message}", error)
            }
    }
    
    /**
     * Get current player state as Flow
     */
    fun observePlayerState(): Flow<com.spotify.protocol.types.PlayerState> = callbackFlow {
        val remote = spotifyAppRemote
        if (remote == null || !remote.isConnected) {
            close(IllegalStateException("Not connected to Spotify"))
            return@callbackFlow
        }
        
        val subscription = remote.playerApi.subscribeToPlayerState()
            .setEventCallback { playerState ->
                trySend(playerState)
            }
            .setErrorCallback { error ->
                close(error)
            }
        
        awaitClose {
            // Cleanup when flow is cancelled
        }
    }
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean = spotifyAppRemote?.isConnected == true
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
    }
    
    companion object {
        private const val TAG = "SpotifyRemoteManager"
        
        @Volatile
        private var instance: SpotifyRemoteManager? = null
        
        fun getInstance(context: Context): SpotifyRemoteManager {
            return instance ?: synchronized(this) {
                instance ?: SpotifyRemoteManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

