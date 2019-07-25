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

package com.pyamsoft.fridge.db.room.dao.entry

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.room.entity.RoomFridgeEntry
import timber.log.Timber

@Dao
internal abstract class RoomFridgeEntryInsertDao internal constructor() : FridgeEntryInsertDao {

  override suspend fun insert(o: FridgeEntry) {
    Timber.d("ROOM: Entry Insert: $o")
    val roomEntry = RoomFridgeEntry.create(o)
    daoInsert(roomEntry)
  }

  @Insert(onConflict = OnConflictStrategy.ABORT)
  internal abstract fun daoInsert(entry: RoomFridgeEntry)

}
