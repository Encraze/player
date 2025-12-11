package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify Artist model
 */
data class Artist(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("uri")
    val uri: String
)


