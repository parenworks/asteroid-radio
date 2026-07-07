package com.asteroid.radio

import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

enum class PlayerState {
    STOPPED, CONNECTING, PLAYING, ERROR
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
        val mediaItem = MediaItem.fromUri(STREAM_URL)
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
        const val STREAM_URL = "http://ice.asteroid.radio/asteroid.aac"
        const val API_URL = "https://asteroid.radio/api/asteroid/recently-played"
    }
}
