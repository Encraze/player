package com.spotifyplayer.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Spotify Device model
 */
data class Device(
    @SerializedName("id")
    val id: String?,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("type")
    val type: String,
    
    @SerializedName("is_active")
    val isActive: Boolean,
    
    @SerializedName("is_restricted")
    val isRestricted: Boolean,
    
    @SerializedName("volume_percent")
    val volumePercent: Int?
)

/**
 * Available devices response
 */
data class DevicesResponse(
    @SerializedName("devices")
    val devices: List<Device>
)


