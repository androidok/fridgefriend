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

import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import com.pyamsoft.fridge.detail.databinding.DetailContainerBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.layout
import javax.inject.Inject

class DetailContainer
@Inject
internal constructor(
    emptyState: DetailEmptyState,
    list: DetailList,
    parent: ViewGroup,
) : BaseUiView<DetailViewState, DetailViewEvent.ListEvent, DetailContainerBinding>(parent) {

  override val viewBinding = DetailContainerBinding::inflate

  override val layoutRoot by boundView { detailContainer }

  init {
    nest(emptyState, list)

    doOnInflate {
      binding.detailContainer.layout {
        emptyState.let {
          connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
          constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
          constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        }

        list.let {
          connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
          constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
          constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        }
      }
    }
  }

  override fun onRender(state: UiRender<DetailViewState>) {
    state.mapChanged { it.bottomOffset }.render(viewScope) { handleBottomMargin(it) }
  }

  private fun handleBottomMargin(height: Int) {
    layoutRoot.updatePadding(bottom = height)
  }
}
