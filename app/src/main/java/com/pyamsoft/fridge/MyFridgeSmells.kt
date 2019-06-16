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

package com.pyamsoft.fridge

import android.app.Application
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.locator.LocationUpdateReceiver
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.pydroid.bootstrap.libraries.OssLibraries
import com.pyamsoft.pydroid.ui.PYDroid
import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher
import com.squareup.moshi.Moshi
import javax.inject.Inject

class MyFridgeSmells : Application() {

  @JvmField @Inject internal var locator: Locator? = null

  private var component: FridgeComponent? = null
  private var refWatcher: RefWatcher? = null

  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      return
    }

    PYDroid.init(
        this,
        getString(R.string.app_name),
        "https://github.com/pyamsoft/myfridgesmells/issues",
        BuildConfig.VERSION_CODE,
        BuildConfig.DEBUG
    ) { provider ->
      val moshi = Moshi.Builder()
          .build()
      component = DaggerFridgeComponent.factory()
          .create(
              provider.theming(),
              moshi,
              provider.enforcer(),
              this,
              provider.imageLoader(),
              LocationUpdateReceiver::class.java
          )
          .also { onInitialized(it) }
    }
  }

  private fun onInitialized(component: FridgeComponent) {
    installRefWatcher()
    addLibraries()

    component.inject(this)
    listenForLocationUpdates()
  }

  private fun listenForLocationUpdates() {
    requireNotNull(locator).listenForUpdates()
  }

  private fun installRefWatcher() {
    if (BuildConfig.DEBUG) {
      refWatcher = LeakCanary.install(this)
    } else {
      refWatcher = RefWatcher.DISABLED
    }
  }

  private fun addLibraries() {
    OssLibraries.add(
        "Room",
        "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/room/",
        "The AndroidX Jetpack Room library. Fluent SQLite database access."
    )
    OssLibraries.add(
        "WorkManager",
        "https://android.googlesource.com/platform/frameworks/support/+/androidx-master-dev/work/",
        "The AndroidX Jetpack WorkManager library. Schedule periodic work in a device friendly way."
    )
    OssLibraries.add(
        "Dagger",
        "https://github.com/google/dagger",
        "A fast dependency injector for Android and Java."
    )
    OssLibraries.add(
        "FastAdapter",
        "https://github.com/mikepenz/fastadapter",
        "The bullet proof, fast and easy to use adapter library, which minimizes developing time to a fraction..."
    )
  }

  override fun getSystemService(name: String): Any? {
    val service = PYDroid.getSystemService(name)
    if (service != null) {
      return service
    }

    if (name == FridgeComponent::class.java.name) {
      return requireNotNull(component)
    } else {
      val serviceFromComponent = getServiceFromComponent(name)
      if (serviceFromComponent != null) {
        return serviceFromComponent
      } else {
        return super.getSystemService(name)
      }
    }
  }

  @CheckResult
  private fun getServiceFromComponent(name: String): Any? {
    return when (name) {
      FridgeEntryQueryDao::class.java.name -> requireNotNull(component).provideFridgeEntryQueryDao()
      FridgeItemQueryDao::class.java.name -> requireNotNull(component).provideFridgeItemQueryDao()
      Butler::class.java.name -> requireNotNull(component).provideButler()
      Locator::class.java.name -> requireNotNull(component).provideLocator()
      else -> null
    }
  }
}
