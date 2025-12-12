package com.spotifyplayer.app.data.api

import com.spotifyplayer.app.data.api.model.SavedTracksResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotifyApiService {

    @GET("me/tracks")
    suspend fun getSavedTracks(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): SavedTracksResponseDto
}

