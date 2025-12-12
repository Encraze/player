package com.spotifyplayer.app.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val id: String,
    val name: String,
    val artists: String?, // comma-separated or JSON string
    val albumName: String?,
    val albumImageUrl: String?,
    val durationMs: Long?,
    val uri: String,
    val addedAt: Long?,   // timestamp from Spotify fetch
    val createdAt: Long?  // timestamp when added to local DB
)

