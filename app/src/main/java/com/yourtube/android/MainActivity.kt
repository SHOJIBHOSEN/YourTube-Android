package com.yourtube.android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val BG = Color(0xFF090A0D)
private val CARD = Color(0xFF15171D)
private val RED = Color(0xFFFF334F)
private val MUTED = Color(0xFFA4A7B0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YourTubeApp() }
    }
}

private data class VideoItem(val title: String, val url: String)

@Composable
private fun YourTubeApp() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("yourtube", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("Home") }
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var playingTitle by remember { mutableStateOf("") }
    var playingUrl by remember { mutableStateOf("") }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var history by remember { mutableStateOf(prefs.getString("history", "").orEmpty().split("\n").filter(String::isNotBlank)) }
    var playlist by remember { mutableStateOf(prefs.getString("playlist", "").orEmpty().split("\n").filter(String::isNotBlank)) }
    var downloadIds by remember { mutableStateOf(prefs.getString("downloads", "").orEmpty().split(",").filter(String::isNotBlank)) }

    fun play(t: String, u: String) {
        playingTitle = t
        playingUrl = u
        val row = "$t|$u"
        history = (listOf(row) + history.filterNot { it == row }).take(100)
        prefs.edit().putString("history", history.joinToString("\n")).apply()
        page = "Player"
    }
    fun download() {
        try {
            val parsed = Uri.parse(url)
            require(parsed.scheme == "https" || parsed.scheme == "http") { "Enter a direct HTTP/HTTPS media URL." }
            val request = DownloadManager.Request(parsed)
                .setTitle(title.ifBlank { "YourTube media" })
                .setDescription("Authorized media download")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(!wifiOnly)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "YourTube_" + System.currentTimeMillis() + ".mp4")
            val id = (ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            downloadIds = (downloadIds + id.toString()).takeLast(100)
            prefs.edit().putString("downloads", downloadIds.joinToString(",")).apply()
            Toast.makeText(ctx, "Download queued", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(ctx, e.message ?: "Download failed", Toast.LENGTH_LONG).show()
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = CARD, primary = RED, onSurface = Color.White)) {
        Scaffold(containerColor = BG, bottomBar = {
            if (page != "Player" && page != "Settings" && page != "History" && page != "Playlist") {
                NavigationBar(containerColor = CARD) {
                    val tabs = listOf("Home", "Shorts", "Subscriptions", "Downloads", "You")
                    val icons = listOf(Icons.Default.Home, Icons.Default.Movie, Icons.Default.Subscriptions, Icons.Default.Download, Icons.Default.Person)
                    tabs.forEachIndexed { i, tab ->
                        NavigationBarItem(selected = page == tab, onClick = { page = tab },
                            icon = { Icon(icons[i], tab) }, label = { Text(tab, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = RED, selectedTextColor = RED,
                                indicatorColor = RED.copy(alpha = .15f), unselectedIconColor = MUTED, unselectedTextColor = MUTED))
                    }
                }
            }
        }) { inset ->
            Column(Modifier.fillMaxSize().background(BG).padding(inset)) {
                if (page != "Player") {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayCircleFilled, null, tint = RED, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("YourTube", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        IconButton(onClick = { page = "Settings" }) { Icon(Icons.Default.Settings, "Settings") }
                    }
                }
                when (page) {
                    "Home", "Shorts" -> {
                        Heading(if (page == "Shorts") "Short videos" else "Play a video")
                        Text("Use a direct media URL you are authorized to access. YouTube links require the official supported integration; this field is for direct MP4/HLS streams.", color = MUTED, modifier = Modifier.padding(horizontal = 16.dp))
                        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), label = { Text("Video title") }, singleLine = true)
                        OutlinedTextField(url, { url = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), label = { Text("Direct MP4/HLS URL") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                        Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { play(title.ifBlank { "Video" }, url) }, enabled = url.startsWith("http")) { Icon(Icons.Default.PlayArrow, null); Text("Play") }
                            OutlinedButton(onClick = ::download, enabled = url.startsWith("http")) { Icon(Icons.Default.Download, null); Text("Download") }
                        }
                        Heading("Recently watched")
                        VideoList(history.mapNotNull(::parseItem), onPlay = ::play, onSave = { item ->
                            playlist = (playlist + "${item.title}|${item.url}").distinct()
                            prefs.edit().putString("playlist", playlist.joinToString("\n")).apply()
                        })
                    }
                    "Subscriptions" -> { Heading("Subscriptions"); Empty("No channels connected", "An authorized account/API connection is required to load subscriptions.") }
                    "Downloads" -> { Heading("Downloads"); Text("Android Download Manager • direct authorized media links only", color = MUTED, modifier = Modifier.padding(horizontal = 16.dp)); Downloads(ctx, downloadIds) }
                    "You" -> {
                        Heading("Your library")
                        NavRow("Watch history", "Recently played videos") { page = "History" }
                        NavRow("Playlist", "Saved video links") { page = "Playlist" }
                        NavRow("Settings", "Playback and download preferences") { page = "Settings" }
                        NavRow("Google sign-in", "Requires OAuth client ID and consent configuration") {
                            Toast.makeText(ctx, "OAuth configuration is not set up yet.", Toast.LENGTH_LONG).show()
                        }
                    }
                    "History" -> { Heading("Watch history"); VideoList(history.mapNotNull(::parseItem), ::play, {}); TextButton(onClick = { history = emptyList(); prefs.edit().putString("history", "").apply() }) { Text("Clear history", color = RED) } }
                    "Playlist" -> { Heading("Playlist"); VideoList(playlist.mapNotNull(::parseItem), ::play, {}) }
                    "Settings" -> {
                        Heading("Settings")
                        ListItem(headlineContent = { Text("Wi-Fi only downloads") }, supportingContent = { Text("Avoid mobile data for downloads") }, trailingContent = { Switch(wifiOnly, { wifiOnly = it; prefs.edit().putBoolean("wifi_only", it).apply() }) }, colors = ListItemDefaults.colors(containerColor = BG))
                        Text("Playback quality depends on the original stream and device. Background playback, SponsorBlock and automatic intro skipping still need their dedicated service/API implementation.", color = MUTED, modifier = Modifier.padding(16.dp))
                    }
                    "Player" -> Player(playingTitle, playingUrl) { page = "Home" }
                }
            }
        }
    }
}

