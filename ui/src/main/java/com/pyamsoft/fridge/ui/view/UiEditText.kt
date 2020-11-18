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
 */

package com.pyamsoft.fridge.ui.view

import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import com.pyamsoft.fridge.ui.databinding.UiEditTextBinding
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiViewEvent

abstract class UiEditText<S : UiEditTextViewState, V : UiViewEvent> protected constructor(
    parent: ViewGroup
) : BaseUiView<S, V, UiEditTextBinding>(parent), UiTextWatcher {

    final override val viewBinding = UiEditTextBinding::inflate

    final override val layoutRoot: View by boundView { uiEditTextContainer }

    private val delegate by lazy {
        UiEditTextDelegate(binding.uiEditText) { oldText, newText ->
            onTextChanged(oldText, newText)
        }
    }

    init {
        doOnTeardown {
            clear()
        }

        doOnInflate {
            delegate.create()
        }

        doOnTeardown {
            delegate.destroy()
        }
    }

    @CallSuper
    override fun onRender(state: S) {
        delegate.render(state.text)
    }

    protected fun setText(text: String) {
        delegate.setText(text)
    }

    protected fun clear() {
        delegate.clear()
    }

    override fun onTextChanged(oldText: String, newText: String) {
    }

}