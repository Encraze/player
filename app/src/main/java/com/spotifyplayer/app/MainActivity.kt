package com.spotifyplayer.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.spotifyplayer.app.auth.LoginActivity
import com.spotifyplayer.app.auth.SpotifyAuthManager
import com.spotifyplayer.app.data.db.AppDatabase
import com.spotifyplayer.app.data.sync.TrackFetcher
import com.spotifyplayer.app.data.repository.StatisticsRepository
import com.spotifyplayer.app.playback.PlaybackController
import com.spotifyplayer.app.playback.PlaybackStateMonitor
import com.spotifyplayer.app.playback.QueueManager
import com.spotifyplayer.app.ui.QueueAdapter
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var authManager: SpotifyAuthManager
    private lateinit var playbackController: PlaybackController
    private lateinit var stateMonitor: PlaybackStateMonitor
    private lateinit var queueManager: QueueManager
    private lateinit var statsRepo: StatisticsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        authManager = SpotifyAuthManager(this)

        if (!authManager.isTokenValid()) {
            navigateToLogin()
            return
        }

        findViewById<TextView?>(R.id.helloText)?.text = getString(R.string.app_name) + "\nAuth ready"

        val statusText = findViewById<TextView>(R.id.statusText)
        val playerStateText = findViewById<TextView>(R.id.playerStateText)
        val connectionStatus = findViewById<TextView>(R.id.connectionStatus)
        val fetchButton = findViewById<Button>(R.id.fetchButton)
        val playToggleButton = findViewById<Button>(R.id.playToggleButton)
        val nextButton = findViewById<Button>(R.id.nextButton)
        val previousButton = findViewById<Button>(R.id.previousButton)
        val queueList = findViewById<RecyclerView>(R.id.queueList)
        val queueAdapter = QueueAdapter()
        queueList.layoutManager = LinearLayoutManager(this)
        queueList.adapter = queueAdapter
        queueAdapter.setOnItemClickListener { item, _ ->
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@MainActivity)
                val tracks = db.trackDao().getAll()
                if (tracks.isEmpty()) return@launch
                queueManager.ensureInitialized(tracks)
                val targetPos = item.rawPosition
                val jumpTrack = queueManager.jumpTo(targetPos, tracks)
                if (jumpTrack == null) {
                    statusText.text = getString(R.string.playback_error, "Cannot jump")
                    return@launch
                }
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, jumpTrack.name)
                val result = playbackController.playTrack(jumpTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsRepo.recordPlay(jumpTrack.id, System.currentTimeMillis())
                }
            }
        }

        playbackController = PlaybackController(this)
        stateMonitor = PlaybackStateMonitor(playbackController.remoteManager())
        queueManager = QueueManager(this)
        statsRepo = StatisticsRepository(AppDatabase.getInstance(this).trackStatisticsDao())

        // Show cached count on start
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val count = db.trackDao().getTrackCount()
            statusText.text = if (count > 0) {
                getString(R.string.cached_tracks, count)
            } else {
                getString(R.string.status_placeholder)
            }
        }

        fetchButton.setOnClickListener {
            lifecycleScope.launch {
                val fetcher = TrackFetcher(this@MainActivity)
                statusText.text = getString(R.string.fetch_started)
                val result = fetcher.fetchAllTracks { current, total, message ->
                    runOnUiThread {
                        statusText.text = "$message"
                    }
                }
                result.onSuccess { count ->
                    lifecycleScope.launch {
                        val db = AppDatabase.getInstance(this@MainActivity)
                        val total = db.trackDao().getTrackCount()
                        val tracks = db.trackDao().getAll()
                        queueManager.ensureInitialized(tracks)
                        refreshQueue(queueAdapter)
                        statusText.text = getString(R.string.fetch_success, total)
                        Toast.makeText(this@MainActivity, "Fetched $count tracks", Toast.LENGTH_SHORT).show()
                    }
                }
                result.onFailure { e ->
                    statusText.text = getString(R.string.fetch_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, "Fetch failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

        playToggleButton.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@MainActivity)
                val tracks = db.trackDao().getAll()
                if (tracks.isEmpty()) {
                    statusText.text = getString(R.string.no_tracks)
                    Toast.makeText(this@MainActivity, R.string.no_tracks, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                queueManager.ensureInitialized(tracks)
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()

                // Determine current state from last known playerStateText / monitor
                val isPlaying = playerStateText.text.contains("playing", ignoreCase = true)

                if (isPlaying) {
                    playbackController.pause()
                    playToggleButton.text = getString(R.string.play_toggle_play)
                    return@launch
                }

                // Ensure current track
                val current = queueManager.getCurrentTrack() ?: run {
                    queueManager.initialize(tracks)
                    queueManager.getCurrentTrack()
                }
                if (current == null) {
                    statusText.text = getString(R.string.playback_error, "No current track")
                    return@launch
                }
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, current.name)
                val result = playbackController.playTrack(current.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsRepo.recordPlay(current.id, System.currentTimeMillis())
                }
                playToggleButton.text = getString(R.string.play_toggle_pause)
            }
        }

        nextButton.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@MainActivity)
                val tracks = db.trackDao().getAll()
                if (tracks.isEmpty()) {
                    statusText.text = getString(R.string.no_tracks)
                    Toast.makeText(this@MainActivity, R.string.no_tracks, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                queueManager.ensureInitialized(tracks)
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                val nextTrack = queueManager.moveToNext(tracks)
                if (nextTrack == null) {
                    statusText.text = getString(R.string.playback_error, "No next track")
                    return@launch
                }
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, nextTrack.name)
                val result = playbackController.playTrack(nextTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsRepo.recordPlay(nextTrack.id, System.currentTimeMillis())
                }
            }
        }

        previousButton.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(this@MainActivity)
                val tracks = db.trackDao().getAll()
                if (tracks.isEmpty()) {
                    statusText.text = getString(R.string.no_tracks)
                    Toast.makeText(this@MainActivity, R.string.no_tracks, Toast.LENGTH_SHORT).show()
                    return@launch
                }
                queueManager.ensureInitialized(tracks)
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                val prevTrack = queueManager.moveToPrevious(tracks)
                if (prevTrack == null) {
                    statusText.text = getString(R.string.playback_error, "No previous track")
                    return@launch
                }
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, prevTrack.name)
                val result = playbackController.playTrack(prevTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsRepo.recordPlay(prevTrack.id, System.currentTimeMillis())
                }
            }
        }

        // Observe player state
        lifecycleScope.launch {
            // initial connect state
            connectionStatus.text = if (playbackController.isConnected()) {
                getString(R.string.conn_status_connected)
            } else {
                getString(R.string.conn_status_disconnected)
            }

            // try to ensure connection with retry once at start
            val conn = playbackController.ensureConnectedWithRetry()
            connectionStatus.text = if (conn.isSuccess) {
                getString(R.string.conn_status_connected)
            } else {
                getString(R.string.conn_failed)
            }

            stateMonitor.playerStateFlow().collectLatest { result ->
                result.onSuccess { state ->
                    val playingLabel = if (state.isPlaying) "playing" else "paused"
                    playerStateText.text = getString(
                        R.string.player_state_format,
                        playingLabel,
                        state.trackName ?: "-",
                        state.artistName ?: "-",
                        state.albumName ?: "-",
                        state.positionMs,
                        state.durationMs
                    )
                    connectionStatus.text = getString(R.string.conn_status_connected)
                    playToggleButton.text = if (state.isPlaying) {
                        getString(R.string.play_toggle_pause)
                    } else {
                        getString(R.string.play_toggle_play)
                    }
                }.onFailure {
                    playerStateText.text = getString(R.string.player_state_placeholder)
                    connectionStatus.text = getString(R.string.conn_status_disconnected)
                    playToggleButton.text = getString(R.string.play_toggle_play)
                }
            }
        }
    }

    private suspend fun refreshQueue(adapter: QueueAdapter) {
        val snapshot = queueManager.getQueueSnapshot()
        val items = snapshot.map { (pos, track, playCount) ->
            val label = when {
                pos == 0 -> "0"
                pos < 0 -> pos.toString()
                else -> "+$pos"
            }
            com.spotifyplayer.app.ui.QueueDisplayItem(
                positionLabel = label,
                title = track.name,
                subtitle = track.artists ?: "",
                playCount = playCount,
                isCurrent = pos == 0,
                rawPosition = pos
            )
        }
        runOnUiThread {
            adapter.submit(items)
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}