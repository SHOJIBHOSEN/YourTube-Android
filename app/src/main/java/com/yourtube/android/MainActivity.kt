package com.yourtube.android

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.yourtube.android.core.media.MediaUrlValidator

private val Bg = Color(0xFF0F0F0F)
private val SurfaceDark = Color(0xFF1D1D1D)
private val SurfaceLight = Color(0xFF292929)
private val Accent = Color(0xFFFF334F)
private val Subtle = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YourTubeApp() }
    }
}

private data class Video(val title: String, val creator: String, val url: String, val duration: String, val category: String, val description: String)
private data class SavedVideo(val title: String, val url: String)

private val sampleVideos = listOf(
    Video("Big Buck Bunny", "Blender Foundation", "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4", "9:56", "Animation", "Open movie sample • direct MP4 playback"),
    Video("Elephant Dream", "Blender Foundation", "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4", "10:53", "Animation", "Open movie sample • direct MP4 playback"),
    Video("For Bigger Joyrides", "Google Demo", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4", "0:15", "Technology", "Short sample clip • direct MP4 playback"),
    Video("For Bigger Escapes", "Google Demo", "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4", "0:15", "Travel", "Short sample clip • direct MP4 playback"),
    Video("Sintel Trailer", "Blender Foundation", "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4", "2:00", "Animation", "Open movie sample • direct MP4 playback"),
    Video("Tears of Steel", "Blender Foundation", "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4", "12:14", "Sci-Fi", "Open movie sample • direct MP4 playback")
)

@Composable
private fun YourTubeApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("yourtube", Context.MODE_PRIVATE) }
    var tab by remember { mutableStateOf("Home") }
    var page by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var inputTitle by remember { mutableStateOf("") }
    var inputUrl by remember { mutableStateOf("") }
    var current by remember { mutableStateOf<Video?>(null) }
    var history by remember { mutableStateOf(prefs.getString("history", "").orEmpty().lines().filter { it.contains("|") }) }
    var playlist by remember { mutableStateOf(prefs.getString("playlist", "").orEmpty().lines().filter { it.contains("|") }) }
    var wifiOnly by remember { mutableStateOf(prefs.getBoolean("wifi_only", true)) }
    var speed by remember { mutableFloatStateOf(1f) }
    var downloads by remember { mutableStateOf(prefs.getString("downloads", "").orEmpty().split(",").filter { it.isNotBlank() }) }

    fun play(video: Video) {
        if (!MediaUrlValidator.isSupported(video.url)) {
            Toast.makeText(context, "Unsupported or invalid media URL", Toast.LENGTH_LONG).show()
            return
        }
        current = video
        page = "Player"
        val row = video.title + "|" + video.url
        history = (listOf(row) + history.filterNot { it == row }).take(100)
        prefs.edit().putString("history", history.joinToString("\n")).apply()
    }
    fun playSaved(row: String) {
        val parts = row.split("|", limit = 2)
        if (parts.size == 2) play(Video(parts[0], "Your library", parts[1], "", "Saved", "Saved video"))
    }
    fun addToPlaylist(video: Video) {
        val row = video.title + "|" + video.url
        playlist = (playlist + row).distinct().take(200)
        prefs.edit().putString("playlist", playlist.joinToString("\n")).apply()
        Toast.makeText(context, "Added to playlist", Toast.LENGTH_SHORT).show()
    }
    fun download(video: Video) {
        try {
            val uri = Uri.parse(video.url)
            val ext = uri.lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase()
            require(ext in setOf("mp4", "m4v", "webm")) { "This download option supports direct MP4/M4V/WebM files." }
            val request = DownloadManager.Request(uri).setTitle(video.title)
                .setDescription("YourTube • authorized direct media")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverRoaming(false).also { request -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) request.setAllowedOverMetered(!wifiOnly) }
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "YourTube_${System.currentTimeMillis()}.$ext")
            val id = (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            downloads = (downloads + id.toString()).takeLast(100)
            prefs.edit().putString("downloads", downloads.joinToString(",")).apply()
            Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(context, e.message ?: "Download failed", Toast.LENGTH_LONG).show() }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = SurfaceDark, onBackground = Color.White, onSurface = Color.White)) {
        Scaffold(
            containerColor = Bg,
            bottomBar = {
                if (page != "Player" && page != "Settings" && page != "History" && page != "Playlist" && page != "Search") {
                    NavigationBar(containerColor = SurfaceDark, tonalElevation = 0.dp) {
                        val tabs = listOf("Home", "Shorts", "Subscriptions", "You")
                        val icons = listOf(Icons.Default.Home, Icons.Default.Movie, Icons.Default.Subscriptions, Icons.Default.AccountCircle)
                        tabs.forEachIndexed { i, name ->
                            NavigationBarItem(selected = tab == name, onClick = { tab = name; page = name },
                                icon = { Icon(icons[i], null) }, label = { Text(name, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(selectedIconColor = Accent, selectedTextColor = Color.White,
                                    indicatorColor = Accent.copy(alpha = .16f), unselectedIconColor = Subtle, unselectedTextColor = Subtle))
                        }
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).background(Bg)) {
                if (page != "Player") {
                    TopBar(onSearch = { page = "Search" }, onSettings = { page = "Settings" })
                }
                when (page) {
                    "Home" -> HomeFeed(query, { query = it; page = "Search" }, category, { category = it }, ::play, ::addToPlaylist, ::download,
                        onAddUrl = { page = "Add video" })
                    "Search" -> SearchPage(query, { query = it }, sampleVideos, ::play, ::addToPlaylist)
                    "Shorts" -> ShortsPage(sampleVideos.filter { it.duration.startsWith("0:") || it.duration.startsWith("2:") }, ::play)
                    "Subscriptions" -> SimplePage("Subscriptions", "Your subscribed channels will appear here after an authorized account connection.", Icons.Default.Subscriptions)
                    "You" -> LibraryPage(onHistory = { page = "History" }, onPlaylist = { page = "Playlist" }, onDownloads = { page = "Downloads" }, onSettings = { page = "Settings" }, onAdd = { page = "Add video" })
                    "History" -> SavedListPage("Watch history", history, ::playSaved, onClear = { history = emptyList(); prefs.edit().putString("history", "").apply() })
                    "Playlist" -> SavedListPage("Your playlist", playlist, ::playSaved, onClear = { playlist = emptyList(); prefs.edit().putString("playlist", "").apply() })
                    "Downloads" -> DownloadsPage(context, downloads)
                    "Settings" -> SettingsPage(wifiOnly, { wifiOnly = it; prefs.edit().putBoolean("wifi_only", it).apply() }, onBack = { page = "You" })
                    "Add video" -> AddVideoPage(inputTitle, { inputTitle = it }, inputUrl, { inputUrl = it },
                        onPlay = { play(Video(inputTitle.ifBlank { "My video" }, "Direct media", inputUrl, "", "Custom", "User-provided direct media")) },
                        onDownload = { download(Video(inputTitle.ifBlank { "My video" }, "Direct media", inputUrl, "", "Custom", "User-provided direct media")) })
                    "Player" -> current?.let { PlayerPage(it, speed, { speed = it }, onBack = { page = "Home"; tab = "Home" }) }
                }
            }
        }
    }
}

@Composable
private fun TopBar(onSearch: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.PlayCircleFilled, null, tint = Accent, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(7.dp))
        Text("YourTube", fontSize = 23.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, modifier = Modifier.weight(1f))
        IconButton(onClick = onSearch) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
        IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = Color.White) }
    }
}

