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

package com.pyamsoft.fridge.detail.expand.move

import android.app.Activity
import androidx.annotation.CheckResult
import androidx.lifecycle.LifecycleOwner
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.item.EntryItemComponent
import com.pyamsoft.fridge.tooltip.balloon.TooltipModule
import com.pyamsoft.fridge.ui.ThemeProviderModule
import dagger.BindsInstance
import dagger.Subcomponent

@Subcomponent(
    modules =
        [
            ThemeProviderModule::class,
            TooltipModule::class,
        ])
internal interface BaseItemMoveComponent {

  /** Not actually used, just here so graph can compile */
  @CheckResult
  @Suppress("FunctionName")
  fun `$$daggerRequiredEntryItemComponent`(): EntryItemComponent.Factory

  @CheckResult fun plusItemMoveComponent(): ItemMoveComponent.Factory

  @Subcomponent.Factory
  interface Factory {

    @CheckResult
    fun create(
        @BindsInstance activity: Activity,
        @BindsInstance owner: LifecycleOwner,
        @BindsInstance itemId: FridgeItem.Id,
        @BindsInstance itemEntryId: FridgeEntry.Id,
    ): BaseItemMoveComponent
  }
}
