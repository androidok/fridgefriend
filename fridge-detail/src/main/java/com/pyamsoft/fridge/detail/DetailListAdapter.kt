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

package com.pyamsoft.fridge.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.JsonMappableFridgeItem
import com.pyamsoft.fridge.detail.DetailListAdapter.DetailViewHolder
import com.pyamsoft.fridge.detail.item.DetailItemComponent
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.CloseExpand
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.DatePick
import com.pyamsoft.fridge.detail.item.DetailItemControllerEvent.ExpandDetails
import com.pyamsoft.fridge.detail.item.DetailItemViewModel
import com.pyamsoft.fridge.detail.item.DetailListItemDate
import com.pyamsoft.fridge.detail.item.DetailListItemGlances
import com.pyamsoft.fridge.detail.item.DetailListItemName
import com.pyamsoft.fridge.detail.item.DetailListItemPresence
import com.pyamsoft.fridge.detail.item.ListItemLifecycle
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.toDp
import timber.log.Timber
import javax.inject.Inject

internal class DetailListAdapter constructor(
  private val editable: Boolean,
  private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent,
  private val callback: Callback
) : ListAdapter<FridgeItem, DetailViewHolder>(object : DiffUtil.ItemCallback<FridgeItem>() {

  override fun areItemsTheSame(
    oldItem: FridgeItem,
    newItem: FridgeItem
  ): Boolean {
    return oldItem.id() == newItem.id()
  }

  override fun areContentsTheSame(
    oldItem: FridgeItem,
    newItem: FridgeItem
  ): Boolean {
    return JsonMappableFridgeItem.from(oldItem) == JsonMappableFridgeItem.from(newItem)
  }

}) {

  override fun getItemViewType(position: Int): Int {
    return R.id.id_item_list_item
  }

  override fun getItemId(position: Int): Long {
    return getItem(position).id()
        .hashCode()
        .toLong()
  }

  @CheckResult
  private fun View.prepareAsItem(): View {
    val horizontalPadding = 16.toDp(this.context)
    val verticalPadding = 8.toDp(this.context)
    this.updatePadding(
        left = horizontalPadding,
        right = horizontalPadding,
        top = verticalPadding,
        bottom = verticalPadding
    )
    return this
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): DetailViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    val v = inflater.inflate(R.layout.listitem_constraint, parent, false)
        .prepareAsItem()
    return DetailItemViewHolder(v, factory)
  }

  override fun onBindViewHolder(
    holder: DetailViewHolder,
    position: Int
  ) {
    val item = getItem(position)
    (holder as DetailItemViewHolder).bind(item, editable, callback)
  }

  override fun onViewRecycled(holder: DetailViewHolder) {
    super.onViewRecycled(holder)
    holder.unbind()
  }

  internal abstract class DetailViewHolder protected constructor(
    view: View
  ) : RecyclerView.ViewHolder(view) {

    abstract fun unbind()

  }

  internal class DetailItemViewHolder internal constructor(
    itemView: View,
    private val factory: (parent: ViewGroup, item: FridgeItem, editable: Boolean) -> DetailItemComponent
  ) : DetailViewHolder(itemView) {

    @JvmField @Inject internal var viewModel: DetailItemViewModel? = null
    @JvmField @Inject internal var name: DetailListItemName? = null
    @JvmField @Inject internal var date: DetailListItemDate? = null
    @JvmField @Inject internal var presence: DetailListItemPresence? = null
    @JvmField @Inject internal var glances: DetailListItemGlances? = null

    private val parent: ConstraintLayout = itemView.findViewById(R.id.listitem_constraint)

    private var lifecycle: ListItemLifecycle? = null
    private var boundItem: FridgeItem? = null

    @CheckResult
    internal fun canArchive(): Boolean {
      return boundItem.let { item ->
        if (item == null) {
          return@let false
        } else {
          return@let !item.isArchived()
        }
      }
    }

    fun bind(
      item: FridgeItem,
      editable: Boolean,
      callback: Callback
    ) {
      boundItem = item
      lifecycle?.unbind()

      factory(parent, item, editable)
          .inject(this)

      val owner = ListItemLifecycle()
      lifecycle = owner

      val name = requireNotNull(name)
      val date = requireNotNull(date)
      val presence = requireNotNull(presence)
      val glances = requireNotNull(glances)

      createComponent(
          null, owner,
          requireNotNull(viewModel),
          name,
          date,
          presence,
          glances
      ) {
        return@createComponent when (it) {
          is ExpandDetails -> callback.onItemExpanded(it.item)
          is DatePick -> callback.onPickDate(it.oldItem, it.year, it.month, it.day)
          is CloseExpand -> Timber.d("Deleted item")
        }
      }

      parent.layout {
        presence.also {
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
          constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
        }

        date.also {
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
          constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
          constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
        }

        name.also {
          connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.START, presence.id(), ConstraintSet.END)
          connect(it.id(), ConstraintSet.END, date.id(), ConstraintSet.START)
          constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
          constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
        }

        glances.also {
          connect(it.id(), ConstraintSet.TOP, date.id(), ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
          connect(it.id(), ConstraintSet.START, date.id(), ConstraintSet.START)
          connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
          constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
        }

      }

      owner.bind()
    }

    override fun unbind() {
      lifecycle?.unbind()

      boundItem = null
      lifecycle = null
      viewModel = null
      name = null
      date = null
      presence = null
    }

    // Kind of hacky
    fun archive() {
      requireNotNull(viewModel).archive()
    }

    // Kind of hacky
    fun delete() {
      requireNotNull(viewModel).delete()
    }

  }

  interface Callback {

    fun onItemExpanded(item: FridgeItem)

    fun onPickDate(
      oldItem: FridgeItem,
      year: Int,
      month: Int,
      day: Int
    )

  }

}

