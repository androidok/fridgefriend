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

package com.pyamsoft.fridge.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.pyamsoft.fridge.BuildConfig
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.R
import com.pyamsoft.fridge.butler.Butler
import com.pyamsoft.fridge.butler.notification.NotificationHandler
import com.pyamsoft.fridge.butler.work.OrderFactory
import com.pyamsoft.fridge.category.CategoryFragment
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.fridge.db.store.NearbyStore
import com.pyamsoft.fridge.db.zone.NearbyZone
import com.pyamsoft.fridge.entry.EntryFragment
import com.pyamsoft.fridge.initOnAppStart
import com.pyamsoft.fridge.locator.DeviceGps
import com.pyamsoft.fridge.map.MapFragment
import com.pyamsoft.fridge.permission.PermissionFragment
import com.pyamsoft.fridge.setting.SettingsFragment
import com.pyamsoft.fridge.ui.SnackbarContainer
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.notify.toNotifyId
import com.pyamsoft.pydroid.ui.Injector
import com.pyamsoft.pydroid.ui.arch.viewModelFactory
import com.pyamsoft.pydroid.ui.changelog.ChangeLogActivity
import com.pyamsoft.pydroid.ui.changelog.buildChangeLog
import com.pyamsoft.pydroid.ui.databinding.LayoutConstraintBinding
import com.pyamsoft.pydroid.ui.util.commit
import com.pyamsoft.pydroid.ui.util.layout
import com.pyamsoft.pydroid.util.doOnStart
import com.pyamsoft.pydroid.util.stableLayoutHideNavigation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.fragment.R as R2

internal class MainActivity : ChangeLogActivity(), VersionChecker {

    override val checkForUpdates = false

    override val applicationIcon = R.mipmap.ic_launcher

    override val versionName = BuildConfig.VERSION_NAME

    override val changelog = buildChangeLog {
        change("Faster database performance")
        change("Better UI responsiveness")
        bugfix("Fix Map page sometimes rendering incorrectly")
        change("Schedule notifications more efficiently to save battery")
    }

    override val fragmentContainerId: Int
        get() = requireNotNull(container).id()

    override val snackbarRoot: ViewGroup
        get() {
            val fm = supportFragmentManager
            val fragment = fm.findFragmentById(fragmentContainerId)
            if (fragment is SnackbarContainer) {
                val container = fragment.container()
                if (container != null) {
                    Timber.d("Return fragment snackbar container: $fragment $container")
                    return container
                }
            }

            val fallbackContainer = requireNotNull(snackbar?.container())
            Timber.d("Return activity snackbar container: $fallbackContainer")
            return fallbackContainer
        }

    private var stateSaver: StateSaver? = null

    @JvmField
    @Inject
    internal var butler: Butler? = null

    @JvmField
    @Inject
    internal var orderFactory: OrderFactory? = null

    @JvmField
    @Inject
    internal var notificationHandler: NotificationHandler? = null

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
    internal var snackbar: MainSnackbar? = null

    @JvmField
    @Inject
    internal var factory: ViewModelProvider.Factory? = null
    private val viewModel by viewModelFactory<MainViewModel> { factory }

    private val handler by lazy(LazyThreadSafetyMode.NONE) { Handler(Looper.getMainLooper()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Fridge)
        super.onCreate(savedInstanceState)
        val binding = LayoutConstraintBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Injector.obtain<FridgeComponent>(applicationContext)
            .plusMainComponent()
            .create(this, binding.layoutConstraint, this)
            .inject(this)

        stableLayoutHideNavigation()

        inflateComponents(binding.layoutConstraint, savedInstanceState)
        beginWork()

        handleIntentExtras(intent)

        // Load default page
        if (savedInstanceState == null) {
            Timber.d("Load default ENTRIES page")
            viewModel.selectPage(MainPage.ENTRIES)
        }
    }

    override fun checkVersionForUpdate() {
        Timber.d("Begin update check")
        viewModel.checkForUpdates()
    }

