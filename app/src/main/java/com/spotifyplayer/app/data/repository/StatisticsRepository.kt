package com.spotifyplayer.app.data.repository

import com.spotifyplayer.app.data.db.dao.TrackStatisticsDao
import com.spotifyplayer.app.data.db.entity.TrackStatisticsEntity

class StatisticsRepository(private val dao: TrackStatisticsDao) {

    suspend fun initStatsForTracks(trackIds: List<String>) {
        val stats = trackIds.map { TrackStatisticsEntity(trackId = it) }
        dao.upsertAll(stats)
    }

    suspend fun recordPlay(trackId: String, timestamp: Long) {
        dao.recordPlay(trackId, timestamp)
    }

    suspend fun recordSkip(trackId: String, timestamp: Long) {
        dao.recordSkip(trackId, timestamp)
    }

    suspend fun recordMultipleSkips(trackIds: List<String>, timestamp: Long) {
        if (trackIds.isEmpty()) return
        dao.recordMultipleSkips(trackIds, timestamp)
    }

    suspend fun getLeastPlayed(limit: Int, excludeIds: List<String>): List<String> =
        dao.getLeastPlayed(limit, excludeIds)
}

