package com.asteroid.radio

import android.app.Application
import android.content.ComponentName
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RadioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TrackRepository()
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState.STOPPED)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _statusMessage = MutableStateFlow("Stopped")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _sleepTimerRemaining = MutableStateFlow(0L)
    val sleepTimerRemaining: StateFlow<Long> = _sleepTimerRemaining.asStateFlow()

    private val _selectedChannel = MutableStateFlow(Channel.CURATED)
    val selectedChannel: StateFlow<Channel> = _selectedChannel.asStateFlow()

    private val _selectedQuality = MutableStateFlow(StreamQuality.AAC)
    val selectedQuality: StateFlow<StreamQuality> = _selectedQuality.asStateFlow()

    private val _nowPlaying = MutableStateFlow("")
    val nowPlaying: StateFlow<String> = _nowPlaying.asStateFlow()

    private val _channelName = MutableStateFlow("")
    val channelName: StateFlow<String> = _channelName.asStateFlow()

    private val authRepository = AuthRepository(application)

    private var fetchJob: Job? = null
    private var sleepJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _playerState.value = PlayerState.CONNECTING
                    _statusMessage.value = "Connecting..."
                }
                Player.STATE_READY -> {
                    _playerState.value = PlayerState.PLAYING
                    _statusMessage.value = "Playing"
                }
                Player.STATE_ENDED -> {
                    _playerState.value = PlayerState.STOPPED
                    _statusMessage.value = "Stream ended"
                }
                Player.STATE_IDLE -> {
                    if (_playerState.value != PlayerState.STOPPED) {
                        _playerState.value = PlayerState.STOPPED
                        _statusMessage.value = "Stopped"
                    }
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            _playerState.value = PlayerState.ERROR
            _statusMessage.value = error.localizedMessage ?: "Playback error"
        }

        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata.get(i)
                if (entry is IcyInfo) {
                    val title = entry.title ?: ""
                    if (title.isNotEmpty()) {
                        _nowPlaying.value = title
                    }
                }
            }
        }
    }

    init {
        connectToService()
        refreshTracks()
        startPeriodicFetch()
        refreshChannelName()
    }

    private fun connectToService() {
        val app = getApplication<Application>()
        val sessionToken = SessionToken(app, ComponentName(app, RadioService::class.java))
        controllerFuture = MediaController.Builder(app, sessionToken).buildAsync()
        val future = controllerFuture ?: return
        val deferred = CompletableDeferred<MediaController>()
        future.addListener({ deferred.complete(future.get()) }, Runnable::run)
        viewModelScope.launch {
            mediaController = withContext(Dispatchers.Main) {
                deferred.await()
            }
            mediaController?.addListener(playerListener)
            if (mediaController?.isPlaying == true) {
                _playerState.value = PlayerState.PLAYING
                _statusMessage.value = "Playing"
            }
        }
    }

    fun togglePlayback() {
        val controller = mediaController ?: return
        when (_playerState.value) {
            PlayerState.STOPPED, PlayerState.ERROR -> {
                val mediaItem = MediaItem.fromUri(_selectedChannel.value.url(_selectedQuality.value))
                controller.setMediaItem(mediaItem)
                controller.prepare()
                controller.play()
                _playerState.value = PlayerState.CONNECTING
                _statusMessage.value = "Connecting..."
            }
            PlayerState.PLAYING, PlayerState.CONNECTING -> {
                controller.stop()
                controller.clearMediaItems()
                _playerState.value = PlayerState.STOPPED
                _statusMessage.value = "Stopped"
            }
        }
    }

    fun selectChannel(channel: Channel) {
        val wasPlaying = _playerState.value == PlayerState.PLAYING || _playerState.value == PlayerState.CONNECTING
        _selectedChannel.value = channel
        _nowPlaying.value = ""
        refreshTracks()
        if (wasPlaying) {
            togglePlayback() // stop
            togglePlayback() // restart with new channel
        }
    }

    fun selectQuality(quality: StreamQuality) {
        val wasPlaying = _playerState.value == PlayerState.PLAYING || _playerState.value == PlayerState.CONNECTING
        _selectedQuality.value = quality
        if (wasPlaying) {
            togglePlayback() // stop
            togglePlayback() // restart with new quality
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerRemaining.value = 0
            return
        }
        _sleepTimerRemaining.value = minutes * 60_000L
        sleepJob = viewModelScope.launch {
            var remaining = minutes * 60_000L
            while (remaining > 0) {
                delay(1000)
                remaining -= 1000
                _sleepTimerRemaining.value = remaining
            }
            togglePlayback()
            _sleepTimerRemaining.value = 0
        }
    }

    fun cancelSleepTimer() {
        sleepJob?.cancel()
        _sleepTimerRemaining.value = 0
    }

    fun refreshTracks() {
        viewModelScope.launch {
            _isLoading.value = true
            val mount = when (_selectedChannel.value) {
                Channel.CURATED -> "asteroid.aac"
                Channel.SHUFFLE -> "asteroid-shuffle.mp3"
            }
            val result = repository.fetchRecentlyPlayed(mount)
            if (result.isNotEmpty()) {
                _tracks.value = result
            }
            _isLoading.value = false
        }
    }

    private fun startPeriodicFetch() {
        fetchJob = viewModelScope.launch {
            while (true) {
                delay(15_000)
                refreshTracks()
                refreshChannelName()
            }
        }
    }

    private fun refreshChannelName() {
        viewModelScope.launch {
            val name = authRepository.fetchChannelName()
            if (name != null) {
                _channelName.value = name
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fetchJob?.cancel()
        sleepJob?.cancel()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}
