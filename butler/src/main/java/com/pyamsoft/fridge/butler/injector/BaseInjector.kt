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

package com.pyamsoft.fridge.butler.injector

import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.params.BaseParameters
import com.pyamsoft.fridge.butler.runner.WorkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

abstract class BaseInjector<P : BaseParameters> protected constructor(
    private val context: Context
) {

    @CheckResult
    suspend fun execute(
        id: UUID,
        tags: Set<String>,
        params: P
    ): WorkResult = withContext(context = Dispatchers.Default) {
        onExecute(
            context.applicationContext,
            id,
            tags,
            params,
        )
    }

    @CheckResult
    protected abstract suspend fun onExecute(
        context: Context,
        id: UUID,
        tags: Set<String>,
        params: P
    ): WorkResult
}
