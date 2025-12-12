package com.spotifyplayer.app.playback

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotifyplayer.app.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SpotifyRemoteManager(private val context: Context) {

    @Volatile
    private var appRemote: SpotifyAppRemote? = null

    val isConnected: Boolean
        get() = appRemote?.isConnected == true

    fun currentRemote(): SpotifyAppRemote? = appRemote

    /**
     * Synchronous ensure connection for internal use (call from background thread).
     */
    fun ensureConnectedBlocking(): Unit {
        if (isConnected) return
        // Blocking connect is not provided; callers should have awaited connect() before.
        throw IllegalStateException("Not connected")
    }

    suspend fun connect(): Result<Unit> = suspendCancellableCoroutine { cont ->
        if (isConnected) {
            cont.resume(Result.success(Unit))
            return@suspendCancellableCoroutine
        }

        val params = ConnectionParams.Builder(BuildConfig.SPOTIFY_CLIENT_ID)
            .setRedirectUri(BuildConfig.SPOTIFY_REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(
            context,
            params,
            object : Connector.ConnectionListener {
                override fun onConnected(remote: SpotifyAppRemote) {
                    appRemote = remote
                    if (cont.isActive) cont.resume(Result.success(Unit))
                }

                override fun onFailure(error: Throwable) {
                    if (cont.isActive) cont.resume(Result.failure(error))
                }
            }
        )

        cont.invokeOnCancellation {
            disconnect()
        }
    }

    fun disconnect() {
        appRemote?.let { SpotifyAppRemote.disconnect(it) }
        appRemote = null
    }

    suspend fun play(uri: String): Result<Unit> {
        val ensure = connect()
        if (ensure.isFailure) return ensure
        return runCatching {
            appRemote?.playerApi?.play(uri)
        }.map { Unit }
    }

    suspend fun pause(): Result<Unit> {
        val ensure = connect()
        if (ensure.isFailure) return ensure
        return runCatching {
            appRemote?.playerApi?.pause()
        }.map { Unit }
    }

    suspend fun resume(): Result<Unit> {
        val ensure = connect()
        if (ensure.isFailure) return ensure
        return runCatching {
            appRemote?.playerApi?.resume()
        }.map { Unit }
    }

    suspend fun connectWithRetry(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 2000L,
        maxDelayMs: Long = 10_000L
    ): Result<Unit> {
        var attempt = 0
        var delayMs = initialDelayMs
        while (attempt < maxAttempts) {
            val result = connect()
            if (result.isSuccess) return result
            attempt++
            if (attempt >= maxAttempts) return result
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(maxDelayMs)
        }
        return Result.failure(IllegalStateException("Unable to connect after $maxAttempts attempts"))
    }
}

