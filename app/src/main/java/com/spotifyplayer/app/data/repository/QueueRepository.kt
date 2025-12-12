package com.spotifyplayer.app.data.repository

import com.spotifyplayer.app.data.db.dao.QueueDao
import com.spotifyplayer.app.data.db.entity.QueueItemEntity

class QueueRepository(private val dao: QueueDao) {

    suspend fun replaceQueue(items: List<QueueItemEntity>) {
        dao.clear()
        if (items.isNotEmpty()) {
            dao.upsertAll(items)
        }
    }

    suspend fun upsertItems(items: List<QueueItemEntity>) {
        if (items.isNotEmpty()) dao.upsertAll(items)
    }

    suspend fun upsertItem(item: QueueItemEntity) = dao.upsert(item)

    suspend fun getOrdered(): List<QueueItemEntity> = dao.getAllOrdered()

    suspend fun getCurrent(): QueueItemEntity? = dao.getCurrent()

    suspend fun clear() = dao.clear()
}

