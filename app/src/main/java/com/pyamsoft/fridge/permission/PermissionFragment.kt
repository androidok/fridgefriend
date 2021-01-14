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

package com.pyamsoft.fridge.permission

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.locator.permission.*
import com.pyamsoft.fridge.locator.permission.PermissionControllerEvent.LocationPermissionRequest
import com.pyamsoft.fridge.main.VersionChecker
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.ui.requireAppBarActivity
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.R
import com.pyamsoft.pydroid.ui.arch.fromViewModelFactory
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import timber.log.Timber
import javax.inject.Inject

internal class PermissionFragment : Fragment(), PermissionConsumer<ForegroundLocationPermission> {

    @JvmField
    @Inject
    internal var permissionHandler: PermissionHandler<ForegroundLocationPermission>? = null

    @JvmField
    @Inject
    internal var requestButton: LocationRequestButton? = null

    @JvmField
    @Inject
    internal var explanation: LocationExplanation? = null

    @JvmField
    @Inject
    internal var factory: FridgeViewModelFactory? = null
    private val viewModel by fromViewModelFactory<LocationPermissionViewModel>(activity = true) {
        factory?.create(requireActivity())
    }

    private var stateSaver: StateSaver? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.layout_constraint, container, false)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutConstraintBinding.bind(view)
        Injector.obtainFromApplication<FridgeComponent>(view.context)
            .plusPermissionComponent()
            .create(
                requireAppBarActivity(),
                binding.layoutConstraint,
                viewLifecycleOwner
            )
            .inject(this)

        val requestButton = requireNotNull(requestButton)
        val explanation = requireNotNull(explanation)
        stateSaver = createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            requestButton,
            explanation
        ) {
            return@createComponent when (it) {
                is LocationPermissionRequest -> requestLocationPermission()
            }
        }

        binding.layoutConstraint.layout {

            explanation.let {
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            requestButton.let {
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                connect(it.id(), ConstraintSet.TOP, explanation.id(), ConstraintSet.BOTTOM)

                constrainWidth(it.id(), ConstraintSet.WRAP_CONTENT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }
        }

        initializeUpdate()
    }

    private fun initializeUpdate() {
        val act = requireActivity()
        if (act is VersionChecker) {
            act.onVersionCheck()
        }
    }

    override fun onRequestPermissions(permissions: Array<out String>, requestCode: Int) {
        requestPermissions(permissions, requestCode)
    }

    override fun onPermissionResponse(grant: PermissionGrant<ForegroundLocationPermission>) {
        if (grant.granted()) {
            pushMapFragmentOncePermissionGranted()
        } else {
            Timber.e("Location permissions denied, cannot show Map")
        }
    }

    private fun requestLocationPermission() {
        viewModel.requestForegroundPermission(this)
    }

    private fun pushMapFragmentOncePermissionGranted() {
        parentFragmentManager.commitNow(viewLifecycleOwner) {
            replace(
                requireArguments().getInt(CONTAINER_ID, 0),
                MapFragment.newInstance(
                    storeId = NearbyStore.Id.EMPTY,
                    zoneId = NearbyZone.Id.EMPTY
                ),
                MapFragment.TAG
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        requireNotNull(permissionHandler).handlePermissionResponse(
            this, requestCode, permissions, grantResults
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        factory = null
        stateSaver = null

        requestButton = null
    }

    companion object {

        private const val CONTAINER_ID = "parent_container_id"
        const val TAG = "PermissionFragment"

        @JvmStatic
        @CheckResult
        fun newInstance(containerId: Int): Fragment {
            return PermissionFragment().apply {
                arguments = Bundle().apply {
                    putInt(CONTAINER_ID, containerId)
                }
            }
        }
    }
}
