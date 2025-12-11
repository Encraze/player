package com.spotifyplayer.app.data.sync

import android.content.Context
import com.spotifyplayer.app.database.AppDatabase
import com.spotifyplayer.app.database.entity.TrackEntity
import com.spotifyplayer.app.database.repository.TrackRepository
import kotlinx.coroutines.flow.Flow

/**
 * Manager for accessing track metadata from local database
 */
class TrackMetadataManager(context: Context) {
    
    private val database = AppDatabase.getInstance(context)
    private val trackRepository = TrackRepository(database.trackDao())
    
    /**
     * Get track by ID
     */
    suspend fun getTrackById(trackId: String): TrackEntity? {
        return trackRepository.getTrackById(trackId)
    }
    
    /**
     * Get all tracks
     */
    suspend fun getAllTracks(): List<TrackEntity> {
        return trackRepository.getAllTracks()
    }
    
    /**
     * Get all tracks as Flow (for reactive UI)
     */
    fun getAllTracksFlow(): Flow<List<TrackEntity>> {
        return trackRepository.getAllTracksFlow()
    }
    
    /**
     * Get track count
     */
    suspend fun getTrackCount(): Int {
        return trackRepository.getTrackCount()
    }
    
    /**
     * Search tracks by name or artist
     */
    suspend fun searchTracks(query: String): List<TrackEntity> {
        return trackRepository.searchTracks(query)
    }
    
    /**
     * Get tracks by IDs (batch lookup)
     */
    suspend fun getTracksByIds(trackIds: List<String>): List<TrackEntity> {
        return trackIds.mapNotNull { trackId ->
            trackRepository.getTrackById(trackId)
        }
    }
    
    /**
     * Check if tracks are cached
     */
    suspend fun areTracksCached(): Boolean {
        return trackRepository.getTrackCount() > 0
    }
    
    /**
     * Get track metadata summary
     */
    suspend fun getMetadataSummary(): MetadataSummary {
        val count = trackRepository.getTrackCount()
        val tracks = if (count > 0) {
            trackRepository.getAllTracks()
        } else {
            emptyList()
        }
        
        val totalDuration = tracks.sumOf { it.durationMs.toLong() }
        val uniqueArtists = tracks.map { it.artists }.distinct().size
        
        return MetadataSummary(
            trackCount = count,
            totalDurationMs = totalDuration,
            uniqueArtistCount = uniqueArtists
        )
    }
}

/**
 * Summary of cached metadata
 */
data class MetadataSummary(
    val trackCount: Int,
    val totalDurationMs: Long,
    val uniqueArtistCount: Int
) {
    fun getTotalDurationFormatted(): String {
        val hours = totalDurationMs / (1000 * 60 * 60)
        val minutes = (totalDurationMs / (1000 * 60)) % 60
        return "${hours}h ${minutes}m"
    }
}

