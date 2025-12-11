package com.spotifyplayer.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spotifyplayer.app.database.dao.*
import com.spotifyplayer.app.database.entity.*

/**
 * Room Database for the app
 */
@Database(
    entities = [
        TrackEntity::class,
        TrackStatisticsEntity::class,
        PlaybackHistoryEntity::class,
        QueueItemEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun trackDao(): TrackDao
    abstract fun trackStatisticsDao(): TrackStatisticsDao
    abstract fun playbackHistoryDao(): PlaybackHistoryDao
    abstract fun queueDao(): QueueDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        private const val DATABASE_NAME = "spotify_player_db"
        
        /**
         * Get singleton instance of the database
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }
        
        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration() // For development - remove in production
                .build()
        }
    }
}


