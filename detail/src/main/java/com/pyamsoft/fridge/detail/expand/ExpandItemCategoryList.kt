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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.detail.databinding.ExpandCategoriesBinding
import com.pyamsoft.fridge.detail.expand.categories.ExpandCategoryComponent
import com.pyamsoft.fridge.detail.expand.categories.ExpandItemCategoryListAdapter
import com.pyamsoft.fridge.detail.expand.categories.ExpandedCategoryViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.core.requireNotNull
import javax.inject.Inject

class ExpandItemCategoryList
@Inject
internal constructor(
    owner: LifecycleOwner,
    parent: ViewGroup,
    factory: ExpandCategoryComponent.Factory,
) : BaseUiView<ExpandedViewState, ExpandedViewEvent.ItemEvent, ExpandCategoriesBinding>(parent) {

  override val viewBinding = ExpandCategoriesBinding::inflate

  override val layoutRoot by boundView { expandItemCategories }

  private var modelAdapter: ExpandItemCategoryListAdapter? = null

  init {
    doOnInflate {
      binding.expandItemCategories.layoutManager =
          LinearLayoutManager(
                  binding.expandItemCategories.context, LinearLayoutManager.HORIZONTAL, false)
              .apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
              }
    }

    doOnInflate {
      modelAdapter =
          ExpandItemCategoryListAdapter(
              owner = owner,
              factory = factory,
              callback =
                  object : ExpandItemCategoryListAdapter.Callback {

                    override fun onCategorySelected(index: Int) {
                      publish(ExpandedViewEvent.ItemEvent.CommitCategory(index))
                    }
                  })
      binding.expandItemCategories.adapter = modelAdapter
    }

    doOnTeardown {
      binding.expandItemCategories.adapter = null

      modelAdapter = null
    }
  }

  @CheckResult
  private fun usingAdapter(): ExpandItemCategoryListAdapter {
    return modelAdapter.requireNotNull()
  }

  override fun onRender(state: UiRender<ExpandedViewState>) {
    state.render(viewScope) { handleCategories(it) }
  }

  private fun handleCategories(state: ExpandedViewState) {
    state.categories.let { categories ->
      when {
        categories.isEmpty() -> clearList()
        else -> setList(categories, state.item?.categoryId())
      }
    }
  }

  private fun setList(
      categories: List<FridgeCategory>,
      selectedCategoryId: FridgeCategory.Id?,
  ) {
    val itemList =
        categories.map { category ->
          val catView =
              if (category.isEmpty()) null
              else {
                ExpandedCategoryViewState.Category(
                    category.id(), category.name(), category.thumbnail())
              }

          val isSelected =
              if (category.isEmpty()) {
                selectedCategoryId == null
              } else {
                category.id() == selectedCategoryId
              }

          return@map ExpandedCategoryViewState(catView, isSelected)
        }
    usingAdapter().submitList(itemList)
  }

  private fun clearList() {
    usingAdapter().submitList(null)
  }
}
