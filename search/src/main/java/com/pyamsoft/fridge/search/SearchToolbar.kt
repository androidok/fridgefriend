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

import android.view.View
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class SearchToolbar
@Inject
internal constructor(
    toolbarActivity: ToolbarActivity,
) :
    UiToolbar<DetailViewState.Sorts, DetailViewState, SearchToolbarViewEvent>(
        withToolbar = { toolbarActivity.withToolbar(it) }) {

  private val itemIdPurchasedDate = View.generateViewId()
  private val itemIdExpirationDate = View.generateViewId()

  init {
    doOnInflate { toolbarActivity.withToolbar { toolbar -> toolbar.setUpEnabled(false) } }

    doOnTeardown { toolbarActivity.withToolbar { toolbar -> toolbar.setUpEnabled(false) } }
  }

  override fun publishSortEvent(sort: State.Sort<DetailViewState.Sorts>) {
    publish(SearchToolbarViewEvent.ChangeSort(sort.original))
  }

  override fun onGetSortForMenuItem(itemId: Int): DetailViewState.Sorts? =
      when (itemId) {
        itemIdCreatedDate -> DetailViewState.Sorts.CREATED
        itemIdName -> DetailViewState.Sorts.NAME
        itemIdPurchasedDate -> DetailViewState.Sorts.PURCHASED
        itemIdExpirationDate -> DetailViewState.Sorts.EXPIRATION
        else -> null
      }

  override fun onCreateAdditionalSortItems(adder: (Int, CharSequence) -> Unit) {
    adder(itemIdPurchasedDate, "Purchase Date")
    adder(itemIdExpirationDate, "Expiration Date")
  }

  override fun onRender(state: UiRender<DetailViewState>) {
    super.onRender(state)
    state.mapChanged { it.listItemPresence }.render(viewScope) { handleExtraSubItems(it) }
  }

  private fun handleExtraSubItems(presence: FridgeItem.Presence) {
    val showExtraMenuItems = presence == FridgeItem.Presence.HAVE
    setItemVisibility(itemIdPurchasedDate, showExtraMenuItems)
    setItemVisibility(itemIdExpirationDate, showExtraMenuItems)
  }
}
