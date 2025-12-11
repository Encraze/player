package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify Track model
 */
data class Track(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("artists")
    val artists: List<Artist>,
    
    @SerializedName("album")
    val album: Album,
    
    @SerializedName("duration_ms")
    val durationMs: Int,
    
    @SerializedName("uri")
    val uri: String,
    
    @SerializedName("track_number")
    val trackNumber: Int?,
    
    @SerializedName("explicit")
    val explicit: Boolean?
) {
    /**
     * Get comma-separated artist names
     */
    fun getArtistNames(): String {
        return artists.joinToString(", ") { it.name }
    }
    
    /**
     * Get album image URL (largest available)
     */
    fun getAlbumImageUrl(): String? {
        return album.images.maxByOrNull { it.height ?: 0 }?.url
    }
}


