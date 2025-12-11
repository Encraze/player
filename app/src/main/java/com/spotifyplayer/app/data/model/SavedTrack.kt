package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify SavedTrack model (user's liked tracks)
 */
data class SavedTrack(
    @SerializedName("added_at")
    val addedAt: String,
    
    @SerializedName("track")
    val track: Track
)

/**
 * Paged response for saved tracks
 */
data class SavedTracksResponse(
    @SerializedName("href")
    val href: String,
    
    @SerializedName("items")
    val items: List<SavedTrack>,
    
    @SerializedName("limit")
    val limit: Int,
    
    @SerializedName("next")
    val next: String?,
    
    @SerializedName("offset")
    val offset: Int,
    
    @SerializedName("previous")
    val previous: String?,
    
    @SerializedName("total")
    val total: Int
)


