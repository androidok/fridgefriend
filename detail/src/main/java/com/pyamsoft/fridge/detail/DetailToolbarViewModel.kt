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

package com.pyamsoft.fridge.detail

import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import com.pyamsoft.pydroid.bus.EventBus
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class DetailToolbarViewModel
@AssistedInject
internal constructor(
    delegate: DetailListStateModel,
    topOffsetBus: EventBus<DetailTopOffset>,
    @Assisted savedState: UiSavedState,
) :
    BaseDetailToolbarViewModel(
        delegate,
        topOffsetBus,
        savedState,
    ) {

  @AssistedFactory
  interface Factory : UiSavedStateViewModelProvider<DetailToolbarViewModel> {
    override fun create(savedState: UiSavedState): DetailToolbarViewModel
  }
}
