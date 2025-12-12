package com.spotifyplayer.app.data.sync

import android.content.Context
import com.spotifyplayer.app.auth.SpotifyAuthManager
import com.spotifyplayer.app.data.api.SpotifyApiClient
import com.spotifyplayer.app.data.db.AppDatabase
import com.spotifyplayer.app.data.db.entity.TrackEntity
import com.spotifyplayer.app.data.repository.StatisticsRepository
import com.spotifyplayer.app.data.repository.TrackRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrackFetcher(
    private val context: Context
) {

    private val authManager = SpotifyAuthManager(context)
    private val db = AppDatabase.getInstance(context)
    private val trackRepo = TrackRepository(db.trackDao())
    private val statsRepo = StatisticsRepository(db.trackStatisticsDao())
    private val api = SpotifyApiClient.getInstance(context).api

    /**
    * Fetch all liked tracks from Spotify, store in DB, and initialize stats.
    * progress: callback(current, total, message)
    */
    suspend fun fetchAllTracks(progress: (current: Int, total: Int, message: String) -> Unit): Result<Int> {
        return withContext(Dispatchers.IO) {
            val token = authManager.getAccessToken()
                ?: return@withContext Result.failure(IllegalStateException("No access token. Please login."))

            var offset = 0
            val limit = 50
            var total = 0
            var fetched = 0
            val toInsert = mutableListOf<TrackEntity>()

            try {
                do {
                    val response = api.getSavedTracks(limit = limit, offset = offset)
                    if (total == 0) total = response.total

                    val items = response.items
                    val mapped = items.mapNotNull { it.track }.mapNotNull { trackDto ->
                        val id = trackDto.id ?: return@mapNotNull null
                        val name = trackDto.name ?: return@mapNotNull null
                        val uri = trackDto.uri ?: return@mapNotNull null

                        TrackEntity(
                            id = id,
                            name = name,
                            artists = trackDto.artists?.mapNotNull { it.name }?.joinToString(", "),
                            albumName = trackDto.album?.name,
                            albumImageUrl = trackDto.album?.images?.firstOrNull()?.url,
                            durationMs = trackDto.duration_ms,
                            uri = uri,
                            addedAt = null,
                            createdAt = System.currentTimeMillis()
                        )
                    }

                    toInsert.addAll(mapped)
                    fetched += mapped.size

                    progress(fetched, total, "Fetched $fetched of $total")

                    offset += limit
                } while (response.next != null)

                // Upsert tracks and initialize stats for new ones
                if (toInsert.isNotEmpty()) {
                    trackRepo.upsertTracks(toInsert)
                    statsRepo.initStatsForTracks(toInsert.map { it.id })
                }

                Result.success(fetched)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

