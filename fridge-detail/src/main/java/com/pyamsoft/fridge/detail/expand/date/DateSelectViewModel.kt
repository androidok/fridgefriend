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
 *
 */

package com.pyamsoft.fridge.detail.expand.date

import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.detail.base.BaseUpdaterViewModel
import com.pyamsoft.pydroid.arch.EventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DateSelectViewModel @Inject internal constructor(
    private val dateSelectBus: EventBus<DateSelectPayload>
) : BaseUpdaterViewModel<DateSelectViewState, DateSelectViewEvent, DateSelectControllerEvent>(
    initialState = DateSelectViewState
) {

    override fun handleViewEvent(event: DateSelectViewEvent) {
    }

    fun publish(
        itemId: String,
        entryId: String,
        year: Int,
        month: Int,
        day: Int
    ) {
        viewModelScope.launch(context = Dispatchers.Default) {
            dateSelectBus.send(
                DateSelectPayload(
                    itemId,
                    entryId,
                    year,
                    month,
                    day
                )
            )
            withContext(context = Dispatchers.Main) { publish(DateSelectControllerEvent.Close) }
        }
    }
}