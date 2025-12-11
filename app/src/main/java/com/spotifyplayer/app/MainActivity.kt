package com.spotifyplayer.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.spotifyplayer.app.auth.LoginActivity
import com.spotifyplayer.app.auth.SpotifyAuthManager
import com.spotifyplayer.app.data.api.ApiErrorHandler
import com.spotifyplayer.app.data.api.SpotifyApiClient
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    
    private lateinit var authManager: SpotifyAuthManager
    private lateinit var apiClient: SpotifyApiClient
    private lateinit var playbackController: com.spotifyplayer.app.playback.PlaybackController
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            authManager = SpotifyAuthManager(this)
            apiClient = SpotifyApiClient.getInstance(this)
            playbackController = com.spotifyplayer.app.playback.PlaybackController(this)
            
            // Check if logged in
            if (!authManager.isLoggedIn()) {
                navigateToLogin()
                return
            }
            
            setupUI()
            testApiCalls()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            navigateToLogin()
        }
    }
    
    private fun setupUI() {
        val statusText = findViewById<TextView>(R.id.statusText)
        val logoutButton = findViewById<Button>(R.id.logoutButton)
        val refreshButton = findViewById<Button>(R.id.refreshButton)
        val playButton = findViewById<Button>(R.id.playButton)
        val pauseButton = findViewById<Button>(R.id.pauseButton)
        val resumeButton = findViewById<Button>(R.id.resumeButton)
        
        statusText.text = "Logged in successfully!\nLoading..."
        
        logoutButton.setOnClickListener {
            authManager.logout()
            navigateToLogin()
        }
        
        refreshButton.setOnClickListener {
            fetchTracksFromSpotify()
        }
        
        // Playback control buttons
        playButton.setOnClickListener {
            playFirstTrack()
        }
        
        pauseButton.setOnClickListener {
            testPausePlayback()
        }
        
        resumeButton.setOnClickListener {
            testResumePlayback()
        }
        
        // Long press Play button to check connection status
        playButton.setOnLongClickListener {
            val isConnected = playbackController.isConnected()
            Toast.makeText(
                this,
                if (isConnected) "✅ Connected to Spotify" else "❌ Not connected",
                Toast.LENGTH_SHORT
            ).show()
            true
        }
    }
    
    /**
     * Check if tracks are cached, otherwise fetch from Spotify
     */
    private fun testApiCalls() {
        val statusText = findViewById<TextView>(R.id.statusText)
        
        lifecycleScope.launch {
            try {
                // Check if we already have tracks in the database
                val database = com.spotifyplayer.app.database.AppDatabase.getInstance(this@MainActivity)
                val trackCount = database.trackDao().getTrackCount()
                
                if (trackCount > 0) {
                    // Tracks already cached
                    val message = """
                        ✅ Ready!
                        
                        $trackCount tracks loaded from cache
                        
                        Tap "Refresh" to sync with Spotify
                    """.trimIndent()
                    
                    statusText.text = message
                    Log.d(TAG, "Loaded $trackCount tracks from local database")
                } else {
                    // First time - fetch from Spotify
                    Log.d(TAG, "No tracks in database, fetching from Spotify...")
                    fetchTracksFromSpotify()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking database", e)
                // If database check fails, try fetching
                fetchTracksFromSpotify()
            }
        }
    }
    
    /**
     * Fetch tracks from Spotify API
     */
    private fun fetchTracksFromSpotify() {
        val statusText = findViewById<TextView>(R.id.statusText)
        
        lifecycleScope.launch {
            try {
                statusText.text = "Fetching your tracks from Spotify..."
                
                // Fetch all tracks
                val trackFetcher = com.spotifyplayer.app.data.sync.TrackFetcher(this@MainActivity)
                
                val result = trackFetcher.fetchAllTracks { current, total, message ->
                    // Update UI with progress
                    lifecycleScope.launch {
                        if (total > 0) {
                            val percentage = (current * 100) / total
                            statusText.text = "$message\n\nProgress: $percentage%"
                        } else {
                            statusText.text = message
                        }
                    }
                }
                
                result.onSuccess { count ->
                    val finalMessage = """
                        ✅ Success!
                        
                        Fetched and stored $count tracks
                        
                        All your liked tracks are now
                        available locally!
                    """.trimIndent()
                    
                    statusText.text = finalMessage
                    Log.d(TAG, "Successfully fetched and stored $count tracks")
                }
                
                result.onFailure { exception ->
                    val errorMsg = exception.message ?: "Unknown error"
                    statusText.text = "❌ Error:\n$errorMsg"
                    Log.e(TAG, "Failed to fetch tracks", exception)
                }
            } catch (e: Exception) {
                val errorMsg = ApiErrorHandler.handleException(e)
                statusText.text = "❌ Error:\n$errorMsg"
                Log.e(TAG, "Exception during track fetch", e)
            }
        }
    }
    
    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    /**
     * Play first track - automatically connects to Spotify if needed
     */
    private fun playFirstTrack() {
        lifecycleScope.launch {
            try {
                // Check if we have tracks
                val database = com.spotifyplayer.app.database.AppDatabase.getInstance(this@MainActivity)
                val tracks = database.trackDao().getAllTracks()
                
                if (tracks.isEmpty()) {
                    Toast.makeText(this@MainActivity, "No tracks available. Fetch tracks first.", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                val firstTrack = tracks.first()
                Log.d(TAG, "Selected track: ${firstTrack.name} by ${firstTrack.artists}")
                
                // Connect to Spotify if not connected
                if (!playbackController.isConnected()) {
                    Log.d(TAG, "Not connected to Spotify. Connecting...")
                    Toast.makeText(this@MainActivity, "Launching Spotify...", Toast.LENGTH_SHORT).show()
                    
                    val connectResult = playbackController.connect()
                    
                    connectResult.onFailure { exception ->
                        val message = """
                            ❌ Failed to connect to Spotify
                            
                            ${exception.message}
                            
                            Make sure Spotify app is installed and you're logged in.
                        """.trimIndent()
                        
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                        Log.e(TAG, "Connection failed: ${exception.message}")
                        return@launch
                    }
                    
                    Log.d(TAG, "✅ Connected to Spotify!")
                }
                
                // Play the track
                Log.d(TAG, "Playing: ${firstTrack.name} (ID: ${firstTrack.id})")
                Toast.makeText(this@MainActivity, "▶ Playing: ${firstTrack.name}", Toast.LENGTH_SHORT).show()
                
                val playResult = playbackController.playTrack(firstTrack.id)
                
                playResult.onSuccess {
                    Toast.makeText(this@MainActivity, "✅ Now playing!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "✅ Playback started successfully")
                }
                
                playResult.onFailure { exception ->
                    Toast.makeText(this@MainActivity, "❌ Playback error: ${exception.message}", Toast.LENGTH_LONG).show()
                    Log.e(TAG, "❌ Playback failed: ${exception.message}")
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Exception in playFirstTrack", e)
            }
        }
    }
    
    /**
     * Test: Pause playback
     */
    private fun testPausePlayback() {
        lifecycleScope.launch {
            try {
                val result = playbackController.pausePlayback()
                
                result.onSuccess {
                    Toast.makeText(this@MainActivity, "✅ Playback paused", Toast.LENGTH_SHORT).show()
                }
                
                result.onFailure { exception ->
                    Toast.makeText(this@MainActivity, "❌ Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    /**
     * Test: Resume playback
     */
    private fun testResumePlayback() {
        lifecycleScope.launch {
            try {
                val result = playbackController.resumePlayback()
                
                result.onSuccess {
                    Toast.makeText(this@MainActivity, "✅ Playback resumed", Toast.LENGTH_SHORT).show()
                }
                
                result.onFailure { exception ->
                    Toast.makeText(this@MainActivity, "❌ Error: ${exception.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    companion object {
        private const val TAG = "MainActivity"
    }
}



