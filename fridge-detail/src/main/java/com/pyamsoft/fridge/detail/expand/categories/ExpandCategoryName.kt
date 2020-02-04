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

package com.pyamsoft.fridge.detail.expand.categories

import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import com.pyamsoft.fridge.detail.R
import javax.inject.Inject

class ExpandCategoryName @Inject internal constructor(
    parent: ViewGroup
) : ExpandCategoryClickable(parent) {

    override val layout: Int = R.layout.expand_category_name

    override val layoutRoot by boundView<TextView>(R.id.expand_category_name)

    init {
        doOnTeardown {
            clear()
        }
    }

    private fun clear() {
        layoutRoot.isVisible = false
        layoutRoot.text = null
    }

    override fun onRender(state: ExpandedCategoryViewState) {
        state.category.let { category ->
            if (category == null || category.name.isBlank()) {
                clear()
            } else {
                layoutRoot.text = category.name
                layoutRoot.isVisible = true
            }
        }
    }
}