    @CheckResult
    private fun handleEntryIntent(intent: Intent): Boolean {
        val stringEntryId = intent.getStringExtra(NotificationHandler.KEY_ENTRY_ID) ?: return false
        viewModel.selectPage(MainPage.ENTRIES) {
            val pres = intent.getStringExtra(NotificationHandler.KEY_PRESENCE_TYPE)
            if (pres == null) {
                Timber.d("New intent had entry key but no presence type")
                return@selectPage
            }

            val entryId = FridgeEntry.Id(stringEntryId)
            val presence = FridgeItem.Presence.valueOf(pres)
            Timber.d("Entries page selected, load entry $entryId with presence: $presence")

            // No good way to figure out when the FM is done transacting from a different context I think
            handler.removeCallbacksAndMessages(null)

            // Assuming that the FM handler uses the main thread, we post twice
            // The first post puts us into the queue and basically waits for everything to clear out
            // this would include the FM pending transactions which may also include the page select
            // commit.
            // Then the second post queues up a new push which will then call its own commit, hopefully once
            // the EntryFragment is done mounting.
            handler.post {
                handler.post {
                    EntryFragment.pushDetailPage(
                        supportFragmentManager,
                        this,
                        fragmentContainerId,
                        entryId,
                        presence
                    )
                }
            }
        }
        return true
    }

    @CheckResult
    private fun handleNearbyIntent(intent: Intent): Boolean {
        val longNearbyId = intent.getLongExtra(NotificationHandler.KEY_NEARBY_ID, 0L)
        if (longNearbyId == 0L) {
            return false
        }

        viewModel.selectPage(MainPage.NEARBY) {
            val nearbyType = intent.getStringExtra(NotificationHandler.KEY_NEARBY_TYPE)
            if (nearbyType == null) {
                Timber.d("New intent had nearby key but no type")
                return@selectPage
            }
            val nearbyStoreId: NearbyStore.Id
            val nearbyZoneId: NearbyZone.Id
            when (nearbyType) {
                NotificationHandler.VALUE_NEARBY_TYPE_STORE -> {
                    nearbyStoreId = NearbyStore.Id(longNearbyId)
                    nearbyZoneId = NearbyZone.Id.EMPTY
                }
                NotificationHandler.VALUE_NEARBY_TYPE_ZONE -> {
                    nearbyStoreId = NearbyStore.Id.EMPTY
                    nearbyZoneId = NearbyZone.Id(longNearbyId)
                }
                else -> return@selectPage
            }

            Timber.d("Map page selected, load nearby: $nearbyStoreId $nearbyZoneId")
        }
        return true
    }

    private fun handleIntentExtras(intent: Intent) {
        if (handleEntryIntent(intent)) {
            Timber.d("New intent handled entry extras")
            return
        }

        if (handleNearbyIntent(intent)) {
            Timber.d("New intent handled nearby extras")
            return
        }

        Timber.d("New intent no extras")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras(intent)
    }

