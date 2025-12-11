package com.spotifyplayer.app.database.repository

import com.spotifyplayer.app.database.dao.TrackDao
import com.spotifyplayer.app.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Track operations
 */
class TrackRepository(private val trackDao: TrackDao) {
    
    /**
     * Insert a track
     */
    suspend fun insertTrack(track: TrackEntity) {
        trackDao.insert(track)
    }
    
    /**
     * Insert multiple tracks
     */
    suspend fun insertTracks(tracks: List<TrackEntity>) {
        trackDao.insertAll(tracks)
    }
    
    /**
     * Update a track
     */
    suspend fun updateTrack(track: TrackEntity) {
        trackDao.update(track)
    }
    
    /**
     * Delete a track
     */
    suspend fun deleteTrack(track: TrackEntity) {
        trackDao.delete(track)
    }
    
    /**
     * Get track by ID
     */
    suspend fun getTrackById(trackId: String): TrackEntity? {
        return trackDao.getTrackById(trackId)
    }
    
    /**
     * Get all tracks
     */
    suspend fun getAllTracks(): List<TrackEntity> {
        return trackDao.getAllTracks()
    }
    
    /**
     * Get all tracks as Flow
     */
    fun getAllTracksFlow(): Flow<List<TrackEntity>> {
        return trackDao.getAllTracksFlow()
    }
    
    /**
     * Get track count
     */
    suspend fun getTrackCount(): Int {
        return trackDao.getTrackCount()
    }
    
    /**
     * Search tracks
     */
    suspend fun searchTracks(query: String): List<TrackEntity> {
        return trackDao.searchTracks(query)
    }
    
    /**
     * Delete all tracks
     */
    suspend fun deleteAllTracks() {
        trackDao.deleteAll()
    }
}


