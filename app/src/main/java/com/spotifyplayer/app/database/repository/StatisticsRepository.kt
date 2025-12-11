package com.spotifyplayer.app.database.repository

import com.spotifyplayer.app.database.dao.TrackStatisticsDao
import com.spotifyplayer.app.database.entity.TrackStatisticsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for Track Statistics operations
 */
class StatisticsRepository(private val statisticsDao: TrackStatisticsDao) {
    
    /**
     * Insert statistics
     */
    suspend fun insertStatistics(statistics: TrackStatisticsEntity) {
        statisticsDao.insert(statistics)
    }
    
    /**
     * Insert multiple statistics
     */
    suspend fun insertAllStatistics(statistics: List<TrackStatisticsEntity>) {
        statisticsDao.insertAll(statistics)
    }
    
    /**
     * Update statistics
     */
    suspend fun updateStatistics(statistics: TrackStatisticsEntity) {
        statisticsDao.update(statistics)
    }
    
    /**
     * Get statistics for a track
     */
    suspend fun getStatistics(trackId: String): TrackStatisticsEntity? {
        return statisticsDao.getStatistics(trackId)
    }
    
    /**
     * Get all statistics as Flow
     */
    fun getAllStatisticsFlow(): Flow<List<TrackStatisticsEntity>> {
        return statisticsDao.getAllStatisticsFlow()
    }
    
    /**
     * Record a play (+1 to play count)
     */
    suspend fun recordPlay(trackId: String) {
        val timestamp = System.currentTimeMillis()
        statisticsDao.incrementPlayCount(trackId, timestamp)
    }
    
    /**
     * Record a skip (+2 to play count)
     */
    suspend fun recordSkip(trackId: String) {
        val timestamp = System.currentTimeMillis()
        statisticsDao.incrementSkipCount(trackId, timestamp)
    }
    
    /**
     * Record multiple skips
     */
    suspend fun recordMultipleSkips(trackIds: List<String>) {
        val timestamp = System.currentTimeMillis()
        trackIds.forEach { trackId ->
            statisticsDao.incrementSkipCount(trackId, timestamp)
        }
    }
    
    /**
     * Get tracks for shuffle (sorted by play count, then timestamp)
     */
    suspend fun getTracksForShuffle(excludeTrackIds: List<String>, limit: Int): List<TrackStatisticsEntity> {
        return if (excludeTrackIds.isEmpty()) {
            statisticsDao.getTracksForShuffleAll(limit)
        } else {
            statisticsDao.getTracksForShuffle(excludeTrackIds, limit)
        }
    }
    
    /**
     * Delete all statistics
     */
    suspend fun deleteAllStatistics() {
        statisticsDao.deleteAll()
    }
}


