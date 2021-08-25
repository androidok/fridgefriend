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

package com.pyamsoft.fridge.entry

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.pyamsoft.fridge.entry.databinding.EntryHeaderItemHolderBinding
import com.pyamsoft.fridge.entry.databinding.EntryListItemHolderBinding
import com.pyamsoft.fridge.entry.item.EntryHeaderViewHolder
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.entry.item.EntryItemViewHolder
import com.pyamsoft.fridge.entry.item.EntryItemViewState
import kotlin.random.Random
import me.zhanghai.android.fastscroll.PopupTextProvider

class EntryListAdapter
internal constructor(
    private val owner: LifecycleOwner,
    private val factory: EntryItemComponent.Factory,
    private val callback: Callback
) : ListAdapter<EntryItemViewState, RecyclerView.ViewHolder>(DIFFER), PopupTextProvider {

  init {
    setHasStableIds(true)
  }

  override fun getItemId(position: Int): Long {
    return when (val item = getItem(position)) {
      is EntryItemViewState.Header -> HASH_CODE_HEADER
      is EntryItemViewState.Item -> item.entry.id().hashCode()
    }.toLong()
  }

  override fun getPopupText(position: Int): String {
    return when (val item = getItem(position)) {
      is EntryItemViewState.Header -> "Top"
      is EntryItemViewState.Item -> item.entry.name()
    }
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      TYPE_HEADER -> {
        val binding = EntryHeaderItemHolderBinding.inflate(inflater, parent, false)
        EntryHeaderViewHolder(binding, owner, factory)
      }
      TYPE_ITEM -> {
        val binding = EntryListItemHolderBinding.inflate(inflater, parent, false)
        EntryItemViewHolder(binding, owner, factory, callback)
      }
      else -> throw IllegalStateException("Invalid view type: $viewType")
    }
  }

  override fun getItemViewType(position: Int): Int {
    return when (getItem(position)) {
      is EntryItemViewState.Header -> TYPE_HEADER
      is EntryItemViewState.Item -> TYPE_ITEM
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    return when (val item = getItem(position)) {
      is EntryItemViewState.Header -> {
        val viewHolder = holder as EntryHeaderViewHolder
        viewHolder.bindState(item)
      }
      is EntryItemViewState.Item -> {
        val viewHolder = holder as EntryItemViewHolder
        viewHolder.bindState(item)
      }
      else ->
          throw IllegalStateException(
              "Cannot bind invalid view holder: ${getItemViewType(position)}")
    }
  }

  interface Callback {

    fun onClick(index: Int)

    fun onLongPress(index: Int)
  }

  companion object {

    private const val TYPE_HEADER = 1
    private const val TYPE_ITEM = 2
    private val HASH_CODE_HEADER = Random.nextInt()

    private val DIFFER =
        object : DiffUtil.ItemCallback<EntryItemViewState>() {

          override fun areItemsTheSame(
              oldItem: EntryItemViewState,
              newItem: EntryItemViewState
          ): Boolean {
            return when (oldItem) {
              is EntryItemViewState.Header -> newItem is EntryItemViewState.Header
              is EntryItemViewState.Item ->
                  when (newItem) {
                    is EntryItemViewState.Item -> oldItem.entry.id() == newItem.entry.id()
                    is EntryItemViewState.Header -> false
                  }
            }
          }

          override fun areContentsTheSame(
              oldItem: EntryItemViewState,
              newItem: EntryItemViewState
          ): Boolean {
            return when (oldItem) {
              is EntryItemViewState.Header -> newItem is EntryItemViewState.Header
              is EntryItemViewState.Item ->
                  when (newItem) {
                    is EntryItemViewState.Item -> oldItem == newItem
                    is EntryItemViewState.Header -> false
                  }
            }
          }
        }
  }
}
