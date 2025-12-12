package com.spotifyplayer.app.data.api.model

data class SavedTracksResponseDto(
    val items: List<SavedTrackItemDto>,
    val next: String?,
    val total: Int
)

data class SavedTrackItemDto(
    val track: SpotifyTrackDto?
)

data class SpotifyTrackDto(
    val id: String?,
    val name: String?,
    val artists: List<SpotifyArtistDto>?,
    val album: SpotifyAlbumDto?,
    val duration_ms: Long?,
    val uri: String?
)

data class SpotifyArtistDto(
    val name: String?
)

data class SpotifyAlbumDto(
    val name: String?,
    val images: List<SpotifyImageDto>?
)

data class SpotifyImageDto(
    val url: String?,
    val height: Int?,
    val width: Int?
)

