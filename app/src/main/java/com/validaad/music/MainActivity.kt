package com.validaad.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

private val Ink = Color(0xFF090B0A)
private val Panel = Color(0xFF171B18)
private val Lime = Color(0xFFC8F169)
private val Muted = Color(0xFF9AA39B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ValidaadMusicApp() }
    }
}

@Composable
private fun ValidaadMusicApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Home", "Search", "Library", "Settings")
    val icons = listOf(Icons.Default.Home, Icons.Default.Search, Icons.Default.LibraryMusic, Icons.Default.Settings)
    MaterialTheme {
        Scaffold(
            containerColor = Ink,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    tabs.forEachIndexed { index, label ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(icons[index], contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier.fillMaxSize().background(Ink).padding(padding).padding(horizontal = 22.dp, vertical = 18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("VALIDAAD", color = Lime, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                        Text("MUSIC", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    }
                    Box(Modifier.size(44.dp).background(Panel, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Album, contentDescription = "Vinyl", tint = Lime, modifier = Modifier.size(27.dp))
                    }
                }
                Spacer(Modifier.height(30.dp))
                when (selectedTab) {
                    0 -> HomeScreen()
                    1 -> SimpleScreen("Find your sound", "Search for music, artists and albums.")
                    2 -> SimpleScreen("Your library", "Your offline songs and playlists will live here.")
                    else -> SimpleScreen("Settings", "Playback, appearance and privacy controls.")
                }
            }
        }
    }
}

@Composable
private fun HomeScreen() {
    Text("YOUR MUSIC. YOUR WORLD.", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
    Spacer(Modifier.height(8.dp))
    Text("Feel every beat.", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(22.dp))
    Surface(color = Panel, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("THE VALIDAAD EXPERIENCE", color = Lime, fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(150.dp).background(Color(0xFF252B27), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(116.dp).background(Color(0xFF111512), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(38.dp).background(Lime, CircleShape))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Your signature player is coming", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("A fresh home for the music you love.", color = Muted, fontSize = 13.sp)
        }
    }
    Spacer(Modifier.height(24.dp))
    Text("EXPLORE", color = Lime, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
    Spacer(Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        ExploreTile("Offline music", "On your device", Modifier.weight(1f))
        ExploreTile("Discover", "Find new sounds", Modifier.weight(1f))
    }
}

@Composable
private fun ExploreTile(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.background(Panel, RoundedCornerShape(18.dp)).padding(15.dp)) {
        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.height(5.dp))
        Text(subtitle, color = Muted, fontSize = 11.sp)
    }
}

@Composable
private fun SimpleScreen(title: String, subtitle: String) {
    Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(subtitle, color = Muted, fontSize = 14.sp)
}
