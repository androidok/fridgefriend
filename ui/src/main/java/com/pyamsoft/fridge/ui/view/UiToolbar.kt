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
import android.view.SubMenu
import android.view.View
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.children
import androidx.core.view.doOnLayout
import androidx.core.view.forEach
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import timber.log.Timber

abstract class UiToolbar<E : Enum<E>, S : UiToolbar.State<E>, V : UiViewEvent>
protected constructor(
    withToolbar: ((Toolbar) -> Unit) -> Unit,
) : UiView<S, V>() {

  private val groupIdSubmenu = View.generateViewId()
  private val itemIdSubmenu = View.generateViewId()
  private val itemIdTitle = View.generateViewId()

  protected val itemIdCreatedDate = View.generateViewId()
  protected val itemIdName = View.generateViewId()

  private var subMenu: SubMenu? = null

  private val publishHandler = Handler(Looper.getMainLooper())

  init {
    doOnInflate { withToolbar { toolbar -> initializeMenuItems(toolbar) } }

    doOnTeardown { withToolbar { toolbar -> toolbar.teardownMenu() } }
  }

  private fun initializeMenuItems(toolbar: Toolbar) {
    toolbar.doOnLayout { subMenu = toolbar.initSubmenu() }
  }

  override fun render(state: UiRender<S>) {
    state.mapChanged { it.toolbarSort }.render(viewScope) { handleSubmenu(it) }
    onRender(state)
  }

  private fun handleSubmenu(currentSort: State.Sort<E>) {
    subMenu?.let { subMenu ->
      subMenu.forEach { item ->
        getSortForMenuItem(item.itemId)?.also { sort ->
          if (currentSort == sort) {
            item.isChecked = true
          }
        }
      }

      // If nothing is checked, thats a no no
      if (subMenu.children.all { !it.isChecked }) {
        Timber.w("SORTS: NOTHING IS CHECKED: $currentSort")
      }
    }
  }

  @CheckResult
  private fun Toolbar.initSubmenu(): SubMenu {
    return this.menu.addSubMenu(groupIdSubmenu, itemIdSubmenu, Menu.NONE, "Sorts").also { subMenu ->
      subMenu.item.setIcon(R.drawable.ic_sort_24dp)
      subMenu.item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
      subMenu.add(Menu.NONE, itemIdTitle, Menu.NONE, "").apply {
        title = buildSpannedString { bold { append("Sorts") } }
      }
      subMenu.add(groupIdSubmenu, itemIdCreatedDate, 1, "Created Date").apply {
        setupSubMenuItem(this)
      }
      subMenu.add(groupIdSubmenu, itemIdName, 2, "Name").apply { setupSubMenuItem(this) }

      createAdditionalSortItems(subMenu)

      subMenu.setGroupCheckable(groupIdSubmenu, true, true)
    }
  }

  private fun setupSubMenuItem(menuItem: MenuItem) {
    menuItem.apply {
      isChecked = false
      val sort = requireNotNull(getSortForMenuItem(itemId))
      setOnMenuItemClickListener(clickListener(sort))
    }
  }

  private fun createAdditionalSortItems(subMenu: SubMenu) {
    val additionalItems = mutableListOf<MenuItem>()
    var order = 3
    onCreateAdditionalSortItems { itemId, title ->
      additionalItems.add(subMenu.add(groupIdSubmenu, itemId, order++, title))
    }
    additionalItems.forEach { setupSubMenuItem(it) }
  }

  private fun Toolbar.teardownMenu() {
    handler?.removeCallbacksAndMessages(null)
    publishHandler.removeCallbacksAndMessages(null)
    teardownSubmenu()
  }

  private fun Toolbar.teardownSubmenu() {
    this.menu.removeGroup(groupIdSubmenu)
    subMenu = null
  }

  @CheckResult
  private fun clickListener(sort: State.Sort<E>): MenuItem.OnMenuItemClickListener {
    return MenuItem.OnMenuItemClickListener {
      publishSortEvent(sort)
      return@OnMenuItemClickListener true
    }
  }

  @CheckResult
  private fun getSortForMenuItem(itemId: Int): State.Sort<E>? {
    return onGetSortForMenuItem(itemId)?.let { State.Sort(sort = it.ordinal, original = it) }
  }

  protected fun setItemVisibility(itemId: Int, visible: Boolean) {
    subMenu?.findItem(itemId)?.isVisible = visible
  }

  protected open fun onCreateAdditionalSortItems(adder: (Int, CharSequence) -> Unit) {}

  protected open fun onRender(state: UiRender<S>) {}

  @CheckResult protected abstract fun onGetSortForMenuItem(itemId: Int): E?

  protected abstract fun publishSortEvent(sort: State.Sort<E>)

  interface State<E : Enum<E>> : UiViewState {
    val toolbarSort: Sort<E>

    data class Sort<E : Enum<E>>(val sort: Int, val original: E)

    fun <E : Enum<E>> E.asToolbarSort(): Sort<E> {
      return Sort(sort = this.ordinal, original = this)
    }
  }
}
