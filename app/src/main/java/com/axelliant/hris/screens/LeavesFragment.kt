package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.LvGridGapDecoration
import com.axelliant.hris.adapter.LvQuotaTileAdapter
import com.axelliant.hris.adapter.LvRequestAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentLeavesBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.leaveatt.LeaveRecord
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsStatus
import com.axelliant.hris.viewmodel.LeaveViewModel
import com.google.gson.Gson
import com.intuit.sdp.R as SdpR
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * Leave screen (iOS "Leave"): the assigned quotas as tiles, this year's leave requests as cards, and a
 * FAB that opens the leave request form. A request opens its details in a [WsDetailSheet].
 */
class LeavesFragment : BaseFragment() {

    private var _binding: FragmentLeavesBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LeaveViewModel by viewModel()

    private val quotaAdapter = LvQuotaTileAdapter()
    private val historyAdapter = LvRequestAdapter { showDetails(it) }

    private var quotasLoaded = false
    private var pendingCalls = 0
    private var errorShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeavesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val gap = resources.getDimensionPixelSize(SdpR.dimen._10sdp)

        binding.lvRvQuotas.layoutManager = GridLayoutManager(requireContext(), QUOTA_COLUMNS)
        binding.lvRvQuotas.addItemDecoration(LvGridGapDecoration(gap, QUOTA_COLUMNS))
        binding.lvRvQuotas.adapter = quotaAdapter

        binding.lvRvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.lvRvHistory.addItemDecoration(LvGridGapDecoration(gap, 1))
        binding.lvRvHistory.adapter = historyAdapter

        binding.lvSwipe.setColorSchemeResources(R.color.ws_primary)
        binding.lvSwipe.setOnRefreshListener { load() }

        binding.lvFab.setOnClickListener {
            AppNavigator.navigateToRequest(bundleOf(AppConst.RequestType to RequestFilter.LEAVE.name))
        }

        observeViewModel()

        childFragmentManager.setFragmentResultListener(SHEET_KEY, viewLifecycleOwner) { _, result ->
            if (result.getString(WsDetailSheet.RESULT_ACTION) == ACTION_EDIT) {
                viewModel.selectedLeave?.let { editRequest(it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun observeViewModel() {
        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })

        viewModel.quotas.observeResult(viewLifecycleOwner, onError = ::onLoadError) { data, _ ->
            quotasLoaded = true
            val quotas = data.allocations
            quotaAdapter.submitList(quotas)
            binding.lvRvQuotas.visibility = if (quotas.isEmpty()) View.GONE else View.VISIBLE
            if (quotas.isEmpty()) binding.lvQuotaEmpty.show() else binding.lvQuotaEmpty.hide()
            callFinished()
        }

        viewModel.history.observeResult(viewLifecycleOwner, onError = ::onLoadError) { data, _ ->
            showHistory(data.leaves.orEmpty())
            callFinished()
        }
    }

    /** Pull-to-refresh and resume both land here; only the first balances load shows the overlay. */
    private fun load() {
        pendingCalls = 2
        errorShown = false
        viewModel.loadQuotas(showLoading = !quotasLoaded && !binding.lvSwipe.isRefreshing)
        viewModel.loadHistory()
    }

    private fun callFinished() {
        if (--pendingCalls <= 0) binding.lvSwipe.isRefreshing = false
    }

    private fun onLoadError(message: String) {
        callFinished()
        // Quotas and history usually fail together; one message is enough.
        if (!errorShown) {
            errorShown = true
            requireContext().showErrorMsg(message, "Leave")
        }
    }

    private fun showHistory(leaves: List<LeaveRecord>) {
        val sorted = leaves.sortedByDescending { WsFormat.parse(it.from_date)?.time ?: 0L }
        historyAdapter.submitList(sorted)
        binding.lvRvHistory.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
        if (sorted.isEmpty()) binding.lvHistoryEmpty.show() else binding.lvHistoryEmpty.hide()

        binding.lvChipRow.visibility = if (sorted.isEmpty()) View.GONE else View.VISIBLE
        val pending = sorted.count { it.rawStatus.equals("Open", true) || it.rawStatus.equals("Pending", true) }
        val approved = sorted.count { it.rawStatus.equals("Approved", true) }
        WsStatus.badge(binding.lvChipPending, getString(R.string.lv_count_pending, pending), WsStatus.colorFor(requireContext(), "Pending"))
        WsStatus.badge(binding.lvChipApproved, getString(R.string.lv_count_approved, approved), WsStatus.colorFor(requireContext(), "Approved"))
    }

    private fun showDetails(leave: LeaveRecord) {
        viewModel.selectedLeave = leave
        val fields = listOf(
            WsDetailSheet.Field(getString(R.string.lv_field_from), WsFormat.date(leave.from_date), half = true),
            WsDetailSheet.Field(getString(R.string.lv_field_to), WsFormat.date(leave.to_date), half = true),
            WsDetailSheet.Field(getString(R.string.lv_field_days), WsFormat.days(leave.days), half = true),
            WsDetailSheet.Field(
                getString(R.string.lv_field_paid),
                leave.paid?.let { getString(if (it) R.string.lv_yes else R.string.lv_no) },
                half = true,
                accent = leave.paid == false
            ),
            WsDetailSheet.Field(getString(R.string.lv_field_reason), leave.reason),
            WsDetailSheet.Field(getString(R.string.lv_field_approver), leave.leave_approver),
            WsDetailSheet.Field(getString(R.string.lv_field_submitted), WsFormat.date(leave.post_date)),
            WsDetailSheet.Field(getString(R.string.lv_field_reference), leave.name)
        )
        // Only a request HRIS still calls "Open" can be changed.
        val actions = if (leave.rawStatus.equals("Open", true)) {
            listOf(WsDetailSheet.Action(ACTION_EDIT, getString(R.string.lv_edit_request)))
        } else {
            emptyList()
        }
        WsDetailSheet.show(
            childFragmentManager,
            title = leave.leave_type?.takeIf { it.isNotBlank() } ?: getString(R.string.lv_leave_fallback),
            subtitle = getString(R.string.lv_sheet_subtitle),
            status = WsStatus.displayLeave(leave.rawStatus),
            fields = fields,
            actions = actions,
            requestKey = SHEET_KEY
        )
    }

    private fun editRequest(leave: LeaveRecord) {
        AppNavigator.navigateToRequest(
            bundleOf(
                AppConst.RequestType to RequestFilter.LEAVE.name,
                AppConst.LeaveRequestParam to Gson().toJson(leave)
            )
        )
    }

    override fun onDestroyView() {
        binding.lvSwipe.isRefreshing = false
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val QUOTA_COLUMNS = 2
        const val SHEET_KEY = "leaveDetailSheet"
        const val ACTION_EDIT = "edit"
    }
}
