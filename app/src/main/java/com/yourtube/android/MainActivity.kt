package com.yourtube.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AppBg = Color(0xFF090A0D)
private val CardBg = Color(0xFF15171D)
private val Accent = Color(0xFFFF334F)
private val Muted = Color(0xFFA4A7B0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { YourTubeApp() }
    }
}

@Composable
private fun YourTubeApp() {
    var selected by remember { mutableStateOf("Home") }
    var query by remember { mutableStateOf("") }
    val tabs = listOf("Home", "Shorts", "Subscriptions", "Downloads", "You")
    val icons = listOf(Icons.Default.Home, Icons.Default.Movie, Icons.Default.Subscriptions, Icons.Default.Download, Icons.Default.Person)

    MaterialTheme(colorScheme = darkColorScheme(
        background = AppBg, surface = CardBg, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(
            containerColor = AppBg,
            bottomBar = {
                NavigationBar(containerColor = CardBg, contentColor = Color.White) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { selected = tab },
                            icon = { Icon(icons[index], contentDescription = tab) },
                            label = { Text(tab, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent,
                                selectedTextColor = Accent,
                                indicatorColor = Accent.copy(alpha = 0.14f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Column(Modifier.fillMaxSize().background(AppBg).padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayCircleFilled, null, tint = Accent, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("YourTube", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = { selected = "Search" }) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White)
                    }
                }
                when (selected) {
                    "Home" -> HomeScreen(query, { query = it })
                    "Shorts" -> EmptyScreen("Shorts", "Short-form video feed will appear here.")
                    "Subscriptions" -> EmptyScreen("Subscriptions", "Your followed channels will appear here.")
                    "Downloads" -> EmptyScreen("Downloads", "Your saved videos will appear here.")
                    "You" -> EmptyScreen("Your account", "Sign-in and your library settings.")
                    else -> HomeScreen(query, { query = it })
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(query: String, onQuery: (String) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search videos", color = Muted) },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Accent, unfocusedBorderColor = Color(0xFF30333B),
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            )
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Music", "Gaming", "News").forEachIndexed { i, label ->
                Surface(color = if (i == 0) Accent else CardBg, shape = RoundedCornerShape(20.dp)) {
                    Text(label, Modifier.padding(horizontal = 15.dp, vertical = 8.dp), color = Color.White, fontSize = 13.sp)
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 18.dp)) {
            items(listOf("Welcome to YourTube", "Your video feed is ready for the next step", "Playback engine foundation included")) { title ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp)) {
                    Box(
                        Modifier.fillMaxWidth().height(190.dp)
                            .background(Color(0xFF20232B), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayCircleOutline, null, tint = Accent, modifier = Modifier.size(58.dp))
                    }
                    Text(title, Modifier.padding(top = 10.dp), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Text("YourTube • Preview", Modifier.padding(top = 4.dp), color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun EmptyScreen(title: String, description: String) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.PlayCircleOutline, null, tint = Accent, modifier = Modifier.size(58.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(description, color = Muted, fontSize = 14.sp)
    }
}
