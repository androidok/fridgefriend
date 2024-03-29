/*
 * Copyright 2021 Peter Kenji Yamanaka
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

import android.content.Context
import androidx.annotation.CheckResult
import androidx.room.Room
import com.pyamsoft.cachify.Cached
import com.pyamsoft.cachify.MemoryCacheStorage
import com.pyamsoft.cachify.MultiCached1
import com.pyamsoft.cachify.MultiCached2
import com.pyamsoft.cachify.cachify
import com.pyamsoft.cachify.multiCachify
import com.pyamsoft.fridge.db.DbApi
import com.pyamsoft.fridge.db.DbCache
import com.pyamsoft.fridge.db.FridgeDb
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryDeleteDao
import com.pyamsoft.fridge.db.category.FridgeCategoryInsertDao
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.category.JsonMappableFridgeCategory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryDeleteDao
import com.pyamsoft.fridge.db.entry.FridgeEntryInsertDao
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.entry.JsonMappableFridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemDb
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.db.room.migrate.Migrate1To2
import dagger.Binds
import dagger.Module
import dagger.Provides
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Qualifier

@Qualifier @Retention(AnnotationRetention.BINARY) private annotation class InternalApi

@Module
abstract class RoomModule {

  @Binds @CheckResult internal abstract fun provideDb(impl: FridgeDbImpl): FridgeDb

  @Binds @CheckResult internal abstract fun provideDbCache(impl: FridgeDbImpl): DbCache

  @Module
  companion object {

    private const val DB_NAME = "fridge_room_db.db"
    private const val SIMILARITY_MIN_SCORE_CUTOFF = 0.45F
    private const val cacheTime = 10L
    private val cacheUnit = MINUTES

    @Provides
    @JvmStatic
    @CheckResult
    @InternalApi
    internal fun provideRoom(context: Context): RoomFridgeDb {
      val appContext = context.applicationContext
      return Room.databaseBuilder(appContext, RoomFridgeDbImpl::class.java, DB_NAME)
          .addMigrations(Migrate1To2)
          .build()
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideEntryCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeEntry>> {
      return cachify<List<FridgeEntry>>(
          storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
        db.roomEntryQueryDao().query(false).map { JsonMappableFridgeEntry.from(it.makeReal()) }
      }
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideAllItemsCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeItem>> {
      return cachify<List<FridgeItem>>(
          storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
        db.roomItemQueryDao().query(false).map { JsonMappableFridgeItem.from(it.makeReal()) }
      }
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideItemsByEntryCache(
        @InternalApi db: RoomFridgeDb
    ): MultiCached1<FridgeEntry.Id, List<FridgeItem>, FridgeEntry.Id> {
      return multiCachify(storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
          id ->
        db.roomItemQueryDao().query(false, id).map { JsonMappableFridgeItem.from(it.makeReal()) }
      }
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideSameNameDifferentPresenceCache(
        @InternalApi db: RoomFridgeDb
    ): MultiCached2<
        FridgeItemDb.QuerySameNameDifferentPresenceKey,
        List<FridgeItem>,
        String,
        FridgeItem.Presence> {
      return multiCachify(storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
          name,
          presence ->
        db.roomItemQueryDao().querySameNameDifferentPresence(false, name, presence).map {
          JsonMappableFridgeItem.from(it.makeReal())
        }
      }
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideSimilarlyNamedCache(
        @InternalApi db: RoomFridgeDb
    ): MultiCached2<
        FridgeItemDb.QuerySimilarNamedKey,
        List<FridgeItemDb.SimilarityScore>,
        FridgeItem.Id,
        String> {
      return multiCachify(storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
          id,
          name ->
        db.roomItemQueryDao()
            .querySimilarNamedItems(false, id, name)

            // Do this step in Kotlin because I don't know how to do this distance algo in SQL
            .asSequence()
            .map { JsonMappableFridgeItem.from(it.makeReal()) }
            .map { fridgeItem ->
              val itemName = fridgeItem.name().lowercase().trim()

              val score =
                  when {
                    itemName == name -> 1.0F
                    itemName.startsWith(name) -> 0.75F
                    itemName.endsWith(name) -> 0.5F
                    else -> itemName.withDistanceRatio(name)
                  }
              return@map FridgeItemDb.SimilarityScore(fridgeItem, score)
            }
            .filterNot { it.score < SIMILARITY_MIN_SCORE_CUTOFF }
            .sortedBy { it.score }
            .toList()
      }
    }

    @CheckResult
    private fun String.withDistanceRatio(str: String): Float {
      // Initialize a zero-matrix
      val s1Len = this.length
      val s2Len = str.length
      val rows = s1Len + 1
      val columns = s2Len + 1
      val matrix = Array(rows) { IntArray(columns) { 0 } }

      // Populate matrix with indices of each character in strings
      for (i in 1 until rows) {
        matrix[i][0] = i
      }

      for (j in 1 until columns) {
        matrix[0][j] = j
      }

      // Calculate the cost of deletes, inserts, and subs
      for (col in 1 until columns) {
        for (row in 1 until rows) {
          // If the character is the same in a given position, cost is 0, else cost is 2
          val cost = if (this[row - 1] == str[col - 1]) 0 else 2

          // The cost of a deletion, insertion, and substitution
          val deleteCost = matrix[row - 1][col] + 1
          val insertCost = matrix[row][col - 1] + 1
          val substitutionCost = matrix[row - 1][col - 1] + cost

          // Populate the matrix
          matrix[row][col] =
              kotlin.math.min(deleteCost, kotlin.math.min(insertCost, substitutionCost))
        }
      }

      // Calculate distance ratio
      val totalLength = (s1Len + s2Len)
      return (totalLength - matrix[s1Len][s2Len]).toFloat() / totalLength
    }

    @DbApi
    @Provides
    @JvmStatic
    @CheckResult
    internal fun provideCategoryCache(@InternalApi db: RoomFridgeDb): Cached<List<FridgeCategory>> {
      return cachify<List<FridgeCategory>>(
          storage = { listOf(MemoryCacheStorage.create(cacheTime, cacheUnit)) }) {
        db.roomCategoryQueryDao().query(false).map { JsonMappableFridgeCategory.from(it) }
      }
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomItemQueryDao(@InternalApi db: RoomFridgeDb): FridgeItemQueryDao {
      return db.roomItemQueryDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomItemInsertDao(@InternalApi db: RoomFridgeDb): FridgeItemInsertDao {
      return db.roomItemInsertDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomItemDeleteDao(@InternalApi db: RoomFridgeDb): FridgeItemDeleteDao {
      return db.roomItemDeleteDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomEntryQueryDao(@InternalApi db: RoomFridgeDb): FridgeEntryQueryDao {
      return db.roomEntryQueryDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomEntryInsertDao(@InternalApi db: RoomFridgeDb): FridgeEntryInsertDao {
      return db.roomEntryInsertDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomEntryDeleteDao(@InternalApi db: RoomFridgeDb): FridgeEntryDeleteDao {
      return db.roomEntryDeleteDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomCategoryQueryDao(
        @InternalApi db: RoomFridgeDb
    ): FridgeCategoryQueryDao {
      return db.roomCategoryQueryDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomCategoryInsertDao(
        @InternalApi db: RoomFridgeDb
    ): FridgeCategoryInsertDao {
      return db.roomCategoryInsertDao()
    }

    @DbApi
    @Provides
    @JvmStatic
    internal fun provideRoomCategoryDeleteDao(
        @InternalApi db: RoomFridgeDb
    ): FridgeCategoryDeleteDao {
      return db.roomCategoryDeleteDao()
    }
  }
}
