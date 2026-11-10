package com.validaad.music

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class YouTubeVideo(val id: String, val title: String, val channel: String, val thumbnail: String)
private val YoutubeInk = Color(0xFF090B0A)
private val YoutubePanel = Color(0xFF171B18)
private val YoutubeLime = Color(0xFFC8F169)
private val YoutubeMuted = Color(0xFF9AA39B)

@Composable
fun YoutubeDiscoverScreen(onOpenLibrary: () -> Unit, controller: MediaController?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Search songs, artists or albums on YouTube.") }
    var streamUrl by remember { mutableStateOf("") }
    var streamTitle by remember { mutableStateOf("") }
    var streamMessage by remember { mutableStateOf("Use a direct HTTPS audio stream that you own or are authorized to play.") }
    var refresh by remember { mutableIntStateOf(0) }

    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { refresh++ }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { refresh++ }
            override fun onPlaybackStateChanged(playbackState: Int) { refresh++ }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                streamMessage = "This audio source could not be played. Check the URL, format and provider permissions."
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }

    fun playAuthorizedStream() {
        val cleanUrl = streamUrl.trim()
        val parsed = runCatching { Uri.parse(cleanUrl) }.getOrNull()
        val host = parsed?.host?.lowercase().orEmpty()
        val blockedHosts = listOf("youtube.com", "youtu.be", "googlevideo.com", "youtube-nocookie.com")
        val isBlockedHost = blockedHosts.any { host == it || host.endsWith(".$it") }
        if (parsed == null || parsed.scheme != "https" || host.isBlank() || isBlockedHost) {
            streamMessage = "Enter a valid HTTPS direct-audio URL from an authorized provider. YouTube links are not supported as audio streams."
            return
        }
        val activeController = controller
        if (activeController == null) {
            streamMessage = "Player is still starting. Please try again in a moment."
            return
        }
        val title = streamTitle.trim().ifBlank { host }
        val item = MediaItem.Builder()
            .setUri(parsed)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist("Online audio").build())
            .build()
        activeController.setMediaItem(item)
        activeController.prepare()
        activeController.play()
        streamMessage = "Starting authorized online audio…"
    }

    fun search() {
        val clean = query.trim()
        if (clean.isEmpty()) return
        if (BuildConfig.YOUTUBE_API_KEY.isBlank()) {
            message = "YouTube API key is not configured in this build. Check the GitHub Actions secret YOUTUBE_API_KEY."
            videos = emptyList()
            return
        }
        loading = true
        message = "Searching YouTube…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchYouTubeVideos(clean) }
            videos = result.first
            message = result.second
            loading = false
        }
    }

    val currentTitle = controller?.currentMediaItem?.mediaMetadata?.title?.toString().orEmpty()
    val currentArtist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString().orEmpty()
    val isPlaying = controller?.isPlaying == true
    val duration = (controller?.duration ?: 0L).coerceAtLeast(0L)
    val position = (controller?.currentPosition ?: 0L).coerceAtLeast(0L)

    Column(Modifier.fillMaxSize()) {
        Text("Online music", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Play authorized audio streams inside VALIDAAD MUSIC.", color = YoutubeMuted, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        Surface(color = YoutubePanel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("PLAY ONLINE AUDIO", color = YoutubeLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = streamTitle, onValueChange = { streamTitle = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Song title (optional)") }
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = streamUrl, onValueChange = { streamUrl = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("https://provider.com/audio/song.mp3") }
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { playAuthorizedStream() },
                    enabled = streamUrl.isNotBlank() && controller != null,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = YoutubeLime, contentColor = YoutubeInk)
                ) { Text("Play inside app") }
                Text(streamMessage, color = YoutubeMuted, fontSize = 11.sp)
                if (currentTitle.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("NOW PLAYING", color = YoutubeLime, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(currentTitle, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(currentArtist, color = YoutubeMuted, fontSize = 11.sp)
                    Slider(
                        value = if (duration > 0) position.coerceAtMost(duration).toFloat() else 0f,
                        onValueChange = { controller?.seekTo(it.toLong()) },
                        valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                        enabled = duration > 0,
                        colors = SliderDefaults.colors(thumbColor = YoutubeLime, activeTrackColor = YoutubeLime)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        IconButton(onClick = { if (isPlaying) controller?.pause() else controller?.play() }) {
                            Icon(if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                                contentDescription = "Play or pause", tint = YoutubeLime, modifier = Modifier.size(42.dp))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("YOUTUBE DISCOVERY", color = YoutubeLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Text("Find songs here; YouTube playback remains in its official app.", color = YoutubeMuted, fontSize = 11.sp)
        OutlinedTextField(
            value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Song, artist or album") }, singleLine = true
        )
        Spacer(Modifier.height(6.dp))
        Button(onClick = { search() }, enabled = query.isNotBlank() && !loading, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = YoutubeLime, contentColor = YoutubeInk)) {
            Text(if (loading) "Searching…" else "Search YouTube")
        }
        Spacer(Modifier.height(6.dp))
        Text(message, color = YoutubeMuted, fontSize = 11.sp)
        if (videos.isNotEmpty()) {
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(videos, key = { it.id }) { video ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}"))
                            context.startActivity(intent)
                        }.padding(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = video.thumbnail, contentDescription = "Video thumbnail", modifier = Modifier.size(width = 100.dp, height = 58.dp))
                        Spacer(Modifier.width(9.dp))
                        Column(Modifier.weight(1f)) {
                            Text(video.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(video.channel, color = YoutubeMuted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Open official YouTube", color = YoutubeLime, fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = YoutubePanel)
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) { Text("Open local music library", color = YoutubeLime) }
    }
}

private suspend fun fetchYouTubeVideos(query: String): Pair<List<YouTubeVideo>, String> {
    return try {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val apiKey = URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY, "UTF-8")
        val url = URL("https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&maxResults=15&q=$encoded&key=$apiKey")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 15000
        }
        try {
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream.bufferedReader().use { it.readText() }
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(body).getJSONObject("error").getString("message") }.getOrNull()
                return emptyList<YouTubeVideo>() to ("YouTube API error ($status): " + (detail ?: "Please check API key, restrictions and quota."))
            }
            val items = JSONObject(body).optJSONArray("items") ?: return emptyList<YouTubeVideo>() to "No results found."
            val found = buildList {
                for (index in 0 until items.length()) {
                    val item = items.optJSONObject(index) ?: continue
                    val id = item.optJSONObject("id")?.optString("videoId").orEmpty()
                    val snippet = item.optJSONObject("snippet") ?: continue
                    if (id.isBlank()) continue
                    val thumbs = snippet.optJSONObject("thumbnails")
                    val thumb = thumbs?.optJSONObject("medium")?.optString("url")
                        ?: thumbs?.optJSONObject("default")?.optString("url").orEmpty()
                    add(YouTubeVideo(id, snippet.optString("title", "Untitled"), snippet.optString("channelTitle", "YouTube"), thumb))
                }
            }
            found to if (found.isEmpty()) "No matching videos found." else "${found.size} results. Tap a result for official YouTube playback."
        } finally { connection.disconnect() }
    } catch (error: Exception) {
        emptyList<YouTubeVideo>() to ("Could not search YouTube: " + (error.message ?: "Unknown network error"))
    }
}
