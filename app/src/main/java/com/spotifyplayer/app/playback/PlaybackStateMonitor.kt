package com.spotifyplayer.app.playback

import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.protocol.types.PlayerState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class SimplePlayerState(
    val trackId: String?,
    val trackName: String?,
    val artistName: String?,
    val albumName: String?,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long
)

class PlaybackStateMonitor(
    private val remoteManager: SpotifyRemoteManager
)

{
    fun playerStateFlow(): Flow<Result<SimplePlayerState>> = callbackFlow {
        val connectResult = remoteManager.connectWithRetry()
        if (connectResult.isFailure) {
            val err = connectResult.exceptionOrNull()
            if (err != null) {
                trySend(Result.failure(err))
                close(err)
            } else {
                close()
            }
            return@callbackFlow
        }

        val remote: SpotifyAppRemote = remoteManager.currentRemote()
            ?: return@callbackFlow

        val subscription = remote.playerApi
            .subscribeToPlayerState()
            .setEventCallback { state ->
                val track = state.track
                val simple = SimplePlayerState(
                    trackId = track?.uri?.substringAfter("spotify:track:"),
                    trackName = track?.name,
                    artistName = track?.artist?.name,
                    albumName = track?.album?.name,
                    isPlaying = !state.isPaused,
                    positionMs = state.playbackPosition,
                    durationMs = track?.duration?.toLong() ?: 0L
                )
                trySend(Result.success(simple))
            }
            .setErrorCallback { throwable ->
                trySend(Result.failure(throwable))
            }

        awaitClose {
            try {
                subscription.cancel()
            } catch (_: Throwable) { /* ignore */ }
        }
    }
}

