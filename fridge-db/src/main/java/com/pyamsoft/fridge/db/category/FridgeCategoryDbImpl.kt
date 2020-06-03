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
 *
 */

package com.pyamsoft.fridge.db.category

import com.pyamsoft.cachify.Cached1
import com.pyamsoft.fridge.db.BaseDbImpl
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Delete
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Insert
import com.pyamsoft.fridge.db.category.FridgeCategoryChangeEvent.Update
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class FridgeCategoryDbImpl internal constructor(
    cache: Cached1<Sequence<FridgeCategory>, Boolean>,
    insertDao: FridgeCategoryInsertDao,
    updateDao: FridgeCategoryUpdateDao,
    deleteDao: FridgeCategoryDeleteDao
) : BaseDbImpl<FridgeCategory, FridgeCategoryChangeEvent>(cache), FridgeCategoryDb {

    private val mutex = Mutex()

    private val realtime = object : FridgeCategoryRealtime {

        override suspend fun listenForChanges(onChange: suspend (event: FridgeCategoryChangeEvent) -> Unit) {
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                onEvent(onChange)
            }
        }
    }

    private val queryDao = object : FridgeCategoryQueryDao {

        override suspend fun query(force: Boolean): List<FridgeCategory> =
            withContext(context = Dispatchers.IO) {
                Enforcer.assertOffMainThread()
                mutex.withLock {
                    if (force) {
                        invalidate()
                    }

                    return@withContext cache.call(force).toList()
                }
            }
    }

    private val insertDao = object : FridgeCategoryInsertDao {

        override suspend fun insert(o: FridgeCategory) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            mutex.withLock { insertDao.insert(o) }
            publish(Insert(o))
        }
    }

    private val updateDao = object : FridgeCategoryUpdateDao {

        override suspend fun update(o: FridgeCategory) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            mutex.withLock { updateDao.update(o) }
            publish(Update(o))
        }
    }

    private val deleteDao = object : FridgeCategoryDeleteDao {

        override suspend fun delete(o: FridgeCategory) = withContext(context = Dispatchers.IO) {
            Enforcer.assertOffMainThread()
            mutex.withLock { deleteDao.delete(o) }
            publish(Delete(o))
        }
    }

    override fun realtime(): FridgeCategoryRealtime {
        return realtime
    }

    override fun queryDao(): FridgeCategoryQueryDao {
        return queryDao
    }

    override fun insertDao(): FridgeCategoryInsertDao {
        return insertDao
    }

    override fun updateDao(): FridgeCategoryUpdateDao {
        return updateDao
    }

    override fun deleteDao(): FridgeCategoryDeleteDao {
        return deleteDao
    }
}
