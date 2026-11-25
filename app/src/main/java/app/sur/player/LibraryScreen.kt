package app.sur.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

private enum class Filter(val label: String) {
    All("সব গান"), Recent("সম্প্রতি শোনা"), Most("বেশি শোনা"),
    Top("পছন্দের"), Low("অপছন্দের"), Added("নতুন যোগ"),
}

@Composable
fun LibraryScreen(vm: PlayerViewModel, s: UiState) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(Filter.All) }

    val list = remember(s.songs, query, filter, s.tick) {
        val st = vm.stats
        var l = when (filter) {
            Filter.All -> s.songs
            Filter.Recent -> s.songs.filter { st.lastPlayed(it.id) > 0 }.sortedByDescending { st.lastPlayed(it.id) }
            Filter.Most -> s.songs.filter { st.plays(it.id) > 0 }.sortedByDescending { st.plays(it.id) }
            Filter.Top -> s.songs.filter { st.rating(it.id) == 1 }
            Filter.Low -> s.songs.filter { st.rating(it.id) == -1 }
            Filter.Added -> s.songs.sortedByDescending { it.addedSec }.take(100)
        }
        if (query.isNotBlank()) {
            l = l.filter {
                it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
            }
        }
        l
    }

    Column {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.weight(1f), singleLine = true,
                placeholder = { Text("গান, শিল্পী বা অ্যালবাম খুঁজুন") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                shape = RoundedCornerShape(16.dp),
            )
            IconButton(vm::playAllShuffled) { Icon(Icons.Filled.Shuffle, "shuffle all") }
        }
        LazyRow(contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Filter.entries) { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) })
            }
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            itemsIndexed(list, key = { _, song -> song.id }) { index, song ->
                val playing = s.current?.id == song.id
                Row(
                    Modifier.fillMaxWidth()
                        .clickable { vm.play(list, index) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = song.artUri, contentDescription = null, contentScale = ContentScale.Crop,
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(song.title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = if (playing) MaterialTheme.colorScheme.primary else Color.White)
                        Text("${song.artist} · ${fmt(song.durationMs)}", maxLines = 1, overflow = TextOverflow.Ellipsis,
                            color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
