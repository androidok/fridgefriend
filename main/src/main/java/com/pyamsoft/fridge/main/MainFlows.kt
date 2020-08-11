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

package com.pyamsoft.fridge.main

import com.pyamsoft.pydroid.arch.UiControllerEvent
import com.pyamsoft.pydroid.arch.UiViewEvent
import com.pyamsoft.pydroid.arch.UiViewState

data class MainViewState(
    val page: MainPage?,
    val appNameRes: Int,
    val countNeeded: Int,
    val countExpiringOrExpired: Int,
    val hasNearby: Boolean
) : UiViewState

sealed class MainViewEvent : UiViewEvent {

    object OpenHave : MainViewEvent()

    object OpenNeed : MainViewEvent()

    object OpenCategory : MainViewEvent()

    object OpenNearby : MainViewEvent()

    object OpenSettings : MainViewEvent()

    data class BottomBarMeasured(val height: Int) : MainViewEvent()
}

sealed class MainControllerEvent : UiControllerEvent {

    data class PushHave internal constructor(val previousPage: MainPage?) : MainControllerEvent()

    data class PushNeed internal constructor(val previousPage: MainPage?) : MainControllerEvent()

    data class PushCategory internal constructor(val previousPage: MainPage?) :
        MainControllerEvent()

    data class PushNearby internal constructor(val previousPage: MainPage?) : MainControllerEvent()

    data class PushSettings internal constructor(val previousPage: MainPage?) :
        MainControllerEvent()

    object VersionCheck : MainControllerEvent()
}