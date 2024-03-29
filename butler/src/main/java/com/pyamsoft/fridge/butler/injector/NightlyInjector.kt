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

package com.pyamsoft.fridge.butler.injector

import android.content.Context
import com.pyamsoft.fridge.butler.params.EmptyParameters
import com.pyamsoft.fridge.butler.runner.NightlyRunner
import com.pyamsoft.fridge.butler.runner.WorkResult
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import java.util.UUID
import javax.inject.Inject

class NightlyInjector(context: Context) : BaseInjector<EmptyParameters>(context) {

  @JvmField @Inject internal var runner: NightlyRunner? = null

  override suspend fun onExecute(
      context: Context,
      id: UUID,
      tags: Set<String>,
      params: EmptyParameters
  ): WorkResult {
    Injector.obtainFromApplication<ButlerComponent>(context).inject(this)

    return runner.requireNotNull().doWork(id, tags, params)
  }
}
