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

package com.pyamsoft.fridge.search

import android.view.ViewGroup
import com.pyamsoft.fridge.detail.DetailViewState
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.ui.theme.ThemeProvider
import com.pyamsoft.pydroid.ui.util.setUpEnabled
import javax.inject.Inject

class SearchToolbar
@Inject
internal constructor(
    parent: ViewGroup,
    themeProvider: ThemeProvider,
) :
    UiToolbar<DetailViewState, SearchViewEvent.ToolbarEvent>(
        parent,
        themeProvider,
    ) {

  init {
    doOnInflate { binding.uiToolbarToolbar.apply { setUpEnabled(false) } }

    doOnTeardown { binding.uiToolbarToolbar.apply { setUpEnabled(false) } }
  }

  override fun provideUpdateSortEvent(type: SortType): SearchViewEvent.ToolbarEvent {
    return SearchViewEvent.ToolbarEvent.UpdateSort(type)
  }

  override fun provideAppBarHeightMeasureEvent(height: Int): SearchViewEvent.ToolbarEvent {
    return SearchViewEvent.ToolbarEvent.TopBarMeasured(height)
  }

  override fun provideTabSwitchedEvent(isHave: Boolean): SearchViewEvent.ToolbarEvent {
    return SearchViewEvent.ToolbarEvent.TabSwitched(isHave)
  }

  override fun provideSearchEvent(query: String): SearchViewEvent.ToolbarEvent {
    return SearchViewEvent.ToolbarEvent.Search(query)
  }
}
