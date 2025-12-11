package com.spotifyplayer.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Playback history entity - stores last 20 played tracks
 */
@Entity(
    tableName = "playback_history",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["played_at"], orders = [Index.Order.DESC]),
        Index(value = ["track_id"])
    ]
)
data class PlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "track_id")
    val trackId: String,
    
    @ColumnInfo(name = "played_at")
    val playedAt: Long,
    
    @ColumnInfo(name = "was_skipped", defaultValue = "0")
    val wasSkipped: Boolean = false,
    
    @ColumnInfo(name = "playback_position")
    val playbackPosition: Int // Position in history for ordering
)


