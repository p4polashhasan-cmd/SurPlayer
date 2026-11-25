package app.sur.player

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class UiState(
    val songs: List<Song> = emptyList(),
    val current: Song? = null,
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val sleepMinutes: Int? = null,
    val tick: Int = 0, // রেটিং/প্লে-কাউন্ট বদলালে UI রিফ্রেশ করার জন্য
)

class PlayerViewModel(private val app: Application) : AndroidViewModel(app) {

    val stats = Stats(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var controller: MediaController? = null
    private var sleepJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = sync()

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            item?.mediaId?.toLongOrNull()?.let { stats.recordPlay(it); bump() }
        }
    }

    init {
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = future.get().also { it.addListener(listener) }
            sync()
        }, ContextCompat.getMainExecutor(app))

        // সিক-বারের জন্য প্রতি ৪০০ms-এ পজিশন আপডেট
        viewModelScope.launch {
            while (true) {
                delay(400)
                controller?.let { c ->
                    if (c.isPlaying) _ui.update { it.copy(position = c.currentPosition) }
                }
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { MediaRepo.load(app) }
            _ui.update { it.copy(songs = list) }
            sync()
        }
    }

    private fun bump() = _ui.update { it.copy(tick = it.tick + 1) }

    private fun sync() {
        val c = controller ?: return
        val id = c.currentMediaItem?.mediaId?.toLongOrNull()
        _ui.update { s ->
            s.copy(
                current = s.songs.firstOrNull { it.id == id },
                playing = c.isPlaying,
                duration = c.duration.coerceAtLeast(0),
                position = c.currentPosition,
                shuffle = c.shuffleModeEnabled,
                repeat = c.repeatMode,
            )
        }
    }

    // ---- কন্ট্রোল ----
    fun play(list: List<Song>, index: Int) {
        val c = controller ?: return
        c.setMediaItems(list.map { it.toMediaItem() }, index, 0)
        c.prepare()
        c.play()
    }

    fun playAllShuffled() {
        val list = _ui.value.songs
        if (list.isEmpty()) return
        controller?.shuffleModeEnabled = true
        play(list, list.indices.random())
    }

    fun toggle() { controller?.let { if (it.isPlaying) it.pause() else it.play() } }
    fun next() { controller?.seekToNextMediaItem() }
    fun prev() { controller?.seekToPreviousMediaItem() }
    fun seekTo(fraction: Float) {
        val c = controller ?: return
        val d = c.duration
        if (d > 0) {
            val p = (d * fraction).toLong()
            c.seekTo(p)
            _ui.update { it.copy(position = p) }
        }
    }

    fun toggleShuffle() { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }

    fun cycleRepeat() {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    // ---- রেটিং ----
    fun rate(song: Song, value: Int) {
        stats.setRating(song.id, if (stats.rating(song.id) == value) 0 else value)
        bump()
    }

    // ---- স্লিপ টাইমার ----
    fun setSleepTimer(minutes: Int?) {
        sleepJob?.cancel()
        _ui.update { it.copy(sleepMinutes = minutes) }
        if (minutes == null) return
        sleepJob = viewModelScope.launch {
            delay(minutes * 60_000L)
            controller?.pause()
            _ui.update { it.copy(sleepMinutes = null) }
        }
    }

    override fun onCleared() {
        controller?.release()
        super.onCleared()
    }
}
