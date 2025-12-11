package com.spotifyplayer.app.database.dao

import androidx.room.*
import com.spotifyplayer.app.database.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Playback History operations
 */
@Dao
interface PlaybackHistoryDao {
    
    /**
     * Insert history entry
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: PlaybackHistoryEntity)
    
    /**
     * Get last N tracks from history
     */
    @Query("SELECT * FROM playback_history ORDER BY played_at DESC LIMIT :limit")
    suspend fun getHistory(limit: Int = 20): List<PlaybackHistoryEntity>
    
    /**
     * Get history as Flow
     */
    @Query("SELECT * FROM playback_history ORDER BY played_at DESC LIMIT :limit")
    fun getHistoryFlow(limit: Int = 20): Flow<List<PlaybackHistoryEntity>>
    
    /**
     * Get history count
     */
    @Query("SELECT COUNT(*) FROM playback_history")
    suspend fun getHistoryCount(): Int
    
    /**
     * Delete oldest entries to keep only last N
     */
    @Query("""
        DELETE FROM playback_history 
        WHERE id NOT IN (
            SELECT id FROM playback_history 
            ORDER BY played_at DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun keepOnlyLastN(keepCount: Int = 20)
    
    /**
     * Clear all history
     */
    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()
    
    /**
     * Add to history and maintain max 20 entries
     */
    @Transaction
    suspend fun addToHistory(history: PlaybackHistoryEntity) {
        insert(history)
        keepOnlyLastN(20)
    }
}


