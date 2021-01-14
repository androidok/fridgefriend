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

package com.pyamsoft.fridge.setting

import android.os.Bundle
import android.view.View
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.createViewModelFactory
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.fridge.ui.applyToolbarOffset
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.settings.AppSettingsFragment
import com.pyamsoft.pydroid.ui.settings.AppSettingsPreferenceFragment
import javax.inject.Inject
import javax.inject.Provider

internal class SettingsFragment : AppSettingsFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.applyToolbarOffset(requireAppBarActivity(), viewLifecycleOwner)
    }

    override fun provideSettingsFragment(): AppSettingsPreferenceFragment {
        return SettingsPreferenceFragment()
    }

    override fun provideSettingsTag(): String {
        return SettingsPreferenceFragment.TAG
    }

    companion object {

        const val TAG = "SettingsFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(): Fragment {
            return SettingsFragment().apply {
                arguments = Bundle().apply {
                }
            }
        }
    }

    internal class SettingsPreferenceFragment : AppSettingsPreferenceFragment() {

        override val preferenceXmlResId: Int = R.xml.preferences

        @JvmField
        @Inject
        internal var provider: Provider<SettingsViewModel>? = null
        private val viewModel by fromViewModelFactory<SettingsViewModel>(activity = true) {
            createViewModelFactory(provider)
        }

        @JvmField
        @Inject
        internal var spacer: SettingsSpacer? = null

        private var stateSaver: StateSaver? = null

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            Injector.obtainFromApplication<FridgeComponent>(view.context)
                .plusSettingsComponent()
                .create(preferenceScreen)
                .inject(this)

            stateSaver = createComponent(
                savedInstanceState,
                viewLifecycleOwner,
                viewModel,
                requireNotNull(spacer)
            ) {}

            initializeUpdate()
        }

        private fun initializeUpdate() {
            val act = requireActivity()
            if (act is VersionChecker) {
                act.onVersionCheck()
            }
        }

        override fun onSaveInstanceState(outState: Bundle) {
            super.onSaveInstanceState(outState)
            stateSaver?.saveState(outState)
        }

        override fun onDestroyView() {
            super.onDestroyView()
            provider = null
            stateSaver = null
        }

        companion object {

            const val TAG = "SettingsPreferenceFragment"
        }
    }
}
