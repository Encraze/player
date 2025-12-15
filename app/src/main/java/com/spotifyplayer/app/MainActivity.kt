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
import com.spotifyplayer.app.data.db.entity.TrackEntity
import com.spotifyplayer.app.playback.PlaybackController
import com.spotifyplayer.app.playback.PlaybackStateMonitor
import com.spotifyplayer.app.playback.QueueManager
import com.spotifyplayer.app.playback.SimplePlayerState
import com.spotifyplayer.app.playback.StatisticsUpdater
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
    private lateinit var statsUpdater: StatisticsUpdater
    private var lastPlayerState: SimplePlayerState? = null
    private var currentPlayingTrackId: String? = null
    private var currentPlayingPosition: Int = 0

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
                val snapshot = queueManager.getQueueSnapshot()
                val selectedTrack = snapshot.firstOrNull { it.first == targetPos }?.second
                if (selectedTrack == null) {
                    statusText.text = getString(R.string.playback_error, "Cannot jump")
                    return@launch
                }
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                if (targetPos < 0) {
                    currentPlayingTrackId = selectedTrack.id
                    currentPlayingPosition = targetPos
                    refreshQueue(queueAdapter)
                    statusText.text = getString(R.string.playing_track, selectedTrack.name)
                    val result = playbackController.playTrack(selectedTrack.id)
                    result.onFailure { e ->
                        statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                        Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                    }.onSuccess {
                        statsUpdater.recordPlay(selectedTrack.id, System.currentTimeMillis())
                    }
                    return@launch
                }
                applySkipAccounting(targetPos = targetPos, snapshot = snapshot)
                val jumpTrack = queueManager.jumpTo(targetPos, tracks)
                if (jumpTrack == null) {
                    statusText.text = getString(R.string.playback_error, "Cannot jump")
                    return@launch
                }
                currentPlayingTrackId = jumpTrack.id
                currentPlayingPosition = 0
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, jumpTrack.name)
                val result = playbackController.playTrack(jumpTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsUpdater.recordPlay(jumpTrack.id, System.currentTimeMillis())
                }
            }
        }

        playbackController = PlaybackController(this)
        stateMonitor = PlaybackStateMonitor(playbackController.remoteManager())
        queueManager = QueueManager(this)
        statsUpdater = StatisticsUpdater(this)

        // Show cached count on start
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@MainActivity)
            val count = db.trackDao().getTrackCount()
            statusText.text = if (count > 0) {
                getString(R.string.cached_tracks, count)
            } else {
                getString(R.string.status_placeholder)
            }
            val tracks = db.trackDao().getAll()
            if (tracks.isNotEmpty()) {
                queueManager.ensureInitialized(tracks)
                currentPlayingTrackId = queueManager.getCurrentTrack()?.id
                currentPlayingPosition = 0
                refreshQueue(queueAdapter)
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
                        currentPlayingTrackId = queueManager.getCurrentTrack()?.id
                        currentPlayingPosition = 0
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

                val isPlaying = lastPlayerState?.isPlaying == true
                if (isPlaying) {
                    playbackController.pause()
                    playToggleButton.text = getString(R.string.play_toggle_play)
                    return@launch
                }

                val current = currentPlayingTrackId?.let { db.trackDao().getById(it) }
                    ?: queueManager.getCurrentTrack()
                    ?: run {
                        queueManager.initialize(tracks)
                        queueManager.getCurrentTrack()
                    }
                if (current == null) {
                    statusText.text = getString(R.string.playback_error, "No current track")
                    return@launch
                }

                currentPlayingTrackId = current.id
                currentPlayingPosition = 0
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, current.name)

                val shouldResume = lastPlayerState?.trackId == current.id && lastPlayerState?.isPlaying == false
                val result = if (shouldResume) {
                    playbackController.resume()
                } else {
                    playbackController.playTrack(current.id)
                }
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    if (!shouldResume) {
                        statsUpdater.recordPlay(current.id, System.currentTimeMillis())
                    }
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
                val snapshot = queueManager.getQueueSnapshot()
                val historyTargetPos = currentPlayingPosition + 1
                val historyEntry = if (currentPlayingPosition < 0) {
                    snapshot.firstOrNull { it.first == historyTargetPos }
                } else null
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                if (historyEntry != null && historyTargetPos <= 0) {
                    currentPlayingPosition = historyTargetPos
                    currentPlayingTrackId = historyEntry.second.id
                    refreshQueue(queueAdapter)
                    statusText.text = getString(R.string.playing_track, historyEntry.second.name)
                    val result = playbackController.playTrack(historyEntry.second.id)
                    result.onFailure { e ->
                        statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                        Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                    }.onSuccess {
                        statsUpdater.recordPlay(historyEntry.second.id, System.currentTimeMillis())
                    }
                    return@launch
                }
                applySkipAccounting(targetPos = 1, snapshot = snapshot)
                val nextTrack = queueManager.moveToNext(tracks)
                if (nextTrack == null) {
                    statusText.text = getString(R.string.playback_error, "No next track")
                    return@launch
                }
                currentPlayingTrackId = nextTrack.id
                currentPlayingPosition = 0
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, nextTrack.name)
                val result = playbackController.playTrack(nextTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsUpdater.recordPlay(nextTrack.id, System.currentTimeMillis())
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
                val snapshot = queueManager.getQueueSnapshot()
                val historyPrevPos = currentPlayingPosition - 1
                val historyEntry = snapshot.firstOrNull { it.first == historyPrevPos && it.first < 0 }
                connectionStatus.text = getString(R.string.conn_retrying)
                playbackController.ensureConnectedWithRetry()
                if (historyEntry != null) {
                    currentPlayingPosition = historyPrevPos
                    currentPlayingTrackId = historyEntry.second.id
                    refreshQueue(queueAdapter)
                    statusText.text = getString(R.string.playing_track, historyEntry.second.name)
                    val result = playbackController.playTrack(historyEntry.second.id)
                    result.onFailure { e ->
                        statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                        Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                    }.onSuccess {
                        statsUpdater.recordPlay(historyEntry.second.id, System.currentTimeMillis())
                    }
                    return@launch
                }
                val prevTrack = queueManager.moveToPrevious(tracks)
                if (prevTrack == null) {
                    statusText.text = getString(R.string.playback_error, "No previous track")
                    return@launch
                }
                currentPlayingTrackId = prevTrack.id
                currentPlayingPosition = 0
                refreshQueue(queueAdapter)
                statusText.text = getString(R.string.playing_track, prevTrack.name)
                val result = playbackController.playTrack(prevTrack.id)
                result.onFailure { e ->
                    statusText.text = getString(R.string.playback_error, e.message ?: "Unknown")
                    Toast.makeText(this@MainActivity, getString(R.string.playback_error, e.message ?: "Unknown"), Toast.LENGTH_LONG).show()
                }.onSuccess {
                    statsUpdater.recordPlay(prevTrack.id, System.currentTimeMillis())
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
                    lastPlayerState = state
                    val stateTrackId = state.trackId
                    if (!stateTrackId.isNullOrEmpty() && stateTrackId != currentPlayingTrackId) {
                        currentPlayingTrackId = stateTrackId
                        val snapshot = queueManager.getQueueSnapshot()
                        updateCurrentPosition(snapshot, stateTrackId)
                        refreshQueue(queueAdapter)
                    }
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

    private suspend fun refreshQueue(adapter: QueueAdapter, playingTrackId: String? = currentPlayingTrackId) {
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
                isCurrent = track.id == playingTrackId,
                rawPosition = pos
            )
        }
        runOnUiThread {
            adapter.submit(items)
        }
    }

    private suspend fun applySkipAccounting(targetPos: Int, snapshot: List<Triple<Int, TrackEntity, Long>>) {
        if (targetPos <= 0) return
        val now = System.currentTimeMillis()
        val current = snapshot.firstOrNull { it.first == 0 }?.second
        current?.let { track ->
            val duration = track.durationMs ?: lastPlayerState?.durationMs ?: 0L
            val position = lastPlayerState?.takeIf { it.trackId == track.id }?.positionMs
            val shouldAddExtra = duration > 0 && position != null && position < duration / 2
            if (shouldAddExtra) {
                statsUpdater.incrementPlayCount(track.id, 1, now)
            }
        }

        val skippedIds = snapshot.filter { it.first in 1 until targetPos }.map { it.second.id }
        if (skippedIds.isNotEmpty()) {
            statsUpdater.incrementPlayCount(skippedIds, 2, now)
        }
    }

    private fun updateCurrentPosition(snapshot: List<Triple<Int, TrackEntity, Long>>, trackId: String) {
        snapshot.firstOrNull { it.second.id == trackId }?.first?.let {
            currentPlayingPosition = it
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}