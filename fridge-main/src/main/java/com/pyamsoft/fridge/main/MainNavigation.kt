/*
 * Copyright 2020 Peter Kenji Yamanaka
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
 *
 */

package com.pyamsoft.fridge.main

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.annotation.IdRes
import androidx.core.view.updatePadding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.pyamsoft.fridge.main.databinding.MainNavigationBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.util.doOnApplyWindowInsets
import timber.log.Timber
import javax.inject.Inject

class MainNavigation @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<MainViewState, MainViewEvent, MainNavigationBinding>(parent) {

    override val viewBinding = MainNavigationBinding::inflate

    override val layoutRoot by boundView { mainBottomNavigationMenu }

    private var navbarBackground: MainNavBackground? = null

    init {
        doOnInflate {
            val nav = binding.mainBottomNavigationMenu
            nav.post {
                // Publish the measured height
                publish(MainViewEvent.BottomBarMeasured(nav.height))
            }
        }

        doOnInflate {
            val nav = binding.mainBottomNavigationMenu
            nav.post {
                // Set the background with a hole
                navbarBackground = MainNavBackground(nav, 156).also {
                    nav.background = it
                }
            }
        }

        doOnTeardown {
            binding.mainBottomNavigationMenu.background = null
            navbarBackground = null
        }

        doOnInflate { reader ->
            val savedPadding = reader.get<Int>(KEY_PADDING)
            if (savedPadding == null) {
                layoutRoot.doOnApplyWindowInsets { v, insets, padding ->
                    v.updatePadding(bottom = padding.bottom + insets.systemWindowInsetBottom)
                }
            } else {
                layoutRoot.updatePadding(bottom = savedPadding)
            }
        }

        doOnSaveState { writer ->
            // We must save the padding here otherwise the padding will constantly increase
            writer.put(KEY_PADDING, layoutRoot.paddingBottom)
        }

        doOnInflate {
            binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener { item ->
                Timber.d("Click nav item: $item")
                return@setOnNavigationItemSelectedListener when (item.itemId) {
                    R.id.menu_item_nav_need -> select(MainViewEvent.OpenNeed)
                    R.id.menu_item_nav_have -> select(MainViewEvent.OpenHave)
                    R.id.menu_item_nav_category -> select(MainViewEvent.OpenCategory)
                    R.id.menu_item_nav_nearby -> select(MainViewEvent.OpenNearby)
                    R.id.menu_item_nav_settings -> select(MainViewEvent.OpenSettings)
                    else -> false
                }
            }
        }

        doOnTeardown {
            binding.mainBottomNavigationMenu.setOnNavigationItemSelectedListener(null)
            binding.mainBottomNavigationMenu.removeBadge(R.id.menu_item_nav_need)
            binding.mainBottomNavigationMenu.removeBadge(R.id.menu_item_nav_have)
        }
    }

    private fun handlePage(state: MainViewState) {
        state.page.let { page ->
            val pageId = getIdForPage(page)
            if (pageId != 0) {
                // Don't mark it selected since this will re-fire the click event
                // binding.mainBottomNavigationMenu.selectedItemId = pageId
                binding.mainBottomNavigationMenu.menu.findItem(pageId).isChecked = true
            }
        }
    }

    private fun handleBadges(state: MainViewState) {
        binding.mainBottomNavigationMenu.applyBadge(R.id.menu_item_nav_need, state.countNeeded)
        binding.mainBottomNavigationMenu.applyBadge(
            R.id.menu_item_nav_have,
            state.countExpiringOrExpired
        )
    }

    override fun onRender(state: MainViewState) {
        layoutRoot.post { handlePage(state) }
        layoutRoot.post { handleBadges(state) }
    }

    private fun BottomNavigationView.applyBadge(@IdRes id: Int, count: Int) {
        if (count <= 0) {
            this.removeBadge(id)
        } else {
            requireNotNull(this.getOrCreateBadge(id)).number = count
        }
    }

    // TODO(Peter): Anti-pattern
    fun onMeasured(ready: (width: Int, height: Int) -> Unit) {
        val nav = binding.mainBottomNavigationMenu
        nav.post { ready(nav.width, nav.height) }
    }

    // TODO(Peter): Anti-pattern
    @CheckResult
    fun getBottomOffset(): Int {
        return binding.mainBottomNavigationMenu.height
    }

    @CheckResult
    private fun getIdForPage(page: MainPage?): Int {
        return if (page == null) 0 else {
            when (page) {
                MainPage.NEED -> R.id.menu_item_nav_need
                MainPage.HAVE -> R.id.menu_item_nav_have
                MainPage.CATEGORY -> R.id.menu_item_nav_category
                MainPage.NEARBY -> R.id.menu_item_nav_nearby
                MainPage.SETTINGS -> R.id.menu_item_nav_settings
            }
        }
    }

    @CheckResult
    private fun select(viewEvent: MainViewEvent): Boolean {
        publish(viewEvent)
        return false
    }

    companion object {
        private const val KEY_PADDING = "key_padding"
    }
}
