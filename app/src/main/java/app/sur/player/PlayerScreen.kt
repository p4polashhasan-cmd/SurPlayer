package app.sur.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlin.random.Random

@Composable
fun PlayerScreen(vm: PlayerViewModel, s: UiState, accent: Color) {
    val song = s.current
    if (song == null) {
        Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
            Text("Library থেকে একটা গান বাছুন", color = Color.White.copy(alpha = 0.7f))
        }
        return
    }
    // s.tick পড়ে রাখলাম যাতে রেটিং বদলালে এই স্ক্রিন রিকম্পোজ হয়
    @Suppress("UNUSED_VARIABLE") val t = s.tick
    val rating = vm.stats.rating(song.id)
    var sleepMenu by remember { mutableStateOf(false) }

    Column(
        Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        AsyncImage(
            model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth().aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = 0.08f)),
        )

        Column {
            Text(song.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(song.artist, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Row {
                IconButton({ vm.rate(song, 1) }) {
                    Icon(if (rating == 1) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp, "like")
                }
                IconButton({ vm.rate(song, -1) }) {
                    Icon(if (rating == -1) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown, "dislike")
                }
            }
            Row {
                Box {
                    IconButton({ sleepMenu = true }) {
                        Icon(Icons.Filled.Bedtime, "sleep timer",
                            tint = if (s.sleepMinutes != null) MaterialTheme.colorScheme.primary else Color.White)
                    }
                    DropdownMenu(sleepMenu, { sleepMenu = false }) {
                        listOf(15, 30, 45, 60).forEach { m ->
                            DropdownMenuItem(text = { Text("$m মিনিট পরে বন্ধ") },
                                onClick = { vm.setSleepTimer(m); sleepMenu = false })
                        }
                        DropdownMenuItem(text = { Text("টাইমার বন্ধ") },
                            onClick = { vm.setSleepTimer(null); sleepMenu = false })
                    }
                }
                IconButton(vm::toggleShuffle) {
                    Icon(Icons.Filled.Shuffle, "shuffle",
                        tint = if (s.shuffle) MaterialTheme.colorScheme.primary else Color.White)
                }
                IconButton(vm::cycleRepeat) {
                    Icon(
                        if (s.repeat == Player.REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        "repeat",
                        tint = if (s.repeat != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else Color.White,
                    )
                }
            }
        }

        val progress = if (s.duration > 0) (s.position.toFloat() / s.duration).coerceIn(0f, 1f) else 0f
        WaveSeek(seed = song.id, progress = progress, onSeek = vm::seekTo)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(fmt(s.position), style = MaterialTheme.typography.bodySmall)
            Text(fmt(s.duration), style = MaterialTheme.typography.bodySmall)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(vm::prev, Modifier.size(64.dp)) { Icon(Icons.Filled.SkipPrevious, "prev", Modifier.size(40.dp)) }
            IconButton(
                vm::toggle,
                Modifier.size(80.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            ) {
                Icon(if (s.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, "play/pause",
                    Modifier.size(44.dp), tint = Color.Black)
            }
            IconButton(vm::next, Modifier.size(64.dp)) { Icon(Icons.Filled.SkipNext, "next", Modifier.size(40.dp)) }
        }
    }
}

/**
 * ওয়েভফর্ম-স্টাইল সিক বার। বারগুলো গানের id থেকে বানানো সাজসজ্জার আকৃতি
 * (আসল অডিও ডিকোড করা হয় না), তবে ট্যাপ বা ড্র্যাগ করলে আসলেই সিক করে।
 */
@Composable
private fun WaveSeek(seed: Long, progress: Float, onSeek: (Float) -> Unit) {
    val bars = remember(seed) { val r = Random(seed); List(64) { 0.25f + r.nextFloat() * 0.75f } }
    val active = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier.fillMaxWidth().height(72.dp)
            .pointerInput(Unit) { detectTapGestures { onSeek((it.x / size.width).coerceIn(0f, 1f)) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
            }
    ) {
        val slot = size.width / bars.size
        val w = slot * 0.6f
        bars.forEachIndexed { i, h ->
            val bh = size.height * h
            drawRoundRect(
                color = if (i.toFloat() / bars.size <= progress) active else Color.White.copy(alpha = 0.3f),
                topLeft = Offset(i * slot, (size.height - bh) / 2),
                size = Size(w, bh),
                cornerRadius = CornerRadius(w / 2),
            )
        }
    }
}
