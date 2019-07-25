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
import android.content.Context
import androidx.annotation.CheckResult
import com.pyamsoft.fridge.FridgeComponent.FridgeProvider
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.ForegroundState
import com.pyamsoft.fridge.butler.workmanager.ButlerModule
import com.pyamsoft.fridge.db.entry.FridgeEntryQueryDao
import com.pyamsoft.fridge.db.item.FridgeItemChangeEvent
import com.pyamsoft.fridge.db.item.FridgeItemQueryDao
import com.pyamsoft.fridge.db.room.RoomModule
import com.pyamsoft.fridge.detail.DatePickerDialogFragment
import com.pyamsoft.fridge.detail.DetailComponent
import com.pyamsoft.fridge.detail.ExpandComponent
import com.pyamsoft.fridge.detail.item.DateSelectPayload
import com.pyamsoft.fridge.entry.EntryComponent
import com.pyamsoft.fridge.locator.GeofenceBroadcastReceiver
import com.pyamsoft.fridge.locator.Geofencer
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.MapComponent
import com.pyamsoft.fridge.locator.PermissionComponent
import com.pyamsoft.fridge.locator.map.LocatorModule
import com.pyamsoft.fridge.main.MainComponent
import com.pyamsoft.fridge.setting.SettingComponent
import com.pyamsoft.pydroid.arch.EventBus
import com.pyamsoft.pydroid.core.Enforcer
import com.pyamsoft.pydroid.loader.ImageLoader
import com.pyamsoft.pydroid.ui.theme.Theming
import com.squareup.moshi.Moshi
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
      FridgeProvider::class,
      RoomModule::class,
      ButlerModule::class,
      LocatorModule::class
    ]
)
internal interface FridgeComponent {

  // For ButlerNotifications classes
  @CheckResult
  fun provideForegroundState(): ForegroundState

  // For BaseWorker Work classes
  @CheckResult
  fun provideButler(): Butler

  // For LocatorWorker Work classes
  @CheckResult
  fun provideLocator(): Locator

  // For GeofenceWorker Work classes
  @CheckResult
  fun provideGeofencer(): Geofencer

  // For ExpirationWorker Work classes
  @CheckResult
  fun provideFridgeEntryQueryDao(): FridgeEntryQueryDao

  // For ExpirationWorker Work classes
  @CheckResult
  fun provideFridgeItemQueryDao(): FridgeItemQueryDao

  //  @CheckResult
  //  fun plusScannerComponent(): OcrComponent.Factory

  @CheckResult
  fun plusExpandComponent(): ExpandComponent.Factory

  @CheckResult
  fun plusDetailComponent(): DetailComponent.Factory

  @CheckResult
  fun plusEntryComponent(): EntryComponent.Factory

  @CheckResult
  fun plusMainComponent(): MainComponent.Factory

  @CheckResult
  fun plusSettingComponent(): SettingComponent.Factory

  @CheckResult
  fun plusMapComponent(): MapComponent.Factory

  @CheckResult
  fun plusPermissionComponent(): PermissionComponent.Factory

  fun inject(dialog: DatePickerDialogFragment)

  fun inject(application: MyFridgeSmells)

  @Component.Factory
  interface Factory {

    @CheckResult
    fun create(
      @BindsInstance theming: Theming,
      @BindsInstance moshi: Moshi,
      @BindsInstance enforcer: Enforcer,
      @BindsInstance application: Application,
      @BindsInstance imageLoader: ImageLoader,
      @BindsInstance receiverClass: Class<out GeofenceBroadcastReceiver>
    ): FridgeComponent

  }

  @Module
  object FridgeProvider {

    @Provides
    @JvmStatic
    @Singleton
    @Named("debug")
    internal fun provideDebug(): Boolean {
      return BuildConfig.DEBUG
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideFakeItemRealtime(): EventBus<FridgeItemChangeEvent> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideDateSelectBus(): EventBus<DateSelectPayload> {
      return EventBus.create()
    }

    @Provides
    @JvmStatic
    internal fun provideContext(application: Application): Context {
      return application
    }

    @Provides
    @JvmStatic
    @Named("app_name")
    internal fun provideAppNameRes(): Int {
      return R.string.app_name
    }
  }
}

