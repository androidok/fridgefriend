/*
 * Copyright 2019 Peter Kenji Yamanaka
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.pyamsoft.fridge.locator.map.osm.popup.zone

import android.location.Location
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoViewEvent.ZoneFavoriteAction
import com.pyamsoft.fridge.locator.map.osm.popup.zone.ZoneInfoViewState.ZoneCached
import com.pyamsoft.pydroid.arch.UiViewModel
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Polygon
import javax.inject.Inject

internal class ZoneInfoViewModel @Inject internal constructor(
    private val interactor: ZoneInfoInteractor,
    zone: NearbyZone
) : UiViewModel<ZoneInfoViewState, ZoneInfoViewEvent, ZoneInfoControllerEvent>(
    initialState = ZoneInfoViewState(
        myLocation = null,
        polygon = null,
        cached = null
    )
) {

    private val zoneId = zone.id()

    init {
        doOnInit {
            findCachedZoneIfExists()
            listenForRealtime()
        }
    }

    private fun listenForRealtime() {
        viewModelScope.launch {
            interactor.listenForNearbyCacheChanges(
                onInsert = { zone ->
                    if (zone.id() == zoneId) {
                        setState { copy(cached = ZoneCached(true)) }
                    }
                },
                onDelete = { zone ->
                    if (zone.id() == zoneId) {
                        setState { copy(cached = ZoneCached(false)) }
                    }
                })
        }
    }

    private fun findCachedZoneIfExists() {
        viewModelScope.launch {
            val cachedZones = interactor.getAllCachedZones()
            setState { copy(cached = ZoneCached(cached = cachedZones.any { it.id() == zoneId })) }
        }
    }

    override fun handleViewEvent(event: ZoneInfoViewEvent) {
        return when (event) {
            is ZoneFavoriteAction -> handleZoneFavoriteAction(event.zone, event.add)
        }
    }

    private fun handleZoneFavoriteAction(
        zone: NearbyZone,
        add: Boolean
    ) {
        viewModelScope.launch {
            if (add) {
                interactor.insertZoneIntoDb(zone)
            } else {
                interactor.deleteZoneFromDb(zone)
            }
        }
    }

    fun updatePolygon(polygon: Polygon) {
        setState { copy(polygon = polygon) }
    }

    fun handleLocationUpdate(location: Location?) {
        setState { copy(myLocation = location) }
    }
}
