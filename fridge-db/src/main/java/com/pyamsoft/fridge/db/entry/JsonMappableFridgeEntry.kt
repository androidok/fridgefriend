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

package com.pyamsoft.fridge.db.entry

import androidx.annotation.CheckResult
import com.squareup.moshi.JsonClass
import java.util.Date

@JsonClass(generateAdapter = true)
data class JsonMappableFridgeEntry internal constructor(
    internal val id: FridgeEntry.Id,
    internal val name: String,
    internal val createdTime: Date,
    internal val isReal: Boolean
) : FridgeEntry {

    override fun id(): FridgeEntry.Id {
        return id
    }

    override fun name(): String {
        return name
    }

    override fun createdTime(): Date {
        return createdTime
    }

    override fun isReal(): Boolean {
        return isReal
    }

    override fun name(name: String): FridgeEntry {
        return this.copy(name = name)
    }

    override fun makeReal(): FridgeEntry {
        return this.copy(isReal = true)
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun from(entry: FridgeEntry): JsonMappableFridgeEntry {
            return if (entry is JsonMappableFridgeEntry) entry else {
                JsonMappableFridgeEntry(
                    entry.id(),
                    entry.name(),
                    entry.createdTime(),
                    entry.isReal()
                )
            }
        }
    }
}
