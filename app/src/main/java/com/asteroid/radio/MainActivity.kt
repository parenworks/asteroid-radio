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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
                    RadioScreen()
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
fun RadioScreen(viewModel: RadioViewModel = viewModel()) {
    val playerState by viewModel.playerState.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val tracks by viewModel.tracks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedChannel by viewModel.selectedChannel.collectAsState()
    val selectedQuality by viewModel.selectedQuality.collectAsState()
    val nowPlaying by viewModel.nowPlaying.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
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
