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

import androidx.annotation.CheckResult
import androidx.lifecycle.viewModelScope
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.ui.BottomOffset
import com.pyamsoft.pydroid.arch.UiSavedState
import com.pyamsoft.pydroid.arch.UiSavedStateViewModel
import com.pyamsoft.pydroid.arch.UiSavedStateViewModelProvider
import com.pyamsoft.pydroid.bus.EventBus
import com.pyamsoft.pydroid.core.requireNotNull
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import javax.inject.Named
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class MainViewModel
@AssistedInject
internal constructor(
    @Assisted savedState: UiSavedState,
    private val interactor: MainInteractor,
    private val bottomOffsetBus: EventBus<BottomOffset>,
    @Named("app_name") appNameRes: Int,
) :
    UiSavedStateViewModel<MainViewState, MainControllerEvent>(
        savedState,
        MainViewState(
            page = DEFAULT_PAGE,
            appNameRes = appNameRes,
            countNeeded = 0,
            countExpiringOrExpired = 0,
            bottomBarHeight = 0)) {

  init {
    viewModelScope.launch(context = Dispatchers.Default) {
      interactor.listenForItemChanges { handleRealtime(it) }
    }

    refreshBadgeCounts()
  }

  fun handleLoadDefaultPage() {
    viewModelScope.launch(context = Dispatchers.Default) {
      val page = restoreSavedState(KEY_PAGE) { DEFAULT_PAGE.asString() }.asPage()
      Timber.d("Loading initial page: $page")
      handleSelectPage(page, force = true)
    }
  }

  private fun handleRealtime(event: FridgeItemChangeEvent) =
      when (event) {
        is FridgeItemChangeEvent.Insert -> refreshBadgeCounts()
        is FridgeItemChangeEvent.Update -> refreshBadgeCounts()
        is FridgeItemChangeEvent.Delete -> refreshBadgeCounts()
      }

  private fun refreshBadgeCounts() {
    viewModelScope.launch(context = Dispatchers.Default) {
      interactor
          .getNeededCount()
          .onFailure { Timber.e(it, "Error getting needed count") }
          .recover { 0 }
          .onSuccess { setState { copy(countNeeded = it) } }
    }

    viewModelScope.launch(context = Dispatchers.Default) {
      interactor
          .getExpiredOrExpiringCount()
          .onFailure { Timber.e(it, "Error getting expired/expiring count") }
          .recover { 0 }
          .onSuccess { setState { copy(countExpiringOrExpired = it) } }
    }
  }

  fun handleConsumeBottomBarHeight(height: Int) {
    viewModelScope.launch(context = Dispatchers.Default) {
      setState(
          stateChange = { copy(bottomBarHeight = height) },
          andThen = { bottomOffsetBus.send(BottomOffset(it.bottomBarHeight)) })
    }
  }

  fun handleSelectPage(
      newPage: MainPage,
      force: Boolean,
  ) {
    Timber.d("Select entry: $newPage")
    refreshBadgeCounts()

    // If the pages match we can just run the after, no need to set and publish
    val oldPage = state.page
    setState(
        stateChange = { copy(page = newPage) },
        andThen = { newState ->
          publishNewSelection(newState.page.requireNotNull(), oldPage, force)
        })
  }

  private suspend inline fun publishNewSelection(
      newPage: MainPage,
      oldPage: MainPage?,
      force: Boolean,
  ) {
    Timber.d("Publish selection: $oldPage -> $newPage")
    putSavedState(KEY_PAGE, newPage.asString())
    publish(MainControllerEvent.PushPage(newPage, oldPage, force))
  }

  companion object {

    private val DEFAULT_PAGE = MainPage.Entries

    @CheckResult
    private fun MainPage.asString(): String {
      return this::class.java.name
    }

    @CheckResult
    private fun String.asPage(): MainPage =
        when (this) {
          MainPage.Entries::class.java.name -> MainPage.Entries
          MainPage.Category::class.java.name -> MainPage.Category
          MainPage.Settings::class.java.name -> MainPage.Settings
          else -> throw IllegalStateException("Cannot convert to MainPage: $this")
        }

    private const val KEY_PAGE = "page"
  }

  @AssistedFactory
  interface Factory : UiSavedStateViewModelProvider<MainViewModel> {
    override fun create(savedState: UiSavedState): MainViewModel
  }
}
