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
import com.pyamsoft.fridge.entry.databinding.EntryHeaderNameBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject
import javax.inject.Named

class EntryHeaderName
@Inject
internal constructor(
    parent: ViewGroup,
    @Named("app_name") appNameRes: Int,
) : BaseUiView<EntryItemViewState.Header, Nothing, EntryHeaderNameBinding>(parent) {

  override val layoutRoot by boundView { entryHeaderName }

  override val viewBinding = EntryHeaderNameBinding::inflate

  init {
    doOnInflate { binding.entryHeaderName.setText(appNameRes) }

    doOnTeardown { binding.entryHeaderName.text = "" }
  }
}
