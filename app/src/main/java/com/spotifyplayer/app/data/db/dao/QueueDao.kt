package com.spotifyplayer.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.spotifyplayer.app.data.db.entity.QueueItemEntity

@Dao
interface QueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: QueueItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<QueueItemEntity>)

    @Query("SELECT * FROM queue_items ORDER BY queuePosition ASC")
    suspend fun getAllOrdered(): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items WHERE queuePosition = 0 LIMIT 1")
    suspend fun getCurrent(): QueueItemEntity?

    @Query("SELECT * FROM queue_items WHERE queuePosition > 0 ORDER BY queuePosition ASC LIMIT :limit")
    suspend fun getNext(limit: Int): List<QueueItemEntity>

    @Query("SELECT * FROM queue_items WHERE queuePosition < 0 ORDER BY queuePosition DESC LIMIT :limit")
    suspend fun getPrevious(limit: Int): List<QueueItemEntity>

    @Query("DELETE FROM queue_items")
    suspend fun clear()

    @Query("DELETE FROM queue_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM queue_items WHERE trackId = :trackId")
    suspend fun deleteByTrackId(trackId: String)
}

