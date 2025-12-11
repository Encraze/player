package com.spotifyplayer.app.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Track entity - stores all liked tracks from Spotify
 */
@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    
    @ColumnInfo(name = "name")
    val name: String,
    
    @ColumnInfo(name = "artists")
    val artists: String, // Comma-separated artist names
    
    @ColumnInfo(name = "album_name")
    val albumName: String,
    
    @ColumnInfo(name = "album_image_url")
    val albumImageUrl: String?,
    
    @ColumnInfo(name = "duration_ms")
    val durationMs: Int,
    
    @ColumnInfo(name = "uri")
    val uri: String,
    
    @ColumnInfo(name = "added_at")
    val addedAt: Long, // Timestamp when fetched from Spotify
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis() // Timestamp when added to local DB
)


