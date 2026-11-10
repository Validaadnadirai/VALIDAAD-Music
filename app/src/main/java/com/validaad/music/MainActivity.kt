package com.validaad.music

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private val Ink = Color(0xFF090B0A)
private val Panel = Color(0xFF171B18)
private val Lime = Color(0xFFC8F169)
private val Muted = Color(0xFF9AA39B)

data class DeviceSong(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: Uri,
    val duration: Long
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ValidaadMusicApp() }
    }
}

@Composable
private fun ValidaadMusicApp() {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var songs by remember { mutableStateOf<List<DeviceSong>>(emptyList()) }
    var permissionGranted by remember {
        mutableStateOf(hasAudioPermission(context))
    }
    var scanMessage by remember { mutableStateOf("Allow access to find music on this device.") }
    var controller by remember { mutableStateOf<MediaController?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var libraryFilter by remember { mutableStateOf("All") }
    var favoriteIds by remember { mutableStateOf(loadFavoriteIds(context)) }
    var recentIds by remember { mutableStateOf(loadRecentIds(context)) }
    var playlists by remember { mutableStateOf(loadPlaylists(context)) }
    var playlistSongs by remember { mutableStateOf(loadPlaylistSongs(context)) }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var showAddSongsDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var playlistName by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        scanMessage = if (granted) "Scanning your music…" else "Music permission was not granted."
    }

    val sessionToken = remember {
        SessionToken(context, android.content.ComponentName(context, MusicPlaybackService::class.java))
    }
    val controllerFuture = remember {
        MediaController.Builder(context, sessionToken).buildAsync()
    }
    DisposableEffect(controllerFuture) {
        controllerFuture.addListener({
            try { controller = controllerFuture.get() } catch (_: Exception) { scanMessage = "Player could not start." }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            MediaController.releaseFuture(controllerFuture)
        }
    }

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            scanMessage = "Scanning your music…"
            songs = withContext(Dispatchers.IO) { loadDeviceSongs(context) }
            scanMessage = if (songs.isEmpty()) "No audio files found. Add music to your phone and scan again." else "${songs.size} songs found on this device."
        }
    }

    LaunchedEffect(controller) {
        while (true) {
            refresh++
            delay(700)
        }
    }

    DisposableEffect(controller) {
        val activeController = controller
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { refresh++ }
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { refresh++ }
            override fun onPlaybackStateChanged(playbackState: Int) { refresh++ }
        }
        activeController?.addListener(listener)
        onDispose { activeController?.removeListener(listener) }
    }

    MaterialTheme {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    listOf("Home", "Search", "Library", "Settings").forEachIndexed { index, label ->
                        val icon = when (index) {
                            0 -> Icons.Default.Home
                            1 -> Icons.Default.Search
                            2 -> Icons.Default.LibraryMusic
                            else -> Icons.Default.Settings
                        }
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Lime, indicatorColor = Color(0xFF303A2B))
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().background(Ink).padding(padding).padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("VALIDAAD", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("MUSIC", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                    Icon(Icons.Default.Album, contentDescription = "Vinyl", tint = Lime, modifier = Modifier.size(34.dp))
                }
                Spacer(Modifier.height(20.dp))
                when (selectedTab) {
                    0 -> HomeScreen(
                        songCount = songs.size,
                        message = scanMessage,
                        onScan = {
                            if (permissionGranted) {
                                songs = emptyList()
                                scanMessage = "Scanning your music…"
                            } else {
                                permissionLauncher.launch(audioPermission())
                            }
                        },
                        onOpenLibrary = { selectedTab = 2 },
                        controller = controller,
                        refresh = refresh
                    )
                    1 -> {
                        Text("Find your music", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Text("Your device songs are available in Library.", color = Muted)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { selectedTab = 2 }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Open Library") }
                    }
                    2 -> LibraryScreen(
                        songs = songs, message = scanMessage,
                        query = searchQuery, onQueryChange = { searchQuery = it },
                        filter = libraryFilter, onFilterChange = { libraryFilter = it },
                        favoriteIds = favoriteIds, recentIds = recentIds, playlists = playlists,
                        playlistSongs = playlistSongs, selectedPlaylist = selectedPlaylist,
                        onSelectPlaylist = { selectedPlaylist = it },
                        onAddSongs = { showAddSongsDialog = true },
                        onRemovePlaylistSong = { id -> selectedPlaylist?.let { name -> playlistSongs = togglePlaylistSong(context, playlistSongs, name, id) } },
                        onCreatePlaylist = { showPlaylistDialog = true },
                        onToggleFavorite = { song -> favoriteIds = toggleFavorite(context, favoriteIds, song.id) },
                        onRequestPermission = { permissionLauncher.launch(audioPermission()) },
                        onPlay = { index ->
                            recentIds = saveRecent(context, recentIds, songs[index].id)
                            playSongList(controller, songs, index)
                        },
                        controller = controller, refresh = refresh
                    )
                    else -> {
                        Text("Settings", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("VALIDAAD MUSIC • Offline player foundation", color = Muted)
                        Spacer(Modifier.height(16.dp))
                        Text("Music access: ${if (permissionGranted) "Allowed" else "Not allowed"}", color = Lime)
                        Text("Your local audio stays on your device.", color = Muted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
    if (showAddSongsDialog && selectedPlaylist != null) {
        val target = selectedPlaylist!!
        AlertDialog(
            onDismissRequest = { showAddSongsDialog = false },
            title = { Text("Add songs to $target") },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(songs, key = { _, song -> song.id }) { _, song ->
                        val included = song.id in (playlistSongs[target] ?: emptySet())
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, color = Muted, fontSize = 11.sp)
                            }
                            TextButton(onClick = {
                                playlistSongs = togglePlaylistSong(context, playlistSongs, target, song.id)
                            }) { Text(if (included) "Remove" else "Add", color = Lime) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddSongsDialog = false }) { Text("Done") } }
        )
    }
    if (showPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Create playlist") },
            text = { OutlinedTextField(value = playlistName, onValueChange = { playlistName = it }, label = { Text("Playlist name") }, singleLine = true) },
            confirmButton = { TextButton(onClick = {
                val clean = playlistName.trim()
                if (clean.isNotEmpty() && playlists.none { it.equals(clean, true) }) playlists = savePlaylist(context, playlists, clean)
                playlistName = ""; showPlaylistDialog = false
            }) { Text("Create") } },
            dismissButton = { TextButton(onClick = { showPlaylistDialog = false }) { Text("Cancel") } }
        )
    }
}

private fun hasAudioPermission(context: android.content.Context): Boolean =
    ContextCompat.checkSelfPermission(context, audioPermission()) == PackageManager.PERMISSION_GRANTED

private fun audioPermission(): String =
    if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE

private fun loadDeviceSongs(context: android.content.Context): List<DeviceSong> {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.IS_MUSIC
    )
    val result = mutableListOf<DeviceSong>()
    context.contentResolver.query(
        collection, projection, "${MediaStore.Audio.Media.IS_MUSIC} != 0", null,
        "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idCol)
            val title = cursor.getString(titleCol) ?: "Unknown title"
            val artist = cursor.getString(artistCol) ?: "Unknown artist"
            val duration = cursor.getLong(durationCol)
            result += DeviceSong(id, title, artist, ContentUris.withAppendedId(collection, id), duration)
        }
    }
    return result
}

