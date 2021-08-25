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

import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.CallSuper
import androidx.annotation.CheckResult
import androidx.appcompat.widget.Toolbar
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.updateLayoutParams
import com.google.android.material.R as R2
import com.google.android.material.tabs.TabLayout
import com.pyamsoft.fridge.ui.R
import com.pyamsoft.fridge.ui.databinding.UiToolbarBinding
import com.pyamsoft.fridge.ui.withRoundedBackground
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.doOnLayoutChanged
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import timber.log.Timber

abstract class UiToolbar<S : UiToolbar.State, V : UiViewEvent>
protected constructor(parent: ViewGroup, theming: ThemeProvider) :
    BaseUiView<S, V, UiToolbarBinding>(parent) {

  override val layoutRoot by boundView { uiToolbarAppbar }

  override val viewBinding = UiToolbarBinding::inflate

  private var subMenu: SubMenu? = null

  private val delegate by lazy(LazyThreadSafetyMode.NONE) {
    UiEditTextDelegate.create(binding.uiToolbarSearch.editText.requireNotNull()) { newText ->
      publish(provideSearchEvent(newText))
      return@create true
    }
  }

  init {
    doOnInflate {
      binding.uiToolbarAppbar.withRoundedBackground(applyAllCorners = true)

      binding.uiToolbarAppbar.outlineProvider = ViewOutlineProvider.BACKGROUND
      binding.uiToolbarToolbar.outlineProvider = null
    }

    doOnInflate {
      binding.uiToolbarAppbar
          .doOnApplyWindowInsets { v, insets, _ ->
            val toolbarTopMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> { this.topMargin = toolbarTopMargin }
          }
          .also { doOnTeardown { it.cancel() } }
    }

    doOnInflate { subMenu = binding.uiToolbarToolbar.initSubmenu() }

    doOnInflate {
      val theme =
          if (theming.isDarkTheme()) {
            R2.style.ThemeOverlay_MaterialComponents
          } else {
            R2.style.ThemeOverlay_MaterialComponents_Light
          }

      binding.uiToolbarToolbar.popupTheme = theme
    }

    doOnTeardown { binding.uiToolbarToolbar.teardownMenu() }

    doOnInflate {
      binding.uiToolbarAppbar.doOnLayoutChanged { v, _, _, _, _, _, _, _, _ ->
        publish(provideAppBarHeightMeasureEvent(v.height))
      }
    }

    doOnInflate {
      addTabs()
      attachListener()
    }

    doOnTeardown { binding.uiToolbarTabs.removeAllTabs() }

    doOnInflate { delegate.handleCreate() }

    doOnTeardown { delegate.handleTeardown() }
  }

  private fun handleSubmenu(currentSort: SortType) {
    subMenu?.let { subMenu ->
      subMenu.forEach { item ->
        getTypeForItemId(item.itemId)?.also { sort ->
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
      subMenu.add(groupIdSubmenu, itemIdCreatedDate, 1, "Created Date").apply { setupItem(this) }
      subMenu.add(groupIdSubmenu, itemIdName, 2, "Name").apply { setupItem(this) }
      subMenu.add(groupIdSubmenu, itemIdPurchaseDate, 3, "Purchase Date").apply { setupItem(this) }
      subMenu.add(groupIdSubmenu, itemIdExpirationDate, 4, "Expiration Date").apply {
        setupItem(this)
      }

      subMenu.setGroupCheckable(groupIdSubmenu, true, true)
    }
  }

  private fun setupItem(menuItem: MenuItem) {
    menuItem.apply {
      isChecked = false
      val type = getTypeForItemId(itemId).requireNotNull()
      setOnMenuItemClickListener {
        publish(provideUpdateSortEvent(type))
        return@setOnMenuItemClickListener true
      }
    }
  }

  private fun Toolbar.teardownMenu() {
    this.menu.removeGroup(groupIdSubmenu)
    subMenu = null
  }

  private fun setItemVisibility(itemId: Int, visible: Boolean) {
    subMenu?.findItem(itemId)?.isVisible = visible
  }

  private fun handleMenuVisibility(isHave: Boolean) {
    setItemVisibility(itemIdPurchaseDate, isHave)
    setItemVisibility(itemIdExpirationDate, isHave)
  }

  private fun attachListener() {
    val listener =
        object : TabLayout.OnTabSelectedListener {

          override fun onTabSelected(tab: TabLayout.Tab) {
            val presence = getTabPresence(tab) ?: return
            publish(provideTabSwitchedEvent(presence == TITLE_HAVE))
          }

          override fun onTabUnselected(tab: TabLayout.Tab) {}

          override fun onTabReselected(tab: TabLayout.Tab) {}
        }

    binding.uiToolbarTabs.apply {
      addOnTabSelectedListener(listener)
      doOnTeardown { removeOnTabSelectedListener(listener) }
    }
  }

  private fun addTabs() {
    binding.uiToolbarTabs.apply {
      addTab(newTab().setText(TITLE_NEED))
      addTab(newTab().setText(TITLE_HAVE))
    }
  }

  @CheckResult
  private fun getTabPresence(tab: TabLayout.Tab): String? {
    val text = tab.text
    if (text == null) {
      Timber.w("No tag found on tab: $tab")
      return null
    }

    return when (text) {
      TITLE_HAVE -> TITLE_HAVE
      TITLE_NEED -> TITLE_NEED
      else -> null
    }
  }

  private fun handlePresence(isHave: Boolean) {
    val tabs = binding.uiToolbarTabs
    for (i in 0 until tabs.tabCount) {
      val tab = tabs.getTabAt(i)
      if (tab == null) {
        Timber.w("No tab found at index: $i")
        continue
      }

      val tag = getTabPresence(tab)
      if (isHave) {
        if (tag == TITLE_HAVE) {
          tabs.selectTab(tab, true)
          break
        }
      } else {
        if (tag == TITLE_NEED) {
          tabs.selectTab(tab, true)
          break
        }
      }
    }
  }

  private fun handleSearch(data: UiEditTextDelegate.Data) {
    delegate.handleTextChanged(data)
  }

  @CheckResult protected abstract fun provideUpdateSortEvent(type: SortType): V

  @CheckResult protected abstract fun provideAppBarHeightMeasureEvent(height: Int): V

  @CheckResult protected abstract fun provideTabSwitchedEvent(isHave: Boolean): V

  @CheckResult protected abstract fun provideSearchEvent(query: String): V

  @CallSuper
  override fun onRender(state: UiRender<S>) {
    state.mapChanged { it.search }.render(viewScope) { handleSearch(it) }
    state.mapChanged { it.sort }.render(viewScope) { handleSubmenu(it) }
    state.mapChanged { it.isHave }.render(viewScope) { handleMenuVisibility(it) }
  }

  interface State : UiViewState {
    val search: UiEditTextDelegate.Data
    val sort: SortType
    val isHave: Boolean
  }

  enum class SortType {
    CREATED_TIME,
    NAME,
    PURCHASE_DATE,
    EXPIRATION_DATE,
  }

  companion object {

    private const val TITLE_HAVE = "HAVE"
    private const val TITLE_NEED = "NEED"

    private val groupIdSubmenu = View.generateViewId()
    private val itemIdSubmenu = View.generateViewId()
    private val itemIdTitle = View.generateViewId()

    private val itemIdCreatedDate = View.generateViewId()
    private val itemIdName = View.generateViewId()
    private val itemIdPurchaseDate = View.generateViewId()
    private val itemIdExpirationDate = View.generateViewId()

    @CheckResult
    private fun getTypeForItemId(itemId: Int): SortType? {
      return when (itemId) {
        itemIdCreatedDate -> SortType.CREATED_TIME
        itemIdName -> SortType.NAME
        itemIdPurchaseDate -> SortType.PURCHASE_DATE
        itemIdExpirationDate -> SortType.EXPIRATION_DATE
        else -> null
      }
    }
  }
}
