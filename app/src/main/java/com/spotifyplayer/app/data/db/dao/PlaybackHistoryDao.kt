package com.spotifyplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotifyplayer.app.data.db.entity.PlaybackHistoryEntity

@Dao
interface PlaybackHistoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: PlaybackHistoryEntity)

    @Query("SELECT * FROM playback_history ORDER BY playedAt DESC LIMIT :limit")
    suspend fun getLast(limit: Int): List<PlaybackHistoryEntity>

    @Query(
        """
        DELETE FROM playback_history
        WHERE id NOT IN (
            SELECT id FROM playback_history
            ORDER BY playedAt DESC
            LIMIT :keep
        )
        """
    )
    suspend fun trimToLast(keep: Int)
}

