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

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.ui.view.UiToolbar
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent

sealed class SearchControllerEvent : UiControllerEvent {

  data class ExpandItem internal constructor(val item: FridgeItem) : SearchControllerEvent()
}

sealed class SearchViewEvent : UiViewEvent {

  sealed class FilterEvent : SearchViewEvent() {

    object ChangeCurrentFilter : FilterEvent()

    object UndoDeleteItem : FilterEvent()

    object ReallyDeleteItemNoUndo : FilterEvent()

    data class AnotherOne internal constructor(val item: FridgeItem) : FilterEvent()
  }

  sealed class ToolbarEvent : SearchViewEvent() {

    data class UpdateSort internal constructor(val type: UiToolbar.SortType) : ToolbarEvent()

    data class TopBarMeasured internal constructor(val height: Int) : ToolbarEvent()

    data class TabSwitched internal constructor(val isHave: Boolean) : ToolbarEvent()

    data class Search internal constructor(val query: String) : ToolbarEvent()
  }
}
