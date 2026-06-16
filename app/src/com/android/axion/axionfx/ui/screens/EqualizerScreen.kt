@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axionfx.ui.screens

import com.android.axion.axionfx.ui.AxionFxViewModel
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.axion.axionfx.R
import com.android.axion.axionfx.domain.AutoEqParser
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SwitchPreference
import com.android.axion.compose.scaffold.AxionScaffold
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

private const val KEY_EQ_ENABLED = "eq_enabled"
private const val KEY_EQ_BAND_PREFIX = "eq_band_"
private const val KEY_ARBITRARY_EQ_BAND_PREFIX = "arbitrary_eq_band_"

private val BAND_LABELS = arrayOf(
    "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k"
)

private val BAND_CENTERS_HZ = floatArrayOf(
    31f, 62f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f
)

private val RESPONSE_FREQUENCIES_HZ = floatArrayOf(
    20f, 31.5f, 40f, 50f, 63f, 78f, 100f, 125f,
    160f, 200f, 250f, 315f, 406f, 500f, 630f, 800f,
    1000f, 1216f, 1600f, 2000f, 2500f, 3150f, 3640f, 4000f,
    5000f, 6299f, 8000f, 10000f, 12500f, 16000f, 18851f, 20000f
)

private val RESPONSE_LABELS = arrayOf(
    "20", "31", "40", "50", "63", "78", "100", "125",
    "160", "200", "250", "315", "406", "500", "630", "800",
    "1k", "1.2k", "1.6k", "2k", "2.5k", "3.1k", "3.6k", "4k",
    "5k", "6.3k", "8k", "10k", "12.5k", "16k", "18.9k", "20k"
)

private val RESPONSE_LABEL_INDICES = setOf(
    0, 4, 7, 10, 13, 16, 19, 23, 26, 31
)

private data class EqPreset(val nameResId: Int, val bands: IntArray)

private val PRESETS = listOf(
    EqPreset(R.string.eq_preset_flat, intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)),
    EqPreset(R.string.eq_preset_bass_boost, intArrayOf(500, 400, 300, 100, 0, 0, 0, 0, 0, 0)),
    EqPreset(R.string.eq_preset_treble_boost, intArrayOf(0, 0, 0, 0, 0, 0, 100, 300, 400, 500)),
    EqPreset(R.string.eq_preset_voice, intArrayOf(-200, -100, 0, 200, 400, 400, 200, 0, -100, -200)),
    EqPreset(R.string.eq_preset_v_shape, intArrayOf(400, 300, 100, 0, -200, -200, 0, 100, 300, 400)),
    EqPreset(R.string.eq_preset_rock, intArrayOf(300, 200, 0, -100, -200, 0, 200, 300, 400, 400)),
)

