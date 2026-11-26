package app.sur.player

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * অডিও ইফেক্ট (ইকুয়ালাইজার, বেস, লাউডনেস)। সার্ভিস আর UI একই প্রসেসে চলে,
 * তাই একটা সিঙ্গেলটন দিয়ে দুই দিকের যোগাযোগ করা হয়েছে।
 */
object Fx {
    private var eq: Equalizer? = null
    private var bassFx: BassBoost? = null
    private var loudFx: LoudnessEnhancer? = null

    var eqOn by mutableStateOf(true)
    var bass by mutableIntStateOf(0)          // 0..1000
    var gainMb by mutableIntStateOf(0)        // 0..1500 millibel
    val levels = mutableStateListOf<Int>()    // প্রতিটি ব্যান্ডের লেভেল (millibel)
    val centerHz = mutableStateListOf<Int>()
    var minLevel by mutableIntStateOf(-1500)
    var maxLevel by mutableIntStateOf(1500)
    val presets = mutableStateListOf<String>()
    var available by mutableStateOf(false)

    fun attach(sessionId: Int) {
        release()
        if (sessionId == 0 || sessionId == -1) return
        try {
            val e = Equalizer(0, sessionId)
            e.enabled = eqOn
            eq = e
            val n = e.numberOfBands.toInt()
            minLevel = e.bandLevelRange[0].toInt()
            maxLevel = e.bandLevelRange[1].toInt()
            if (levels.size != n) {
                levels.clear(); centerHz.clear()
                repeat(n) { levels += 0; centerHz += e.getCenterFreq(it.toShort()) / 1000 }
            }
            levels.forEachIndexed { i, l -> e.setBandLevel(i.toShort(), l.toShort()) }
            presets.clear()
            repeat(e.numberOfPresets.toInt()) { presets += e.getPresetName(it.toShort()) }

            bassFx = BassBoost(0, sessionId).apply {
                enabled = true
                if (strengthSupported) setStrength(bass.toShort())
            }
            loudFx = LoudnessEnhancer(sessionId).apply {
                enabled = true
                setTargetGain(gainMb)
            }
            available = true
        } catch (t: Throwable) {
            available = false
        }
    }

    fun release() {
        eq?.release(); bassFx?.release(); loudFx?.release()
        eq = null; bassFx = null; loudFx = null
    }

    fun setEnabled(on: Boolean) { eqOn = on; eq?.enabled = on }

    fun setBand(i: Int, level: Int) {
        val v = level.coerceIn(minLevel, maxLevel)
        levels[i] = v
        eq?.setBandLevel(i.toShort(), v.toShort())
    }

    fun usePreset(i: Int) {
        val e = eq ?: return
        try {
            e.usePreset(i.toShort())
            levels.indices.forEach { levels[it] = e.getBandLevel(it.toShort()).toInt() }
        } catch (_: Throwable) {}
    }

    fun reset() { levels.indices.forEach { setBand(it, 0) }; updateBass(0); setGain(0) }

    fun updateBass(v: Int) {
        bass = v
        try { bassFx?.setStrength(v.toShort()) } catch (_: Throwable) {}
    }

    fun setGain(mb: Int) {
        gainMb = mb
        try { loudFx?.setTargetGain(mb) } catch (_: Throwable) {}
    }
}
