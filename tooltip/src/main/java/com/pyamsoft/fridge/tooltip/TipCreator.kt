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

import androidx.annotation.CheckResult

interface TipCreator<B : Any, R : Tip> {

    @CheckResult
    fun top(): R

    @CheckResult
    fun top(builder: B.() -> B): R
}
