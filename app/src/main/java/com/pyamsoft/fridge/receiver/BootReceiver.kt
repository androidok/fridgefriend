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

package com.pyamsoft.fridge.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import timber.log.Timber

internal class BootReceiver internal constructor() : BroadcastReceiver() {

  @Inject @JvmField internal var butler: Butler? = null

  @Inject @JvmField internal var orderFactory: OrderFactory? = null

  private val scope by lazy(LazyThreadSafetyMode.NONE) { MainScope() }

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
      Timber.d("Boot completed!")
      inject(context.applicationContext)

      scope.launch(context = Dispatchers.Default) {
        butler.requireNotNull().initOnAppStart(orderFactory.requireNotNull())
      }
    }
  }

  private fun inject(context: Context) {
    if (butler == null || orderFactory == null) {
      Injector.obtainFromApplication<FridgeComponent>(context.applicationContext).inject(this)
    }
  }
}
