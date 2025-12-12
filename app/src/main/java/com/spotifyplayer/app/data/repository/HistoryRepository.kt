package com.spotifyplayer.app.data.repository

import com.spotifyplayer.app.data.db.dao.PlaybackHistoryDao
import com.spotifyplayer.app.data.db.entity.PlaybackHistoryEntity

class HistoryRepository(private val dao: PlaybackHistoryDao) {

    suspend fun addToHistory(trackId: String, wasSkipped: Boolean, playedAt: Long, playbackPosition: Int? = null) {
        dao.insert(
            PlaybackHistoryEntity(
                trackId = trackId,
                playedAt = playedAt,
                wasSkipped = wasSkipped,
                playbackPosition = playbackPosition
            )
        )
    }

    suspend fun getRecent(limit: Int = 20): List<PlaybackHistoryEntity> = dao.getLast(limit)

    suspend fun trimTo(limit: Int = 20) {
        dao.trimToLast(limit)
    }
}

