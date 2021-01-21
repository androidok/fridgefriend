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

package com.pyamsoft.fridge.butler

import androidx.annotation.CheckResult
import java.util.Calendar

interface ButlerPreferences {

    @CheckResult
    suspend fun getLastNotificationTimeNeeded(): Long

    suspend fun markNotificationNeeded(calendar: Calendar)

    @CheckResult
    suspend fun getLastNotificationTimeExpiringSoon(): Long

    suspend fun markNotificationExpiringSoon(calendar: Calendar)

    @CheckResult
    suspend fun getLastNotificationTimeExpired(): Long

    suspend fun markNotificationExpired(calendar: Calendar)

    @CheckResult
    suspend fun getLastNotificationTimeNightly(): Long

    suspend fun markNotificationNightly(calendar: Calendar)
}