@Composable
private fun HomeFeed(query: String, onQuery: (String) -> Unit, category: String, onCategory: (String) -> Unit,
                     onPlay: (Video) -> Unit, onSave: (Video) -> Unit, onDownload: (Video) -> Unit, onAdd: () -> Unit) {
    val categories = listOf("All", "Animation", "Technology", "Travel", "Sci-Fi")
    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                categories.forEach { item ->
                    FilterChip(selected = category == item, onClick = { onCategory(item) }, label = { Text(item) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color.White, selectedLabelColor = Color.Black,
                            labelColor = Color.White, containerColor = SurfaceDark))
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text("Explore videos", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("A starter library of openly accessible demo media. Add your own direct stream URLs anytime.", color = Subtle, fontSize = 12.sp)
            }
        }
        items(sampleVideos.filter { category == "All" || it.category == category }) { video ->
            VideoCard(video, onPlay, onSave, onDownload)
        }
        item {
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)) {
                Icon(Icons.Default.AddLink, null); Spacer(Modifier.width(8.dp)); Text("Add a direct video URL")
            }
            Text("YouTube catalog and account features are not connected yet. This feed currently contains sample media, not live YouTube recommendations.",
                color = Subtle, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun VideoCard(video: Video, onPlay: (Video) -> Unit, onSave: (Video) -> Unit, onDownload: (Video) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onPlay(video) }.padding(bottom = 14.dp)) {
        Box(Modifier.fillMaxWidth().height(190.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF303F55), Color(0xFF542C45), Color(0xFF182D32)))),
            contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayCircleFilled, null, tint = Color.White.copy(alpha = .9f), modifier = Modifier.size(58.dp))
            Text(video.duration, color = Color.White, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd)
                .padding(10.dp).background(Color.Black.copy(alpha = .75f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 3.dp))
            Text(video.category.uppercase(), color = Color.White.copy(alpha = .8f), fontSize = 10.sp, modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(SurfaceLight), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VideoLibrary, null, tint = Accent)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(video.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(video.creator + " • " + video.description, color = Subtle, fontSize = 11.sp, maxLines = 2)
            }
            var expanded by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { expanded = true }) { Icon(Icons.Default.MoreVert, "More", tint = Subtle) }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("Save to playlist") }, onClick = { expanded = false; onSave(video) })
                    DropdownMenuItem(text = { Text("Download") }, onClick = { expanded = false; onDownload(video) })
                }
            }
        }
    }
}

