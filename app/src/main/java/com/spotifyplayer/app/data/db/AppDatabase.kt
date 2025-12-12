package com.spotifyplayer.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spotifyplayer.app.data.db.entity.PlaybackHistoryEntity
import com.spotifyplayer.app.data.db.entity.QueueItemEntity
import com.spotifyplayer.app.data.db.entity.TrackEntity
import com.spotifyplayer.app.data.db.entity.TrackStatisticsEntity

@Database(
    entities = [
        TrackEntity::class,
        TrackStatisticsEntity::class,
        PlaybackHistoryEntity::class,
        QueueItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): com.spotifyplayer.app.data.db.dao.TrackDao
    abstract fun trackStatisticsDao(): com.spotifyplayer.app.data.db.dao.TrackStatisticsDao
    abstract fun playbackHistoryDao(): com.spotifyplayer.app.data.db.dao.PlaybackHistoryDao
    abstract fun queueDao(): com.spotifyplayer.app.data.db.dao.QueueDao

    companion object {
        private const val DB_NAME = "spotify_player.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                ).build().also { INSTANCE = it }
            }
    }
}

