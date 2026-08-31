@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axionfx.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.axion.axionfx.R
import androidx.compose.ui.platform.LocalContext
import com.android.axion.axionfx.AxionFxController
import com.android.axion.axionfx.device.DeviceProfile
import com.android.axion.axionfx.device.DeviceProfileManager
import com.android.axion.axionfx.domain.EffectDefaults
import com.android.axion.axionfx.domain.EffectKeys
import com.android.axion.axionfx.service.AxionFxService
import com.android.axion.axionfx.ui.AxionFxViewModel
import com.android.axion.axionfx.ui.components.EffectSlider
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.ListPreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SwitchPreference

@Composable
fun DashboardScreen(
    viewModel: AxionFxViewModel,
    onNavigate: (String) -> Unit,
    scrollState: ScrollState = rememberScrollState(),
) {
    val bassModeOptions = listOf(
        "0" to stringResource(R.string.bass_mode_natural),
        "1" to stringResource(R.string.bass_mode_punch),
        "2" to stringResource(R.string.bass_mode_subwoofer),
    )

    val context = LocalContext.current
    val fx = viewModel.interactor
    val masterEnabled by AxionFxService.masterEnabledFlow.collectAsState()
    var outputGain by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.OUTPUT_GAIN, EffectDefaults.OUTPUT_GAIN).toFloat()) }

    var bassEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.BASS_ENABLED, EffectDefaults.BASS_ENABLED)) }
    var bassMode by remember { mutableIntStateOf(viewModel.loadInt(EffectKeys.BASS_MODE, EffectDefaults.BASS_MODE)) }
    var bassGain by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.BASS_GAIN, EffectDefaults.BASS_GAIN).toFloat()) }
    var widenerEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.WIDENER_ENABLED, EffectDefaults.WIDENER_ENABLED)) }
    var widenerWidth by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.WIDENER_WIDTH, EffectDefaults.WIDENER_WIDTH).toFloat()) }
    var reverbEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.REVERB_ENABLED, EffectDefaults.REVERB_ENABLED)) }
    var reverbWet by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.REVERB_WET, EffectDefaults.REVERB_WET).toFloat()) }
    var reverbRoomSize by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.REVERB_ROOM, EffectDefaults.REVERB_ROOM).toFloat()) }
    var compressorEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.COMPRESSOR_ENABLED, EffectDefaults.COMPRESSOR_ENABLED)) }
    var tubeEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.TUBE_ENABLED, EffectDefaults.TUBE_ENABLED)) }
    var tubeDrive by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.TUBE_DRIVE, EffectDefaults.TUBE_DRIVE).toFloat()) }
    var tubeMix by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.TUBE_MIX, EffectDefaults.TUBE_MIX).toFloat()) }
    var agcEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.AGC_ENABLED, EffectDefaults.AGC_ENABLED)) }
    var crossfeedEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.CROSSFEED_ENABLED, EffectDefaults.CROSSFEED_ENABLED)) }
    var crossfeedLevel by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.CROSSFEED_LEVEL, EffectDefaults.CROSSFEED_LEVEL).toFloat()) }
    var surroundEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.SURROUND_ENABLED, EffectDefaults.SURROUND_ENABLED)) }
    var spatialEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.SPATIAL_ENABLED, EffectDefaults.SPATIAL_ENABLED)) }
    var spatialWidth by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.SPATIAL_WIDTH, EffectDefaults.SPATIAL_WIDTH).toFloat()) }
    var limiterEnabled by remember { mutableStateOf(viewModel.loadBoolean(EffectKeys.LIMITER_ENABLED, EffectDefaults.LIMITER_ENABLED)) }

    // Hoisted variables that were previously local to PreferenceGroup items
    var outputPan by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.OUTPUT_PAN, EffectDefaults.OUTPUT_PAN).toFloat()) }
    var limiterThreshold by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.LIMITER_THRESHOLD, EffectDefaults.LIMITER_THRESHOLD).toFloat()) }
    var surroundDelay by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.SURROUND_DELAY, EffectDefaults.SURROUND_DELAY).toFloat()) }
    var surroundWidth by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.SURROUND_WIDTH, EffectDefaults.SURROUND_WIDTH).toFloat()) }
    var spatialBlend by remember { mutableFloatStateOf(viewModel.loadInt(EffectKeys.SPATIAL_BLEND, EffectDefaults.SPATIAL_BLEND).toFloat()) }

    val activeCount = viewModel.activeEffectCount()
    val isActive = masterEnabled

    LaunchedEffect(Unit) { AxionFxService.primeFromContext(context) }
    val deviceCategory by AxionFxService.currentDeviceCategoryFlow.collectAsState()
    val deviceName by AxionFxService.currentDeviceNameFlow.collectAsState()
    val appliedPresetName by AxionFxService.appliedPresetNameFlow.collectAsState()
    val boundPresetName = DeviceProfileManager.displayName(
        DeviceProfileManager.getBinding(
            viewModel.repo.prefs,
            DeviceProfile.Fixed(deviceCategory),
        )
    )
    val chipSubtitle = appliedPresetName
        ?: boundPresetName
        ?: stringResource(R.string.dashboard_device_chip_unbound)

    val motionScheme = MaterialTheme.motionScheme
    val statusColor by animateColorAsState(
        targetValue = if (isActive) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = motionScheme.defaultEffectsSpec(),
        label = "statusColor"
    )

    var showResetDialog by remember { mutableStateOf(false) }
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_dialog_title)) },
            text = { Text(stringResource(R.string.reset_dialog_message)) },
            confirmButton = {
                FilledTonalButton(onClick = {
                    showResetDialog = false
                    fx.resetAll()
                    AxionFxService.updateMasterEnabledFlow(EffectDefaults.MASTER_ENABLED)
                    outputGain = EffectDefaults.OUTPUT_GAIN.toFloat()
                    outputPan = EffectDefaults.OUTPUT_PAN.toFloat()
                    bassEnabled = EffectDefaults.BASS_ENABLED
                    bassMode = EffectDefaults.BASS_MODE
                    bassGain = EffectDefaults.BASS_GAIN.toFloat()
                    widenerEnabled = EffectDefaults.WIDENER_ENABLED
                    widenerWidth = EffectDefaults.WIDENER_WIDTH.toFloat()
                    reverbEnabled = EffectDefaults.REVERB_ENABLED
                    reverbWet = EffectDefaults.REVERB_WET.toFloat()
                    reverbRoomSize = EffectDefaults.REVERB_ROOM.toFloat()
                    compressorEnabled = EffectDefaults.COMPRESSOR_ENABLED
                    tubeEnabled = EffectDefaults.TUBE_ENABLED
                    tubeDrive = EffectDefaults.TUBE_DRIVE.toFloat()
                    tubeMix = EffectDefaults.TUBE_MIX.toFloat()
                    agcEnabled = EffectDefaults.AGC_ENABLED
                    crossfeedEnabled = EffectDefaults.CROSSFEED_ENABLED
                    crossfeedLevel = EffectDefaults.CROSSFEED_LEVEL.toFloat()
                    surroundEnabled = EffectDefaults.SURROUND_ENABLED
                    surroundDelay = EffectDefaults.SURROUND_DELAY.toFloat()
                    surroundWidth = EffectDefaults.SURROUND_WIDTH.toFloat()
                    spatialEnabled = EffectDefaults.SPATIAL_ENABLED
                    spatialWidth = EffectDefaults.SPATIAL_WIDTH.toFloat()
                    spatialBlend = EffectDefaults.SPATIAL_BLEND.toFloat()
                    limiterEnabled = EffectDefaults.LIMITER_ENABLED
                    limiterThreshold = EffectDefaults.LIMITER_THRESHOLD.toFloat()
                }) { Text(stringResource(R.string.reset_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.reset_cancel))
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            FilledTonalButton(onClick = { showResetDialog = true }) {
                Icon(
                    Icons.Rounded.Replay,
                    contentDescription = stringResource(R.string.reset_all),
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            var showStats by remember { mutableStateOf(false) }
            val audioStats = remember { viewModel.getAudioStats() }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerLow
                    )
                    .clickable { showStats = !showStats }
                    .padding(24.dp)
            ) {
                val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(statusColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = if (isActive) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isActive) stringResource(R.string.status_active)
                                    else stringResource(R.string.status_disabled),
                                style = MaterialTheme.typography.titleMedium,
                                color = contentColor,
                            )
                            Text(
                                text = if (isActive) stringResource(R.string.status_effects_active, activeCount, outputGain.toInt())
                                else stringResource(R.string.status_tap_to_enable),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isActive) {
                            val statsChevron by animateFloatAsState(
                                targetValue = if (showStats) 180f else 0f,
                                animationSpec = motionScheme.fastSpatialSpec(),
                                label = "statsChevron",
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(contentColor.copy(alpha = 0.12f))
                                    .padding(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .graphicsLayer { rotationZ = statsChevron },
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(contentColor.copy(alpha = 0.10f))
                            .clickable { onNavigate("device_profiles") }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    ) {
                        Icon(
                            imageVector = categoryIcon(deviceCategory),
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = deviceName ?: stringResource(categoryTitleRes(deviceCategory)),
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = chipSubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = contentColor.copy(alpha = 0.75f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = contentColor.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    AnimatedVisibility(
                        visible = showStats && isActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut(),
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            HorizontalDivider(
                                color = contentColor.copy(alpha = 0.15f),
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                            AudioStatRow(stringResource(R.string.stat_sample_rate), audioStats.sampleRate, contentColor)
                            AudioStatRow(stringResource(R.string.stat_buffer_size), audioStats.bufferSize, contentColor)
                            AudioStatRow(stringResource(R.string.stat_channels), audioStats.channels, contentColor)
                            AudioStatRow(stringResource(R.string.stat_bit_depth), audioStats.bitDepth, contentColor)
                            AudioStatRow(stringResource(R.string.stat_session), audioStats.sessionId, contentColor)
                            AudioStatRow(stringResource(R.string.stat_latency), audioStats.latency, contentColor)
                        }
                    }
                }
            }



            PreferenceGroup {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.master_enable_title),
                        summary = stringResource(R.string.master_enable_summary),
                        checked = masterEnabled,
                        onCheckedChange = {
                            fx.setMasterEnabled(it)
                            AxionFxService.updateMasterEnabledFlow(it)
                            if (it) AxionFxService.start(context) else AxionFxService.stop(context)
                        },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.category_library), collapsible = true, initiallyExpanded = false) {
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_presets),
                        summary = stringResource(R.string.nav_presets_summary),
                        icon = Icons.Rounded.LibraryMusic,
                        onClick = { onNavigate("presets") },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_device_profiles),
                        summary = stringResource(R.string.nav_device_profiles_summary),
                        icon = Icons.Rounded.Headphones,
                        onClick = { onNavigate("device_profiles") },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.category_master), collapsible = true, initiallyExpanded = false) {
                item {
                    EffectSlider(
                        title = stringResource(R.string.output_gain_title),
                        summary = stringResource(R.string.output_gain_summary),
                        value = outputGain,
                        valueRange = 0f..200f,
                        unit = "%",
                        enabled = isActive,
                        onValueChange = {
                            outputGain = it
                            fx.setOutputGain(it.toInt())
                        },
                        onReset = {
                            outputGain = 100f
                            fx.setOutputGain(100)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.output_pan_title),
                        summary = stringResource(R.string.output_pan_summary),
                        value = outputPan,
                        valueRange = -100f..100f,
                        enabled = isActive,
                        onValueChange = {
                            outputPan = it
                            fx.setOutputPan(it.toInt())
                        },
                        onReset = {
                            outputPan = EffectDefaults.OUTPUT_PAN.toFloat()
                            fx.setOutputPan(EffectDefaults.OUTPUT_PAN)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.limiter_title),
                        summary = stringResource(R.string.limiter_summary),
                        checked = limiterEnabled,
                        enabled = isActive,
                        onCheckedChange = {
                            limiterEnabled = it
                            fx.setLimiterEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.limiter_threshold_title),
                        summary = stringResource(R.string.limiter_threshold_summary),
                        value = limiterThreshold,
                        valueRange = -60f..0f,
                        unit = "dB",
                        enabled = isActive && limiterEnabled,
                        onValueChange = {
                            limiterThreshold = it
                            fx.setLimiterThreshold(it.toInt())
                        },
                        onReset = {
                            limiterThreshold = EffectDefaults.LIMITER_THRESHOLD.toFloat()
                            fx.setLimiterThreshold(EffectDefaults.LIMITER_THRESHOLD)
                        },
                    )
                }
            }


            PreferenceGroup(title = stringResource(R.string.category_effects), collapsible = true, initiallyExpanded = false) {
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_equalizer),
                        summary = stringResource(R.string.nav_equalizer_summary),
                        icon = Icons.Rounded.GraphicEq,
                        onClick = { onNavigate("equalizer") },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_fir_eq),
                        summary = stringResource(R.string.nav_fir_eq_summary),
                        icon = Icons.Rounded.Tune,
                        onClick = { onNavigate("fir_eq") },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_multiband),
                        summary = stringResource(R.string.nav_multiband_summary),
                        icon = Icons.Rounded.Equalizer,
                        onClick = { onNavigate("multiband") },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.nav_convolver),
                        summary = stringResource(R.string.nav_convolver_summary),
                        icon = Icons.Rounded.Memory,
                        onClick = { onNavigate("convolver") },
                    )
                }
            }


            PreferenceGroup(title = stringResource(R.string.category_bass), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.bass_boost_title),
                        summary = stringResource(R.string.bass_boost_summary),
                        checked = bassEnabled,
                        onCheckedChange = {
                            bassEnabled = it
                            fx.setBassEnabled(it)
                        },
                    )
                }
                item {
                    ListPreference(
                        title = stringResource(R.string.bass_mode_title),
                        summary = bassModeOptions.find { it.first == bassMode.toString() }?.second,
                        options = bassModeOptions,
                        value = bassMode.toString(),
                        onValueChange = {
                            bassMode = it.toInt()
                            fx.setBassMode(bassMode)
                        },
                        enabled = bassEnabled,
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.bass_gain_title),
                        summary = stringResource(R.string.bass_gain_summary),
                        value = bassGain,
                        valueRange = 0f..1500f,
                        unit = "cB",
                        enabled = bassEnabled,
                        onValueChange = {
                            bassGain = it
                            fx.setBassGain(it.toInt())
                        },
                        onReset = {
                            bassGain = 0f
                            fx.setBassGain(0)
                        },
                    )
                }
            }


            PreferenceGroup(title = stringResource(R.string.category_stereo), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.widener_title),
                        summary = stringResource(R.string.widener_summary),
                        checked = widenerEnabled,
                        onCheckedChange = {
                            widenerEnabled = it
                            fx.setWidenerEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.widener_width_title),
                        summary = stringResource(R.string.widener_width_summary),
                        value = widenerWidth,
                        valueRange = 0f..300f,
                        unit = "%",
                        enabled = widenerEnabled,
                        onValueChange = {
                            widenerWidth = it
                            fx.setWidenerWidth(it.toInt())
                        },
                        onReset = {
                            widenerWidth = 100f
                            fx.setWidenerWidth(100)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.crossfeed_title),
                        summary = stringResource(R.string.crossfeed_summary),
                        checked = crossfeedEnabled,
                        onCheckedChange = {
                            crossfeedEnabled = it
                            fx.setCrossfeedEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.crossfeed_level_title),
                        summary = stringResource(R.string.crossfeed_level_summary),
                        value = crossfeedLevel,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = crossfeedEnabled,
                        onValueChange = {
                            crossfeedLevel = it
                            fx.setCrossfeedLevel(it.toInt())
                        },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.category_surround), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.surround_title),
                        summary = stringResource(R.string.surround_summary),
                        checked = surroundEnabled,
                        onCheckedChange = {
                            surroundEnabled = it
                            fx.setSurroundEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.surround_delay_title),
                        summary = stringResource(R.string.surround_delay_summary),
                        value = surroundDelay,
                        valueRange = 100f..4000f,
                        unit = "ms/100",
                        enabled = surroundEnabled,
                        onValueChange = {
                            surroundDelay = it
                            fx.setSurroundDelay(it.toInt())
                        },
                        onReset = {
                            surroundDelay = EffectDefaults.SURROUND_DELAY.toFloat()
                            fx.setSurroundDelay(EffectDefaults.SURROUND_DELAY)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.surround_width_title),
                        summary = stringResource(R.string.surround_width_summary),
                        value = surroundWidth,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = surroundEnabled,
                        onValueChange = {
                            surroundWidth = it
                            fx.setSurroundWidth(it.toInt())
                        },
                        onReset = {
                            surroundWidth = EffectDefaults.SURROUND_WIDTH.toFloat()
                            fx.setSurroundWidth(EffectDefaults.SURROUND_WIDTH)
                        },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.category_spatial), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.spatial_title),
                        summary = stringResource(R.string.spatial_summary),
                        checked = spatialEnabled,
                        onCheckedChange = {
                            spatialEnabled = it
                            fx.setSpatialEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.spatial_blend_title),
                        summary = stringResource(R.string.spatial_blend_summary),
                        value = spatialBlend,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = spatialEnabled,
                        onValueChange = {
                            spatialBlend = it
                            fx.setSpatialBlend(it.toInt())
                        },
                        onReset = {
                            spatialBlend = EffectDefaults.SPATIAL_BLEND.toFloat()
                            fx.setSpatialBlend(EffectDefaults.SPATIAL_BLEND)
                        },
                    )
                }
            }


            PreferenceGroup(title = stringResource(R.string.category_dynamics), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.compressor_title),
                        summary = stringResource(R.string.compressor_summary),
                        checked = compressorEnabled,
                        onCheckedChange = {
                            compressorEnabled = it
                            fx.setCompressorEnabled(it)
                        },
                    )
                }
                item {
                    SwitchPreference(
                        title = stringResource(R.string.agc_title),
                        summary = stringResource(R.string.agc_summary),
                        checked = agcEnabled,
                        onCheckedChange = {
                            agcEnabled = it
                            fx.setAgcEnabled(it)
                        },
                    )
                }
            }


            PreferenceGroup(title = stringResource(R.string.category_reverb), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.reverb_title),
                        summary = stringResource(R.string.reverb_summary),
                        checked = reverbEnabled,
                        onCheckedChange = {
                            reverbEnabled = it
                            fx.setReverbEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.reverb_room_title),
                        summary = stringResource(R.string.reverb_room_summary),
                        value = reverbRoomSize,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = reverbEnabled,
                        onValueChange = {
                            reverbRoomSize = it
                            fx.setReverbRoomSize(it.toInt())
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.reverb_wet_title),
                        summary = stringResource(R.string.reverb_wet_summary),
                        value = reverbWet,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = reverbEnabled,
                        onValueChange = {
                            reverbWet = it
                            fx.setReverbWet(it.toInt())
                        },
                    )
                }
            }

            PreferenceGroup(title = stringResource(R.string.category_saturation), collapsible = true, initiallyExpanded = false) {
                item {
                    SwitchPreference(
                        title = stringResource(R.string.tube_title),
                        summary = stringResource(R.string.tube_summary),
                        checked = tubeEnabled,
                        onCheckedChange = {
                            tubeEnabled = it
                            fx.setTubeEnabled(it)
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.tube_drive_title),
                        summary = stringResource(R.string.tube_drive_summary),
                        value = tubeDrive,
                        valueRange = 10f..500f,
                        enabled = tubeEnabled,
                        onValueChange = {
                            tubeDrive = it
                            fx.setTubeDrive(it.toInt())
                        },
                    )
                }
                item {
                    EffectSlider(
                        title = stringResource(R.string.tube_mix_title),
                        summary = stringResource(R.string.tube_mix_summary),
                        value = tubeMix,
                        valueRange = 0f..100f,
                        unit = "%",
                        enabled = tubeEnabled,
                        onValueChange = {
                            tubeMix = it
                            fx.setTubeMix(it.toInt())
                        },
                    )
                }
                item {
                    ClickablePreference(
                        title = stringResource(R.string.exciter_nav_title),
                        summary = stringResource(R.string.exciter_nav_summary),
                        onClick = { onNavigate("exciter") },
                    )
                }
            }

            Text(
                text = stringResource(R.string.power_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AudioStatRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