@Composable
private fun SearchPage(query: String, onQuery: (String) -> Unit, videos: List<Video>, onPlay: (Video) -> Unit, onSave: (Video) -> Unit) {
    Column {
        OutlinedTextField(value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            singleLine = true, placeholder = { Text("Search videos in your library") }, leadingIcon = { Icon(Icons.Default.Search, null) })
        val results = videos.filter { it.title.contains(query, true) || it.creator.contains(query, true) || it.category.contains(query, true) }
        LazyColumn { items(results) { VideoCard(it, onPlay, onSave, {}) } }
        if (results.isEmpty()) Text("No matching videos in the current sample library.", color = Subtle, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun ShortsPage(shorts: List<Video>, onPlay: (Video) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Shorts", fontSize = 23.sp, fontWeight = FontWeight.Bold) }
        items(shorts) { video ->
            Box(Modifier.fillMaxWidth().height(390.dp).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF4A2542), Color(0xFF1D4050), Color(0xFF181818))))
                .clickable { onPlay(video) }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PlayCircleFilled, null, tint = Color.White, modifier = Modifier.size(64.dp))
                    Text(video.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(video.creator, color = Color.LightGray)
                    Text("Tap to play sample clip", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
        item { Text("These are sample short clips, not a live YouTube Shorts feed.", color = Subtle, fontSize = 11.sp) }
    }
}

@Composable
private fun PlayerPage(video: Video, speed: Float, onSpeed: (Float) -> Unit, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var controller by remember { mutableStateOf<androidx.media3.session.MediaController?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val controllerFuture = remember {
        val token = androidx.media3.session.SessionToken(
            context,
            android.content.ComponentName(context, com.yourtube.android.playback.YourTubePlaybackService::class.java)
        )
        androidx.media3.session.MediaController.Builder(context, token).buildAsync()
    }

    DisposableEffect(controllerFuture) {
        controllerFuture.addListener({
            try {
                val mediaController = controllerFuture.get()
                mediaController.setMediaItem(MediaItem.fromUri(video.url))
                mediaController.prepare()
                mediaController.playWhenReady = true
                mediaController.setPlaybackParameters(PlaybackParameters(speed))
                controller = mediaController
            } catch (t: Throwable) {
                error = t.message ?: "Unable to connect to playback service"
            }
        }, java.util.concurrent.Executor { command -> command.run() })
        onDispose {
            androidx.media3.session.MediaController.releaseFuture(controllerFuture)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Text(video.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1)
            var menu by remember { mutableStateOf(false) }
            Box {
                TextButton(onClick = { menu = true }) { Text("${speed}x", color = Accent) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf(.5f, .75f, 1f, 1.25f, 1.5f, 2f).forEach { value ->
                        DropdownMenuItem(text = { Text("${value}x") }, onClick = {
                            menu = false
                            onSpeed(value)
                            controller?.setPlaybackParameters(PlaybackParameters(value))
                        })
                    }
                }
            }
        }
        val activeController = controller
        if (activeController != null) {
            AndroidView(
                factory = { PlayerView(it).apply { player = activeController; useController = true; setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING) } },
                update = { it.player = activeController },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        } else {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black), contentAlignment = Alignment.Center) {
                if (error != null) Text(error!!, color = Color.White)
                else CircularProgressIndicator(color = Accent)
            }
        }
        Text(video.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        Text(video.creator + " • " + video.description, color = Subtle, modifier = Modifier.padding(horizontal = 16.dp))
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AssistChip(onClick = { controller?.seekBack() }, label = { Text("−10 sec") }, leadingIcon = { Icon(Icons.Default.Replay10, null) })
            AssistChip(onClick = { controller?.seekForward() }, label = { Text("+10 sec") }, leadingIcon = { Icon(Icons.Default.Forward10, null) })
        }
        Text("MediaSession playback • notification and lock-screen controls • speed control", color = Subtle, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun LibraryPage(onHistory: () -> Unit, onPlaylist: () -> Unit, onDownloads: () -> Unit, onSettings: () -> Unit, onAdd: () -> Unit) {
    LazyColumn {
        item { Text("You", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
        item { Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryTile("History", Icons.Default.History, onHistory, Modifier.weight(1f))
            LibraryTile("Playlists", Icons.Default.PlaylistPlay, onPlaylist, Modifier.weight(1f))
        } }
        item { Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LibraryTile("Downloads", Icons.Default.Download, onDownloads, Modifier.weight(1f))
            LibraryTile("Add video", Icons.Default.AddLink, onAdd, Modifier.weight(1f))
        } }
        item { LibraryRow("Settings", "Playback and download preferences", Icons.Default.Settings, onSettings) }
        item { LibraryRow("Account", "Sign-in integration is not configured", Icons.Default.AccountCircle) {} }
    }
}

@Composable
private fun LibraryTile(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.clickable(onClick = click), colors = CardDefaults.cardColors(containerColor = SurfaceDark)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) { Icon(icon, null, tint = Accent); Spacer(Modifier.height(12.dp)); Text(title, fontWeight = FontWeight.SemiBold) }
    }
}
@Composable
private fun LibraryRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, click: () -> Unit) {
    ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle, color = Subtle) },
        leadingContent = { Icon(icon, null, tint = Accent) }, modifier = Modifier.clickable(onClick = click),
        colors = ListItemDefaults.colors(containerColor = Bg))
}

