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

package com.pyamsoft.fridge.search

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailListStateModel
import com.pyamsoft.fridge.detail.DetailTopOffset
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import com.pyamsoft.pydroid.bus.EventBus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SearchToolbarViewModel
@AssistedInject
internal constructor(
  private val delegate: DetailListStateModel,
  private val topOffsetBus: EventBus<DetailTopOffset>,
  @Assisted savedState: UiSavedState,
) :
    UiSavedStateViewModel<DetailViewState, UnitControllerEvent>(
        savedState,
        initialState = delegate.initialState,
    ) {

  init {
    val scope = viewModelScope
    val job =
        delegate.bindState(
            scope, Renderable { state -> state.render(scope) { scope.setState { it } } })
    doOnCleared { job.cancel() }
    doOnCleared { delegate.clear() }

    delegate.initialize(scope)

    viewModelScope.launch(context = Dispatchers.Default) {
      val sort = restoreSavedState(KEY_SORT) { UiToolbar.SortType.CREATED_TIME }
      handleSort(sort)
    }

    viewModelScope.launch(context = Dispatchers.Default) {
      val search = restoreSavedState(KEY_SEARCH) { "" }
      handleSearch(search)
    }
  }

  fun handleSort(newSort: UiToolbar.SortType) {
    delegate.handleUpdateSort(viewModelScope, newSort) { putSavedState(KEY_SORT, it.name) }
  }

  fun handleTopBarMeasured(height: Int) {
    viewModelScope.launch(context = Dispatchers.Default) {
      topOffsetBus.send(DetailTopOffset(height))
    }
  }

  fun handleTabsSwitched(isHave: Boolean) {
    val presence = if (isHave) FridgeItem.Presence.HAVE else FridgeItem.Presence.NEED
    delegate.handlePresenceSwitch(viewModelScope, presence)
  }

  fun handleSearch(query: String) {
    delegate.handleUpdateSearch(viewModelScope, query) { putSavedState(KEY_SEARCH, it) }
  }

  companion object {
    private const val KEY_SEARCH = "search"
    private const val KEY_SORT = "sort"
  }

  @AssistedFactory
  interface Factory : UiSavedStateViewModelProvider<SearchToolbarViewModel> {
    override fun create(savedState: UiSavedState): SearchToolbarViewModel
  }
}
