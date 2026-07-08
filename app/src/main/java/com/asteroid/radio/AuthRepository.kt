package com.asteroid.radio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

data class AuthState(
    val loggedIn: Boolean = false,
    val isAdmin: Boolean = false,
    val username: String? = null
)

data class UserProfile(
    val username: String = "",
    val email: String = "",
    val role: String = "",
    val createdAt: String = "",
    val lastActive: String = ""
)

data class ListeningStats(
    val totalListenTime: Long = 0,
    val tracksPlayed: Int = 0,
    val favoriteGenre: String = ""
)

data class UserTrack(
    val title: String = "",
    val artist: String = "",
    val playedAt: String = "",
    val duration: Long = 0
)

data class TopArtist(
    val name: String = "",
    val playCount: Int = 0
)

class AuthRepository(context: Context) {

    private val cookieStore = PersistentCookieStore(context)
    private val client = OkHttpClient.Builder()
        .cookieJar(cookieStore)
        .build()

    suspend fun login(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val formBody = FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build()

            val request = Request.Builder()
                .url("https://asteroid.radio/login")
                .post(formBody)
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val success = !body.contains("Invalid username or password") &&
                                  !body.contains("display-error") &&
                                  cookieStore.hasSessionCookie()
                    if (success) {
                        cookieStore.persist()
                    }
                    success
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Login failed", e)
            false
        }
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/logout")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Logout failed", e)
        } finally {
            cookieStore.clear()
            cookieStore.persist()
        }
    }

    suspend fun checkAuthStatus(): AuthState = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/api/asteroid/auth-status")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext AuthState()
                val body = response.body?.string() ?: return@withContext AuthState()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                AuthState(
                    loggedIn = data.optBoolean("loggedIn", false),
                    isAdmin = data.optBoolean("isAdmin", false),
                    username = data.optString("username", null)
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Auth status check failed", e)
            AuthState()
        }
    }

    fun hasPersistedSession(): Boolean = cookieStore.hasSessionCookie()

    fun loadPersistedCookies() = cookieStore.load()

    suspend fun fetchChannelName(): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null
                val regex = Regex("""<option value="curated">[^<]*</option>""")
                val match = regex.find(html) ?: return@withContext null
                val text = match.value
                    .replace("<option value=\"curated\">", "")
                    .replace("</option>", "")
                    .trim()
                if (text.isNotEmpty()) text else null
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Channel name fetch failed", e)
            null
        }
    }

    suspend fun fetchUserProfile(): UserProfile? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/api/asteroid/user/profile")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val user = data.optJSONObject("user") ?: return@withContext null
                UserProfile(
                    username = user.optString("username", ""),
                    email = user.optString("email", ""),
                    role = user.optString("role", ""),
                    createdAt = user.optString("created_at", ""),
                    lastActive = user.optString("last_active", "")
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Profile fetch failed", e)
            null
        }
    }

    suspend fun fetchListeningStats(): ListeningStats? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/api/asteroid/user/listening-stats")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val stats = data.optJSONObject("stats") ?: return@withContext null
                ListeningStats(
                    totalListenTime = stats.optLong("total_listen_time", 0),
                    tracksPlayed = stats.optInt("tracks_played", 0),
                    favoriteGenre = stats.optString("favorite_genre", "")
                )
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Stats fetch failed", e)
            null
        }
    }

    suspend fun fetchRecentTracks(limit: Int = 10): List<UserTrack> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/api/asteroid/user/recent-tracks?limit=$limit")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val tracksArr = data.optJSONArray("tracks") ?: return@withContext emptyList()
                val tracks = mutableListOf<UserTrack>()
                for (i in 0 until tracksArr.length()) {
                    val t = tracksArr.getJSONObject(i)
                    tracks.add(UserTrack(
                        title = t.optString("title", ""),
                        artist = t.optString("artist", ""),
                        playedAt = t.optString("played_at", ""),
                        duration = t.optLong("duration", 0)
                    ))
                }
                tracks
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Recent tracks fetch failed", e)
            emptyList()
        }
    }

    suspend fun fetchTopArtists(limit: Int = 10): List<TopArtist> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://asteroid.radio/api/asteroid/user/top-artists?limit=$limit")
                .header("User-Agent", "AsteroidRadio/1.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: json
                val arr = data.optJSONArray("artists") ?: return@withContext emptyList()
                val artists = mutableListOf<TopArtist>()
                for (i in 0 until arr.length()) {
                    val a = arr.getJSONObject(i)
                    artists.add(TopArtist(
                        name = a.optString("name", "Unknown"),
                        playCount = a.optInt("play_count", 0)
                    ))
                }
                artists
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Top artists fetch failed", e)
            emptyList()
        }
    }

}

class PersistentCookieStore(private val context: Context) : CookieJar {

    private val cookies = ConcurrentHashMap<String, MutableList<Cookie>>()
    private val cookieFile: File by lazy {
        File(context.filesDir, "cookies.txt")
    }

    init {
        load()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val host = url.host
        return cookies[host]?.toList() ?: emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val list = this.cookies.getOrPut(host) { mutableListOf() }
        for (cookie in cookies) {
            list.removeAll { it.name == cookie.name }
            list.add(cookie)
        }
    }

    fun hasSessionCookie(): Boolean {
        for ((_, list) in cookies) {
            if (list.any { it.name == "radiance-session" }) return true
        }
        return false
    }

    fun clear() {
        cookies.clear()
    }

    fun persist() {
        try {
            val sb = StringBuilder()
            for ((host, list) in cookies) {
                for (cookie in list) {
                    sb.append("$host|${cookie.name}|${cookie.value}|${cookie.expiresAt}\n")
                }
            }
            cookieFile.writeText(sb.toString())
        } catch (e: Exception) {
            Log.e("CookieStore", "Persist failed", e)
        }
    }

    fun load() {
        try {
            if (!cookieFile.exists()) return
            for (line in cookieFile.readLines()) {
                val parts = line.split("|")
                if (parts.size < 4) continue
                val host = parts[0]
                val name = parts[1]
                val value = parts[2]
                val expiresAt = parts[3].toLongOrNull() ?: Long.MAX_VALUE
                if (expiresAt < System.currentTimeMillis()) continue
                val cookie = Cookie.Builder()
                    .name(name)
                    .value(value)
                    .domain(host)
                    .expiresAt(expiresAt)
                    .build()
                cookies.getOrPut(host) { mutableListOf() }.add(cookie)
            }
        } catch (e: Exception) {
            Log.e("CookieStore", "Load failed", e)
        }
    }
}
