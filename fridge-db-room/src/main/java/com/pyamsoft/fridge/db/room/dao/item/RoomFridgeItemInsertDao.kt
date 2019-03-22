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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.room.dao.applyDbSchedulers
import com.pyamsoft.fridge.db.room.entity.RoomFridgeItem
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

@Dao
internal abstract class RoomFridgeItemInsertDao internal constructor() :
  FridgeItemInsertDao {

  override fun insert(item: FridgeItem): Completable {
    return Single.just(item)
      .map { RoomFridgeItem.create(it) }
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoInsert(it) }
          .applyDbSchedulers()
      }
  }

  @Insert(onConflict = OnConflictStrategy.FAIL)
  internal abstract fun daoInsert(item: RoomFridgeItem)

  override fun insertGroup(items: List<FridgeItem>): Completable {
    return Observable.fromIterable(items)
      .map { RoomFridgeItem.create(it) }
      .toList()
      .flatMapCompletable {
        return@flatMapCompletable Completable.fromAction { daoInsertGroup(it) }
          .applyDbSchedulers()
      }
  }

  @Insert(onConflict = OnConflictStrategy.FAIL)
  internal abstract fun daoInsertGroup(items: List<RoomFridgeItem>)

}