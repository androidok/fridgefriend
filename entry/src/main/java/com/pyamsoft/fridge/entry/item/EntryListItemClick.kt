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

import android.view.ViewGroup
import com.pyamsoft.pydroid.arch.UiRender
import com.pyamsoft.pydroid.arch.UiView
import com.pyamsoft.pydroid.ui.util.setOnDebouncedClickListener
import javax.inject.Inject
import timber.log.Timber

class EntryListItemClick
@Inject
internal constructor(
    parent: ViewGroup,
) : UiView<EntryItemViewState.Item, EntryItemViewEvent>() {

  init {
    doOnInflate {
      parent.setOnDebouncedClickListener {
        Timber.d("Entry on click")
        publish(EntryItemViewEvent.OnClick)
      }

      parent.setOnLongClickListener {
        publish(EntryItemViewEvent.OnLongPress)
        return@setOnLongClickListener true
      }
    }

    doOnTeardown {
      parent.setOnDebouncedClickListener(null)
      parent.setOnLongClickListener(null)
    }
  }

  override fun render(state: UiRender<EntryItemViewState.Item>) {}
}
