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

package com.pyamsoft.fridge.entry.create

import android.text.InputType
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import com.pyamsoft.fridge.entry.databinding.CreateEntryNameBinding
import com.pyamsoft.fridge.ui.setEditable
import com.pyamsoft.fridge.ui.view.UiEditTextDelegate
import com.pyamsoft.pydroid.arch.BaseUiView
import com.pyamsoft.pydroid.arch.UiRender
import javax.inject.Inject
import timber.log.Timber

class CreateEntryName
@Inject
internal constructor(
    parent: ViewGroup,
) : BaseUiView<CreateEntryViewState, CreateEntryViewEvent, CreateEntryNameBinding>(parent) {

  override val viewBinding = CreateEntryNameBinding::inflate

  override val layoutRoot by boundView { createEntryNameRoot }

  private val delegate by lazy(LazyThreadSafetyMode.NONE) {
    UiEditTextDelegate.create(binding.createEntryNameValue) { newText ->
      publish(CreateEntryViewEvent.NameChanged(newText))
      return@create true
    }
  }

  init {
    doOnInflate { delegate.handleCreate() }

    doOnTeardown { delegate.handleTeardown() }

    doOnInflate {
      binding.createEntryNameValue.apply {
        setEditable(EDITABLE_INPUT_TYPE)
        imeOptions = EditorInfo.IME_ACTION_GO
        setImeActionLabel("Create", EditorInfo.IME_ACTION_GO)
      }
    }

    doOnInflate {
      binding.createEntryNameValue.setOnEditorActionListener { _, _, event ->
        if (event != null) {
          if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER) {
            Timber.d("Commit on IME action Enter")
            publish(CreateEntryViewEvent.Commit)
            return@setOnEditorActionListener true
          }
        }

        return@setOnEditorActionListener false
      }
    }

    doOnTeardown { binding.createEntryNameValue.setOnEditorActionListener(null) }
  }

  private fun handleName(data: UiEditTextDelegate.Data) {
    delegate.handleTextChanged(data)
  }

  override fun onRender(state: UiRender<CreateEntryViewState>) {
    state.mapChanged { it.entryName }.render(viewScope) { handleName(it) }
  }

  companion object {
    private const val EDITABLE_INPUT_TYPE =
        InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_CAP_WORDS
  }
}