@Composable
fun EqualizerScreen(viewModel: AxionFxViewModel, onBackClick: () -> Unit) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    var enabled by remember { mutableStateOf(viewModel.loadBoolean(KEY_EQ_ENABLED, false)) }
    var mode by remember { mutableIntStateOf(0) }
    val bandGains = remember {
        Array(10) { mutableFloatStateOf(viewModel.loadInt("$KEY_EQ_BAND_PREFIX$it", 0).toFloat()) }
    }
    val responseGains = remember {
        val legacyValues = FloatArray(bandGains.size) { bandGains[it].floatValue }
        Array(RESPONSE_FREQUENCIES_HZ.size) { index ->
            val savedValue = viewModel.repo.getInt("$KEY_ARBITRARY_EQ_BAND_PREFIX$index", Int.MIN_VALUE)
            mutableFloatStateOf(
                if (savedValue != Int.MIN_VALUE) {
                    savedValue.toFloat()
                } else {
                    interpolateMagnitude(BAND_CENTERS_HZ, legacyValues, RESPONSE_FREQUENCIES_HZ[index])
                },
            )
        }
    }

    fun syncResponseFromBands() {
        val values = FloatArray(bandGains.size) { bandGains[it].floatValue }
        responseGains.forEachIndexed { index, state ->
            state.floatValue = interpolateMagnitude(BAND_CENTERS_HZ, values, RESPONSE_FREQUENCIES_HZ[index])
        }
    }

    fun ensureEqActive() {
        enabled = true
        viewModel.interactor.setMasterEnabled(true)
        viewModel.interactor.setEqEnabled(true)
    }

    fun applyLegacyBandsFromResponse() {
        val values = FloatArray(responseGains.size) { responseGains[it].floatValue }
        for (i in 0..9) {
            val gain = interpolateMagnitude(RESPONSE_FREQUENCIES_HZ, values, BAND_CENTERS_HZ[i])
                .roundToInt()
                .coerceIn(-1200, 1200)
            bandGains[i].floatValue = gain.toFloat()
            viewModel.interactor.setEqBandLevel(i, gain)
        }
    }

    fun applyResponseToBands() {
        ensureEqActive()
        responseGains.forEachIndexed { index, state ->
            viewModel.interactor.setArbitraryEqBandLevel(index, state.floatValue.roundToInt().coerceIn(-1200, 1200))
        }
        applyLegacyBandsFromResponse()
    }

    fun applyResponseNode(index: Int, value: Float) {
        ensureEqActive()
        val gain = value.roundToInt().coerceIn(-1200, 1200)
        responseGains[index].floatValue = gain.toFloat()
        viewModel.interactor.setArbitraryEqBandLevel(index, gain)
        applyLegacyBandsFromResponse()
    }

    fun applySimpleTone(bass: Float, mid: Float, treble: Float) {
        responseGains.forEachIndexed { index, state ->
            state.floatValue = simpleToneGain(
                frequency = RESPONSE_FREQUENCIES_HZ[index],
                bass = bass,
                mid = mid,
                treble = treble,
            )
        }
        applyResponseToBands()
    }

    fun smoothResponse() {
        val values = FloatArray(responseGains.size) { responseGains[it].floatValue }
        responseGains.forEachIndexed { index, state ->
            val previous = values.getOrElse(index - 1) { values[index] }
            val next = values.getOrElse(index + 1) { values[index] }
            state.floatValue = ((previous + values[index] * 2f + next) / 4f)
                .roundToInt()
                .toFloat()
        }
        applyResponseToBands()
    }

    fun applyBands() {
        ensureEqActive()
        syncResponseFromBands()
        responseGains.forEachIndexed { index, state ->
            viewModel.interactor.setArbitraryEqBandLevel(index, state.floatValue.roundToInt().coerceIn(-1200, 1200))
        }
        for (i in 0..9) {
            val v = bandGains[i].floatValue.toInt()
            viewModel.interactor.setEqBandLevel(i, v)
        }
    }

    fun applyAutoEq(text: String): Boolean {
        val result = AutoEqParser.parse(text, RESPONSE_FREQUENCIES_HZ) ?: return false
        result.bandGainsCentibels.forEachIndexed { index, gain ->
            responseGains[index].floatValue = gain.toFloat()
        }
        applyResponseToBands()
        result.preampCentibels?.let { preamp ->
            val outputGain = (100f * 10f.pow(preamp / 2000f)).roundToInt().coerceIn(0, 200)
            viewModel.interactor.setOutputGain(outputGain)
        }
        mode = 1
        ensureEqActive()
        return true
    }

    val importSuccess = stringResource(R.string.eq_autoeq_import_success)
    val importFailed = stringResource(R.string.eq_autoeq_import_failed)
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader().readText()
            }.orEmpty()
            val message = if (applyAutoEq(text)) importSuccess else importFailed
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(context, importFailed, Toast.LENGTH_SHORT).show()
        }
    }

    AxionScaffold(title = stringResource(R.string.nav_equalizer), onBackClick = onBackClick) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PreferenceGroup(title = stringResource(R.string.eq_category)) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.eq_enable_title),
                        summary = stringResource(R.string.eq_enable_summary),
                        checked = enabled,
                        onCheckedChange = {
                            enabled = it
                            viewModel.interactor.setEqEnabled(it)
                        },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.eq_autoeq_import_title),
                        summary = stringResource(R.string.eq_autoeq_import_summary),
                        onClick = {
                            importLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                        },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = { mode = 0 },
                    modifier = Modifier.weight(1f),
                    colors = if (mode == 0) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) else ButtonDefaults.filledTonalButtonColors(),
                ) { Text(stringResource(R.string.eq_simple)) }
                FilledTonalButton(
                    onClick = { mode = 1 },
                    modifier = Modifier.weight(1f),
                    colors = if (mode == 1) ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) else ButtonDefaults.filledTonalButtonColors(),
                ) { Text(stringResource(R.string.eq_arbitrary_mode)) }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = {
                    fadeIn().togetherWith(fadeOut()).using(SizeTransform(clip = false))
                },
                label = "eqMode",
            ) { currentMode ->
                when (currentMode) {
                    0 -> SimpleEqMode(
                        bandGains = bandGains,
                        enabled = enabled,
                        onToneChange = { bass, mid, treble -> applySimpleTone(bass, mid, treble) },
                    )
                    else -> ArbitraryResponseEqMode(
                        responseGains = responseGains,
                        enabled = enabled,
                        onEnable = { ensureEqActive() },
                        onResponseChange = { index, value -> applyResponseNode(index, value) },
                        onSmooth = { smoothResponse() },
                        onReset = {
                            responseGains.forEach { it.floatValue = 0f }
                            applyResponseToBands()
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

}

@Composable
private fun SimpleEqMode(
    bandGains: Array<MutableFloatState>,
    enabled: Boolean,
    onToneChange: (Float, Float, Float) -> Unit,
) {
    var bass by remember { mutableFloatStateOf(0f) }
    var mid by remember { mutableFloatStateOf(0f) }
    var treble by remember { mutableFloatStateOf(0f) }

    fun syncFromBands() {
        bass = (bandGains[0].floatValue + bandGains[1].floatValue + bandGains[2].floatValue) / 3f / 50f
        mid = (bandGains[3].floatValue + bandGains[4].floatValue + bandGains[5].floatValue + bandGains[6].floatValue) / 4f / 50f
        treble = (bandGains[7].floatValue + bandGains[8].floatValue + bandGains[9].floatValue) / 3f / 50f
    }

    fun applyTone() {
        onToneChange(bass, mid, treble)
    }

    remember { syncFromBands(); true }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularEqControl(
            bass = bass, mid = mid, treble = treble,
            enabled = enabled,
            onBassChange = { bass = it; applyTone() },
            onMidChange = { mid = it; applyTone() },
            onTrebleChange = { treble = it; applyTone() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .aspectRatio(1f),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESETS.take(3).forEach { preset ->
                    FilledTonalButton(
                        onClick = {
                            for (i in 0..9) bandGains[i].floatValue = preset.bands[i].toFloat()
                            syncFromBands()
                            applyTone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    ) { Text(stringResource(preset.nameResId), style = MaterialTheme.typography.labelMedium) }
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PRESETS.drop(3).forEach { preset ->
                    FilledTonalButton(
                        onClick = {
                            for (i in 0..9) bandGains[i].floatValue = preset.bands[i].toFloat()
                            syncFromBands()
                            applyTone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                    ) { Text(stringResource(preset.nameResId), style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@Composable
private fun CircularEqControl(
    bass: Float, mid: Float, treble: Float,
    enabled: Boolean,
    onBassChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onTrebleChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surfaceBright = MaterialTheme.colorScheme.surfaceBright
    val labelStyle = TextStyle(fontSize = 11.sp)
    val valueStyle = TextStyle(fontSize = 13.sp)

    val labelMid = stringResource(R.string.eq_label_mid)
    val labelBass = stringResource(R.string.eq_label_bass)
    val labelTreble = stringResource(R.string.eq_label_treble)

    var activeAxis by remember { mutableIntStateOf(-1) }
    val currentBassChange by rememberUpdatedState(onBassChange)
    val currentMidChange by rememberUpdatedState(onMidChange)
    val currentTrebleChange by rememberUpdatedState(onTrebleChange)

    val angles = remember { doubleArrayOf(-PI / 2, -PI / 2 + 2 * PI / 3, -PI / 2 + 4 * PI / 3) }

    fun calcValueForAxis(pos: Offset, w: Int, h: Int, axisIdx: Int): Float {
        val cx = w / 2f
        val cy = h / 2f
        val baseR = w / 2f * 0.35f
        val angle = angles[axisIdx]
        val axDx = cos(angle).toFloat()
        val axDy = sin(angle).toFloat()
        val dot = (pos.x - cx) * axDx + (pos.y - cy) * axDy
        return ((dot / baseR - 1f) / 0.8f * 10f).coerceIn(-10f, 10f)
    }

    fun findNearestAxis(pos: Offset, w: Int, h: Int): Int {
        val cx = w / 2f
        val cy = h / 2f
        val touchAngle = atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
        var bestIdx = 0
        var bestDiff = Double.MAX_VALUE
        for (i in 0..2) {
            val diff = atan2(sin(touchAngle - angles[i]), cos(touchAngle - angles[i]))
            val absDiff = abs(diff)
            if (absDiff < bestDiff) { bestDiff = absDiff; bestIdx = i }
        }
        val dist = hypot((pos.x - cx).toDouble(), (pos.y - cy).toDouble())
        return if (dist < w * 0.48f) bestIdx else -1
    }

    fun dispatchToAxis(axisIdx: Int, value: Float) {
        when (axisIdx) {
            0 -> currentMidChange(value)
            1 -> currentBassChange(value)
            2 -> currentTrebleChange(value)
        }
    }

    fun dispatchDiagonal(pos: Offset, w: Int, h: Int) {
        val cx = w / 2f
        val cy = h / 2f
        val touchAngle = atan2((pos.y - cy).toDouble(), (pos.x - cx).toDouble())
        for (i in 0..2) {
            val diff = atan2(sin(touchAngle - angles[i]), cos(touchAngle - angles[i]))
            val absDiff = abs(diff)
            val weight = cos(absDiff * 1.5).toFloat().coerceAtLeast(0f)
            if (weight > 0.15f) {
                val v = calcValueForAxis(pos, w, h, i) * weight
                dispatchToAxis(i, v.coerceIn(-10f, 10f))
            }
        }
    }

    Canvas(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val axis = findNearestAxis(down.position, size.width, size.height)
                    if (axis < 0) return@awaitEachGesture
                    down.consume()

                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val touchAngle = atan2((down.position.y - cy).toDouble(), (down.position.x - cx).toDouble())
                    val nearestDiff = abs(atan2(sin(touchAngle - angles[axis]), cos(touchAngle - angles[axis])))
                    val isDiagonal = nearestDiff > PI / 6

                    if (isDiagonal) {
                        activeAxis = -1
                        dispatchDiagonal(down.position, size.width, size.height)
                    } else {
                        activeAxis = axis
                        val v = calcValueForAxis(down.position, size.width, size.height, axis)
                        dispatchToAxis(axis, v)
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) break
                        change.consume()
                        if (isDiagonal) {
                            dispatchDiagonal(change.position, size.width, size.height)
                        } else {
                            val v = calcValueForAxis(change.position, size.width, size.height, axis)
                            dispatchToAxis(axis, v)
                        }
                    }
                    activeAxis = -1
                }
            }
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.width / 2f * 0.9f
        val baseR = size.width / 2f * 0.35f

        drawCircle(surfaceBright, outerR, Offset(cx, cy))
        drawCircle(outline.copy(alpha = 0.15f), outerR, Offset(cx, cy), style = Stroke(1.5f))

        val values = floatArrayOf(mid, bass, treble)
        val labels = arrayOf(labelMid, labelBass, labelTreble)

        for (i in 0..2) {
            val ex = cx + outerR * 0.88f * cos(angles[i]).toFloat()
            val ey = cy + outerR * 0.88f * sin(angles[i]).toFloat()
            drawLine(outline.copy(alpha = 0.12f), Offset(cx, cy), Offset(ex, ey), strokeWidth = 1f)

            for (dot in 1..7) {
                val dr = baseR * (0.3f + dot * 0.2f)
                val dx = cx + dr * cos(angles[i]).toFloat()
                val dy = cy + dr * sin(angles[i]).toFloat()
                drawCircle(outline.copy(alpha = 0.4f), 3.5f, Offset(dx, dy))
            }
        }

        drawCircle(outline.copy(alpha = 0.06f), baseR, Offset(cx, cy), style = Stroke(0.8f))

        val segments = 72
        val wavePath = Path()
        for (s in 0..segments) {
            val t = s.toFloat() / segments
            val segAngle = t * 2.0 * PI - PI / 2

            var r = baseR
            for (i in 0..2) {
                val norm = (values[i] / 10f).coerceIn(-1f, 1f)
                val influence = baseR * norm * 0.8f
                val diff = segAngle - angles[i]
                val wrap = atan2(sin(diff), cos(diff))
                val w = cos(wrap * 0.75).toFloat().coerceAtLeast(0f)
                r += influence * w * w
            }

            val px = cx + r * cos(segAngle).toFloat()
            val py = cy + r * sin(segAngle).toFloat()
            if (s == 0) wavePath.moveTo(px, py) else wavePath.lineTo(px, py)
        }
        wavePath.close()

        drawPath(wavePath, primaryContainer.copy(alpha = 0.12f))
        drawPath(wavePath, primary.copy(alpha = 0.4f), style = Stroke(2f, cap = StrokeCap.Round))

        val points = Array(3) { i ->
            val norm = (values[i] / 10f).coerceIn(-1f, 1f)
            val r = baseR * (1f + norm * 0.8f)
            Offset(cx + r * cos(angles[i]).toFloat(), cy + r * sin(angles[i]).toFloat())
        }

        for (i in 0..2) {
            val isActive = (activeAxis == i)
            drawCircle(primary.copy(alpha = if (isActive) 0.2f else 0.1f), if (isActive) 32f else 26f, points[i])
            drawCircle(if (isActive) primary else onSurface, if (isActive) 18f else 14f, points[i])
        }

        for (i in 0..2) {
            val sign = if (values[i] >= 0) "+" else ""
            val label = textMeasurer.measure(labels[i], labelStyle)
            val value = textMeasurer.measure("${sign}${"%.1f".format(values[i])}", valueStyle)
            val labelR = outerR * 0.78f
            val lx = cx + labelR * cos(angles[i]).toFloat()
            val ly = cy + labelR * sin(angles[i]).toFloat()

            drawText(label, onSurface.copy(alpha = 0.6f), Offset(lx - label.size.width / 2f, ly - label.size.height - 2f))
            drawText(value, onSurface, Offset(lx - value.size.width / 2f, ly + 2f))
        }
    }
}

@Composable
private fun ArbitraryResponseEqMode(
    responseGains: Array<MutableFloatState>,
    enabled: Boolean,
    onEnable: () -> Unit,
    onResponseChange: (Int, Float) -> Unit,
    onSmooth: () -> Unit,
    onReset: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.eq_arbitrary_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(R.string.eq_arbitrary_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.eq_magnitude_response),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
            ) {
                EqLineGraph(
                    responseGains = responseGains,
                    labels = RESPONSE_LABELS,
                    labeledIndices = RESPONSE_LABEL_INDICES,
                    enabled = enabled,
                    onEnable = onEnable,
                    onResponseChange = onResponseChange,
                    modifier = Modifier
                        .width(1080.dp)
                        .height(260.dp),
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            FilledTonalButton(onClick = onSmooth) {
                Text(stringResource(R.string.eq_smooth_curve))
            }
            FilledTonalButton(onClick = onReset) {
                Icon(Icons.Rounded.Replay, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.eq_reset))
            }
        }
    }
}

@Composable
private fun EqLineGraph(
    responseGains: Array<MutableFloatState>,
    labels: Array<String>,
    labeledIndices: Set<Int>,
    enabled: Boolean,
    onEnable: () -> Unit,
    onResponseChange: (Int, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outline = MaterialTheme.colorScheme.outlineVariant
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall
    val decibelUnit = stringResource(R.string.eq_db_unit)
    var activeNode by remember { mutableIntStateOf(-1) }

    fun valueToY(value: Float, top: Float, height: Float): Float {
        val normalized = ((value.coerceIn(-1200f, 1200f) + 1200f) / 2400f)
        return top + height * (1f - normalized)
    }

    fun yToValue(y: Float, top: Float, height: Float): Float {
        val normalized = (1f - ((y - top) / height)).coerceIn(0f, 1f)
        return ((normalized * 2400f) - 1200f).roundToInt().toFloat()
    }

    fun bandX(index: Int, start: Float, width: Float): Float =
        start + width * index / responseGains.lastIndex.coerceAtLeast(1)

    fun nearestBand(x: Float, width: Int): Int {
        val graphLeft = 72f
        val graphRight = width - 72f
        val graphWidth = graphRight - graphLeft
        return ((x - graphLeft) / graphWidth * responseGains.lastIndex)
            .roundToInt()
            .coerceIn(0, responseGains.lastIndex)
    }

    fun updateBandFromY(band: Int, y: Float, height: Int) {
        val graphTop = 24f
        val graphBottom = height - 72f
        val graphHeight = graphBottom - graphTop
        if (!enabled) onEnable()
        onResponseChange(band, yToValue(y, graphTop, graphHeight))
    }

    Canvas(
        modifier = modifier.pointerInput(enabled) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val band = nearestBand(down.position.x, size.width)
                var isVerticalAdjustment = false

                val slopChange = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
                    if (abs(overSlop.y) > abs(overSlop.x)) {
                        isVerticalAdjustment = true
                        activeNode = band
                        updateBandFromY(band, change.position.y, size.height)
                        change.consume()
                    }
                }
                if (slopChange == null || !isVerticalAdjustment) {
                    activeNode = -1
                    return@awaitEachGesture
                }

                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    val delta = change.positionChange()
                    if (abs(delta.y) >= abs(delta.x) * 0.45f) {
                        updateBandFromY(band, change.position.y, size.height)
                        change.consume()
                    }
                }
                activeNode = -1
            }
        },
    ) {
        val graphLeft = 72f
        val graphRight = size.width - 72f
        val graphTop = 24f
        val graphBottom = size.height - 72f
        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop
        val zeroY = valueToY(0f, graphTop, graphHeight)

        for (step in -12..12 step 6) {
            val y = valueToY(step * 100f, graphTop, graphHeight)
            drawLine(
                color = outline.copy(alpha = if (step == 0) 0.5f else 0.22f),
                start = Offset(graphLeft, y),
                end = Offset(graphRight, y),
                strokeWidth = if (step == 0) 2f else 1f,
            )
        }

        for (index in responseGains.indices) {
            val x = bandX(index, graphLeft, graphWidth)
            drawLine(
                color = outline.copy(alpha = 0.16f),
                start = Offset(x, graphTop),
                end = Offset(x, graphBottom),
                strokeWidth = 1f,
            )
        }

        val points = responseGains.indices.map { index ->
            Offset(
                x = bandX(index, graphLeft, graphWidth),
                y = valueToY(responseGains[index].floatValue, graphTop, graphHeight),
            )
        }
        val linePath = smoothPath(points)
        val fillPath = smoothFillPath(points, zeroY)

        drawPath(fillPath, primary.copy(alpha = if (enabled) 0.14f else 0.05f))
        drawLine(
            color = primary.copy(alpha = if (enabled) 0.24f else 0.08f),
            start = Offset(graphLeft, zeroY),
            end = Offset(graphRight, zeroY),
            strokeWidth = 3f,
            cap = StrokeCap.Round,
        )
        drawPath(
            linePath,
            primary.copy(alpha = if (enabled) 1f else 0.38f),
            style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        responseGains.indices.forEach { index ->
            val x = bandX(index, graphLeft, graphWidth)
            val y = valueToY(responseGains[index].floatValue, graphTop, graphHeight)
            val isActive = index == activeNode
            drawCircle(
                color = primaryContainer.copy(alpha = if (enabled) 0.8f else 0.24f),
                radius = if (isActive) 13f else 9f,
                center = Offset(x, y),
            )
            drawCircle(
                color = primary.copy(alpha = if (enabled) 1f else 0.38f),
                radius = if (isActive) 6f else 4f,
                center = Offset(x, y),
            )
            if (isActive) {
                val gain = responseGains[index].floatValue / 100f
                val sign = if (gain >= 0f) "+" else ""
                drawCenteredText(
                    "${labels[index]}  $sign${"%.1f".format(gain)} $decibelUnit",
                    labelStyle,
                    onSurface,
                    Offset(x, (y - 24f).coerceAtLeast(graphTop - 10f)),
                    textMeasurer,
                )
            }
            if (index in labeledIndices) {
                drawCenteredText(labels[index], labelStyle, onSurfaceVariant, Offset(x, graphBottom + 28f), textMeasurer)
            }
        }

        drawCenteredText("12", labelStyle, onSurface.copy(alpha = 0.7f), Offset(graphLeft - 40f, graphTop), textMeasurer)
        drawCenteredText("0", labelStyle, onSurface.copy(alpha = 0.7f), Offset(graphLeft - 40f, zeroY), textMeasurer)
        drawCenteredText("-12", labelStyle, onSurface.copy(alpha = 0.7f), Offset(graphLeft - 40f, graphBottom), textMeasurer)
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (index in 0 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]
        val controlX = (current.x + next.x) / 2f
        path.cubicTo(
            controlX,
            current.y,
            controlX,
            next.y,
            next.x,
            next.y,
        )
    }
    return path
}

private fun smoothFillPath(points: List<Offset>, baselineY: Float): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, baselineY)
    path.lineTo(points.first().x, points.first().y)
    for (index in 0 until points.lastIndex) {
        val current = points[index]
        val next = points[index + 1]
        val controlX = (current.x + next.x) / 2f
        path.cubicTo(
            controlX,
            current.y,
            controlX,
            next.y,
            next.x,
            next.y,
        )
    }
    path.lineTo(points.last().x, baselineY)
    path.close()
    return path
}

private fun simpleToneGain(frequency: Float, bass: Float, mid: Float, treble: Float): Float {
    val bassGain = (bass * 50f).coerceIn(-1200f, 1200f)
    val midGain = (mid * 50f).coerceIn(-1200f, 1200f)
    val trebleGain = (treble * 50f).coerceIn(-1200f, 1200f)
    val logFrequency = ln(frequency)
    val bassWeight = lowShelfWeight(frequency, 250f)
    val midWeight = gaussianWeight(logFrequency, ln(1000f), 0.82f)
    val trebleWeight = highShelfWeight(frequency, 4000f)
    return (bassGain * bassWeight + midGain * midWeight + trebleGain * trebleWeight)
        .coerceIn(-1200f, 1200f)
}

private fun lowShelfWeight(frequency: Float, cornerFrequency: Float): Float =
    (1f / (1f + (frequency / cornerFrequency).pow(1.35f))).coerceIn(0f, 1f)

private fun highShelfWeight(frequency: Float, cornerFrequency: Float): Float =
    (1f / (1f + (cornerFrequency / frequency).pow(1.35f))).coerceIn(0f, 1f)

private fun gaussianWeight(value: Float, center: Float, width: Float): Float {
    val distance = (value - center) / width
    return exp(-0.5f * distance * distance).coerceIn(0f, 1f)
}

private fun interpolateMagnitude(frequencies: FloatArray, values: FloatArray, targetFrequency: Float): Float {
    if (frequencies.isEmpty() || values.isEmpty()) return 0f
    if (targetFrequency <= frequencies.first()) return values.first()
    if (targetFrequency >= frequencies.last()) return values.last()
    val upperIndex = frequencies.indexOfFirst { it >= targetFrequency }
    val lowerFrequency = frequencies[upperIndex - 1]
    val upperFrequency = frequencies[upperIndex]
    val lowerValue = values[upperIndex - 1]
    val upperValue = values[upperIndex]
    val position = (ln(targetFrequency) - ln(lowerFrequency)) / (ln(upperFrequency) - ln(lowerFrequency))
    return lowerValue + (upperValue - lowerValue) * position
}

private fun DrawScope.drawCenteredText(
    text: String,
    style: TextStyle,
    color: Color,
    center: Offset,
    textMeasurer: TextMeasurer,
) {
    val measuredText = textMeasurer.measure(text, style)
    drawText(
        measuredText,
        color,
        Offset(center.x - measuredText.size.width / 2f, center.y - measuredText.size.height / 2f),
    )
}
