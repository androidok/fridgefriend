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

package com.pyamsoft.fridge.detail

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.db.category.FridgeCategoryQueryDao
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.guarantee.EntryGuarantee
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemDeleteDao
import com.pyamsoft.fridge.db.item.FridgeItemInsertDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemRealtime
import com.pyamsoft.fridge.db.persist.PersistentCategories
import com.pyamsoft.fridge.preference.DetailPreferences
import com.pyamsoft.fridge.preference.SearchPreferences
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.core.ResultWrapper
import com.pyamsoft.pydroid.util.PreferenceListener
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

internal class DetailInteractor
@Inject
internal constructor(
    private val entryGuarantee: EntryGuarantee,
    private val itemQueryDao: FridgeItemQueryDao,
    private val itemInsertDao: FridgeItemInsertDao,
    private val itemDeleteDao: FridgeItemDeleteDao,
    private val itemRealtime: FridgeItemRealtime,
    private val entryQueryDao: FridgeEntryQueryDao,
    private val persistentCategories: PersistentCategories,
    private val categoryQueryDao: FridgeCategoryQueryDao,
    private val detailPreferences: DetailPreferences,
    private val searchPreferences: SearchPreferences,
) {

  @CheckResult
  suspend fun isQuickAddEnabled(): Boolean =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.isQuickAddEnabled()
      }

  @CheckResult
  suspend fun isSearchEmptyStateShowAll(): Boolean =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext searchPreferences.isEmptyStateAllItems()
      }

  @CheckResult
  suspend fun isZeroCountConsideredConsumed(): Boolean =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.isZeroCountConsideredConsumed()
      }

  @CheckResult
  suspend fun getExpiringSoonRange(): Int =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.getExpiringSoonRange()
      }

  @CheckResult
  suspend fun isSameDayExpired(): Boolean =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.isSameDayExpired()
      }

  @CheckResult
  suspend fun listenForExpiringSoonRangeChanged(
      onChange: (newRange: Int) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.watchForExpiringSoonChange(onChange)
      }

  @CheckResult
  suspend fun listenForSearchEmptyStateChanged(
      onChange: (newState: Boolean) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext searchPreferences.watchForSearchEmptyStateChange(onChange)
      }

  @CheckResult
  suspend fun listenForSameDayExpiredChanged(
      onChange: (newSameDay: Boolean) -> Unit
  ): PreferenceListener =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext detailPreferences.watchForSameDayExpiredChange(onChange)
      }

  @CheckResult
  suspend fun findSameNamedItems(item: FridgeItem): ResultWrapper<Collection<FridgeItem>> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        // Return no results for an item we already HAVE
        // This will currently only prompt the user when they mark something as NEED that they
        // already own
        if (item.presence() == Presence.HAVE) {
          return@withContext ResultWrapper.success(emptyList())
        }

        return@withContext try {
          ResultWrapper.success(
              itemQueryDao.querySameNameDifferentPresence(
                  false, item.name().trim().lowercase(), item.presence()))
        } catch (e: Throwable) {
          Timber.e(e, "Error finding same named items: $item")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun findSimilarNamedItems(item: FridgeItem): ResultWrapper<Collection<FridgeItem>> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext try {
          ResultWrapper.success(
              itemQueryDao.querySimilarNamedItems(
                  false, item.id(), item.name().trim().lowercase()))
        } catch (e: Throwable) {
          Timber.e(e, "Error finding similar named items: $item")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun loadAllCategories(): ResultWrapper<List<FridgeCategory>> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          persistentCategories.guaranteePersistentCategoriesCreated()
          ResultWrapper.success(categoryQueryDao.query(false))
        } catch (e: Throwable) {
          Timber.e(e, "Error loading all categories")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun loadEntry(entryId: FridgeEntry.Id): ResultWrapper<FridgeEntry> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        ensureEntryId(entryId)
        return@withContext try {
          ResultWrapper.success(entryQueryDao.query(false).first { it.id() == entryId })
        } catch (e: Throwable) {
          Timber.e(e, "Error loading entry $entryId")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun resolveItem(
      itemId: FridgeItem.Id,
      entryId: FridgeEntry.Id,
      presence: Presence,
  ): ResultWrapper<FridgeItem> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        ensureEntryId(entryId)
        return@withContext if (itemId.isEmpty()) {
          createNewItem(entryId, presence)
        } else {
          loadItem(itemId, entryId)
        }
      }

  /** Create a new FridgeItem */
  @CheckResult
  private fun createNewItem(
      entryId: FridgeEntry.Id,
      presence: Presence
  ): ResultWrapper<FridgeItem> {
    Enforcer.assertOffMainThread()
    ensureEntryId(entryId)
    return try {
      ResultWrapper.success(FridgeItem.create(entryId = entryId, presence = presence))
    } catch (e: Throwable) {
      Timber.e(e, "Error creating new item $entryId")
      ResultWrapper.failure(e)
    }
  }

  /**
   * If the itemId parameter is blank, this will crash the app.
   *
   * This should only be called on items that already exist in the db.
   */
  @CheckResult
  suspend fun loadItem(
      itemId: FridgeItem.Id,
      entryId: FridgeEntry.Id,
  ): ResultWrapper<FridgeItem> =
      getItems(entryId, true).map { items -> items.first { it.id() == itemId } }

  @CheckResult
  suspend fun getItems(entryId: FridgeEntry.Id, force: Boolean): ResultWrapper<List<FridgeItem>> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          ensureEntryId(entryId)
          ResultWrapper.success(itemQueryDao.query(force, entryId))
        } catch (e: Throwable) {
          Timber.e(e, "Error getting items for entry: $entryId")
          ResultWrapper.failure(e)
        }
      }

  @CheckResult
  suspend fun getAllItems(force: Boolean): ResultWrapper<List<FridgeItem>> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext try {
          ResultWrapper.success(itemQueryDao.query(force))
        } catch (e: Throwable) {
          Timber.e(e, "Error getting all items")
          ResultWrapper.failure(e)
        }
      }

  suspend fun listenForAllChanges(onChange: suspend (event: FridgeItemChangeEvent) -> Unit) =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        return@withContext itemRealtime.listenForChanges(onChange)
      }

  suspend fun listenForChanges(
      id: FridgeEntry.Id,
      onChange: suspend (event: FridgeItemChangeEvent) -> Unit,
  ) =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()
        ensureEntryId(id)
        return@withContext itemRealtime.listenForChanges(id, onChange)
      }

  @CheckResult
  suspend fun commit(item: FridgeItem): ResultWrapper<Unit> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          if (FridgeItem.isValidName(item.name())) {
            val entry = entryGuarantee.existing(item.entryId(), FridgeEntry.DEFAULT_NAME)
            Timber.d("Guarantee entry exists: $entry")
            commitItem(item)
          } else {
            Timber.w("Do not commit invalid name FridgeItem: $item")
          }
          ResultWrapper.success(Unit)
        } catch (e: Throwable) {
          Timber.e(e, "Error committing item: $item")
          ResultWrapper.failure(e)
        }
      }

  private suspend fun commitItem(item: FridgeItem) {
    Enforcer.assertOffMainThread()
    Timber.d("Insert or replace item [${item.id()}]: $item")
    if (itemInsertDao.insert(item)) {
      Timber.d("Item inserted: $item")
    } else {
      Timber.d("Item updated: $item")
    }
  }

  @CheckResult
  suspend fun delete(
      item: FridgeItem,
      offerUndo: Boolean,
  ): ResultWrapper<Unit> =
      withContext(context = Dispatchers.Default) {
        Enforcer.assertOffMainThread()

        return@withContext try {
          require(item.isReal()) { "Cannot delete item that is not real: $item" }
          Timber.d("Deleting item [${item.id()}]: $item")
          if (itemDeleteDao.delete(item, offerUndo)) {
            Timber.d("Item deleted: $item")
          }

          ResultWrapper.success(Unit)
        } catch (e: Throwable) {
          Timber.e(e, "Error deleting item: $item")
          ResultWrapper.failure(e)
        }
      }

  companion object {

    @JvmStatic
    private fun ensureEntryId(entryId: FridgeEntry.Id) {
      require(!entryId.isEmpty()) { "FridgeEntry.Id cannot be empty" }
    }
  }
}
