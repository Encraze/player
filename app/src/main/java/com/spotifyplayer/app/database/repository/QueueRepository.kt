package com.spotifyplayer.app.database.repository

import com.spotifyplayer.app.database.dao.QueueDao
import com.spotifyplayer.app.database.entity.QueueItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Queue operations
 */
class QueueRepository(private val queueDao: QueueDao) {
    
    /**
     * Insert queue item
     */
    suspend fun insertQueueItem(trackId: String, position: Int) {
        val queueItem = QueueItemEntity(
            trackId = trackId,
            queuePosition = position
        )
        queueDao.insert(queueItem)
    }
    
    /**
     * Insert multiple queue items
     */
    suspend fun insertQueueItems(items: List<Pair<String, Int>>) {
        val queueItems = items.map { (trackId, position) ->
            QueueItemEntity(
                trackId = trackId,
                queuePosition = position
            )
        }
        queueDao.insertAll(queueItems)
    }
    
    /**
     * Get all queue items
     */
    suspend fun getQueueItems(): List<QueueItemEntity> {
        return queueDao.getQueueItems()
    }
    
    /**
     * Get queue items as Flow
     */
    fun getQueueItemsFlow(): Flow<List<QueueItemEntity>> {
        return queueDao.getQueueItemsFlow()
    }
    
    /**
     * Get current track (position = 0)
     */
    suspend fun getCurrentTrack(): QueueItemEntity? {
        return queueDao.getCurrentTrack()
    }
    
    /**
     * Get next N tracks
     */
    suspend fun getNextTracks(count: Int): List<QueueItemEntity> {
        return queueDao.getNextTracks(count)
    }
    
    /**
     * Get previous N tracks
     */
    suspend fun getPreviousTracks(count: Int): List<QueueItemEntity> {
        return queueDao.getPreviousTracks(count)
    }
    
    /**
     * Get queue item by position
     */
    suspend fun getQueueItemByPosition(position: Int): QueueItemEntity? {
        return queueDao.getQueueItemByPosition(position)
    }
    
    /**
     * Delete queue item by position
     */
    suspend fun deleteByPosition(position: Int) {
        queueDao.deleteByPosition(position)
    }
    
    /**
     * Shift all positions (for queue advancement)
     */
    suspend fun shiftAllPositions(offset: Int) {
        queueDao.shiftAllPositions(offset)
    }
    
    /**
     * Clear entire queue
     */
    suspend fun clearQueue() {
        queueDao.clearQueue()
    }
    
    /**
     * Get queue size
     */
    suspend fun getQueueSize(): Int {
        return queueDao.getQueueSize()
    }
}


