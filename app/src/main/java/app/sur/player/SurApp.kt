package app.sur.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

private val Fallback = Color(0xFF3D2A6B)

fun fmt(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)

/** অ্যালবাম আর্ট থেকে ডমিনেন্ট রং বের করে ব্যাকগ্রাউন্ড গ্রেডিয়েন্টে ব্যবহার করে। */
@Composable
fun rememberAccent(song: Song?): Color {
    val ctx = LocalContext.current
    var target by remember { mutableStateOf(Fallback) }
    LaunchedEffect(song?.id) {
        if (song == null) { target = Fallback; return@LaunchedEffect }
        val req = ImageRequest.Builder(ctx).data(song.artUri).allowHardware(false).build()
        val bmp = (ctx.imageLoader.execute(req) as? SuccessResult)?.drawable?.toBitmap()
        target = if (bmp != null) {
            val p = Palette.from(bmp).generate()
            Color(p.getDarkVibrantColor(p.getDarkMutedColor(Fallback.toArgb())))
        } else Fallback
    }
    return animateColorAsState(target, label = "accent").value
}

@Composable
fun SurApp(vm: PlayerViewModel) {
    val s by vm.ui.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    val accent = rememberAccent(s.current)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(accent, Color(0xFF0B0B10))))
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    0 -> PlayerScreen(vm, s, accent)
                    1 -> EqScreen()
                    else -> LibraryScreen(vm, s)
                }
            }
            if (tab != 0 && s.current != null) MiniPlayer(vm, s) { tab = 0 }
            NavigationBar(containerColor = Color.Black.copy(alpha = 0.35f)) {
                val items = listOf(
                    Icons.Filled.MusicNote to "Playing",
                    Icons.Filled.Equalizer to "EQ",
                    Icons.Filled.LibraryMusic to "Library",
                )
                items.forEachIndexed { i, (icon, label) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.White.copy(alpha = 0.15f)
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayer(vm: PlayerViewModel, s: UiState, open: () -> Unit) {
    val song = s.current ?: return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable(onClick = open)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray),
        )
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.6f), style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = vm::toggle) {
            Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "play/pause")
        }
    }
}
