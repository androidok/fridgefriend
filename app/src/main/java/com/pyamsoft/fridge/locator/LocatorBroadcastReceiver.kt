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

package com.pyamsoft.fridge.locator

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pyamsoft.fridge.butler.Locator
import timber.log.Timber

internal class LocatorBroadcastReceiver internal constructor() : BroadcastReceiver() {

  override fun onReceive(
    context: Context?,
    intent: Intent?
  ) {
    if (context == null || intent == null) {
      Timber.e("Context or Intent is null. Stopping.")
      return
    }

    if (intent.action != Locator.UPDATE_LISTENER_ACTION) {
      Timber.e("Intent action: ${intent.action}")
      Timber.e("No match expected: ${Locator.UPDATE_LISTENER_ACTION}. Stopping")
      return
    }
  }

}
