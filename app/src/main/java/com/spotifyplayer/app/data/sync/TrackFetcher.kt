package com.spotifyplayer.app.data.sync

import android.content.Context
import android.util.Log
import com.spotifyplayer.app.data.api.ApiErrorHandler
import com.spotifyplayer.app.data.api.SpotifyApiClient
import com.spotifyplayer.app.database.AppDatabase
import com.spotifyplayer.app.database.entity.TrackEntity
import com.spotifyplayer.app.database.entity.TrackStatisticsEntity
import com.spotifyplayer.app.data.model.SavedTrack
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fetches all liked tracks from Spotify and stores them in the database
 */
class TrackFetcher(private val context: Context) {
    
    private val apiClient = SpotifyApiClient.getInstance(context)
    private val database = AppDatabase.getInstance(context)
    private val trackDao = database.trackDao()
    private val statisticsDao = database.trackStatisticsDao()
    
    /**
     * Fetch all liked tracks with progress updates
     */
    suspend fun fetchAllTracks(
        onProgress: (current: Int, total: Int, message: String) -> Unit
    ): Result<Int> {
        return try {
            var offset = 0
            val limit = 50
            var totalTracks = 0
            var fetchedCount = 0
            var hasMore = true
            
            onProgress(0, 0, "Starting to fetch tracks...")
            
            while (hasMore) {
                // Fetch page
                val response = apiClient.apiService.getSavedTracks(limit, offset)
                
                if (response.isSuccessful) {
                    val savedTracksResponse = response.body()
                    
                    if (savedTracksResponse != null) {
                        // Update total on first page
                        if (offset == 0) {
                            totalTracks = savedTracksResponse.total
                            Log.d(TAG, "Total tracks to fetch: $totalTracks")
                        }
                        
                        // Process this batch
                        val tracks = savedTracksResponse.items
                        if (tracks.isNotEmpty()) {
                            processBatch(tracks)
                            fetchedCount += tracks.size
                            
                            onProgress(
                                fetchedCount,
                                totalTracks,
                                "Fetched $fetchedCount of $totalTracks tracks..."
                            )
                            
                            Log.d(TAG, "Fetched batch: $fetchedCount / $totalTracks")
                        }
                        
                        // Check if there are more pages
                        hasMore = savedTracksResponse.next != null
                        offset += limit
                        
                        // Small delay to avoid rate limiting
                        if (hasMore) {
                            delay(100)
                        }
                    } else {
                        hasMore = false
                    }
                } else {
                    val errorMsg = ApiErrorHandler.handleError(response)
                    Log.e(TAG, "Error fetching tracks: $errorMsg")
                    return Result.failure(Exception(errorMsg))
                }
            }
            
            onProgress(fetchedCount, totalTracks, "Complete! Fetched $fetchedCount tracks.")
            Log.d(TAG, "Successfully fetched all $fetchedCount tracks")
            
            Result.success(fetchedCount)
        } catch (e: Exception) {
            val errorMsg = ApiErrorHandler.handleException(e)
            Log.e(TAG, "Exception while fetching tracks", e)
            Result.failure(Exception(errorMsg))
        }
    }
    
    /**
     * Process a batch of tracks and insert into database
     */
    private suspend fun processBatch(savedTracks: List<SavedTrack>) {
        val trackEntities = mutableListOf<TrackEntity>()
        val statisticsEntities = mutableListOf<TrackStatisticsEntity>()
        
        for (savedTrack in savedTracks) {
            val track = savedTrack.track
            
            // Transform to TrackEntity
            val trackEntity = TrackEntity(
                id = track.id,
                name = track.name,
                artists = track.getArtistNames(),
                albumName = track.album.name,
                albumImageUrl = track.getAlbumImageUrl(),
                durationMs = track.durationMs,
                uri = track.uri,
                addedAt = parseTimestamp(savedTrack.addedAt),
                createdAt = System.currentTimeMillis()
            )
            
            trackEntities.add(trackEntity)
            
            // Create initial statistics entry (only if doesn't exist)
            val existingStats = statisticsDao.getStatistics(track.id)
            if (existingStats == null) {
                val statisticsEntity = TrackStatisticsEntity(
                    trackId = track.id,
                    playCount = 0,
                    skipCount = 0,
                    lastPlayedAt = null,
                    lastSkippedAt = null,
                    totalPlayTimeMs = 0
                )
                statisticsEntities.add(statisticsEntity)
            }
        }
        
        // Batch insert
        if (trackEntities.isNotEmpty()) {
            trackDao.insertAll(trackEntities)
            Log.d(TAG, "Inserted ${trackEntities.size} tracks")
        }
        
        if (statisticsEntities.isNotEmpty()) {
            statisticsDao.insertAll(statisticsEntities)
            Log.d(TAG, "Initialized ${statisticsEntities.size} statistics entries")
        }
    }
    
    /**
     * Parse ISO 8601 timestamp to milliseconds
     */
    private fun parseTimestamp(timestamp: String): Long {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(timestamp)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse timestamp: $timestamp", e)
            System.currentTimeMillis()
        }
    }
    
    /**
     * Get current track count in database
     */
    suspend fun getCurrentTrackCount(): Int {
        return trackDao.getTrackCount()
    }
    
    companion object {
        private const val TAG = "TrackFetcher"
    }
}


