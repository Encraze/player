package com.spotifyplayer.app.data.repository

import com.spotifyplayer.app.data.db.dao.TrackDao
import com.spotifyplayer.app.data.db.entity.TrackEntity

class TrackRepository(private val trackDao: TrackDao) {

    suspend fun upsertTracks(tracks: List<TrackEntity>) {
        trackDao.upsertAll(tracks)
    }

    suspend fun upsertTrack(track: TrackEntity) {
        trackDao.upsert(track)
    }

    suspend fun getTrack(id: String): TrackEntity? = trackDao.getById(id)

    suspend fun getAllTracks(): List<TrackEntity> = trackDao.getAll()

    suspend fun getCount(): Int = trackDao.getTrackCount()
}

