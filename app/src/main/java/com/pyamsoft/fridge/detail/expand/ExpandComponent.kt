/*
 * Copyright 2020 Peter Kenji Yamanaka
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

import android.app.Activity
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.pyamsoft.fridge.ThemeProviderModule
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules = [
        ExpandItemModule::class,
        ThemeProviderModule::class
    ]
)
internal interface ExpandComponent {

    fun inject(dialog: ExpandedItemDialog)

    @Subcomponent.Factory
    interface Factory {

        @CheckResult
        fun create(
            @BindsInstance savedStateRegistryOwner: SavedStateRegistryOwner,
            @BindsInstance activity: Activity,
            @BindsInstance parent: ViewGroup,
            @BindsInstance owner: LifecycleOwner,
            @BindsInstance itemId: FridgeItem.Id,
            @BindsInstance itemEntryId: FridgeEntry.Id,
            @BindsInstance defaultPresence: Presence
        ): ExpandComponent
    }
}
