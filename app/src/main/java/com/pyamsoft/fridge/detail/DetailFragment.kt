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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.fridge.theme.R
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.arch.asFactory
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.newUiController
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.R as R2
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class DetailFragment : Fragment(), SnackbarContainer {

  @JvmField @Inject internal var container: DetailContainer? = null

  @JvmField @Inject internal var addNew: DetailAddItemView? = null

  @JvmField @Inject internal var toolbar: DetailToolbar? = null

  @JvmField @Inject internal var listFactory: DetailListViewModel.Factory? = null
  private val listViewModel by viewModels<DetailListViewModel> {
    listFactory.requireNotNull().asFactory(this)
  }

  @JvmField @Inject internal var addFactory: DetailAddViewModel.Factory? = null
  private val addViewModel by viewModels<DetailAddViewModel> {
    addFactory.requireNotNull().asFactory(this)
  }

  @JvmField @Inject internal var toolbarFactory: DetailToolbarViewModel.Factory? = null
  private val toolbarViewModel by viewModels<DetailToolbarViewModel> {
    toolbarFactory.requireNotNull().asFactory(this)
  }

  private var stateSaver: StateSaver? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R2.layout.layout_coordinator, container, false).apply {
      // Cover the existing Entry List
      setBackgroundResource(R.color.windowBackground)
    }
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)

    val entryId = FridgeEntry.Id(requireArguments().getString(ENTRY).requireNotNull())
    val presence = Presence.valueOf(requireArguments().getString(PRESENCE).requireNotNull())

    val binding = LayoutCoordinatorBinding.bind(view)
    Injector.obtainFromApplication<FridgeComponent>(view.context)
        .plusDetailComponent()
        .create(
            this,
            requireActivity(),
            viewLifecycleOwner,
            entryId,
            presence,
        )
        .plusDetailComponent()
        .create(binding.layoutCoordinator)
        .inject(this)

    val listSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            listViewModel,
            controller =
                newUiController {
                  return@newUiController when (it) {
                    is DetailControllerEvent.ListEvent.ExpandItem -> openExisting(it.item)
                  }
                },
            container.requireNotNull(),
        ) {
          return@createComponent when (it) {
            is DetailViewEvent.ListEvent.ChangeItemPresence ->
                listViewModel.handleCommitPresence(it.index)
            is DetailViewEvent.ListEvent.ConsumeItem -> listViewModel.handleConsume(it.index)
            is DetailViewEvent.ListEvent.DecreaseItemCount ->
                listViewModel.handleDecreaseCount(it.index)
            is DetailViewEvent.ListEvent.DeleteItem -> listViewModel.handleDelete(it.index)
            is DetailViewEvent.ListEvent.ForceRefresh -> listViewModel.handleRefreshList(true)
            is DetailViewEvent.ListEvent.IncreaseItemCount ->
                listViewModel.handleIncreaseCount(it.index)
            is DetailViewEvent.ListEvent.RestoreItem -> listViewModel.handleRestore(it.index)
            is DetailViewEvent.ListEvent.SpoilItem -> listViewModel.handleSpoil(it.index)
            is DetailViewEvent.ListEvent.ExpandItem -> listViewModel.handleExpand(it.index)
          }
        }

    val addSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            addViewModel,
            controller =
                newUiController {
                  return@newUiController when (it) {
                    is DetailControllerEvent.AddEvent.AddNew -> createItem(it.entryId, it.presence)
                  }
                },
            addNew.requireNotNull(),
        ) {
          return@createComponent when (it) {
            is DetailViewEvent.ButtonEvent.AddNew -> addViewModel.handleAddNew()
            is DetailViewEvent.ButtonEvent.AnotherOne -> addViewModel.handleAddAgain(it.item)
            is DetailViewEvent.ButtonEvent.ClearListError -> addViewModel.handleClearListError()
            is DetailViewEvent.ButtonEvent.ReallyDeleteItemNoUndo ->
                addViewModel.handleDeleteForever()
            is DetailViewEvent.ButtonEvent.UndoDeleteItem -> addViewModel.handleUndoDelete()
          }
        }

    val toolbarSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            toolbarViewModel,
            controller = newUiController {},
            toolbar.requireNotNull(),
        ) {
          return@createComponent when (it) {
            is DetailViewEvent.ToolbarEvent.Back -> requireActivity().onBackPressed()
            is DetailViewEvent.ToolbarEvent.UpdateSort -> toolbarViewModel.handleSort(it.type)
            is DetailViewEvent.ToolbarEvent.TopBarMeasured ->
                toolbarViewModel.handleTopBarMeasured(it.height)
            is DetailViewEvent.ToolbarEvent.TabSwitched ->
                toolbarViewModel.handleTabsSwitched(it.isHave)
            is DetailViewEvent.ToolbarEvent.Search -> toolbarViewModel.handleSearch(it.query)
          }
        }

    stateSaver =
        object : StateSaver {

          private val savers =
              arrayOf(
                  listSaver,
                  toolbarSaver,
                  addSaver,
              )

          override fun saveState(outState: Bundle) {
            savers.forEach { it.saveState(outState) }
          }

          override fun saveState(outState: UiSavedStateWriter) {
            savers.forEach { it.saveState(outState) }
          }
        }
  }

  override fun container(): CoordinatorLayout? {
    return addNew?.container()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    stateSaver?.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    addFactory = null
    listFactory = null
    toolbarFactory = null

    container = null
    addNew = null
    toolbar = null

    stateSaver = null
  }

  private fun createItem(entryId: FridgeEntry.Id, presence: Presence) {
    showExpandDialog(ExpandedItemDialog.createNew(entryId, presence))
  }

  private fun openExisting(item: FridgeItem) {
    showExpandDialog(ExpandedItemDialog.openExisting(item))
  }

  private fun showExpandDialog(dialogFragment: DialogFragment) {
    dialogFragment.show(requireActivity(), ExpandedItemDialog.TAG)
  }

  companion object {

    private const val ENTRY = "entry"
    private const val PRESENCE = "presence"

    @JvmStatic
    @CheckResult
    fun newInstance(
        entryId: FridgeEntry.Id,
        filterPresence: Presence,
    ): Fragment {
      return DetailFragment().apply {
        arguments =
            Bundle().apply {
              putString(ENTRY, entryId.id)
              putString(PRESENCE, filterPresence.name)
            }
      }
    }
  }
}
