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

package com.pyamsoft.fridge.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.NotificationHandler
import com.pyamsoft.fridge.butler.Notifications
import com.pyamsoft.fridge.category.CategoryFragment
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.permission.PermissionFragment
import com.pyamsoft.fridge.setting.SettingsDialog
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.factory
import com.pyamsoft.pydroid.ui.rating.ChangeLogBuilder
import com.pyamsoft.pydroid.ui.rating.RatingActivity
import com.pyamsoft.pydroid.ui.rating.buildChangeLog
import com.pyamsoft.pydroid.ui.util.commitNow
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.ui.util.show
import com.pyamsoft.pydroid.util.makeWindowSexy
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

internal class MainActivity : RatingActivity(), VersionChecker {

    override val checkForUpdates: Boolean = false

    override val applicationIcon: Int = R.mipmap.ic_launcher

    override val versionName: String = BuildConfig.VERSION_NAME

    override val changeLogTheme: Int = R.style.Theme_Fridge_Dialog

    override val versionCheckTheme: Int = R.style.Theme_Fridge_Dialog

    override val changeLogLines: ChangeLogBuilder = buildChangeLog {
    }

    override val fragmentContainerId: Int
        get() = requireNotNull(container).id()

    override val snackbarRoot: ViewGroup
        get() {
            val containerFragment = supportFragmentManager.findFragmentById(fragmentContainerId)
            if (containerFragment is SnackbarContainer) {
                val snackbarContainer = containerFragment.getSnackbarContainer()
                if (snackbarContainer != null) {
                    return snackbarContainer
                }
            }

            return requireNotNull(rootView)
        }

    private var rootView: ConstraintLayout? = null
    private var stateSaver: StateSaver? = null

    @JvmField
    @Inject
    internal var butler: Butler? = null

    @JvmField
    @Inject
    internal var toolbar: MainToolbar? = null
    @JvmField
    @Inject
    internal var navigation: MainNavigation? = null
    @JvmField
    @Inject
    internal var container: MainContainer? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by factory<MainViewModel> { factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fridge_Normal)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_constraint)

        val view = findViewById<ConstraintLayout>(R.id.layout_constraint)
        rootView = view

        Injector.obtain<FridgeComponent>(applicationContext)
            .plusMainComponent()
            .create(this, view, getPage(intent), this)
            .inject(this)

        view.makeWindowSexy()
        inflateComponents(view, savedInstanceState)
    }

    @CheckResult
    private fun getPage(intent: Intent): MainPage {
        val presenceString =
            intent.getStringExtra(FridgeItem.Presence.KEY) ?: FridgeItem.Presence.NEED.name
        return when (FridgeItem.Presence.valueOf(presenceString)) {
            FridgeItem.Presence.HAVE -> MainPage.HAVE
            FridgeItem.Presence.NEED -> MainPage.NEED
        }
    }

    override fun checkVersionForUpdate() {
        viewModel.checkForUpdates()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        viewModel.selectPage(getPage(intent))
    }

    override fun onStart() {
        super.onStart()
        checkNearbyFragmentPermissions()
        beginWork()
    }

    private fun beginWork() {
        this.lifecycleScope.launch(context = Dispatchers.Default) {
            requireNotNull(butler).initOnAppStart(
                Butler.Parameters(
                    forceNotification = true
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        clearLaunchNotification()
    }

    private fun clearLaunchNotification() {
        val notificationId = intent.getIntExtra(NotificationHandler.NOTIFICATION_ID_KEY, 0)
        if (notificationId != 0) {
            Notifications.cancel(this, notificationId)
        }
    }

    private fun inflateComponents(
        constraintLayout: ConstraintLayout,
        savedInstanceState: Bundle?
    ) {
        val container = requireNotNull(container)
        val toolbar = requireNotNull(toolbar)
        val navigation = requireNotNull(navigation)
        stateSaver = createComponent(
            savedInstanceState, this, viewModel,
            container,
            toolbar,
            navigation
        ) {
            return@createComponent when (it) {
                is MainControllerEvent.PushHave -> pushHave()
                is MainControllerEvent.PushNeed -> pushNeed()
                is MainControllerEvent.PushCategory -> pushCategory()
                is MainControllerEvent.PushNearby -> pushNearby()
                is MainControllerEvent.NavigateToSettings -> showSettingsDialog()
                is MainControllerEvent.VersionCheck -> checkForUpdate()
            }
        }

        constraintLayout.layout {

            toolbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            navigation.also {
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainHeight(it.id(), ConstraintSet.WRAP_CONTENT)
            }

            container.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
    }

    private fun pushHave() {
        pushPresenceFragment(FridgeItem.Presence.HAVE)
    }

    private fun pushNeed() {
        pushPresenceFragment(FridgeItem.Presence.NEED)
    }

    private fun pushCategory() {
        val fm = supportFragmentManager

        Timber.d("Pushing category fragment")
        fm.commitNow(this) {
            replace(fragmentContainerId, CategoryFragment.newInstance(), CategoryFragment.TAG)
        }
    }

    private fun pushNearby() {
        val fm = supportFragmentManager
        if (
            fm.findFragmentByTag(MapFragment.TAG) == null &&
            fm.findFragmentByTag(PermissionFragment.TAG) == null
        ) {
            commitNearbyFragment(fm)
        }
    }

    private fun checkNearbyFragmentPermissions() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PermissionFragment.TAG) != null) {
            if (viewModel.canShowMap()) {
                // Replace permission with map
                commitNearbyFragment(fm)
            }
        } else if (fm.findFragmentByTag(MapFragment.TAG) != null) {
            if (!viewModel.canShowMap()) {
                // Replace map with permission
                commitNearbyFragment(fm)
            }
        }
    }

    private fun commitNearbyFragment(fragmentManager: FragmentManager) {
        fragmentManager.commitNow(this) {
            val container = fragmentContainerId
            if (viewModel.canShowMap()) {
                replace(container, MapFragment.newInstance(), MapFragment.TAG)
            } else {
                replace(
                    container,
                    PermissionFragment.newInstance(container),
                    PermissionFragment.TAG
                )
            }
        }
    }

    private fun showSettingsDialog() {
        SettingsDialog().show(this, SettingsDialog.TAG)
    }

    private fun pushPresenceFragment(presence: FridgeItem.Presence) {
        val fm = supportFragmentManager

        Timber.d("Pushing fragment: $presence")
        val tag = "${EntryFragment.TAG}|${presence.name}"
        fm.commitNow(this) {
            replace(fragmentContainerId, EntryFragment.newInstance(presence), tag)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        stateSaver?.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == DeviceGps.ENABLE_GPS_REQUEST_CODE) {
            handleGpsRequest(resultCode)
        }
    }

    private fun handleGpsRequest(resultCode: Int) {
        viewModel.publishGpsChange(resultCode == Activity.RESULT_OK)
    }

    override fun onDestroy() {
        super.onDestroy()
        rootView = null
        stateSaver = null

        toolbar = null
        container = null
        navigation = null

        factory = null
    }
}
