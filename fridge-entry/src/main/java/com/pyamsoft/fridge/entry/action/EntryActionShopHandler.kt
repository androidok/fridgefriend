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

package com.pyamsoft.fridge.entry.action

import com.pyamsoft.fridge.entry.action.EntryActionShopHandler.ShopEvent
import com.pyamsoft.fridge.entry.action.EntryActionShopHandler.ShopEvent.Shop
import com.pyamsoft.fridge.entry.action.EntryShop.Callback
import com.pyamsoft.pydroid.arch.UiEventHandler
import com.pyamsoft.pydroid.core.bus.EventBus
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

internal class EntryActionShopHandler @Inject internal constructor(
  bus: EventBus<ShopEvent>
) : UiEventHandler<ShopEvent, Callback>(bus),
  Callback {

  override fun onShopClicked() {
    publish(Shop)
  }

  override fun handle(delegate: Callback): Disposable {
    return listen()
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe {
        return@subscribe when (it) {
          is Shop -> delegate.onShopClicked()
        }
      }
  }

  sealed class ShopEvent {
    object Shop : ShopEvent()
  }

}
