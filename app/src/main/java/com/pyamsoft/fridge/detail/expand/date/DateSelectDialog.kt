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

package com.pyamsoft.fridge.detail.expand.date

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import androidx.annotation.CheckResult
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.pyamsoft.fridge.FridgeComponent
import com.pyamsoft.fridge.core.FridgeViewModelFactory
import com.pyamsoft.fridge.core.today
import com.pyamsoft.fridge.db.entry.FridgeEntry
import com.pyamsoft.fridge.db.item.FridgeItem
import com.pyamsoft.pydroid.arch.StateSaver
import com.pyamsoft.pydroid.arch.UiController
import com.pyamsoft.pydroid.arch.UnitViewEvent
import com.pyamsoft.pydroid.arch.UnitViewState
import com.pyamsoft.pydroid.arch.createComponent
import com.pyamsoft.pydroid.core.requireNotNull
import com.pyamsoft.pydroid.inject.Injector
import java.util.Calendar
import javax.inject.Inject

internal class DateSelectDialog :
    AppCompatDialogFragment(), UiController<DateSelectControllerEvent> {

  @JvmField @Inject internal var factory: FridgeViewModelFactory? = null
  private val viewModel by activityViewModels<DateSelectViewModel> {
    factory.requireNotNull().create(requireActivity())
  }

  private var stateSaver: StateSaver? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.obtainFromApplication<FridgeComponent>(requireContext())
        .plusDateSelectComponent()
        .inject(this)

    stateSaver =
        createComponent<UnitViewState, UnitViewEvent, DateSelectControllerEvent>(
            savedInstanceState, this, viewModel, this) {}
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

    val today = today()
    val itemId = FridgeItem.Id(requireArguments().getString(ITEM).requireNotNull())
    val entryId = FridgeEntry.Id(requireArguments().getString(ENTRY).requireNotNull())
    var initialYear = requireArguments().getInt(YEAR, 0)
    var initialMonth = requireArguments().getInt(MONTH, 0)
    var initialDay = requireArguments().getInt(DAY, 0)
    if (initialYear == 0) {
      initialYear = today.get(Calendar.YEAR)
    }
    if (initialMonth == 0) {
      initialMonth = today.get(Calendar.MONTH)
    }
    if (initialDay == 0) {
      initialDay = today.get(Calendar.DAY_OF_MONTH)
    }

    val listener =
        DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
          viewModel.handleDateSelected(itemId, entryId, year, month, dayOfMonth)
        }

    return DatePickerDialog(requireActivity(), listener, initialYear, initialMonth, initialDay)
        .apply { datePicker.minDate = today.timeInMillis }
  }

  override fun onControllerEvent(event: DateSelectControllerEvent) {
    return when (event) {
      is DateSelectControllerEvent.Close -> dismiss()
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    stateSaver?.saveState(outState)
    super.onSaveInstanceState(outState)
  }

  companion object {

    const val TAG = "DatePickerDialogFragment"
    private const val ENTRY = "entry"
    private const val ITEM = "item"
    private const val YEAR = "year"
    private const val MONTH = "month"
    private const val DAY = "day"

    @JvmStatic
    @CheckResult
    fun newInstance(
        item: FridgeItem,
        year: Int,
        month: Int,
        day: Int,
    ): DialogFragment {
      return DateSelectDialog().apply {
        arguments =
            Bundle().apply {
              putString(ITEM, item.id().id)
              putString(ENTRY, item.entryId().id)
              putInt(YEAR, year)
              putInt(MONTH, month)
              putInt(DAY, day)
            }
      }
    }
  }
}