@Composable
private fun Player(title: String, url: String, back: () -> Unit) {
    val ctx = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(ctx).build().apply { setMediaItem(MediaItem.fromUri(url)); prepare(); playWhenReady = true }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        }
        AndroidView(factory = { PlayerView(it).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Text("Media3 playback • stream-supported quality", color = MUTED, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun Downloads(ctx: Context, ids: List<String>) {
    if (ids.isEmpty()) { Empty("No downloads", "Add a direct authorized media URL on Home."); return }
    val manager = remember { ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    LazyColumn {
        items(ids.reversed()) { id ->
            val state = try {
                manager.query(DownloadManager.Query().setFilterById(id.toLong())).use { c ->
                    if (!c.moveToFirst()) "Not found" else when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                        DownloadManager.STATUS_FAILED -> "Failed"
                        DownloadManager.STATUS_RUNNING -> "Downloading"
                        DownloadManager.STATUS_PAUSED -> "Paused"
                        else -> "Queued"
                    }
                }
            } catch (_: Exception) { "Unavailable" }
            ListItem(headlineContent = { Text("Download $id") }, supportingContent = { Text(state, color = MUTED) },
                leadingContent = { Icon(Icons.Default.Download, null, tint = RED) }, colors = ListItemDefaults.colors(containerColor = BG))
        }
    }
}

@Composable
private fun VideoList(items: List<VideoItem>, onPlay: (String, String) -> Unit, onSave: (VideoItem) -> Unit) {
    if (items.isEmpty()) { Empty("Nothing here yet", "Play a direct media URL to populate this section."); return }
    LazyColumn {
        items(items) { item ->
            Card(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = CARD)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayCircle, null, tint = RED, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text(item.url, color = MUTED, maxLines = 1, fontSize = 10.sp)
                    }
                    IconButton(onClick = { onPlay(item.title, item.url) }) { Icon(Icons.Default.PlayArrow, "Play") }
                    IconButton(onClick = { onSave(item) }) { Icon(Icons.Default.PlaylistAdd, "Save") }
                }
            }
        }
    }
}

@Composable
private fun NavRow(title: String, subtitle: String, click: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle, color = MUTED) },
        modifier = Modifier.clickable(onClick = click), colors = ListItemDefaults.colors(containerColor = BG))
}
@Composable
private fun Heading(value: String) { Text(value, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
@Composable
private fun Empty(title: String, message: String) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.VideoLibrary, null, tint = RED, modifier = Modifier.size(46.dp))
        Spacer(Modifier.height(10.dp)); Text(title, color = Color.White, fontWeight = FontWeight.Bold)
        Text(message, color = MUTED, fontSize = 12.sp)
    }
}
private fun parseItem(raw: String): VideoItem? {
    val parts = raw.split("|", limit = 2)
    return if (parts.size == 2 && parts[1].isNotBlank()) VideoItem(parts[0], parts[1]) else null
}
