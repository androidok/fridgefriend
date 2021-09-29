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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailViewEvent
import com.pyamsoft.fridge.detail.expand.ExpandedItemDialog
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiSavedStateWriter
import com.pyamsoft.pydroid.arch.asFactory
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.arch.emptyController
import com.pyamsoft.pydroid.arch.newUiController
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.R as R2
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import com.pyamsoft.pydroid.ui.util.show
import javax.inject.Inject

internal class SearchFragment : Fragment() {

  @JvmField @Inject internal var container: SearchContainer? = null

  @JvmField @Inject internal var filter: SearchFilter? = null

  @JvmField @Inject internal var listViewFactory: SearchListViewModel.Factory? = null
  private val listViewModel by viewModels<SearchListViewModel> {
    listViewFactory.requireNotNull().asFactory(this)
  }

  @JvmField @Inject internal var filterViewFactory: SearchFilterViewModel.Factory? = null
  private val filterViewModel by viewModels<SearchFilterViewModel> {
    filterViewFactory.requireNotNull().asFactory(this)
  }

  @JvmField @Inject internal var toolbar: SearchToolbar? = null

  @JvmField @Inject internal var toolbarFactory: SearchToolbarViewModel.Factory? = null
  private val toolbarViewModel by viewModels<SearchToolbarViewModel> {
    toolbarFactory.requireNotNull().asFactory(this)
  }

  private var stateSaver: StateSaver? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R2.layout.layout_coordinator, container, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)

    val entryId = FridgeEntry.Id(requireArguments().getString(ENTRY).requireNotNull())
    val presence =
        FridgeItem.Presence.valueOf(requireArguments().getString(PRESENCE).requireNotNull())

    val binding = LayoutCoordinatorBinding.bind(view)
    Injector.obtainFromApplication<FridgeComponent>(view.context)
        .plusSearchComponent()
        .create(
            this,
            requireActivity(),
            viewLifecycleOwner,
            entryId,
            presence,
        )
        .plusSearchComponent()
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
                    is SearchControllerEvent.ExpandItem -> openExisting(it.item)
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
            is DetailViewEvent.ListEvent.ExpandItem -> listViewModel.handleExpand(it.index)
            is DetailViewEvent.ListEvent.ForceRefresh -> listViewModel.handleRefreshList(true)
            is DetailViewEvent.ListEvent.IncreaseItemCount ->
                listViewModel.handleIncreaseCount(it.index)
            is DetailViewEvent.ListEvent.RestoreItem -> listViewModel.handleRestore(it.index)
            is DetailViewEvent.ListEvent.SpoilItem -> listViewModel.handleSpoil(it.index)
          }
        }

    val filterSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            filterViewModel,
            controller = emptyController(),
            filter.requireNotNull(),
        ) {
          return@createComponent when (it) {
            is SearchViewEvent.FilterEvent.AnotherOne -> filterViewModel.handleAddAgain(it.item)
            is SearchViewEvent.FilterEvent.ChangeCurrentFilter ->
                filterViewModel.handleUpdateShowing()
            is SearchViewEvent.FilterEvent.ReallyDeleteItemNoUndo ->
                filterViewModel.handleDeleteForever()
            is SearchViewEvent.FilterEvent.UndoDeleteItem -> filterViewModel.handleUndoDelete()
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
            is SearchViewEvent.ToolbarEvent.UpdateSort -> toolbarViewModel.handleSort(it.type)
            is SearchViewEvent.ToolbarEvent.TopBarMeasured ->
                toolbarViewModel.handleTopBarMeasured(it.height)
            is SearchViewEvent.ToolbarEvent.TabSwitched ->
                toolbarViewModel.handleTabsSwitched(it.isHave)
            is SearchViewEvent.ToolbarEvent.Search -> toolbarViewModel.handleSearch(it.query)
          }
        }

    stateSaver =
        object : StateSaver {

          private val savers =
              arrayOf(
                  listSaver,
                  filterSaver,
                  toolbarSaver,
              )

          override fun saveState(outState: Bundle) {
            savers.forEach { it.saveState(outState) }
          }

          override fun saveState(outState: UiSavedStateWriter) {
            savers.forEach { it.saveState(outState) }
          }
        }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    stateSaver?.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    filterViewFactory = null
    listViewFactory = null
    toolbarFactory = null

    filter = null
    container = null
    toolbar = null

    stateSaver = null
  }

  private fun openExisting(item: FridgeItem) {
    ExpandedItemDialog.openExisting(item).show(requireActivity(), ExpandedItemDialog.TAG)
  }

  companion object {

    internal const val TAG = "SearchFragment"
    private const val ENTRY = "entry"
    private const val PRESENCE = "presence"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return SearchFragment().apply {
        arguments =
            Bundle().apply {
              putString(ENTRY, FridgeEntry.Id.EMPTY.id)
              putString(PRESENCE, FridgeItem.Presence.NEED.name)
            }
      }
    }
  }
}