@Composable
private fun SavedListPage(title: String, rows: List<String>, onPlay: (String) -> Unit, onClear: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onClear) { Text("Clear", color = Accent) }
        }
        if (rows.isEmpty()) Text("Nothing here yet. Play or save a video first.", color = Subtle, modifier = Modifier.padding(20.dp))
        LazyColumn { items(rows) { row ->
            val name = row.substringBefore("|")
            ListItem(headlineContent = { Text(name) }, supportingContent = { Text(row.substringAfter("|", ""), color = Subtle, maxLines = 1) },
                leadingContent = { Icon(Icons.Default.PlayCircle, null, tint = Accent) },
                modifier = Modifier.clickable { onPlay(row) }, colors = ListItemDefaults.colors(containerColor = Bg))
        } }
    }
}

@Composable
private fun AddVideoPage(title: String, onTitle: (String) -> Unit, url: String, onUrl: (String) -> Unit, onPlay: () -> Unit, onDownload: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Add a video", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Paste a direct media file or stream URL that you are authorized to access.", color = Subtle)
        OutlinedTextField(title, onTitle, modifier = Modifier.fillMaxWidth(), label = { Text("Video title") }, singleLine = true)
        OutlinedTextField(url, onUrl, modifier = Modifier.fillMaxWidth(), label = { Text("Direct MP4 / WebM / HLS / DASH URL") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), minLines = 2)
        Button(onClick = onPlay, enabled = MediaUrlValidator.isSupported(url), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.PlayArrow, null); Text("Play now") }
        OutlinedButton(onClick = onDownload, enabled = url.startsWith("http"), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Download, null); Text("Download file") }
        Text("YouTube watch links are not direct media URLs. Live YouTube search, channels and account access need a separately configured permitted integration.", color = Subtle, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsPage(wifiOnly: Boolean, onWifiOnly: (Boolean) -> Unit, onBack: () -> Unit) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }; Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
        ListItem(headlineContent = { Text("Wi-Fi only downloads") }, supportingContent = { Text("Avoid mobile data for downloads", color = Subtle) },
            trailingContent = { Switch(wifiOnly, onWifiOnly) }, colors = ListItemDefaults.colors(containerColor = Bg))
        ListItem(headlineContent = { Text("Player") }, supportingContent = { Text("Media3 • speed control • seek controls", color = Subtle) }, colors = ListItemDefaults.colors(containerColor = Bg))
        ListItem(headlineContent = { Text("Privacy") }, supportingContent = { Text("History and playlists are stored locally on this device.", color = Subtle) }, colors = ListItemDefaults.colors(containerColor = Bg))
        Text("Live YouTube feed, OAuth, background playback service and SponsorBlock are not enabled in this build.", color = Subtle, modifier = Modifier.padding(16.dp))
    }
}

@Composable
private fun DownloadsPage(context: Context, ids: List<String>) {
    if (ids.isEmpty()) { SimplePage("Downloads", "No downloads yet. Add a direct media URL and choose Download.", Icons.Default.Download); return }
    val manager = remember { context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager }
    LazyColumn {
        item { Text("Downloads", fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp)) }
        items(ids.reversed()) { id ->
            val status = try {
                manager.query(DownloadManager.Query().setFilterById(id.toLong())).use { cursor ->
                    if (!cursor.moveToFirst()) "Not found" else when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_SUCCESSFUL -> "Completed"
                        DownloadManager.STATUS_FAILED -> "Failed"
                        DownloadManager.STATUS_RUNNING -> "Downloading"
                        DownloadManager.STATUS_PAUSED -> "Paused"
                        else -> "Queued"
                    }
                }
            } catch (_: Exception) { "Unavailable" }
            ListItem(headlineContent = { Text("Download $id") }, supportingContent = { Text(status, color = Subtle) },
                leadingContent = { Icon(Icons.Default.Download, null, tint = Accent) }, colors = ListItemDefaults.colors(containerColor = Bg))
        }
    }
}

@Composable
private fun SimplePage(title: String, message: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, color = Subtle)
    }
}
