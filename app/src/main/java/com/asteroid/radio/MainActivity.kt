package com.asteroid.radio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AsteroidRadioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF000000)
                ) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AsteroidRadioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00FF00),
            onPrimary = Color(0xFF000000),
            surface = Color(0xFF000000),
            onSurface = Color(0xFF00FF00),
            background = Color(0xFF000000),
            onBackground = Color(0xFF00FF00)
        ),
        content = content
    )
}

@Composable
fun AppContent(
    radioViewModel: RadioViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel()
) {
    var showLoginScreen by remember { mutableStateOf(false) }
    var showProfileScreen by remember { mutableStateOf(false) }
    val authState by authViewModel.authState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // User bar at top
        UserBar(
            authState = authState,
            onLoginClick = { showLoginScreen = true },
            onProfileClick = {
                showProfileScreen = true
                authViewModel.loadProfile()
            },
            onLogoutClick = {
                authViewModel.logout()
                showLoginScreen = false
                showProfileScreen = false
            }
        )

        if (showLoginScreen && !authState.loggedIn) {
            LoginScreen(
                authViewModel = authViewModel,
                onBack = { showLoginScreen = false }
            )
        } else if (showProfileScreen && authState.loggedIn) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBack = { showProfileScreen = false }
            )
        } else {
            if (authState.loggedIn) showLoginScreen = false
            RadioScreen(viewModel = radioViewModel)
        }
    }
}

