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

import android.view.ViewGroup
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.DebouncedOnClickListener
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class DetailToolbar
@Inject
internal constructor(
    parent: ViewGroup,
    themeProvider: ThemeProvider,
) :
    UiToolbar<DetailViewState, DetailViewEvent.ToolbarEvent>(
        parent,
        themeProvider,
    ) {

  init {
    doOnInflate {
      binding.uiToolbarToolbar.apply {
        setUpEnabled(true)
        setNavigationOnClickListener(
            DebouncedOnClickListener.create { publish(DetailViewEvent.ToolbarEvent.Back) })
      }
    }

    doOnTeardown {
      binding.uiToolbarToolbar.apply {
        setUpEnabled(false)
        setNavigationOnClickListener(null)
      }
    }
  }

  override fun provideUpdateSortEvent(type: SortType): DetailViewEvent.ToolbarEvent {
    return DetailViewEvent.ToolbarEvent.UpdateSort(type)
  }

  override fun provideAppBarHeightMeasureEvent(height: Int): DetailViewEvent.ToolbarEvent {
    return DetailViewEvent.ToolbarEvent.TopBarMeasured(height)
  }

  override fun provideTabSwitchedEvent(isHave: Boolean): DetailViewEvent.ToolbarEvent {
    return DetailViewEvent.ToolbarEvent.TabSwitched(isHave)
  }

  override fun provideSearchEvent(query: String): DetailViewEvent.ToolbarEvent {
    return DetailViewEvent.ToolbarEvent.Search(query)
  }
}
