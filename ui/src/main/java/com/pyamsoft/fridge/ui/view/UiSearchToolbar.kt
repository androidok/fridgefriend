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

package com.pyamsoft.fridge.ui.view

import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.CheckResult
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.view.doOnLayout
import androidx.core.view.iterator
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiViewEvent

abstract class UiSearchToolbar<E : Enum<E>, S : UiSearchToolbar.State<E>, V : UiViewEvent>
protected constructor(
    withToolbar: ((Toolbar) -> Unit) -> Unit,
) : UiToolbar<E, S, V>(withToolbar) {

  private val groupIdSearch = View.generateViewId()
  private val itemIdSearch = View.generateViewId()

  private var searchItem: MenuItem? = null

  private val publishHandler = Handler(Looper.getMainLooper())

  // NOTE(Peter): Hack because Android does not allow us to use Controlled view components like
  // React does by binding input and drawing to the render loop.
  //
  // This initialRenderPerformed variable allows us to set the initial state of a view once, and
  // bind listeners to
  // it because the state.item is only available in render instead of inflate. Once the firstRender
  // has set the view component up, the actual input will no longer be tracked via state render
  // events,
  // so the input is uncontrolled.
  private var initialRenderPerformed = false

  init {
    doOnInflate { withToolbar { toolbar -> initializeMenuItems(toolbar) } }

    doOnTeardown { withToolbar { toolbar -> toolbar.teardownMenu() } }
  }

  private fun initializeMenuItems(toolbar: Toolbar) {
    toolbar.doOnLayout { searchItem = toolbar.initSearchItem() }
  }

  override fun onRender(state: UiRender<S>) {
    super.onRender(state)
    state.mapChanged { it.toolbarSearch }.render(viewScope) { handleInitialSearch(it) }
  }

  private fun handleInitialSearch(search: String) {
    if (initialRenderPerformed) {
      return
    }
    initialRenderPerformed = true

    val item = searchItem ?: return
    val searchView = item.actionView as? SearchView ?: return

    if (search.isNotBlank()) {
      if (item.expandActionView()) {
        searchView.setQuery(search, true)
      }
    }
  }

  private fun publishSearch(query: String) {
    publishHandler.removeCallbacksAndMessages(null)
    publishHandler.postDelayed({ publishSearchEvent(query) }, SEARCH_PUBLISH_TIMEOUT)
  }

  private fun Toolbar.setVisibilityOfNonSearchItems(visible: Boolean) {
    for (item in this.menu) {
      if (item.itemId != itemIdSearch) {
        item.isVisible = visible
      }
    }
  }

  @CheckResult
  private fun Toolbar.initSearchItem(): MenuItem {
    val toolbar = this
    return this.menu.add(groupIdSearch, itemIdSearch, Menu.NONE, "Search").apply {
      setIcon(R.drawable.ic_search_24dp)
      setShowAsAction(
          MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW)
      setOnActionExpandListener(
          object : MenuItem.OnActionExpandListener {

            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
              // Need to post so this fires after all other UI work in toolbar
              toolbar.post { setVisibilityOfNonSearchItems(false) }
              return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
              // Need to post so this fires after all other UI work in toolbar
              toolbar.post { setVisibilityOfNonSearchItems(true) }
              return true
            }
          })
      actionView =
          SearchView(toolbar.context).apply {
            setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                  override fun onQueryTextSubmit(query: String): Boolean {
                    publishSearch(query)
                    return true
                  }

                  override fun onQueryTextChange(newText: String): Boolean {
                    publishSearch(newText)
                    return true
                  }
                })
          }
    }
  }

  private fun Toolbar.teardownMenu() {
    handler?.removeCallbacksAndMessages(null)
    publishHandler.removeCallbacksAndMessages(null)
    setVisibilityOfNonSearchItems(true)
    teardownSearch()
  }

  private fun Toolbar.teardownSearch() {
    this.menu.removeGroup(groupIdSearch)
    searchItem = null
  }

  @CheckResult
  protected fun isSearchExpanded(): Boolean {
    return searchItem?.isActionViewExpanded ?: false
  }

  protected abstract fun publishSearchEvent(search: String)

  companion object {
    private const val SEARCH_PUBLISH_TIMEOUT = 400L
  }

  interface State<E : Enum<E>> : UiToolbar.State<E> {
    val toolbarSearch: String
  }
}
