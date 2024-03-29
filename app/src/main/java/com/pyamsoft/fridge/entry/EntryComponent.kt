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

import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import com.pyamsoft.fridge.core.FragmentScope
import com.pyamsoft.fridge.core.ViewModelFactoryModule
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.multibindings.ClassKey
import dagger.multibindings.IntoMap
import javax.inject.Named

@FragmentScope
@Subcomponent(
    modules =
        [
            EntryComponent.ComponentModule::class,
            ViewModelFactoryModule::class,
        ])
internal interface EntryComponent {

  fun inject(fragment: EntryFragment)

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance parent: ViewGroup,
    ): EntryComponent
  }

  @Module
  abstract class ComponentModule {

    @Binds
    @IntoMap
    @ClassKey(EntryViewModel::class)
    internal abstract fun bindViewModel(impl: EntryViewModel): ViewModel

    @Module
    companion object {

      @Named("entry_show_header")
      @Provides
      @JvmStatic
      @CheckResult
      internal fun provideShowHeader(): Boolean {
        return true
      }
    }
  }
}
