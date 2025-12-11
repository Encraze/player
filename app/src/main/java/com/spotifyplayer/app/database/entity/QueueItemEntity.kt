package com.spotifyplayer.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Queue item entity - stores current playback queue
 * Note: This table is temporary and doesn't persist across app restarts
 * 
 * Queue positions:
 * - Negative values (-20 to -1): History (20 tracks)
 * - 0: Current track
 * - Positive values (1 to 30): Upcoming tracks (30 tracks)
 */
@Entity(
    tableName = "queue_items",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["track_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["queue_position"], unique = true),
        Index(value = ["track_id"])
    ]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "track_id")
    val trackId: String,
    
    @ColumnInfo(name = "queue_position")
    val queuePosition: Int, // 0 = current, negative = history, positive = upcoming
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long = System.currentTimeMillis()
)


