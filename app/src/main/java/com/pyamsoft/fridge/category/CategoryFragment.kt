/*
 * Copyright 2021 Peter Kenji Yamanaka
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

package com.pyamsoft.fridge.category

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiController
import com.pyamsoft.pydroid.arch.UnitControllerEvent
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import com.pyamsoft.pydroid.ui.R as R2
import com.pyamsoft.pydroid.ui.databinding.LayoutCoordinatorBinding
import javax.inject.Inject

internal class CategoryFragment : Fragment(), UiController<UnitControllerEvent> {

  @JvmField @Inject internal var list: CategoryListView? = null

  @JvmField @Inject internal var factory: FridgeViewModelFactory? = null
  private val viewModel by activityViewModels<CategoryViewModel> {
    factory.requireNotNull().create(requireActivity())
  }

  private var stateSaver: StateSaver? = null

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?,
  ): View? {
    return inflater.inflate(R2.layout.layout_coordinator, container, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)

    val binding = LayoutCoordinatorBinding.bind(view)
    Injector.obtainFromApplication<FridgeComponent>(view.context)
        .plusCategoryComponent()
        .create(
            requireActivity(),
            viewLifecycleOwner,
        )
        .plusCategoryComponent()
        .create(binding.layoutCoordinator)
        .inject(this)

    stateSaver =
        createComponent(
            savedInstanceState,
            viewLifecycleOwner,
            viewModel,
            this,
            list.requireNotNull(),
        ) {}
  }

  override fun onControllerEvent(event: UnitControllerEvent) {}

  override fun onSaveInstanceState(outState: Bundle) {
    stateSaver?.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    stateSaver = null
    factory = null
  }

  companion object {

    const val TAG = "CategoryFragment"

    @JvmStatic
    @CheckResult
    fun newInstance(): Fragment {
      return CategoryFragment().apply { arguments = Bundle().apply {} }
    }
  }
}
