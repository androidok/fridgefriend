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

package com.pyamsoft.fridge.locator.map

import androidx.annotation.CheckResult
import com.pyamsoft.fridge.locator.Locator
import com.pyamsoft.fridge.locator.map.gms.GmsLocator
import com.pyamsoft.fridge.locator.map.osm.api.NearbyLocationApi
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BODY
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
abstract class LocatorModule {

  @Binds
  @CheckResult
  internal abstract fun bindLocator(impl: GmsLocator): Locator

  @Module
  companion object {

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideRetrofit(
      @Named("debug") debug: Boolean,
      moshi: Moshi
    ): Retrofit {
      val baseUrl = "https://overpass-api.de/api/interpreter"
      val client = OkHttpClient.Builder()
          .apply {
            if (debug) {
              addInterceptor(HttpLoggingInterceptor().apply {
                level = BODY
              })
            }
          }
          .build()
      return Retrofit.Builder()
          .baseUrl(baseUrl)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .client(client)
          .build()
    }

    @Provides
    @JvmStatic
    @Singleton
    internal fun provideNearbyLocationApi(retrofit: Retrofit): NearbyLocationApi {
      return retrofit.create(NearbyLocationApi::class.java)
    }

  }

}
