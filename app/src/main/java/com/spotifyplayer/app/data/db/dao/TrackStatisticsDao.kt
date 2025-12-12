package com.spotifyplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotifyplayer.app.data.db.entity.TrackStatisticsEntity

@Dao
interface TrackStatisticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stats: TrackStatisticsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(stats: List<TrackStatisticsEntity>)

    @Query(
        """
        UPDATE track_statistics
        SET playCount = playCount + 1,
            lastPlayedAt = :timestamp
        WHERE trackId = :trackId
        """
    )
    suspend fun recordPlay(trackId: String, timestamp: Long)

    @Query(
        """
        UPDATE track_statistics
        SET playCount = playCount + 2,
            lastPlayedAt = :timestamp
        WHERE trackId = :trackId
        """
    )
    suspend fun recordSkip(trackId: String, timestamp: Long)

    @Query(
        """
        UPDATE track_statistics
        SET playCount = playCount + 2,
            lastPlayedAt = :timestamp
        WHERE trackId IN (:trackIds)
        """
    )
    suspend fun recordMultipleSkips(trackIds: List<String>, timestamp: Long)

    @Query(
        """
        SELECT trackId FROM track_statistics
        WHERE (:excludeSize = 0 OR trackId NOT IN (:excludeIds))
        ORDER BY playCount ASC,
                 COALESCE(lastPlayedAt, 0) ASC
        LIMIT :limit
        """
    )
    suspend fun getLeastPlayed(
        limit: Int,
        excludeIds: List<String>,
        excludeSize: Int = excludeIds.size
    ): List<String>

    @Query(
        """
        SELECT trackId, playCount
        FROM track_statistics
        WHERE trackId IN (:ids)
        """
    )
    suspend fun getPlayCounts(ids: List<String>): List<TrackPlayCount>
}

data class TrackPlayCount(
    val trackId: String,
    val playCount: Long
)

