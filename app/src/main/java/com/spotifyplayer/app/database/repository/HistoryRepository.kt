package com.spotifyplayer.app.database.repository

import com.spotifyplayer.app.database.dao.PlaybackHistoryDao
import com.spotifyplayer.app.database.entity.PlaybackHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Playback History operations
 */
class HistoryRepository(private val historyDao: PlaybackHistoryDao) {
    
    /**
     * Add to history (maintains max 20 entries)
     */
    suspend fun addToHistory(trackId: String, wasSkipped: Boolean, position: Int) {
        val history = PlaybackHistoryEntity(
            trackId = trackId,
            playedAt = System.currentTimeMillis(),
            wasSkipped = wasSkipped,
            playbackPosition = position
        )
        historyDao.addToHistory(history)
    }
    
    /**
     * Get history (last N tracks)
     */
    suspend fun getHistory(limit: Int = 20): List<PlaybackHistoryEntity> {
        return historyDao.getHistory(limit)
    }
    
    /**
     * Get history as Flow
     */
    fun getHistoryFlow(limit: Int = 20): Flow<List<PlaybackHistoryEntity>> {
        return historyDao.getHistoryFlow(limit)
    }
    
    /**
     * Get history count
     */
    suspend fun getHistoryCount(): Int {
        return historyDao.getHistoryCount()
    }
    
    /**
     * Clear all history
     */
    suspend fun clearHistory() {
        historyDao.clearHistory()
    }
}


