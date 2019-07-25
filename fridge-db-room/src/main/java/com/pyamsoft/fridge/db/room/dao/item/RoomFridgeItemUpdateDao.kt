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

package com.pyamsoft.fridge.db.room.dao.item

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Update
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemUpdateDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import timber.log.Timber

@Dao
internal abstract class RoomFridgeItemUpdateDao internal constructor() : FridgeItemUpdateDao {

  override suspend fun update(o: FridgeItem) {
    Timber.d("ROOM: Item Update: $o")
    val roomItem = RoomFridgeItem.create(o)
    daoUpdate(roomItem)
  }

  @Update(onConflict = OnConflictStrategy.ABORT)
  internal abstract fun daoUpdate(item: RoomFridgeItem)

}
