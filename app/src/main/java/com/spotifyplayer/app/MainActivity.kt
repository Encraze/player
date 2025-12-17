package com.spotifyplayer.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
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
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import coil.load
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {
    private lateinit var authManager: SpotifyAuthManager
    private lateinit var playbackController: PlaybackController
    private lateinit var stateMonitor: PlaybackStateMonitor
    private lateinit var queueManager: QueueManager
    private lateinit var statsUpdater: StatisticsUpdater
    private lateinit var connectionStatusDot: View
    private lateinit var coverArtView: ImageView
    private lateinit var playToggleButton: MaterialButton
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

        val statusText = findViewById<TextView>(R.id.statusText)
        val playerStateText = findViewById<TextView>(R.id.playerStateText)
        connectionStatusDot = findViewById(R.id.connectionStatusDot)
        coverArtView = findViewById(R.id.coverArt)
        val fetchButton = findViewById<Button>(R.id.fetchButton)
        playToggleButton = findViewById(R.id.playToggleButton)
        val nextButton = findViewById<MaterialButton>(R.id.nextButton)
        val previousButton = findViewById<MaterialButton>(R.id.previousButton)
        val queueList = findViewById<RecyclerView>(R.id.queueList)
        val queueAdapter = QueueAdapter()
        queueList.layoutManager = LinearLayoutManager(this)
        queueList.adapter = queueAdapter
        setPlayButtonState(false)
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
                updateConnectionIndicator(ConnectionIndicator.RETRYING)
                playbackController.ensureConnectedWithRetry()
                if (targetPos < 0) {
                    currentPlayingTrackId = selectedTrack.id
                    currentPlayingPosition = targetPos
                    refreshQueue(queueAdapter)
                    statusText.text = getString(R.string.playing_track, selectedTrack.artists)
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
                updateConnectionIndicator(ConnectionIndicator.RETRYING)
                playbackController.ensureConnectedWithRetry()

                val isPlaying = lastPlayerState?.isPlaying == true
                if (isPlaying) {
                    playbackController.pause()
                    setPlayButtonState(false)
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
                setPlayButtonState(true)
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
                updateConnectionIndicator(ConnectionIndicator.RETRYING)
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
                updateConnectionIndicator(ConnectionIndicator.RETRYING)
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
            updateConnectionIndicator(
                if (playbackController.isConnected()) {
                    ConnectionIndicator.CONNECTED
                } else {
                    ConnectionIndicator.DISCONNECTED
                }
            )

            // try to ensure connection with retry once at start
            val conn = playbackController.ensureConnectedWithRetry()
            updateConnectionIndicator(
                if (conn.isSuccess) {
                    ConnectionIndicator.CONNECTED
                } else {
                    ConnectionIndicator.FAILED
                }
            )

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
                    playerStateText.text = getString(
                        R.string.player_state_format,
                        state.artistName ?: "-",
                        state.albumName ?: "-",
                        state.positionMs,
                        state.durationMs
                    )
                    updateConnectionIndicator(ConnectionIndicator.CONNECTED)
                    setPlayButtonState(state.isPlaying)
                }.onFailure {
                    playerStateText.text = getString(R.string.player_state_placeholder)
                    updateConnectionIndicator(ConnectionIndicator.DISCONNECTED)
                    setPlayButtonState(false)
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
                rawPosition = pos,
                albumImageUrl = track.albumImageUrl
            )
        }
        val currentAlbumUrl = snapshot.firstOrNull { it.second.id == playingTrackId }?.second?.albumImageUrl
        runOnUiThread {
            adapter.submit(items)
            coverArtView.load(currentAlbumUrl) {
                placeholder(R.drawable.album_placeholder)
                error(R.drawable.album_placeholder)
                crossfade(true)
            }
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

    private fun updateConnectionIndicator(status: ConnectionIndicator) {
        val color = ContextCompat.getColor(this, status.colorRes)
        ViewCompat.setBackgroundTintList(connectionStatusDot, ColorStateList.valueOf(color))
        connectionStatusDot.contentDescription = getString(status.contentDescRes)
    }

    private fun setPlayButtonState(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        playToggleButton.setIconResource(icon)
        playToggleButton.contentDescription = getString(
            if (isPlaying) R.string.play_toggle_pause else R.string.play_toggle_play
        )
    }

    private enum class ConnectionIndicator(@ColorRes val colorRes: Int, val contentDescRes: Int) {
        CONNECTED(R.color.status_connected, R.string.conn_status_connected),
        DISCONNECTED(R.color.status_disconnected, R.string.conn_status_disconnected),
        RETRYING(R.color.status_retrying, R.string.conn_retrying),
        FAILED(R.color.status_disconnected, R.string.conn_failed)
    }
}