private fun playSongList(controller: MediaController?, songs: List<DeviceSong>, startIndex: Int) {
    if (controller == null || songs.isEmpty()) return
    val mediaItems = songs.map { song ->
        MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle(song.title).setArtist(song.artist).build()
            )
            .build()
    }
    controller.setMediaItems(mediaItems, startIndex, 0L)
    controller.prepare()
    controller.play()
}

@Composable
private fun HomeScreen(
    songCount: Int,
    message: String,
    onScan: () -> Unit,
    onOpenLibrary: () -> Unit,
    controller: MediaController?,
    refresh: Int
) {
    Text("YOUR MUSIC. YOUR WORLD.", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(7.dp))
    Text("Feel every beat.", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(18.dp))
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("OFFLINE MUSIC", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LibraryMusic, tint = Lime, modifier = Modifier.size(38.dp), contentDescription = null)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("$songCount songs found", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                    Text(message, color = Muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick = onScan, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (songCount > 0) "Scan again" else "Scan phone music")
            }
            TextButton(onClick = onOpenLibrary, modifier = Modifier.align(Alignment.End)) { Text("Open library", color = Lime) }
        }
    }
    Spacer(Modifier.height(18.dp))
    NowPlayingCard(controller, refresh)
}

@Composable
private fun LibraryScreen(
    songs: List<DeviceSong>, message: String,
    query: String, onQueryChange: (String) -> Unit,
    filter: String, onFilterChange: (String) -> Unit,
    favoriteIds: Set<Long>, recentIds: List<Long>, playlists: List<String>,
    playlistSongs: Map<String, Set<Long>>, selectedPlaylist: String?,
    onSelectPlaylist: (String?) -> Unit, onAddSongs: () -> Unit,
    onRemovePlaylistSong: (Long) -> Unit,
    onCreatePlaylist: () -> Unit, onToggleFavorite: (DeviceSong) -> Unit,
    onRequestPermission: () -> Unit, onPlay: (Int) -> Unit,
    controller: MediaController?, refresh: Int
) {
    Text("Your library", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
    Text(message, color = Muted, fontSize = 12.sp)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Search songs or artists") }, leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("All", "Favorites", "Recent").forEach { item -> FilterChip(selected = filter == item, onClick = { onFilterChange(item) }, label = { Text(item) }) }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("PLAYLISTS (" + playlists.size + ")", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onCreatePlaylist) { Text("+ Create", color = Lime) }
    }
    if (playlists.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            playlists.forEach { name ->
                TextButton(onClick = { onSelectPlaylist(if (selectedPlaylist == name) null else name) }) {
                    Text("${if (selectedPlaylist == name) "▾" else "▸"} $name  (${playlistSongs[name]?.size ?: 0})", color = if (selectedPlaylist == name) Lime else Color.White)
                }
            }
        }
    }
    if (selectedPlaylist != null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(selectedPlaylist, color = Lime, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onAddSongs) { Text("+ Add songs", color = Lime) }
        }
        val memberSongs = songs.filter { it.id in (playlistSongs[selectedPlaylist] ?: emptySet()) }
        if (memberSongs.isEmpty()) Text("No songs in this playlist yet.", color = Muted, fontSize = 12.sp)
        memberSongs.forEach { member ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(member.title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { onRemovePlaylistSong(member.id) }) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from playlist", tint = Muted)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    if (songs.isEmpty()) {
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.MusicNote, tint = Lime, modifier = Modifier.size(38.dp), contentDescription = null)
                Text("No songs to show", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Allow music access or scan your phone.", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Button(onClick = onRequestPermission, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("Allow / Scan") }
            }
        }
    } else {
        val visibleSongs = songs.withIndex().filter { item ->
            val song = item.value
            (query.isBlank() || song.title.contains(query, true) || song.artist.contains(query, true)) &&
                when (filter) { "Favorites" -> song.id in favoriteIds; "Recent" -> song.id in recentIds; else -> true }
        }.sortedBy { item -> if (filter == "Recent") recentIds.indexOf(item.value.id).let { if (it < 0) Int.MAX_VALUE else it } else item.index }
        if (visibleSongs.isEmpty()) Text("No matching songs. Try another search or filter.", color = Muted)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(bottom = 12.dp)) {
            itemsIndexed(visibleSongs, key = { _, item -> item.value.id }) { _, item ->
                val song = item.value
                Row(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).clickable { onPlay(item.index) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MusicNote, tint = Lime, contentDescription = null, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(song.title, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(song.artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { onToggleFavorite(song) }) {
                        Icon(if (song.id in favoriteIds) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Favorite", tint = if (song.id in favoriteIds) Lime else Muted)
                    }
                    Text(formatTime(song.duration), color = Muted, fontSize = 11.sp)
                }
            }
        }
    }
    NowPlayingCard(controller, refresh)
}

