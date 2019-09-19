/*
 * Copyright 2019 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.detail.base

import android.view.ViewGroup
import android.widget.EditText
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.detail.R
import com.pyamsoft.fridge.detail.item.DetailItemViewEvent
import com.pyamsoft.fridge.detail.item.DetailItemViewState
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener

abstract class BaseItemCount protected constructor(
    parent: ViewGroup,
    initialItem: FridgeItem
) : BaseUiView<DetailItemViewState, DetailItemViewEvent>(parent) {

    final override val layout: Int = R.layout.detail_list_item_count

    final override val layoutRoot by boundView<ViewGroup>(R.id.detail_item_count)

    protected val countView by boundView<EditText>(R.id.detail_item_count_editable)

    // Don't bind nameView text based on state in onRender
    // Android does not re-render fast enough for edits to keep up
    init {
        doOnInflate {
            setCount(item = initialItem)
        }

        doOnTeardown {
            countView.text.clear()
            countView.setOnDebouncedClickListener(null)
        }
    }

    protected fun setCount(item: FridgeItem) {
        val count = item.count()
        val countText = if (count > 0) "$count" else ""
        countView.setTextKeepState(countText)
    }
}
