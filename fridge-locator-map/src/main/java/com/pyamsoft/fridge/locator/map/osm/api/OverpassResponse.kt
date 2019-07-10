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

package com.pyamsoft.fridge.locator.map.osm.api

import androidx.annotation.CheckResult
import com.squareup.moshi.JsonClass
import java.util.Collections

@JsonClass(generateAdapter = true)
internal data class OverpassResponse internal constructor(
  internal val elements: List<OsmNodeOrWay>?
) {

  @CheckResult
  fun elements(): List<OsmNodeOrWay> {
    return elements.let {
      if (it == null) {
        throw RuntimeException("OverpassResponse: elements was null")
      } else {
        return@let Collections.unmodifiableList(it)
      }
    }
  }

  // Needed to generate static adapter
  companion object
}
