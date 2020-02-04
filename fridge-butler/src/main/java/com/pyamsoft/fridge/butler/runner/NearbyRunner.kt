/*
 * Copyright 2020 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.butler.runner

import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ButlerPreferences
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemPreferences
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.store.NearbyStoreQueryDao
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.db.zone.NearbyZoneQueryDao
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

internal abstract class NearbyRunner protected constructor(
    handler: NotificationHandler,
    butler: Butler,
    butlerPreferences: ButlerPreferences,
    fridgeItemPreferences: FridgeItemPreferences,
    enforcer: Enforcer,
    fridgeEntryQueryDao: FridgeEntryQueryDao,
    fridgeItemQueryDao: FridgeItemQueryDao,
    private val storeDb: NearbyStoreQueryDao,
    private val zoneDb: NearbyZoneQueryDao
) : FridgeRunner(
    handler,
    butler,
    butlerPreferences,
    fridgeItemPreferences,
    enforcer,
    fridgeEntryQueryDao,
    fridgeItemQueryDao
) {

    protected suspend fun withNearbyData(func: suspend (stores: List<NearbyStore>, zones: List<NearbyZone>) -> Unit) =
        coroutineScope {
            val storeJob = async { storeDb.query(false) }
            val zoneJob = async { zoneDb.query(false) }
            val nearbyStores = storeJob.await()
            val nearbyZones = zoneJob.await()
            func(nearbyStores, nearbyZones)
        }
}