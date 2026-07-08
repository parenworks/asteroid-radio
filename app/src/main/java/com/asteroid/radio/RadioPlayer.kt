package com.asteroid.radio

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

enum class PlayerState {
    STOPPED, CONNECTING, PLAYING, ERROR
}

enum class Channel(val label: String, val prefix: String) {
    CURATED("Curated", "asteroid"),
    SHUFFLE("Shuffle", "asteroid-shuffle");

    fun url(quality: StreamQuality): String = when (this) {
        CURATED -> when (quality) {
            StreamQuality.AAC -> "http://ice.asteroid.radio/$prefix.aac"
            StreamQuality.MP3_HQ -> "http://ice.asteroid.radio/$prefix.mp3"
            StreamQuality.MP3_LQ -> "http://ice.asteroid.radio/$prefix-low.mp3"
        }
        SHUFFLE -> "http://ice.asteroid.radio/$prefix.mp3"
    }
}

enum class StreamQuality(val label: String, val description: String) {
    AAC("AAC", "AAC 96kbps Stereo"),
    MP3_HQ("MP3 HQ", "MP3 128kbps Stereo"),
    MP3_LQ("MP3 LQ", "MP3 64kbps Mono"),
}

class RadioPlayer(private val player: ExoPlayer) {

    var onStateChange: ((PlayerState, String) -> Unit)? = null

    private var state: PlayerState = PlayerState.STOPPED

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    state = PlayerState.CONNECTING
                    onStateChange?.invoke(state, "Connecting...")
                }
                Player.STATE_READY -> {
                    state = PlayerState.PLAYING
                    onStateChange?.invoke(state, "Playing")
                }
                Player.STATE_ENDED -> {
                    state = PlayerState.STOPPED
                    onStateChange?.invoke(state, "Stream ended")
                }
                Player.STATE_IDLE -> {
                    if (state != PlayerState.STOPPED) {
                        state = PlayerState.STOPPED
                        onStateChange?.invoke(state, "Stopped")
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("RadioPlayer", "Playback error", error)
            state = PlayerState.ERROR
            onStateChange?.invoke(state, error.localizedMessage ?: "Playback error")
        }
    }

    init {
        player.addListener(listener)
    }

    fun startStream() {
        val mediaItem = MediaItem.fromUri(Channel.CURATED.url(StreamQuality.AAC))
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
        state = PlayerState.CONNECTING
        onStateChange?.invoke(state, "Connecting...")
    }

    fun stopStream() {
        player.stop()
        player.clearMediaItems()
        state = PlayerState.STOPPED
        onStateChange?.invoke(state, "Stopped")
    }

    fun release() {
        player.removeListener(listener)
        player.release()
    }

    companion object {
        const val API_URL = "https://asteroid.radio/api/asteroid/recently-played"
    }
}
