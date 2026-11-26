package app.sur.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun EqScreen() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Equalizer", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = Fx::reset) { Text("Reset") }
                Switch(Fx.eqOn, Fx::setEnabled)
            }
        }

        if (!Fx.available) {
            Text("গান একবার চালালে ইকুয়ালাইজার চালু হবে।", color = Color.White.copy(alpha = 0.7f))
            return@Column
        }

        // ব্যান্ড স্লাইডার (সংখ্যা ডিভাইস অনুযায়ী বদলায়, সাধারণত ৫টি)
        Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Fx.levels.forEachIndexed { i, level ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${level / 100}dB", style = MaterialTheme.typography.labelSmall)
                    VerticalBand(
                        value = level, min = Fx.minLevel, max = Fx.maxLevel,
                        onChange = { Fx.setBand(i, it) },
                        modifier = Modifier.weight(1f).width(48.dp),
                    )
                    val hz = Fx.centerHz.getOrElse(i) { 0 }
                    Text(if (hz >= 1000) "${hz / 1000}k" else "$hz", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        if (Fx.presets.isNotEmpty()) {
            Text("Presets", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(Fx.presets) { i, name ->
                    FilterChip(selected = false, onClick = { Fx.usePreset(i) }, label = { Text(name) })
                }
            }
        }

        Text("Bass boost", style = MaterialTheme.typography.labelLarge)
        Slider(Fx.bass.toFloat(), { Fx.updateBass(it.toInt()) }, valueRange = 0f..1000f)

        Text("Loudness gain", style = MaterialTheme.typography.labelLarge)
        Slider(Fx.gainMb.toFloat(), { Fx.setGain(it.toInt()) }, valueRange = 0f..1500f)
    }
}

/** নিজে আঁকা উল্লম্ব স্লাইডার: ওপরে ড্র্যাগ করলে লেভেল বাড়ে, নিচে কমে। */
@Composable
private fun VerticalBand(value: Int, min: Int, max: Int, onChange: (Int) -> Unit, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    fun fromY(y: Float, h: Float): Int {
        val f = 1f - (y / h).coerceIn(0f, 1f)
        return (min + f * (max - min)).toInt()
    }
    Canvas(
        modifier
            .pointerInput(min, max) { detectTapGestures { onChange(fromY(it.y, size.height.toFloat())) } }
            .pointerInput(min, max) {
                detectDragGestures { change, _ -> onChange(fromY(change.position.y, size.height.toFloat())) }
            }
    ) {
        val cx = size.width / 2
        drawLine(Color.White.copy(alpha = 0.25f), Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 6f)
        val f = (value - min).toFloat() / (max - min)
        val y = size.height * (1f - f)
        val zeroY = size.height * (1f - (0 - min).toFloat() / (max - min))
        drawLine(accent, Offset(cx, zeroY), Offset(cx, y), strokeWidth = 8f)
        drawCircle(accent, radius = 14f, center = Offset(cx, y))
    }
}
