package com.spotifyplayer.app.playback

import android.content.Context
import com.spotifyplayer.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlaybackController(context: Context) {

    private val remote = SpotifyRemoteManager(context.applicationContext)
    private val db = AppDatabase.getInstance(context.applicationContext)

    suspend fun playTrack(trackId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val track = db.trackDao().getById(trackId)
            ?: return@withContext Result.failure(IllegalArgumentException("Track not found: $trackId"))
        if (track.uri.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Track missing URI"))
        }
        remote.play(track.uri)
    }

    suspend fun pause(): Result<Unit> = remote.pause()

    suspend fun resume(): Result<Unit> = remote.resume()

    fun isConnected(): Boolean = remote.isConnected

    fun disconnect() = remote.disconnect()

    fun remoteManager(): SpotifyRemoteManager = remote

    suspend fun ensureConnectedWithRetry(): Result<Unit> = remote.connectWithRetry()
}

