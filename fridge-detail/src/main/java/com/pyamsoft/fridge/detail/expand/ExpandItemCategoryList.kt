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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.category.FridgeCategory
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.expand.categories.ExpandItemCategoryListAdapter
import com.pyamsoft.fridge.detail.expand.categories.ExpandedCategoryViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

class ExpandItemCategoryList @Inject internal constructor(
    parent: ViewGroup,
    owner: LifecycleOwner,
    componentCreator: ExpandCategoryComponentCreator
) : BaseUiView<ExpandItemViewState, ExpandedItemViewEvent>(parent) {

    override val layout: Int = R.layout.expand_categories

    override val layoutRoot by boundView<RecyclerView>(R.id.expand_item_categories)

    private var modelAdapter: ExpandItemCategoryListAdapter? = null

    init {
        doOnInflate {
            layoutRoot.layoutManager = LinearLayoutManager(
                layoutRoot.context,
                LinearLayoutManager.HORIZONTAL,
                false
            ).apply {
                isItemPrefetchEnabled = true
                initialPrefetchItemCount = 3
            }
        }

        doOnInflate {
            modelAdapter = ExpandItemCategoryListAdapter(
                owner = owner,
                componentCreator = componentCreator,
                callback = object : ExpandItemCategoryListAdapter.Callback {

                    override fun onCategorySelected(index: Int) {
                        publish(ExpandedItemViewEvent.CommitCategory(index))
                    }
                })
            layoutRoot.adapter = usingAdapter().apply { setHasStableIds(true) }
        }

        doOnTeardown {
            // Throws
            // layoutRoot.adapter = null
            clearList()

            modelAdapter = null
        }
    }

    @CheckResult
    private fun usingAdapter(): ExpandItemCategoryListAdapter {
        return requireNotNull(modelAdapter)
    }

    override fun onRender(state: ExpandItemViewState) {
        state.categories.let { categories ->
            when {
                categories.isEmpty() -> clearList()
                else -> setList(categories, state.item?.categoryId())
            }
        }
    }

    private fun setList(
        categories: List<FridgeCategory>,
        selectedCategoryId: String?
    ) {
        val itemList = categories.map { category ->
            val catView = if (category.id().isBlank()) null else {
                ExpandedCategoryViewState.Category(
                    category.id(),
                    category.name(),
                    category.thumbnail()
                )
            }

            val isSelected = if (category.id().isBlank()) {
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