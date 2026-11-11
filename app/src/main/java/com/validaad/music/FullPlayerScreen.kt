package com.validaad.music

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import androidx.media3.session.MediaController

@Composable
fun FullPlayerScreen(controller: MediaController?, refresh: Int, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
    val artist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Choose a song from your library"
    val duration = (controller?.duration ?: 0L).coerceAtLeast(0L)
    val position = (controller?.currentPosition ?: 0L).coerceAtLeast(0L)
    val playing = controller?.isPlaying == true
    val transition = rememberInfiniteTransition(label = "vinyl")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "vinylRotation"
    )
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF090B0A)).padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Close player", tint = Color.White) }
            Spacer(Modifier.weight(1f))
            Text("NOW PLAYING", color = Color(0xFFC8F169), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { controller?.shuffleModeEnabled = !(controller?.shuffleModeEnabled ?: false) }) {
                Icon(Icons.Default.Shuffle, contentDescription = "Shuffle", tint = if (controller?.shuffleModeEnabled == true) Color(0xFFC8F169) else Color.LightGray)
            }
        }
        Spacer(Modifier.weight(0.6f))
        Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize().rotate(if (playing) rotation else 0f)) {
                drawCircle(Color(0xFF222722))
                drawCircle(Color(0xFF343A35), radius = size.minDimension * 0.46f)
                drawCircle(Color(0xFF171B18), radius = size.minDimension * 0.42f)
                drawCircle(Color(0xFF343A35), radius = size.minDimension * 0.34f)
                drawCircle(Color(0xFF171B18), radius = size.minDimension * 0.30f)
                drawCircle(Color(0xFFC8F169), radius = size.minDimension * 0.13f)
                drawCircle(Color(0xFF090B0A), radius = size.minDimension * 0.035f)
            }
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color(0xFF090B0A), modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.weight(0.6f))
        Column(Modifier.fillMaxWidth()) {
            Text("YOUR MUSIC • YOUR MOMENT", color = Color(0xFFC8F169), fontSize = 10.sp, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(8.dp))
            Text(title, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Color(0xFF9AA39B), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(18.dp))
            Slider(
                value = if (duration > 0) position.coerceAtMost(duration).toFloat() else 0f,
                onValueChange = { controller?.seekTo(it.toLong()) },
                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                enabled = duration > 0,
                colors = SliderDefaults.colors(thumbColor = Color(0xFFC8F169), activeTrackColor = Color(0xFFC8F169), inactiveTrackColor = Color(0xFF343A35))
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(position), color = Color(0xFF9AA39B), fontSize = 11.sp)
                Text(formatTime(duration), color = Color(0xFF9AA39B), fontSize = 11.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { controller?.seekToPreviousMediaItem() }) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(34.dp))
                }
                FilledIconButton(
                    onClick = { if (playing) controller?.pause() else controller?.play() },
                    modifier = Modifier.size(70.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFC8F169), contentColor = Color(0xFF090B0A))
                ) {
                    Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play or pause", modifier = Modifier.size(38.dp))
                }
                IconButton(onClick = { controller?.seekToNextMediaItem() }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(34.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    val mode = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
                    controller?.repeatMode = when (mode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                }) {
                    val mode = controller?.repeatMode ?: Player.REPEAT_MODE_OFF
                    Icon(Icons.Default.Repeat, contentDescription = "Repeat", tint = if (mode == Player.REPEAT_MODE_OFF) Color.LightGray else Color(0xFFC8F169))
                }
                Text(
                    when (controller?.repeatMode ?: Player.REPEAT_MODE_OFF) {
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        else -> "Repeat off"
                    },
                    color = Color(0xFF9AA39B),
                    fontSize = 12.sp
                )
                Spacer(Modifier.width(14.dp))
                Icon(Icons.Default.QueueMusic, contentDescription = "Queue", tint = Color(0xFF9AA39B))
                Spacer(Modifier.width(5.dp))
                Text("Queue: ${controller?.mediaItemCount ?: 0}", color = Color(0xFF9AA39B), fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
    }
}
