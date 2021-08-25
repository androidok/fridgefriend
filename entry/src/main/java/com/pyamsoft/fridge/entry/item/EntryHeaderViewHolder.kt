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

package com.pyamsoft.fridge.entry.item

import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.entry.databinding.EntryHeaderItemHolderBinding
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.createViewBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.util.doOnDestroy
import javax.inject.Inject

class EntryHeaderViewHolder
internal constructor(
    binding: EntryHeaderItemHolderBinding,
    owner: LifecycleOwner,
    factory: EntryItemComponent.Factory,
) : RecyclerView.ViewHolder(binding.root), ViewBinder<EntryItemViewState.Header> {

  private val viewBinder: ViewBinder<EntryItemViewState.Header>

  @Inject @JvmField internal var name: EntryHeaderName? = null

  @Inject @JvmField internal var icon: EntryHeaderIcon? = null

  init {
    factory.create(binding.entryHeaderHolder).inject(this)

    viewBinder =
        createViewBinder(
            name.requireNotNull(),
            icon.requireNotNull(),
        ) {}

    owner.doOnDestroy { teardown() }
  }

  override fun bindState(state: EntryItemViewState.Header) {
    viewBinder.bindState(state)
  }

  override fun teardown() {
    viewBinder.teardown()

    name = null
    icon = null
  }
}
