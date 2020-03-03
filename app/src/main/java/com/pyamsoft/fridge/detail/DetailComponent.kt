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

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.pyamsoft.fridge.ThemeProviderModule
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.core.ViewModelKey
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.detail.DetailComponent.ViewModelModule
import com.pyamsoft.fridge.tooltip.TooltipModule
import com.pyamsoft.pydroid.arch.UiViewModel
import com.pyamsoft.pydroid.ui.app.ToolbarActivity
import dagger.Binds
import dagger.BindsInstance
import dagger.Module
import dagger.Subcomponent
import dagger.multibindings.IntoMap
import javax.inject.Named

@Subcomponent(modules = [ViewModelModule::class, DetailListModule::class, TooltipModule::class, ThemeProviderModule::class])
internal interface DetailComponent {

    fun inject(fragment: DetailFragment)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance activity: Activity,
            @BindsInstance parent: ViewGroup,
            @BindsInstance toolbarActivity: ToolbarActivity,
            @BindsInstance owner: LifecycleOwner,
            @BindsInstance @Named("entry_id") entryId: FridgeEntry.Id,
            @BindsInstance filterPresence: Presence
        ): DetailComponent
    }

    @Module
    abstract class ViewModelModule {

        @Binds
        internal abstract fun bindViewModelFactory(factory: FridgeViewModelFactory): ViewModelProvider.Factory

        @Binds
        @IntoMap
        @ViewModelKey(DetailViewModel::class)
        internal abstract fun detailViewModel(viewModel: DetailViewModel): UiViewModel<*, *, *>
    }
}
