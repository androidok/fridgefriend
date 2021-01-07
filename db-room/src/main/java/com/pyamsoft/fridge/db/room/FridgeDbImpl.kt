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
 */

package com.pyamsoft.fridge.db.room

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategoryDb
import com.pyamsoft.fridge.db.entry.FridgeEntryDb
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.store.NearbyStoreDb
import com.pyamsoft.fridge.db.zone.NearbyZoneDb
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FridgeDbImpl @Inject internal constructor(
    private val itemDb: FridgeItemDb,
    private val entryDb: FridgeEntryDb,
    private val storeDb: NearbyStoreDb,
    private val zoneDb: NearbyZoneDb,
    private val categoryDb: FridgeCategoryDb,
) : FridgeDb {

    @CheckResult
    override fun items(): FridgeItemDb {
        return itemDb
    }

    @CheckResult
    override fun entries(): FridgeEntryDb {
        return entryDb
    }

    @CheckResult
    override fun stores(): NearbyStoreDb {
        return storeDb
    }

    @CheckResult
    override fun zones(): NearbyZoneDb {
        return zoneDb
    }

    @CheckResult
    override fun categories(): FridgeCategoryDb {
        return categoryDb
    }
}
