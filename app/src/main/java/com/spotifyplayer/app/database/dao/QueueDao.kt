package com.spotifyplayer.app.database.dao

import androidx.room.*
import com.spotifyplayer.app.database.entity.QueueItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for Queue operations
 */
@Dao
interface QueueDao {
    
    /**
     * Insert queue item (replace if exists)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(queueItem: QueueItemEntity)
    
    /**
     * Insert multiple queue items
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(queueItems: List<QueueItemEntity>)
    
    /**
     * Delete queue item
     */
    @Delete
    suspend fun delete(queueItem: QueueItemEntity)
    
    /**
     * Get all queue items ordered by position
     */
    @Query("SELECT * FROM queue_items ORDER BY queue_position ASC")
    suspend fun getQueueItems(): List<QueueItemEntity>
    
    /**
     * Get queue items as Flow
     */
    @Query("SELECT * FROM queue_items ORDER BY queue_position ASC")
    fun getQueueItemsFlow(): Flow<List<QueueItemEntity>>
    
    /**
     * Get current track (position = 0)
     */
    @Query("SELECT * FROM queue_items WHERE queue_position = 0")
    suspend fun getCurrentTrack(): QueueItemEntity?
    
    /**
     * Get next N tracks (position > 0)
     */
    @Query("SELECT * FROM queue_items WHERE queue_position > 0 ORDER BY queue_position ASC LIMIT :count")
    suspend fun getNextTracks(count: Int): List<QueueItemEntity>
    
    /**
     * Get previous N tracks (position < 0)
     */
    @Query("SELECT * FROM queue_items WHERE queue_position < 0 ORDER BY queue_position DESC LIMIT :count")
    suspend fun getPreviousTracks(count: Int): List<QueueItemEntity>
    
    /**
     * Get queue item by position
     */
    @Query("SELECT * FROM queue_items WHERE queue_position = :position")
    suspend fun getQueueItemByPosition(position: Int): QueueItemEntity?
    
    /**
     * Delete queue item by position
     */
    @Query("DELETE FROM queue_items WHERE queue_position = :position")
    suspend fun deleteByPosition(position: Int)
    
    /**
     * Shift all positions by offset (for queue advancement)
     */
    @Query("UPDATE queue_items SET queue_position = queue_position + :offset")
    suspend fun shiftAllPositions(offset: Int)
    
    /**
     * Clear entire queue
     */
    @Query("DELETE FROM queue_items")
    suspend fun clearQueue()
    
    /**
     * Get queue size
     */
    @Query("SELECT COUNT(*) FROM queue_items")
    suspend fun getQueueSize(): Int
}


