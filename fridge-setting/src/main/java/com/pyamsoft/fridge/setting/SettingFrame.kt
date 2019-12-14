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

import android.view.ViewGroup
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiSavedState
import javax.inject.Inject

class SettingFrame @Inject internal constructor(
    parent: ViewGroup
) : BaseUiView<SettingViewState, SettingViewEvent>(parent) {

    override val layout: Int = R.layout.setting_frame

    override val layoutRoot by boundView<ViewGroup>(R.id.setting_frame)

    override fun onRender(
        state: SettingViewState,
        savedState: UiSavedState
    ) {
    }
}
