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

package com.pyamsoft.fridge.db.item

import androidx.annotation.CheckResult
import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem.Presence
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Delete
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent.Insert
import com.pyamsoft.pydroid.core.Enforcer
import java.util.Locale
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class FridgeItemDbImpl internal constructor(
    cache: Cached1<Sequence<FridgeItem>, Boolean>,
    insertDao: FridgeItemInsertDao,
    deleteDao: FridgeItemDeleteDao
) : BaseDbImpl<FridgeItem, FridgeItemChangeEvent>(cache), FridgeItemDb {

    private val realtime = object : FridgeItemRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: FridgeItemChangeEvent) -> Unit) =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                onEvent(onChange)
            }

        override suspend fun listenForChanges(
            id: FridgeEntry.Id,
            onChange: suspend (event: FridgeItemChangeEvent) -> Unit
        ) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            onEvent { event ->
                if (event.entryId == id) {
                    onChange(event)
                }
            }
        }
    }

    private val queryDao = object : FridgeItemQueryDao {

        @CheckResult
        private suspend fun queryAsSequence(force: Boolean): Sequence<FridgeItem> {
            Enforcer.assertOffMainThread()
            if (force) {
                invalidate()
            }

            return cache.call(force)
        }

        override suspend fun query(force: Boolean): List<FridgeItem> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext queryAsSequence(force).toList()
            }

        override suspend fun query(force: Boolean, id: FridgeEntry.Id): List<FridgeItem> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                return@withContext queryAsSequence(force)
                    .filter { it.entryId() == id }
                    .toList()
            }

        override suspend fun querySameNameDifferentPresence(
            force: Boolean,
            name: String,
            presence: Presence
        ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            return@withContext queryAsSequence(force)
                .filter { it.isReal() }
                .filterNot { it.isArchived() }
                .filter { it.presence() == presence }
                .filter { item ->
                    val cleanName = name.toLowerCase(Locale.getDefault()).trim()
                    val itemName = item.name().toLowerCase(Locale.getDefault()).trim()
                    return@filter itemName == cleanName
                }
                .toList()
        }

        override suspend fun querySimilarNamedItems(
            force: Boolean,
            item: FridgeItem
        ): List<FridgeItem> = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            val sequence = queryAsSequence(force)
                .filter { it.isReal() }
                .filterNot { it.id() == item.id() }
                .map { fridgeItem ->
                    val name = item.name().toLowerCase(Locale.getDefault()).trim()
                    val itemName = fridgeItem.name().toLowerCase(Locale.getDefault()).trim()

                    val score = when {
                        itemName == name -> 1.0F
                        itemName.startsWith(name) -> 0.75F
                        itemName.endsWith(name) -> 0.5F
                        else -> itemName.withDistanceRatio(name)
                    }
                    return@map SimilarityScore(fridgeItem, score)
                }
                .filterNot { it.score < SIMILARITY_MIN_SCORE_CUTOFF }
                .sortedBy { it.score }

            if (sequence.none()) {
                return@withContext emptyList<FridgeItem>()
            }

            return@withContext sequence
                .chunked(SIMILARITY_MAX_ITEM_COUNT)
                .first()
                .map { it.item }
                .toList()
        }

        @CheckResult
        private fun String.withDistanceRatio(str: String): Float {
            // Initialize a zero-matrix
            val s1Len = this.length
            val s2Len = str.length
            val rows = s1Len + 1
            val columns = s2Len + 1
            val matrix = Array(rows) { IntArray(columns) { 0 } }

            // Populate matrix with indices of each character in strings
            for (i in 1 until rows) {
                matrix[i][0] = i
            }

            for (j in 1 until columns) {
                matrix[0][j] = j
            }

            // Calculate the cost of deletes, inserts, and subs
            for (col in 1 until columns) {
                for (row in 1 until rows) {
                    // If the character is the same in a given position, cost is 0, else cost is 2
                    val cost = if (this[row - 1] == str[col - 1]) 0 else 2

                    // The cost of a deletion, insertion, and substitution
                    val deleteCost = matrix[row - 1][col] + 1
                    val insertCost = matrix[row][col - 1] + 1
                    val substitutionCost = matrix[row - 1][col - 1] + cost

                    // Populate the matrix
                    matrix[row][col] = min(deleteCost, min(insertCost, substitutionCost))
                }
            }

            // Calculate distance ratio
            val totalLength = (s1Len + s2Len)
            return (totalLength - matrix[s1Len][s2Len]).toFloat() / totalLength
        }
    }

    private val insertDao = object : FridgeItemInsertDao {

        override suspend fun insert(o: FridgeItem) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            insertDao.insert(o)
            publish(Insert(o.makeReal()))
        }
    }

    private val deleteDao = object : FridgeItemDeleteDao {

        override suspend fun delete(o: FridgeItem) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            deleteDao.delete(o)
            publish(Delete(o.makeReal()))
        }
    }

    override fun realtime(): FridgeItemRealtime {
        return realtime
    }

    override fun queryDao(): FridgeItemQueryDao {
        return queryDao
    }

    override fun insertDao(): FridgeItemInsertDao {
        return insertDao
    }

    override fun deleteDao(): FridgeItemDeleteDao {
        return deleteDao
    }

    private data class SimilarityScore internal constructor(val item: FridgeItem, val score: Float)

    companion object {

        private const val SIMILARITY_MIN_SCORE_CUTOFF = 0.45F
        private const val SIMILARITY_MAX_ITEM_COUNT = 6
    }
}
