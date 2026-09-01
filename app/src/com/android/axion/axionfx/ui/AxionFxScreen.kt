@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.android.axion.axionfx.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.android.axion.axionfx.R
import com.android.axion.axionfx.ui.screens.ConvolverScreen
import com.android.axion.axionfx.ui.screens.DashboardScreen
import com.android.axion.axionfx.ui.screens.DeviceProfilesScreen
import com.android.axion.axionfx.ui.screens.EqualizerScreen
import com.android.axion.axionfx.ui.screens.ExciterScreen
import com.android.axion.axionfx.ui.screens.FirEqScreen
import com.android.axion.axionfx.ui.screens.MultibandScreen
import com.android.axion.axionfx.ui.screens.PresetsScreen

@Composable
fun AxionFxScreen(viewModel: AxionFxViewModel) {
    var currentScreen by rememberSaveable { mutableStateOf<String?>(null) }
    val dashboardScrollState = rememberScrollState()
    val motionScheme = MaterialTheme.motionScheme
    val isDualPane = LocalConfiguration.current.screenWidthDp >= 840
    val saveableStateHolder = rememberSaveableStateHolder()

    if (isDualPane) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(0.5f)) {
                saveableStateHolder.SaveableStateProvider(key = "dashboard") {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { currentScreen = it },
                        scrollState = dashboardScrollState,
                    )
                }
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            )
            Box(modifier = Modifier.weight(0.5f)) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(motionScheme.defaultEffectsSpec())
                            .togetherWith(fadeOut(motionScheme.fastEffectsSpec()))
                            .using(SizeTransform(clip = false))
                    },
                    label = "detail"
                ) { screen ->
                    saveableStateHolder.SaveableStateProvider(key = screen ?: "placeholder") {
                        DetailContent(screen = screen, viewModel = viewModel, onBack = { currentScreen = null })
                    }
                }
            }
        }
    } else {
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = {
                val enterSpec = motionScheme.defaultSpatialSpec<IntOffset>()
                val exitSpec = motionScheme.fastSpatialSpec<IntOffset>()
                if (targetState != null) {
                    (slideInHorizontally(enterSpec) { it / 3 } + fadeIn(motionScheme.defaultEffectsSpec()))
                        .togetherWith(slideOutHorizontally(exitSpec) { -it / 3 } + fadeOut(motionScheme.fastEffectsSpec()))
                } else {
                    (slideInHorizontally(enterSpec) { -it / 3 } + fadeIn(motionScheme.defaultEffectsSpec()))
                        .togetherWith(slideOutHorizontally(exitSpec) { it / 3 } + fadeOut(motionScheme.fastEffectsSpec()))
                }.using(SizeTransform(clip = false))
            },
            label = "screen"
        ) { screen ->
            saveableStateHolder.SaveableStateProvider(key = screen ?: "dashboard") {
                when (screen) {
                    null -> DashboardScreen(
                        viewModel = viewModel,
                        onNavigate = { currentScreen = it },
                        scrollState = dashboardScrollState,
                    )
                    else -> DetailContent(screen = screen, viewModel = viewModel, onBack = { currentScreen = null })
                }
            }
        }
    }
}

@Composable
private fun DetailContent(
    screen: String?,
    viewModel: AxionFxViewModel,
    onBack: () -> Unit,
) {
    when (screen) {
        "equalizer" -> EqualizerScreen(viewModel = viewModel, onBackClick = onBack)
        "fir_eq" -> FirEqScreen(viewModel = viewModel, onBackClick = onBack)
        "multiband" -> MultibandScreen(viewModel = viewModel, onBackClick = onBack)
        "exciter" -> ExciterScreen(viewModel = viewModel, onBackClick = onBack)
        "convolver" -> ConvolverScreen(viewModel = viewModel, onBackClick = onBack)
        "presets" -> PresetsScreen(viewModel = viewModel, onBackClick = onBack)
        "device_profiles" -> DeviceProfilesScreen(viewModel = viewModel, onBackClick = onBack)
        else -> DetailPlaceholder()
    }
}

@Composable
private fun DetailPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Tune,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.detail_placeholder),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}
