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

import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.detail.snackbar.CustomSnackbar
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiView
import javax.inject.Inject

class SearchFilter
@Inject
internal constructor(
    private val owner: LifecycleOwner,
    private val parent: ViewGroup,
) : UiView<DetailViewState, SearchViewEvent.FilterEvent>() {

  override fun render(state: UiRender<DetailViewState>) {
    state.mapChanged { it.undoable }.render(viewScope) { handleUndo(it) }
  }

  private fun handleUndo(undoable: DetailViewState.Undoable?) {
    if (undoable != null) {
      showUndoSnackbar(undoable)
    }
  }

  private fun showUndoSnackbar(undoable: DetailViewState.Undoable) {
    val item = undoable.item
    val canAddAgain = undoable.canQuickAdd

    val message =
        when {
          item.isConsumed() -> "Consumed ${item.name()}"
          item.isSpoiled() -> "${item.name()} spoiled"
          else -> "Removed ${item.name()}"
        }

    CustomSnackbar.Break.bindTo(owner) {
      long(
          parent,
          message,
          onHidden = { _, _ -> publish(SearchViewEvent.FilterEvent.ReallyDeleteItemNoUndo) }) {
        // If we have consumed/spoiled this item
        // We can offer it as 're-add'
        if ((item.isConsumed() || item.isSpoiled()) && canAddAgain) {
          setAction1("Again") { publish(SearchViewEvent.FilterEvent.AnotherOne(item)) }
        }

        // Restore the old item
        setAction2("Undo") { publish(SearchViewEvent.FilterEvent.UndoDeleteItem) }
      }
    }
  }
}
