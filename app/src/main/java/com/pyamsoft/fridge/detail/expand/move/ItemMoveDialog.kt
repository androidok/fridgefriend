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

package com.pyamsoft.fridge.detail.expand.move

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryViewState
import com.pyamsoft.fridge.entry.ReadOnlyEntryList
import com.pyamsoft.fridge.entry.ReadOnlyListEvents
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.newUiController
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.R
import com.pyamsoft.pydroid.ui.app.makeFullscreen
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.widget.shadow.DropshadowView
import javax.inject.Inject

internal class ItemMoveDialog : AppCompatDialogFragment() {

  @JvmField @Inject internal var factory: FridgeViewModelFactory? = null
  private val viewModel by viewModels<ItemMoveViewModel> { factory.requireNotNull().create(this) }
  private val listViewModel by viewModels<ItemMoveListViewModel> {
    factory.requireNotNull().create(this)
  }

  @JvmField @Inject internal var toolbar: MoveItemToolbar? = null

  @JvmField @Inject internal var list: ReadOnlyEntryList? = null

  private var stateSaver: StateSaver? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R.layout.layout_constraint, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    makeFullscreen()

    val binding = LayoutConstraintBinding.bind(view)
    val itemId = FridgeItem.Id(requireArguments().getString(ITEM).requireNotNull())
    val entryId = FridgeEntry.Id(requireArguments().getString(ENTRY).requireNotNull())

    val parent = binding.layoutConstraint
    Injector.obtainFromApplication<FridgeComponent>(view.context)
        .plusItemMoveComponent()
        .create(
            requireActivity(),
            viewLifecycleOwner,
            itemId,
            entryId,
        )
        .plusItemMoveComponent()
        .create(parent)
        .inject(this)

    val list = list.requireNotNull()
    val toolbar = toolbar.requireNotNull()
    val dropshadow = DropshadowView.createTyped<ItemMoveViewState, ItemMoveViewEvent>(parent)

    val listSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            controller =
                newUiController {
                  return@newUiController when (it) {
                    is ItemMoveListControllerEvent.Selected -> handleEntrySelect(it.entry)
                  }
                },
            list,
        ) {
          return@createComponent when (it) {
            is ReadOnlyListEvents.ForceRefresh -> listViewModel.handleRefreshList()
            is ReadOnlyListEvents.Select -> listViewModel.handleSelectEntry(it.index)
          }
        }

    val moveSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            controller =
                newUiController {
                  return@newUiController when (it) {
                    is ItemMoveControllerEvent.Close -> dismiss()
                  }
                },
            toolbar,
            dropshadow,
        ) {
          return@createComponent when (it) {
            is ItemMoveViewEvent.ChangeSort -> handleSort(it.sort)
            is ItemMoveViewEvent.Close -> dismiss()
            is ItemMoveViewEvent.SearchQuery -> handleSearch(it.search)
          }
        }

    stateSaver =
        object : StateSaver {

          private val savers =
              arrayOf(
                  listSaver,
                  moveSaver,
              )

          override fun saveState(outState: Bundle) {
            savers.forEach { it.saveState(outState) }
          }

          override fun saveState(outState: UiSavedStateWriter) {
            savers.forEach { it.saveState(outState) }
          }
        }

    parent.layout {
      toolbar.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      dropshadow.also {
        connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      list.also {
        connect(it.id(), ConstraintSet.TOP, toolbar.id(), ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    factory = null

    list = null
    toolbar = null
    stateSaver = null
  }

  private fun handleSearch(search: String) {
    listViewModel.handleUpdateSearch(search)
  }

  private fun handleSort(sort: EntryViewState.Sorts) {
    listViewModel.handleUpdateSort(sort)
  }

  private fun handleEntrySelect(entry: FridgeEntry) {
    viewModel.handleMoveItemToEntry(entry)
  }

  companion object {

    const val TAG = "ItemMoveDialog"
    private const val ENTRY = "entry"
    private const val ITEM = "item"

    @JvmStatic
    @CheckResult
    fun newInstance(
        item: FridgeItem,
    ): DialogFragment {
      return ItemMoveDialog().apply {
        arguments =
            Bundle().apply {
              putString(ITEM, item.id().id)
              putString(ENTRY, item.entryId().id)
            }
      }
    }
  }
}
