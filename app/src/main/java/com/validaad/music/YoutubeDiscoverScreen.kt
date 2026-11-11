package com.validaad.music

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private data class YouTubeVideo(val id: String, val title: String, val channel: String, val thumbnail: String)
private val YoutubeLime = Color(0xFFC8F169)
private val YoutubeMuted = Color(0xFF9AA39B)
private val YoutubePanel = Color(0xFF171B18)

@Composable
fun YoutubeDiscoverScreen(onOpenLibrary: () -> Unit, controller: androidx.media3.session.MediaController?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var videos by remember { mutableStateOf<List<YouTubeVideo>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Discover songs, artists and albums.") }

    fun search() {
        val clean = query.trim()
        if (clean.isEmpty()) return
        if (BuildConfig.YOUTUBE_API_KEY.isBlank()) {
            message = "YouTube discovery is not configured in this build."
            videos = emptyList()
            return
        }
        loading = true
        message = "Searching…"
        scope.launch {
            val result = withContext(Dispatchers.IO) { fetchYouTubeVideos(clean) }
            videos = result.first
            message = result.second
            loading = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        Text("Discover music", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Find music and open official YouTube playback.", color = YoutubeMuted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Song, artist or album") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { search() },
            enabled = query.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = YoutubeLime, contentColor = Color(0xFF090B0A))
        ) { Text(if (loading) "Searching…" else "Search music") }
        Spacer(Modifier.height(8.dp))
        Text(message, color = YoutubeMuted, fontSize = 11.sp)
        if (videos.isNotEmpty()) {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 8.dp)) {
                items(videos, key = { it.id }) { video ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.id}")))
                        }.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(model = video.thumbnail, contentDescription = "Video thumbnail", modifier = Modifier.size(108.dp, 62.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(video.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(video.channel, color = YoutubeMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Official YouTube playback", color = YoutubeLime, fontSize = 10.sp)
                        }
                    }
                    HorizontalDivider(color = YoutubePanel)
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
            Text("Open my offline music library", color = YoutubeLime)
        }
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
                return emptyList<YouTubeVideo>() to ("YouTube API error ($status): " + (detail ?: "Check API key, restrictions and quota."))
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
            found to if (found.isEmpty()) "No matching videos found." else "${found.size} results found."
        } finally { connection.disconnect() }
    } catch (error: Exception) {
        emptyList<YouTubeVideo>() to ("Could not search: " + (error.message ?: "Network error"))
    }
}
