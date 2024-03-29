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
import com.pyamsoft.fridge.entry.databinding.EntryHeaderIconBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.loader.ImageLoader
import javax.inject.Inject
import javax.inject.Named

class EntryHeaderIcon
@Inject
internal constructor(
    imageLoader: ImageLoader,
    @Named("app_icon") appIconRes: Int,
    parent: ViewGroup,
) : BaseUiView<EntryItemViewState.Header, Nothing, EntryHeaderIconBinding>(parent) {

  override val layoutRoot by boundView { entryHeaderIcon }

  override val viewBinding = EntryHeaderIconBinding::inflate

  init {
    doOnInflate {
      imageLoader.asDrawable().load(appIconRes).into(binding.entryHeaderIcon).also {
        doOnTeardown { it.dispose() }
      }
    }
  }
}
