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

package com.pyamsoft.fridge.main

import android.view.ViewGroup
import com.pyamsoft.fridge.main.databinding.MainContainerBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import javax.inject.Inject

// We don't use a scrolling behavior so that this entire container can scroll below the AppBar
class MainContainer @Inject internal constructor(parent: ViewGroup) :
    BaseUiView<MainViewState, MainViewEvent, MainContainerBinding>(parent) {

  override val viewBinding = MainContainerBinding::inflate

  override val layoutRoot by boundView { mainContainer }
}
