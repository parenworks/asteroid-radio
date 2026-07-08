package com.asteroid.radio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application)

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _listeningStats = MutableStateFlow<ListeningStats?>(null)
    val listeningStats: StateFlow<ListeningStats?> = _listeningStats.asStateFlow()

    private val _recentTracks = MutableStateFlow<List<UserTrack>>(emptyList())
    val recentTracks: StateFlow<List<UserTrack>> = _recentTracks.asStateFlow()

    private val _topArtists = MutableStateFlow<List<TopArtist>>(emptyList())
    val topArtists: StateFlow<List<TopArtist>> = _topArtists.asStateFlow()

    private val _isLoadingProfile = MutableStateFlow(false)
    val isLoadingProfile: StateFlow<Boolean> = _isLoadingProfile.asStateFlow()

    init {
        checkAuthStatus()
    }

    fun login(username: String, password: String) {
        _isLoggingIn.value = true
        _loginError.value = null
        viewModelScope.launch {
            val success = repository.login(username, password)
            _isLoggingIn.value = false
            if (success) {
                checkAuthStatus()
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState()
        }
    }

    fun checkAuthStatus() {
        viewModelScope.launch {
            _authState.value = repository.checkAuthStatus()
        }
    }

    fun loadProfile() {
        _isLoadingProfile.value = true
        viewModelScope.launch {
            _profile.value = repository.fetchUserProfile()
            _listeningStats.value = repository.fetchListeningStats()
            _recentTracks.value = repository.fetchRecentTracks(10)
            _topArtists.value = repository.fetchTopArtists(10)
            _isLoadingProfile.value = false
        }
    }
}
