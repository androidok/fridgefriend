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

package com.pyamsoft.fridge.setting

import com.pyamsoft.fridge.setting.SettingControllerEvent.NavigateUp
import com.pyamsoft.fridge.setting.SettingViewEvent.Navigate
import com.pyamsoft.pydroid.arch.UiViewModel
import javax.inject.Inject

class SettingToolbarViewModel @Inject internal constructor(
) : UiViewModel<SettingViewState, SettingViewEvent, SettingControllerEvent>(
    initialState = SettingViewState(name = "Settings")
) {

    override fun handleViewEvent(event: SettingViewEvent) {
        return when (event) {
            is Navigate -> publish(NavigateUp)
        }
    }
}
