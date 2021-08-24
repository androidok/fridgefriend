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
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.detail.base.UpdateDelegate
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.fridge.ui.view.asEditData
import com.pyamsoft.highlander.highlander
import com.pyamsoft.pydroid.arch.UiStateModel
import com.pyamsoft.pydroid.bus.EventConsumer
import com.pyamsoft.pydroid.core.ResultWrapper
import com.pyamsoft.pydroid.util.PreferenceListener
import java.util.Date
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// Used to share this single state model between entire fragment scope
// Used for Detail module and Search module
@FragmentScope
class DetailListStateModel
@Inject
internal constructor(
    private val interactor: DetailInteractor,
    private val entryId: FridgeEntry.Id,
    private val bottomOffsetBus: EventConsumer<BottomOffset>,
    private val topOffsetBus: EventConsumer<DetailTopOffset>,
    listItemPresence: FridgeItem.Presence,
) :
    UiStateModel<DetailViewState>(
        initialState =
            DetailViewState(
                entry = null,
                search = "".asEditData(),
                isLoading = false,
                showing = DetailViewState.Showing.FRESH,
                sort = UiToolbar.SortType.CREATED_TIME,
                listError = null,
                undoable = null,
                expirationRange = null,
                isSameDayExpired = null,
                isShowAllItemsEmptyState = null,
                listItemPresence = listItemPresence,
                items = emptyList(),
                showAllEntries = entryId.isEmpty(),
                topOffset = 0,
                bottomOffset = 0,
            ),
    ) {

  private val updateDelegate = UpdateDelegate(interactor) { handleError(it) }

  private var expirationListener: PreferenceListener? = null
  private var sameDayListener: PreferenceListener? = null
  private var searchEmptyStateListener: PreferenceListener? = null

  private val undoRunner =
      highlander<ResultWrapper<Unit>, FridgeItem> { item ->
        require(item.isReal()) { "Cannot undo for non-real item: $item" }
        interactor.commit(item.invalidateSpoiled().invalidateConsumption())
      }

  private val refreshRunner =
      highlander<ResultWrapper<List<FridgeItem>>, Boolean> { force ->
        if (state.showAllEntries) {
          interactor.getAllItems(force)
        } else {
          interactor.getItems(entryId, force)
        }
      }

  fun initialize(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      bottomOffsetBus.onEvent { setState { copy(bottomOffset = it.height) } }
    }

    scope.launch(context = Dispatchers.Default) {
      topOffsetBus.onEvent { setState { copy(topOffset = it.height) } }
    }

    scope.launch(context = Dispatchers.Default) {
      if (!state.showAllEntries) {
        interactor
            .loadEntry(entryId)
            .onSuccess { setState { copy(entry = it) } }
            .onFailure { Timber.e(it, "Failed to initialize entry") }
            .onFailure { setState { copy(entry = null) } }
      }
    }

    scope.launch(context = Dispatchers.Default) {
      val range = interactor.getExpiringSoonRange()
      setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
    }

    scope.launch(context = Dispatchers.Default) {
      val isSame = interactor.isSameDayExpired()
      setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(isSame)) }
    }

    scope.launch(context = Dispatchers.Default) {
      val show = interactor.isSearchEmptyStateShowAll()
      setState { copy(isShowAllItemsEmptyState = DetailViewState.ShowAllItemsEmptyState(show)) }
    }

    scope.launch(context = Dispatchers.Default) {
      expirationListener =
          interactor.listenForExpiringSoonRangeChanged { range ->
            setState { copy(expirationRange = DetailViewState.ExpirationRange(range)) }
          }
    }

    scope.launch(context = Dispatchers.Default) {
      sameDayListener =
          interactor.listenForSameDayExpiredChanged { same ->
            setState { copy(isSameDayExpired = DetailViewState.IsSameDayExpired(same)) }
          }
    }

    scope.launch(context = Dispatchers.Default) {
      searchEmptyStateListener =
          interactor.listenForSearchEmptyStateChanged { show ->
            setState {
              copy(isShowAllItemsEmptyState = DetailViewState.ShowAllItemsEmptyState(show))
            }
          }
    }

    scope.launch(context = Dispatchers.Default) {
      if (state.showAllEntries) {
        interactor.listenForAllChanges { handleRealtime(it) }
      } else {
        interactor.listenForChanges(entryId) { handleRealtime(it) }
      }
    }

    handleRefreshList(scope, false)
  }

  override fun clear() {
    super.clear()
    updateDelegate.teardown()

    sameDayListener?.cancel()
    sameDayListener = null

    expirationListener?.cancel()
    expirationListener = null
  }

  private fun CoroutineScope.handleRealtime(event: FridgeItemChangeEvent) =
      when (event) {
        is FridgeItemChangeEvent.Insert -> handleRealtimeInsert(event.item)
        is FridgeItemChangeEvent.Update -> handleRealtimeUpdate(event.item)
        is FridgeItemChangeEvent.Delete -> handleRealtimeDelete(event.item, event.offerUndo)
      }

  private fun insertOrUpdate(
      items: MutableList<FridgeItem>,
      item: FridgeItem,
  ) {
    if (!checkExists(items, item)) {
      items.add(item)
    } else {
      for ((index, oldItem) in items.withIndex()) {
        if (oldItem.id() == item.id()) {
          items[index] = item
          break
        }
      }
    }
  }

  @CheckResult
  private suspend fun FridgeItem.asUndoable(): DetailViewState.Undoable {
    val canQuickAdd = interactor.isQuickAddEnabled()
    return DetailViewState.Undoable(item = this, canQuickAdd = canQuickAdd)
  }

  private fun CoroutineScope.handleRealtimeInsert(item: FridgeItem) {
    setState {
      val newItems = items.toMutableList().also { insertOrUpdate(it, item) }
      regenerateItems(newItems)
    }
  }

  private fun CoroutineScope.handleRealtimeUpdate(item: FridgeItem) {
    setState {
      val newItems = items.toMutableList().also { insertOrUpdate(it, item) }
      regenerateItems(newItems)
          .copy(
              // Show undo banner if we are archiving this item, otherwise no-op
              undoable = if (item.isArchived()) item.asUndoable() else undoable)
    }
  }

  private fun CoroutineScope.handleRealtimeDelete(item: FridgeItem, offerUndo: Boolean) {
    setState {
      val newItems = items.filterNot { it.id() == item.id() }
      regenerateItems(newItems)
          .copy(
              // Show undo banner
              undoable = if (offerUndo) item.asUndoable() else null)
    }
  }

  @CheckResult
  private fun DetailViewState.regenerateItems(items: List<FridgeItem>): DetailViewState {
    val newItems = prepareListItems(items)
    return copy(items = newItems)
  }

  private fun CoroutineScope.handleListRefreshed(items: List<FridgeItem>) {
    setState { regenerateItems(items) }
  }

  private fun CoroutineScope.handleListRefreshError(throwable: Throwable) {
    setState { copy(listError = throwable) }
  }

  private fun CoroutineScope.handleError(throwable: Throwable?) {
    setState { copy(listError = throwable) }
  }

  @CheckResult
  private fun DetailViewState.prepareListItems(items: List<FridgeItem>): List<FridgeItem> {
    val dateSorter =
        Comparator<FridgeItem> { o1, o2 ->
          when (sort) {
            UiToolbar.SortType.CREATED_TIME -> o1.createdTime().compareTo(o2.createdTime())
            UiToolbar.SortType.NAME -> o1.name().compareTo(o2.name(), ignoreCase = true)
            UiToolbar.SortType.PURCHASE_DATE -> o1.purchaseTime().compareTo(o2.purchaseTime())
            UiToolbar.SortType.EXPIRATION_DATE -> o1.expireTime().compareTo(o2.expireTime())
          }
        }

    return filterValid(items)
        .filter { it.presence() == listItemPresence }
        .sortedWith(dateSorter)
        .toList()
  }

  // Compare dates which may be null
  // Null dates come after non-null dates
  @CheckResult
  private fun Date?.compareTo(other: Date?): Int {
    return if (this == null && other == null) 0
    else {
      if (other == null) -1 else this?.compareTo(other) ?: 1
    }
  }

  private fun updateCount(scope: CoroutineScope, item: FridgeItem) {
    if (!item.isArchived()) {
      updateDelegate.updateItem(scope, item)
    }
  }

  private fun changePresence(
      scope: CoroutineScope,
      oldItem: FridgeItem,
      newPresence: FridgeItem.Presence
  ) {
    updateDelegate.updateItem(scope, oldItem.presence(newPresence))
  }

  fun handleUpdateSort(
      scope: CoroutineScope,
      newSort: UiToolbar.SortType,
      andThen: suspend (UiToolbar.SortType) -> Unit,
  ) {
    scope.setState(
        stateChange = { copy(sort = newSort) },
        andThen = { newState ->
          handleRefreshList(this, false)
          andThen(newState.sort)
        })
  }

  fun handlePresenceSwitch(scope: CoroutineScope, presence: FridgeItem.Presence) {
    scope.setState(
        stateChange = {
          copy(
              listItemPresence = presence,
              // Reset the showing
              showing = DetailViewState.Showing.FRESH,
              // Reset the sort
              sort = UiToolbar.SortType.CREATED_TIME,
          )
        },
        andThen = { handleRefreshList(this, false) })
  }

  fun handleDeleteForever(scope: CoroutineScope) {
    scope.setState { copy(undoable = null) }
  }

  fun handleUpdateSearch(
      scope: CoroutineScope,
      newSearch: String,
      andThen: suspend (String) -> Unit,
  ) {
    scope.setState(
        stateChange = {
          val cleanSearch =
              if (newSearch.isNotBlank()) newSearch.trim().asEditData() else "".asEditData(true)
          copy(search = cleanSearch)
        },
        andThen = { newState ->
          handleRefreshList(this, false)
          andThen(newState.search.text)
        })
  }

  private inline fun withItemAt(index: Int, block: (FridgeItem) -> Unit) {
    block(state.displayedItems[index])
  }

  fun handleDecreaseCount(scope: CoroutineScope, index: Int) {
    withItemAt(index) { item ->
      val newCount = item.count() - 1
      val newItem = item.count(max(1, newCount))
      updateCount(scope, newItem)

      if (newCount <= 0 && newItem.presence() == FridgeItem.Presence.HAVE) {
        scope.launch(context = Dispatchers.Default) {
          if (interactor.isZeroCountConsideredConsumed()) {
            handleConsume(this, index)
          }
        }
      }
    }
  }

  fun handleIncreaseCount(scope: CoroutineScope, index: Int) {
    withItemAt(index) { updateCount(scope, it.count(it.count() + 1)) }
  }

  fun handleUndoDelete(scope: CoroutineScope) {
    scope.launch(context = Dispatchers.Default) {
      val u = requireNotNull(state.undoable)
      val item = u.item
      undoRunner.call(item).onFailure { Timber.e(it, "Error undoing item: ${item.id()}") }
    }
  }

  fun handleToggleArchived(
      scope: CoroutineScope,
      andThen: suspend (DetailViewState.Showing) -> Unit
  ) {
    scope.setState(
        stateChange = {
          val newShowing =
              when (showing) {
                DetailViewState.Showing.FRESH -> DetailViewState.Showing.CONSUMED
                DetailViewState.Showing.CONSUMED -> DetailViewState.Showing.SPOILED
                DetailViewState.Showing.SPOILED -> DetailViewState.Showing.FRESH
              }
          copy(showing = newShowing)
        },
        andThen = { newState ->
          handleRefreshList(this, false)
          andThen(newState.showing)
        })
  }

  fun handleRefreshList(scope: CoroutineScope, force: Boolean) {
    scope.setState(
        stateChange = { copy(isLoading = true) },
        andThen = {
          refreshRunner
              .call(force)
              .onSuccess { handleListRefreshed(it) }
              .onFailure { Timber.e(it, "Error refreshing item list") }
              .onFailure { handleListRefreshError(it) }

          setState(stateChange = { copy(isLoading = false) })
        })
  }

  fun handleCommitPresence(scope: CoroutineScope, index: Int) {
    withItemAt(index) { changePresence(scope, it, it.presence().flip()) }
  }

  fun handleConsume(scope: CoroutineScope, index: Int) {
    withItemAt(index) { updateDelegate.consumeItem(scope, it) }
  }

  fun handleRestore(scope: CoroutineScope, index: Int) {
    withItemAt(index) { updateDelegate.restoreItem(scope, it) }
  }

  fun handleSpoil(scope: CoroutineScope, index: Int) {
    withItemAt(index) { updateDelegate.spoilItem(scope, it) }
  }

  fun handleDelete(scope: CoroutineScope, index: Int) {
    withItemAt(index) { updateDelegate.deleteItem(scope, it) }
  }

  fun handleClearListError(scope: CoroutineScope) {
    scope.handleError(null)
  }

  fun handleUpdateFilter(scope: CoroutineScope, showing: DetailViewState.Showing) {
    scope.setState { copy(showing = showing) }
  }

  fun handleAddAgain(scope: CoroutineScope, oldItem: FridgeItem) {
    scope.launch(context = Dispatchers.Default) {
      Timber.d("Add again - create a new item with copied fields")
      val newItem =
          FridgeItem.create(entryId = oldItem.entryId(), presence = FridgeItem.Presence.NEED)
              .name(oldItem.name())
              .count(oldItem.count())
              .apply { oldItem.categoryId()?.also { this.categoryId(it) } }

      interactor.commit(newItem)
    }
  }

  companion object {

    @JvmStatic
    @CheckResult
    private fun checkExists(
        items: List<FridgeItem>,
        item: FridgeItem,
    ): Boolean {
      return items.any { item.id() == it.id() && item.entryId() == it.entryId() }
    }
  }
}
