/*
 * Copyright 2025-2026 AxionOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.axion.axionfx.service

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.android.axion.axionfx.data.EffectRepository
import com.android.axion.axionfx.domain.EffectDefaults
import com.android.axion.axionfx.domain.EffectInteractor
import com.android.axion.axionfx.domain.EffectKeys

class AxionFxTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val prefs = AxionFxService.getPrefs(this)
        val repo = EffectRepository(prefs)
        val interactor = EffectInteractor(repo)
        val currentState = repo.getBoolean(EffectKeys.MASTER_ENABLED, EffectDefaults.MASTER_ENABLED)
        val newState = !currentState

        interactor.setMasterEnabled(newState)
        AxionFxService.updateMasterEnabledFlow(newState)

        AxionFxService.start(this)

        updateTile()
    }

    private fun updateTile() {
        val prefs = AxionFxService.getPrefs(this)
        val enabled = prefs.getBoolean(EffectKeys.MASTER_ENABLED, EffectDefaults.MASTER_ENABLED)
        val tile = qsTile ?: return
        tile.state = if (enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
