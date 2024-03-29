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

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import com.google.android.material.R
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.databinding.ExpandToolbarBinding
import com.pyamsoft.fridge.ui.withRoundedBackground
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.loader.ImageTarget
import com.pyamsoft.pydroid.ui.R as R2
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import com.pyamsoft.pydroid.util.asDp
import com.pyamsoft.pydroid.util.tintWith
import javax.inject.Inject

class MoveItemToolbar
@Inject
internal constructor(
    imageLoader: ImageLoader,
    parent: ViewGroup,
    theming: ThemeProvider,
) : BaseUiView<ItemMoveViewState, ItemMoveViewEvent, ExpandToolbarBinding>(parent) {

  override val viewBinding = ExpandToolbarBinding::inflate

  override val layoutRoot by boundView { expandToolbar }

  init {
    doOnInflate {
      // Set the theme before inflating the delegate or popup theme will be wrong.
      val theme =
          if (theming.isDarkTheme()) {
            R.style.ThemeOverlay_MaterialComponents
          } else {
            R.style.ThemeOverlay_MaterialComponents_Light
          }

      binding.expandToolbar.apply {
        popupTheme = theme
        ViewCompat.setElevation(this, 8f.asDp(context).toFloat())
      }
    }

    doOnInflate {
      imageLoader
          .asDrawable()
          .load(R2.drawable.ic_close_24dp)
          .mutate { it.tintWith(Color.WHITE) }
          .into(
              object : ImageTarget<Drawable> {

                override fun clear() {
                  binding.expandToolbar.navigationIcon = null
                }

                override fun setImage(image: Drawable) {
                  binding.expandToolbar.setUpEnabled(true, image)
                }
              })
          .also { loaded -> doOnTeardown { loaded.dispose() } }
    }

    doOnInflate { binding.expandToolbar.withRoundedBackground() }

    doOnInflate {
      binding.expandToolbar.setNavigationOnClickListener(
          DebouncedOnClickListener.create { publish(ItemMoveViewEvent.Close) })
    }

    doOnTeardown { clear() }
  }

  private fun clear() {
    binding.expandToolbar.setNavigationOnClickListener(null)
    binding.expandToolbar.setOnMenuItemClickListener(null)
  }

  private fun handleItem(item: FridgeItem?) {
    if (item == null) {
      binding.expandToolbar.title = ""
    } else {
      binding.expandToolbar.title = "Move ${item.name()}"
    }
  }

  override fun onRender(state: UiRender<ItemMoveViewState>) {
    state.mapChanged { it.item }.render(viewScope) { handleItem(it) }
  }
}
