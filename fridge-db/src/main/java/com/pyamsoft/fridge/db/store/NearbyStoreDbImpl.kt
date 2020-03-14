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

package com.pyamsoft.fridge.db.store

import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Delete
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Insert
import com.pyamsoft.fridge.db.store.NearbyStoreChangeEvent.Update
import com.pyamsoft.pydroid.core.Enforcer
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class NearbyStoreDbImpl internal constructor(
    private val enforcer: Enforcer,
    private val db: NearbyStoreDb,
    private val dbQuery: suspend (force: Boolean) -> Sequence<NearbyStore>
) : NearbyStoreDb {

    private val mutex = Mutex()

    private val queryDao = object : NearbyStoreQueryDao {

        override suspend fun query(force: Boolean): List<NearbyStore> {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                if (force) {
                    invalidate()
                }

                return dbQuery(force)
                    .toList()
            }
        }
    }

    private val insertDao = object : NearbyStoreInsertDao {

        override suspend fun insert(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                db.insertDao()
                    .insert(o)
            }
            publishRealtime(Insert(o))
        }
    }

    private val updateDao = object : NearbyStoreUpdateDao {

        override suspend fun update(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                db.updateDao()
                    .update(o)
            }
            publishRealtime(Update(o))
        }
    }

    private val deleteDao = object : NearbyStoreDeleteDao {

        override suspend fun delete(o: NearbyStore) {
            enforcer.assertNotOnMainThread()
            mutex.withLock {
                db.deleteDao()
                    .delete(o)
            }
            publishRealtime(Delete(o))
        }
    }

    private suspend fun publishRealtime(event: NearbyStoreChangeEvent) {
        enforcer.assertNotOnMainThread()
        invalidate()
        publish(event)
    }

    override fun invalidate() {
        db.invalidate()
    }

    override suspend fun publish(event: NearbyStoreChangeEvent) {
        enforcer.assertNotOnMainThread()
        db.publish(event)
    }

    override fun realtime(): NearbyStoreRealtime {
        return db.realtime()
    }

    override fun queryDao(): NearbyStoreQueryDao {
        return queryDao
    }

    override fun insertDao(): NearbyStoreInsertDao {
        return insertDao
    }

    override fun updateDao(): NearbyStoreUpdateDao {
        return updateDao
    }

    override fun deleteDao(): NearbyStoreDeleteDao {
        return deleteDao
    }
}
