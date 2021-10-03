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
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.isArchived
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent

data class DetailViewState
internal constructor(
    override val sort: UiToolbar.SortType,
    override val search: UiEditTextDelegate.Data,
    val showAllEntries: Boolean,
    val items: List<FridgeItem>,
    val isLoading: Boolean,
    val entry: FridgeEntry?,
    val listError: Throwable?,
    val undoable: Undoable?,
    val expirationRange: ExpirationRange?,
    val isSameDayExpired: IsSameDayExpired?,
    val isShowAllItemsEmptyState: ShowAllItemsEmptyState?,
    val listItemPresence: FridgeItem.Presence,
    val topOffset: Int,
    val bottomOffset: Int,
) : UiToolbar.State {

  // All currently displayed list items
  val displayedItems: List<FridgeItem>

  init {
    displayedItems = getOnlyVisibleItems()
  }
  @CheckResult
  private fun getOnlyVisibleItems(): List<FridgeItem> {
    // Default to showing all
    val showAllItems = if (showAllEntries) isShowAllItemsEmptyState?.showAll ?: true else true

    val query = search
    return items
        .asSequence()
        .filterNot { it.isArchived() }
        .filter { it.matchesQuery(query.text, showAllItems) }
        .toList()
  }

  override val isHave: Boolean = listItemPresence == FridgeItem.Presence.HAVE

  data class Undoable internal constructor(val item: FridgeItem, val canQuickAdd: Boolean)

  @CheckResult
  internal fun FridgeItem.matchesQuery(query: String, defaultValue: Boolean): Boolean {
    // Empty query always matches
    return if (query.isBlank()) defaultValue
    else {
      this.name().contains(query, ignoreCase = true)
    }
  }

  @CheckResult
  internal fun filterValid(items: List<FridgeItem>): Sequence<FridgeItem> {
    return items.asSequence().filterNot { it.isEmpty() }
  }

  data class ExpirationRange internal constructor(val range: Int)

  data class IsSameDayExpired internal constructor(val isSame: Boolean)

  data class ShowAllItemsEmptyState internal constructor(val showAll: Boolean)
}

sealed class DetailViewEvent : UiViewEvent {

  sealed class ButtonEvent : DetailViewEvent() {

    object AddNew : ButtonEvent()

    object ClearListError : ButtonEvent()

    object UndoDeleteItem : ButtonEvent()

    object ReallyDeleteItemNoUndo : ButtonEvent()

    data class AnotherOne internal constructor(val item: FridgeItem) : ButtonEvent()
  }

  sealed class ListEvent : DetailViewEvent() {

    object ForceRefresh : ListEvent()

    data class ExpandItem internal constructor(val index: Int) : ListEvent()

    data class IncreaseItemCount internal constructor(val index: Int) : ListEvent()

    data class DecreaseItemCount internal constructor(val index: Int) : ListEvent()

    data class ConsumeItem internal constructor(val index: Int) : ListEvent()

    data class DeleteItem internal constructor(val index: Int) : ListEvent()

    data class RestoreItem internal constructor(val index: Int) : ListEvent()

    data class SpoilItem internal constructor(val index: Int) : ListEvent()

    data class ChangeItemPresence internal constructor(val index: Int) : ListEvent()
  }

  sealed class ToolbarEvent : DetailViewEvent() {

    data class UpdateSort internal constructor(val type: UiToolbar.SortType) : ToolbarEvent()

    data class TopBarMeasured internal constructor(val height: Int) : ToolbarEvent()

    data class TabSwitched internal constructor(val isHave: Boolean) : ToolbarEvent()

    data class Search internal constructor(val query: String) : ToolbarEvent()

    object Back : ToolbarEvent()
  }
}

sealed class DetailControllerEvent : UiControllerEvent {

  sealed class ListEvent : DetailControllerEvent() {

    data class ExpandItem internal constructor(val item: FridgeItem) : ListEvent()
  }

  sealed class AddEvent : DetailControllerEvent() {

    data class AddNew
    internal constructor(val entryId: FridgeEntry.Id, val presence: FridgeItem.Presence) :
        AddEvent()
  }
}
