package com.spotifyplayer.app.database.dao

import androidx.room.*
import com.spotifyplayer.app.database.entity.TrackStatisticsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Track Statistics operations
 */
@Dao
interface TrackStatisticsDao {
    
    /**
     * Insert statistics (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: TrackStatisticsEntity)
    
    /**
     * Insert multiple statistics (replace if exist)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(statistics: List<TrackStatisticsEntity>)
    
    /**
     * Update statistics
     */
    @Update
    suspend fun update(statistics: TrackStatisticsEntity)
    
    /**
     * Get statistics for a track
     */
    @Query("SELECT * FROM track_statistics WHERE track_id = :trackId")
    suspend fun getStatistics(trackId: String): TrackStatisticsEntity?
    
    /**
     * Get all statistics as Flow
     */
    @Query("SELECT * FROM track_statistics")
    fun getAllStatisticsFlow(): Flow<List<TrackStatisticsEntity>>
    
    /**
     * Increment play count (+1 for play)
     */
    @Query("UPDATE track_statistics SET play_count = play_count + 1, last_played_at = :timestamp WHERE track_id = :trackId")
    suspend fun incrementPlayCount(trackId: String, timestamp: Long)
    
    /**
     * Increment skip count (+2 for skip)
     */
    @Query("UPDATE track_statistics SET play_count = play_count + 2, skip_count = skip_count + 1, last_skipped_at = :timestamp WHERE track_id = :trackId")
    suspend fun incrementSkipCount(trackId: String, timestamp: Long)
    
    /**
     * Get tracks sorted for shuffle (least played first, oldest first)
     * Excludes tracks in the provided list
     */
    @Query("""
        SELECT * FROM track_statistics 
        WHERE track_id NOT IN (:excludeTrackIds)
        ORDER BY 
            play_count ASC, 
            CASE WHEN last_played_at IS NULL THEN 0 ELSE last_played_at END ASC
        LIMIT :limit
    """)
    suspend fun getTracksForShuffle(excludeTrackIds: List<String>, limit: Int): List<TrackStatisticsEntity>
    
    /**
     * Get tracks sorted for shuffle (when no exclusions)
     */
    @Query("""
        SELECT * FROM track_statistics 
        ORDER BY 
            play_count ASC, 
            CASE WHEN last_played_at IS NULL THEN 0 ELSE last_played_at END ASC
        LIMIT :limit
    """)
    suspend fun getTracksForShuffleAll(limit: Int): List<TrackStatisticsEntity>
    
    /**
     * Delete all statistics
     */
    @Query("DELETE FROM track_statistics")
    suspend fun deleteAll()
}


