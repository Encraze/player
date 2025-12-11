package com.spotifyplayer.app.database.dao

import androidx.room.*
import com.spotifyplayer.app.database.entity.TrackEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Track operations
 */
@Dao
interface TrackDao {
    
    /**
     * Insert a track (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(track: TrackEntity)
    
    /**
     * Insert multiple tracks (replace if exist)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<TrackEntity>)
    
    /**
     * Update a track
     */
    @Update
    suspend fun update(track: TrackEntity)
    
    /**
     * Delete a track
     */
    @Delete
    suspend fun delete(track: TrackEntity)
    
    /**
     * Get track by ID
     */
    @Query("SELECT * FROM tracks WHERE id = :trackId")
    suspend fun getTrackById(trackId: String): TrackEntity?
    
    /**
     * Get all tracks
     */
    @Query("SELECT * FROM tracks ORDER BY name ASC")
    suspend fun getAllTracks(): List<TrackEntity>
    
    /**
     * Get all tracks as Flow (for reactive updates)
     */
    @Query("SELECT * FROM tracks ORDER BY name ASC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>
    
    /**
     * Get track count
     */
    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
    
    /**
     * Search tracks by name or artist
     */
    @Query("SELECT * FROM tracks WHERE name LIKE '%' || :query || '%' OR artists LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchTracks(query: String): List<TrackEntity>
    
    /**
     * Delete all tracks
     */
    @Query("DELETE FROM tracks")
    suspend fun deleteAll()
}


