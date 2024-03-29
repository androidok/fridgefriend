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

package com.pyamsoft.fridge.detail.item

import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.detail.databinding.DetailListItemHolderBinding
import com.pyamsoft.pydroid.arch.ViewBinder
import com.pyamsoft.pydroid.arch.createViewBinder
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnDestroy
import javax.inject.Inject

class DetailItemViewHolder
internal constructor(
    binding: DetailListItemHolderBinding,
    editable: Boolean,
    owner: LifecycleOwner,
    factory: DetailItemComponent.Factory,
    callback: DetailListAdapter.Callback
) : RecyclerView.ViewHolder(binding.root), ViewBinder<DetailItemViewState> {

  @JvmField @Inject internal var clickView: DetailListItemClick? = null

  @JvmField @Inject internal var nameView: DetailListItemName? = null

  @JvmField @Inject internal var presenceView: DetailListItemPresence? = null

  @JvmField @Inject internal var countView: DetailListItemCount? = null

  @JvmField @Inject internal var extraContainer: DetailListItemContainer? = null

  private val viewBinder: ViewBinder<DetailItemViewState>

  init {
    factory.create(binding.detailListItem, editable).inject(this)

    val extra = extraContainer.requireNotNull()
    val click = clickView.requireNotNull()
    val count = countView.requireNotNull()
    val name = nameView.requireNotNull()
    val presence = presenceView.requireNotNull()
    viewBinder =
        createViewBinder(
            click,
            name,
            extra,
            count,
            presence,
        ) {
          return@createViewBinder when (it) {
            is DetailItemViewEvent.ExpandItem -> callback.onItemExpanded(bindingAdapterPosition)
            is DetailItemViewEvent.CommitPresence ->
                callback.onPresenceChange(bindingAdapterPosition)
            is DetailItemViewEvent.IncreaseCount -> callback.onIncreaseCount(bindingAdapterPosition)
            is DetailItemViewEvent.DecreaseCount -> callback.onDecreaseCount(bindingAdapterPosition)
          }
        }

    binding.detailListItem.layout {
      presence.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
      }

      extra.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
        constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
      }

      count.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.END, extra.id(), ConstraintSet.START)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
        constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
      }

      name.also {
        connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
        connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
        connect(it.id(), ConstraintSet.END, count.id(), ConstraintSet.START)
        constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
        constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
      }
    }

    owner.doOnDestroy { teardown() }
  }

  override fun bindState(state: DetailItemViewState) {
    viewBinder.bindState(state)
  }

  override fun teardown() {
    viewBinder.teardown()
    clickView = null
    nameView = null
    presenceView = null
    extraContainer = null
    countView = null
  }
}
