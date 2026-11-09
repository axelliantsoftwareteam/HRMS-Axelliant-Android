package com.axelliant.hris.screens

import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.RequestsOverviewViewModel
import com.axelliant.hris.viewmodel.RequestsOverviewViewModel.Period
import com.axelliant.hris.viewmodel.RequestsDateRange
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentRequestsOverviewBinding
import com.google.android.material.datepicker.MaterialDatePicker

/** Binds the Requests period controls to the same themed range picker used by Attendance. */
internal class RequestsPeriodPicker(
    private val parent: Fragment,
    private val binding: FragmentRequestsOverviewBinding,
    private val model: RequestsOverviewViewModel
) {
    private val owner = parent.viewLifecycleOwner
    private var restoring = false

    /** Restores applied controls and reconnects any picker restored by the child fragment manager. */
    fun bind() {
        render()
        binding.rqSegment.addOnButtonCheckedListener { _, id, checked ->
            if (!checked || restoring) return@addOnButtonCheckedListener
            if (id == R.id.rq_btn_custom) pickRange() else {
                model.period = if (id == R.id.rq_btn_month) Period.THIS_MONTH else Period.LAST_7_DAYS
                render()
                model.load(ListLoadKind.FILTER)
            }
        }
        binding.rqBtnCustom.setOnClickListener { if (model.period == Period.CUSTOM) pickRange() }
        @Suppress("UNCHECKED_CAST")
        val restored = parent.childFragmentManager.findFragmentByTag(TAG) as? MaterialDatePicker<Pair<Long, Long>>
        restored?.let(::connect)
    }

    private fun render() {
        restoring = true
        binding.rqSegment.check(when (model.period) {
            Period.LAST_7_DAYS -> R.id.rq_btn_week
            Period.THIS_MONTH -> R.id.rq_btn_month
            Period.CUSTOM -> R.id.rq_btn_custom
        })
        restoring = false
        val (start, end) = model.currentRange()
        binding.rqTvRange.text = parent.getString(R.string.att_date_range, WsFormat.reversedDay(start), WsFormat.reversedDay(end))
    }

    private fun pickRange() {
        val manager = parent.childFragmentManager
        if (manager.isStateSaved || manager.findFragmentByTag(TAG) != null) return
        val (start, end) = model.currentRange()
        val builder = MaterialDatePicker.Builder.dateRangePicker().setTitleText(R.string.att_range_picker_title)
        val first = RequestsDateRange.utcMillis(start)
        val last = RequestsDateRange.utcMillis(end)
        if (first != null && last != null) builder.setSelection(Pair(first, last))
        val picker = builder.build()
        connect(picker)
        picker.show(manager, TAG)
    }

    private fun connect(picker: MaterialDatePicker<Pair<Long, Long>>) {
        picker.addOnPositiveButtonClickListener { selection ->
            if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return@addOnPositiveButtonClickListener
            val first = selection.first ?: return@addOnPositiveButtonClickListener
            val last = selection.second ?: return@addOnPositiveButtonClickListener
            model.setCustomRange(first, last)
            render()
            model.load(ListLoadKind.FILTER)
        }
        picker.addOnDismissListener {
            if (owner.lifecycle.currentState != Lifecycle.State.DESTROYED) render()
        }
    }

    private companion object {
        const val TAG = "requests-date-range"
    }
}
