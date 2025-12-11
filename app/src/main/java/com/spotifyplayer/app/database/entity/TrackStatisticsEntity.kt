package com.spotifyplayer.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Track statistics entity - stores play counts and timestamps for each track
 */
@Entity(
    tableName = "track_statistics",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["track_id"], unique = true),
        Index(value = ["play_count", "last_played_at"]) // For shuffle sorting
    ]
)
data class TrackStatisticsEntity(
    @PrimaryKey
    @ColumnInfo(name = "track_id")
    val trackId: String,
    
    @ColumnInfo(name = "play_count", defaultValue = "0")
    val playCount: Int = 0,
    
    @ColumnInfo(name = "skip_count", defaultValue = "0")
    val skipCount: Int = 0,
    
    @ColumnInfo(name = "last_played_at")
    val lastPlayedAt: Long? = null, // NULL if never played
    
    @ColumnInfo(name = "last_skipped_at")
    val lastSkippedAt: Long? = null, // NULL if never skipped
    
    @ColumnInfo(name = "total_play_time_ms", defaultValue = "0")
    val totalPlayTimeMs: Long = 0 // Optional: total time track has been played
)


