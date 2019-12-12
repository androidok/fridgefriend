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

import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class DetailViewState(
    val isLoading: Loading?,
    val items: List<FridgeItem>?,
    val showArchived: Boolean,
    val listError: Throwable?,
    val undoableItem: FridgeItem?,
    val actionVisible: ActionVisible?,
    val isExpanded: Expanded?
) : UiViewState {

    data class Expanded internal constructor(val expanded: Boolean)

    data class ActionVisible internal constructor(val visible: Boolean)

    data class Loading internal constructor(val isLoading: Boolean)
}

sealed class DetailViewEvent : UiViewEvent {

    object AddNewItemEvent : DetailViewEvent()

    object ForceRefresh : DetailViewEvent()

    data class ExpandItem internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class PickDate internal constructor(
        val oldItem: FridgeItem,
        val year: Int,
        val month: Int,
        val day: Int
    ) : DetailViewEvent()

    object ToggleArchiveVisibility : DetailViewEvent()

    data class UndoDelete internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ReallyDeleteNoUndo internal constructor(val item: FridgeItem) : DetailViewEvent()

    data class ScrollActionVisibilityChange internal constructor(
        val visible: Boolean
    ) : DetailViewEvent()

    object DoneScrollActionVisibilityChange : DetailViewEvent()
}

sealed class DetailControllerEvent : UiControllerEvent {

    data class AddNew internal constructor(val entryId: String) : DetailControllerEvent()

    data class ExpandForEditing internal constructor(
        val item: FridgeItem
    ) : DetailControllerEvent()

    data class DatePick internal constructor(
        val oldItem: FridgeItem,
        val year: Int,
        val month: Int,
        val day: Int
    ) : DetailControllerEvent()

    object EntryArchived : DetailControllerEvent()
}
