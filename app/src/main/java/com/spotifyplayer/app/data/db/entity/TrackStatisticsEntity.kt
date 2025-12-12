package com.spotifyplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "track_statistics",
    foreignKeys = [
        ForeignKey(
            entity = TrackEntity::class,
            parentColumns = ["id"],
            childColumns = ["trackId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("trackId"),
        Index(value = ["playCount", "lastPlayedAt"])
    ]
)
data class TrackStatisticsEntity(
    @PrimaryKey val trackId: String,
    val playCount: Long = 0,
    val lastPlayedAt: Long? = null,
    val totalPlayTimeMs: Long = 0
)

