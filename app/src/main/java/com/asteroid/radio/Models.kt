package com.asteroid.radio

data class Track(
    val title: String = "",
    val artist: String = "",
    val song: String = "",
    val timestamp: Long = 0
)

data class RecentlyPlayedResponse(
    val data: RecentlyPlayedData? = null
)

data class RecentlyPlayedData(
    val tracks: List<TrackData>? = null
)

data class TrackData(
    val title: String? = null,
    val artist: String? = null,
    val song: String? = null,
    val timestamp: Long? = null
)