@Composable
private fun NowPlayingCard(controller: MediaController?, refresh: Int) {
    val title = controller?.currentMediaItem?.mediaMetadata?.title?.toString() ?: "Nothing playing"
    val artist = controller?.currentMediaItem?.mediaMetadata?.artist?.toString() ?: "Choose a song from Library"
    val duration = (controller?.duration ?: 0L).coerceAtLeast(0L)
    val position = (controller?.currentPosition ?: 0L).coerceAtLeast(0L)
    val isPlaying = controller?.isPlaying == true
    Surface(color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text("NOW PLAYING", color = Lime, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Spacer(Modifier.height(5.dp))
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(artist, color = Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Slider(
                value = if (duration > 0) position.coerceAtMost(duration).toFloat() else 0f,
                onValueChange = { controller?.seekTo(it.toLong()) },
                valueRange = 0f..(duration.coerceAtLeast(1L).toFloat()),
                enabled = duration > 0,
                colors = SliderDefaults.colors(thumbColor = Lime, activeTrackColor = Lime)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(position), color = Muted, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { controller?.seekToPreviousMediaItem() }) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", tint = Color.White) }
                    IconButton(onClick = {
                        if (isPlaying) controller?.pause() else controller?.play()
                    }) {
                        Icon(if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled, contentDescription = "Play pause", tint = Lime, modifier = Modifier.size(42.dp))
                    }
                    IconButton(onClick = { controller?.seekToNextMediaItem() }) { Icon(Icons.Default.SkipNext, contentDescription = "Next", tint = Color.White) }
                }
                Text(formatTime(duration), color = Muted, fontSize = 10.sp)
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", total / 60, total % 60)
}
