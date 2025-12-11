package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify Album model
 */
data class Album(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("images")
    val images: List<Image>,
    
    @SerializedName("uri")
    val uri: String
)

data class Image(
    @SerializedName("url")
    val url: String,
    
    @SerializedName("height")
    val height: Int?,
    
    @SerializedName("width")
    val width: Int?
)