@Composable
fun UserBar(
    authState: AuthState,
    onLoginClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF001100))
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (authState.loggedIn) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = Color(0xFF00AA00),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                TextButton(
                    onClick = onProfileClick,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00AA00))
                ) {
                    Text(authState.username ?: "user", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
                if (authState.isAdmin) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "[ADMIN]",
                        color = Color(0xFFFFAA00),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            TextButton(
                onClick = onLogoutClick,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF884400))
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("LOGOUT", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        } else {
            Text(
                text = "GUEST",
                color = Color(0xFF444444),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
            TextButton(
                onClick = onLoginClick,
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00AA00))
            ) {
                Text("LOGIN", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoggingIn by authViewModel.isLoggingIn.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "LOGIN",
            color = Color(0xFF00FF00),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 32.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF00FF00),
                unfocusedTextColor = Color(0xFF00AA00),
                focusedBorderColor = Color(0xFF00FF00),
                unfocusedBorderColor = Color(0xFF004400),
                focusedLabelColor = Color(0xFF00FF00),
                unfocusedLabelColor = Color(0xFF006600),
                cursorColor = Color(0xFF00FF00)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                TextButton(
                    onClick = { passwordVisible = !passwordVisible },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        if (passwordVisible) "HIDE" else "SHOW",
                        color = Color(0xFF006600),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF00FF00),
                unfocusedTextColor = Color(0xFF00AA00),
                focusedBorderColor = Color(0xFF00FF00),
                unfocusedBorderColor = Color(0xFF004400),
                focusedLabelColor = Color(0xFF00FF00),
                unfocusedLabelColor = Color(0xFF006600),
                cursorColor = Color(0xFF00FF00)
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp)
        )

        if (loginError != null) {
            Text(
                text = loginError!!,
                color = Color(0xFFFF4444),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Button(
            onClick = {
                authViewModel.login(username, password)
            },
            enabled = !isLoggingIn && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003300),
                contentColor = Color(0xFF00FF00)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            if (isLoggingIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF00FF00),
                    strokeWidth = 2.dp
                )
            } else {
                Text("LOGIN", fontFamily = FontFamily.Monospace, fontSize = 16.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF006600))
        ) {
            Text("BACK", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val profile by authViewModel.profile.collectAsState()
    val stats by authViewModel.listeningStats.collectAsState()
    val recentTracks by authViewModel.recentTracks.collectAsState()
    val topArtists by authViewModel.topArtists.collectAsState()
    val isLoading by authViewModel.isLoadingProfile.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "PROFILE",
            color = Color(0xFF00FF00),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp).padding(top = 32.dp),
                color = Color(0xFF00FF00),
                strokeWidth = 2.dp
            )
        } else if (profile != null) {
            // Profile info
            ProfileRow("USERNAME", profile!!.username)
            ProfileRow("EMAIL", profile!!.email)
            ProfileRow("ROLE", profile!!.role.uppercase())

            Spacer(modifier = Modifier.height(16.dp))

            // Listening stats
            Text(
                text = "LISTENING STATS",
                color = Color(0xFF00FF00),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            stats?.let {
                ProfileRow("TRACKS PLAYED", it.tracksPlayed.toString())
                ProfileRow("LISTEN TIME", formatListenTime(it.totalListenTime))
                ProfileRow("FAV GENRE", it.favoriteGenre)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Top artists
            if (topArtists.isNotEmpty()) {
                Text(
                    text = "TOP ARTISTS",
                    color = Color(0xFF00FF00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                topArtists.forEach { artist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = artist.name,
                            color = Color(0xFF00CC00),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${artist.playCount}x",
                            color = Color(0xFF006600),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Recent tracks
            if (recentTracks.isNotEmpty()) {
                Text(
                    text = "RECENT TRACKS",
                    color = Color(0xFF00FF00),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                recentTracks.forEach { track ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = track.title,
                            color = Color(0xFF00CC00),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (track.artist.isNotEmpty()) {
                            Text(
                                text = track.artist,
                                color = Color(0xFF006600),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = onBack,
            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF006600))
        ) {
            Text("BACK TO RADIO", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
        }
    }
}

@Composable
fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color(0xFF006600),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = Color(0xFF00CC00),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

fun formatListenTime(seconds: Long): String {
    if (seconds <= 0) return "0m"
    val hours = seconds / 3600
    val mins = (seconds % 3600) / 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
fun RadioScreen(viewModel: RadioViewModel = viewModel()) {
    val playerState by viewModel.playerState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()
    val channelName by viewModel.channelName.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF000000))
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "ASTEROID RADIO",
            color = Color(0xFF00FF00),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
        )
        Text(
            text = "The Station at the End of Time",
            color = Color(0xFF00CC00),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Channel selector
        ChannelSelector(
            selectedChannel = selectedChannel,
            onSelect = { viewModel.selectChannel(it) }
        )

        if (selectedChannel == Channel.CURATED) {
            Spacer(modifier = Modifier.height(8.dp))

            // Quality selector (curated only — shuffle has fixed quality)
            QualitySelector(
                selectedQuality = selectedQuality,
                onSelect = { viewModel.selectQuality(it) }
            )
        } else {
            Text(
                text = "MP3 96kbps (fixed)",
                color = Color(0xFF006600),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val dotColor = when (playerState) {
                PlayerState.PLAYING -> Color(0xFF00FF00)
                PlayerState.CONNECTING -> Color(0xFFFFCC00)
                PlayerState.ERROR -> Color(0xFFFF3333)
                PlayerState.STOPPED -> Color(0xFF444444)
            }
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusMessage,
                color = Color(0xFF00FF00),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Channel name (curated playlist name or Shuffle)
        val displayName = when (selectedChannel) {
            Channel.CURATED -> channelName.ifEmpty { "Curated" }
            Channel.SHUFFLE -> "Shuffle"
        }
        if (displayName.isNotEmpty()) {
            Text(
                text = displayName,
                color = Color(0xFF00AA00),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        // Now playing from ICY metadata
        if (nowPlaying.isNotEmpty()) {
            Text(
                text = nowPlaying,
                color = Color(0xFF00CC00),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Play/Stop button
        Button(
            onClick = { viewModel.togglePlayback() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = when (playerState) {
                    PlayerState.PLAYING, PlayerState.CONNECTING -> Color(0xFF003300)
                    else -> Color(0xFF002200)
                },
                contentColor = Color(0xFF00FF00)
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            val buttonText = if (playerState == PlayerState.PLAYING || playerState == PlayerState.CONNECTING) {
                "STOP"
            } else {
                "PLAY"
            }
            val icon = if (playerState == PlayerState.PLAYING || playerState == PlayerState.CONNECTING) {
                Icons.Filled.Stop
            } else {
                Icons.Filled.PlayArrow
            }
            Icon(icon, contentDescription = buttonText, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = buttonText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sleep timer
        SleepTimerSection(
            sleepTimerRemaining = viewModel.sleepTimerRemaining.collectAsState().value,
            onSetTimer = { viewModel.setSleepTimer(it) },
            onCancelTimer = { viewModel.cancelSleepTimer() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Now Playing
        if (tracks.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "NOW PLAYING",
                    color = Color(0xFF00AA00),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = tracks[0].title.ifEmpty { tracks[0].song },
                    color = Color(0xFF00FF00),
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                if (tracks[0].artist.isNotEmpty()) {
                    Text(
                        text = tracks[0].artist,
                        color = Color(0xFF00CC00),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                thickness = 1.dp,
                color = Color(0xFF003300)
            )
        }

        // Recently Played header
        Text(
            text = "RECENTLY PLAYED",
            color = Color(0xFF00AA00),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        // Track list
        if (tracks.isEmpty() && isLoading) {
            Text(
                text = "Loading...",
                color = Color(0xFF00AA00),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(16.dp)
            )
        } else if (tracks.isEmpty()) {
            Text(
                text = "No tracks available",
                color = Color(0xFF006600),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            tracks.take(10).forEachIndexed { index, track ->
                TrackRow(track = track, index = index)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun TrackRow(track: Track, index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = track.title.ifEmpty { track.song },
            color = Color(0xFF00DD00),
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace
        )
        if (track.artist.isNotEmpty()) {
            Text(
                text = track.artist,
                color = Color(0xFF008800),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun SleepTimerSection(
    sleepTimerRemaining: Long,
    onSetTimer: (Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val timerActive = sleepTimerRemaining > 0
    val minutes = sleepTimerRemaining / 60_000
    val seconds = (sleepTimerRemaining % 60_000) / 1000

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (timerActive) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLEEP: %02d:%02d".format(minutes, seconds),
                    color = Color(0xFFFFAA00),
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                TextButton(
                    onClick = onCancelTimer,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFFF6644)
                    )
                ) {
                    Text("CANCEL", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SLEEP:",
                    color = Color(0xFF006600),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                listOf(15, 30, 45, 60).forEach { mins ->
                    TextButton(
                        onClick = { onSetTimer(mins) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFF00AA00)
                        )
                    ) {
                        Text("${mins}m", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelSelector(
    selectedChannel: Channel,
    onSelect: (Channel) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "CHANNEL:",
            color = Color(0xFF006600),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Channel.entries.forEach { channel ->
            val isSelected = channel == selectedChannel
            TextButton(
                onClick = { onSelect(channel) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSelected) Color(0xFF00FF00) else Color(0xFF006600),
                    containerColor = if (isSelected) Color(0xFF003300) else Color(0xFF001100)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(channel.label, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QualitySelector(
    selectedQuality: StreamQuality,
    onSelect: (StreamQuality) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "QUALITY:",
            color = Color(0xFF006600),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        StreamQuality.entries.forEach { quality ->
            val isSelected = quality == selectedQuality
            TextButton(
                onClick = { onSelect(quality) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (isSelected) Color(0xFF00FF00) else Color(0xFF006600),
                    containerColor = if (isSelected) Color(0xFF003300) else Color(0xFF001100)
                ),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(quality.label, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
            }
        }
    }
}
