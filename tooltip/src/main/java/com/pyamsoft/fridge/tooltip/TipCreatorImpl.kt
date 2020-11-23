/*
 * Copyright 2020 Peter Kenji Yamanaka
 *
 * Licensed under the Apache LicenseVersion 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writingsoftware
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KINDeither express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pyamsoft.fridge.tooltip

import android.app.Activity
import androidx.annotation.CheckResult

internal abstract class TipCreatorImpl<B : Any, R : Tip> protected constructor(
    activity: Activity
) : BalloonBuilderCreator(activity), TipCreator<B, R> {

    private val empty: B.() -> B = { this }

    @CheckResult
    protected abstract fun build(builder: B.() -> B): BalloonCreator

    @CheckResult
    protected abstract fun create(creator: BalloonCreator, direction: TipDirection): R

    final override fun top(): R {
        return top(empty)
    }

    final override fun top(builder: B.() -> B): R {
        return create(build(builder), TipDirection.TOP)
    }

}
