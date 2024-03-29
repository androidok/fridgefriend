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

package com.pyamsoft.fridge.detail.expand

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import com.pyamsoft.fridge.core.ViewModelFactoryModule
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap

@Subcomponent(
    modules =
        [
            ExpandComponent.ComponentModule::class,
            ViewModelFactoryModule::class,
        ])
internal interface ExpandComponent {

  fun inject(dialog: ExpandedItemDialog)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance parent: ViewGroup,
    ): ExpandComponent
  }

  @Module
  abstract class ComponentModule {

    @Binds
    @IntoMap
    @ClassKey(ExpandItemViewModel::class)
    internal abstract fun bindViewModel(
        impl: ExpandItemViewModel.Factory
    ): UiSavedStateViewModelProvider<out ViewModel>
  }
}
