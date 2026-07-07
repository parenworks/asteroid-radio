package com.asteroid.radio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class TrackRepository {

    private val client = OkHttpClient()

    suspend fun fetchRecentlyPlayed(): List<Track> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(RadioPlayer.API_URL)
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("TrackRepository", "HTTP ${response.code}")
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                parseTracks(body)
            }
        } catch (e: Exception) {
            Log.e("TrackRepository", "Fetch failed", e)
            emptyList()
        }
    }

    private fun parseTracks(json: String): List<Track> {
        val tracks = mutableListOf<Track>()
        try {
            val root = JSONObject(json)
            val data = root.optJSONObject("data") ?: return tracks
            val tracksArr = data.optJSONArray("tracks") ?: return tracks

            for (i in 0 until tracksArr.length()) {
                val t = tracksArr.getJSONObject(i)
                tracks.add(Track(
                    title = t.optString("title", ""),
                    artist = t.optString("artist", ""),
                    song = t.optString("song", ""),
                    timestamp = t.optLong("timestamp", 0)
                ))
            }
        } catch (e: Exception) {
            Log.e("TrackRepository", "Parse failed", e)
        }
        return tracks
    }
}