    override fun onBackPressed() {
        onBackPressedDispatcher.also { dispatcher ->
            if (dispatcher.hasEnabledCallbacks()) {
                dispatcher.onBackPressed()
            } else {
                super.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkNearbyFragmentPermissions()
    }

    private fun beginWork() {
        this.lifecycleScope.launch(context = Dispatchers.Default) {
            initOnAppStart(requireNotNull(butler), requireNotNull(orderFactory))
        }
    }

    override fun onResume() {
        super.onResume()
        clearLaunchNotification()
    }

    private fun clearLaunchNotification() {
        val id = intent.getIntExtra(NotificationHandler.KEY_NOTIFICATION_ID, 0)
        if (id != 0) {
            requireNotNull(notificationHandler).cancel(id.toNotifyId())
        }
    }

    private fun inflateComponents(
        constraintLayout: ConstraintLayout,
        savedInstanceState: Bundle?
    ) {
        val container = requireNotNull(container)
        val toolbar = requireNotNull(toolbar)
        val navigation = requireNotNull(navigation)
        val snackbar = requireNotNull(snackbar)
        stateSaver = createComponent(
            savedInstanceState,
            this,
            viewModel,
            container,
            toolbar,
            navigation,
            snackbar
        ) {
            return@createComponent when (it) {
                is MainControllerEvent.PushEntry -> pushEntry(it.previousPage)
                is MainControllerEvent.PushCategory -> pushCategory(it.previousPage)
                is MainControllerEvent.PushNearby -> pushNearby(it.previousPage)
                is MainControllerEvent.PushSettings -> pushSettings(it.previousPage)
                is MainControllerEvent.VersionCheck -> performUpdate()
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
                connect(
                    it.id(),
                    ConstraintSet.BOTTOM,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.BOTTOM
                )
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }

            snackbar.also {
                connect(it.id(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.BOTTOM, navigation.id(), ConstraintSet.TOP)
                connect(it.id(), ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                connect(it.id(), ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constrainHeight(it.id(), ConstraintSet.MATCH_CONSTRAINT)
                constrainWidth(it.id(), ConstraintSet.MATCH_CONSTRAINT)
            }
        }
    }

    private fun performUpdate() {
        Timber.d("Do check for update")
        checkForUpdate()
    }

    private fun pushSettings(previousPage: MainPage?) {
        commitPage(
            SettingsFragment.newInstance(),
            MainPage.SETTINGS,
            previousPage,
            SettingsFragment.TAG
        )
    }

    private fun pushCategory(previousPage: MainPage?) {
        commitPage(
            CategoryFragment.newInstance(),
            MainPage.CATEGORY,
            previousPage,
            CategoryFragment.TAG
        )
    }

    private fun pushNearby(previousPage: MainPage?) {
        val fm = supportFragmentManager
        if (
            fm.findFragmentByTag(MapFragment.TAG) == null &&
            fm.findFragmentByTag(PermissionFragment.TAG) == null
        ) {
            commitNearbyFragment(previousPage)
        }
    }

    private fun checkNearbyFragmentPermissions() {
        val fm = supportFragmentManager
        if (fm.findFragmentByTag(PermissionFragment.TAG) != null) {
            viewModel.withForegroundPermission(withPermission = {
                // Replace permission with map
                // Don't need animation because we are already on this page
                Timber.d("Permission gained, commit Map fragment")
                commitMapFragment(null, forcePush = true)
            })
        } else if (fm.findFragmentByTag(MapFragment.TAG) != null) {
            viewModel.withForegroundPermission(withoutPermission = {
                // Replace map with permission
                // Don't need animation because we are already on this page
                Timber.d("Permission lost, commit Permission fragment")
                commitPermissionFragment(null, forcePush = true)
            })
        }
    }

    private fun commitMapFragment(previousPage: MainPage?, forcePush: Boolean = false) {
        commitPage(
            MapFragment.newInstance(),
            MainPage.NEARBY,
            previousPage,
            MapFragment.TAG,
            forcePush
        )
    }

    private fun commitPermissionFragment(previousPage: MainPage?, forcePush: Boolean = false) {
        commitPage(
            PermissionFragment.newInstance(fragmentContainerId),
            MainPage.NEARBY,
            previousPage,
            PermissionFragment.TAG,
            forcePush
        )
    }

    private fun commitNearbyFragment(previousPage: MainPage?) {
        viewModel.withForegroundPermission(
            withPermission = { commitMapFragment(previousPage) },
            withoutPermission = { commitPermissionFragment(previousPage) }
        )
    }

    private fun pushEntry(previousPage: MainPage?) {
        commitPage(
            EntryFragment.newInstance(fragmentContainerId),
            MainPage.ENTRIES,
            previousPage,
            EntryFragment.TAG
        )
    }

    private fun commitPage(
        fragment: Fragment,
        newPage: MainPage,
        previousPage: MainPage?,
        tag: String,
        forcePush: Boolean = false
    ) {
        val fm = supportFragmentManager
        val container = fragmentContainerId

        val push = when {
            previousPage != null -> true
            fm.findFragmentById(container) == null -> true
            else -> false
        }

        if (push || forcePush) {
            if (forcePush) {
                Timber.d("Force commit fragment: $tag")
            } else {
                Timber.d("Commit fragment: $tag")
            }

            this.doOnStart {
                // Clear the back stack (for entry->detail stack)
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

                fm.commit(this) {
                    decideAnimationForPage(previousPage, newPage)
                    replace(container, fragment, tag)
                }
            }
        }
    }

    private fun FragmentTransaction.decideAnimationForPage(oldPage: MainPage?, newPage: MainPage) {
        val (enter, exit) = when (newPage) {
            MainPage.ENTRIES -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                MainPage.CATEGORY, MainPage.NEARBY, MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.ENTRIES -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.CATEGORY -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                MainPage.ENTRIES -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.NEARBY, MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.CATEGORY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.NEARBY -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                MainPage.ENTRIES, MainPage.CATEGORY -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.SETTINGS -> R.anim.slide_in_left to R.anim.slide_out_right
                MainPage.NEARBY -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
            MainPage.SETTINGS -> when (oldPage) {
                null -> R2.anim.fragment_open_enter to R2.anim.fragment_open_exit
                MainPage.ENTRIES, MainPage.CATEGORY, MainPage.NEARBY -> R.anim.slide_in_right to R.anim.slide_out_left
                MainPage.SETTINGS -> throw IllegalStateException("Cannot move from $oldPage to $newPage")
            }
        }
        setCustomAnimations(enter, exit, enter, exit)
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
        stateSaver = null

        toolbar = null
        container = null
        navigation = null
        snackbar = null

        factory = null

        handler.removeCallbacksAndMessages(null)
    }
}
