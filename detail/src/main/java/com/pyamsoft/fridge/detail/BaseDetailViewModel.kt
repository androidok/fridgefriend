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

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.Renderable
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel

abstract class BaseDetailViewModel<C : UiControllerEvent>
protected constructor(
    private val delegate: DetailListStateModel,
    savedState: UiSavedState,
) : UiSavedStateViewModel<DetailViewState, C>(savedState, initialState = delegate.initialState) {

  init {
    val scope = viewModelScope
    val job =
        delegate.bindState(
            scope, Renderable { state -> state.render(scope) { scope.setState { it } } })
    doOnCleared { job.cancel() }
    doOnCleared { delegate.clear() }

    delegate.initialize(scope)
  }

  fun handleAddAgain(item: FridgeItem) {
    delegate.handleAddAgain(viewModelScope, item)
  }

  fun handleDeleteForever() {
    delegate.handleDeleteForever(viewModelScope)
  }

  fun handleUndoDelete() {
    delegate.handleUndoDelete(viewModelScope)
  }
}
