package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ApprovalRow
import com.axelliant.hris.adapter.ApprovalsAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentApprovalsBinding
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.ui.WsListLoading
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.approvals.ApprovalItem
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.ApprovalsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Manager's Approvals inbox: pending requests as cards with Approve/Reject, bulk actions and paging. */
class ApprovalsFragment : BaseFragment() {

    private var _binding: FragmentApprovalsBinding? = null
    private val binding get() = _binding!!
    private var loading: WsListLoading? = null

    private val viewModel: ApprovalsViewModel by viewModel()

    /** The request whose details sheet is open (actions in the sheet refer to it). */
    private var sheetItem: ApprovalItem? = null

    private val adapter by lazy {
        ApprovalsAdapter(
            onOpen = ::showDetails,
            onReject = { confirm(it, approve = false) },
            onApprove = { confirm(it, approve = true) }
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentApprovalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apprList.adapter = adapter
        binding.apprList.itemAnimator = null
        binding.apprRefresh.setOnRefreshListener {
            viewModel.load(viewModel.pageIndex, ListLoadKind.PULL_REFRESH)
        }
        binding.apprBtnRetry.setOnClickListener { viewModel.load(viewModel.pageIndex) }
        binding.apprBtnRejectAll.setOnClickListener { confirm(null, approve = false) }
        binding.apprBtnApproveAll.setOnClickListener { confirm(null, approve = true) }
        binding.apprBtnPrev.setOnClickListener { viewModel.movePage(-1) }
        binding.apprBtnNext.setOnClickListener { viewModel.movePage(1) }

        childFragmentManager.setFragmentResultListener(SHEET_KEY, viewLifecycleOwner) { _, bundle ->
            val item = sheetItem ?: return@setFragmentResultListener
            when (bundle.getString(WsDetailSheet.RESULT_ACTION)) {
                ACTION_APPROVE -> confirm(item, approve = true)
                ACTION_REJECT -> confirm(item, approve = false)
            }
        }

        loading = WsListLoading.attach(viewLifecycleOwner, binding.apprRefresh, { viewModel.load() },
            emptyTitle = getString(R.string.appr_empty_title), emptyMessage = getString(R.string.appr_empty_message))
            .also { it.bind(viewModel.listLoading()) }
        viewModel.listLoading().observe(viewLifecycleOwner) { binding.apprRefresh.isRefreshing = it.pullRefreshing }
        viewModel.acting.observe(viewLifecycleOwner) { if (it) showDialog() else hideDialog() }
        viewModel.message.observe(viewLifecycleOwner, EventObserver { msg ->
            if (msg.isError) requireContext().showErrorMsg(msg.text) else requireContext().showSuccessMsg(msg.text)
        })
        viewModel.changed.observe(viewLifecycleOwner) { render() }

        render()
        if (!viewModel.hasLoaded && viewModel.listLoading().value?.isLoading != true && viewModel.listLoading().value?.error == null) viewModel.load(0)
    }

    private fun render() {
        val b = _binding ?: return
        val visible = viewModel.visibleItems
        b.apprRefresh.isEnabled = !viewModel.isActing
        b.apprCount.text = visible.size.toString()
        b.apprBulkRow.visibility = if (visible.isEmpty()) View.GONE else View.VISIBLE
        b.apprBtnRejectAll.isEnabled = viewModel.canAct
        b.apprBtnApproveAll.isEnabled = viewModel.canAct
        adapter.submitList(visible.map { ApprovalRow(it, viewModel.canAct) })

        b.apprStateBlock.visibility = if (viewModel.hasLoaded && visible.isEmpty()) View.VISIBLE else View.GONE
        if (viewModel.hasLoaded && visible.isEmpty()) b.apprState.show(R.drawable.ws_ic_verified,
            getString(R.string.appr_empty_title), getString(R.string.appr_empty_message))
        b.apprBtnRetry.visibility = View.GONE
        val showPager = viewModel.total > viewModel.items.size || viewModel.pageIndex > 0
        b.apprPager.visibility = if (showPager) View.VISIBLE else View.GONE
        b.apprPageRange.text = viewModel.pageRange
        b.apprBtnPrev.isEnabled = viewModel.canGoBack
        b.apprBtnPrev.alpha = if (viewModel.canGoBack) 1f else 0.4f
        b.apprBtnNext.isEnabled = viewModel.canGoForward
        b.apprBtnNext.alpha = if (viewModel.canGoForward) 1f else 0.4f
    }

    /** Asks for confirmation before approving/rejecting one request, or all visible ones when [item] is null. */
    private fun confirm(item: ApprovalItem?, approve: Boolean) {
        val count = if (item == null) viewModel.visibleItems.size else 1
        if (item == null && count == 0) {
            requireContext().showErrorMsg(getString(R.string.appr_none_pending))
            return
        }
        val scope = if (count > 1) getString(R.string.appr_scope_many, count) else getString(R.string.appr_scope_one)
        val verb = getString(if (approve) R.string.appr_verb_approve else R.string.appr_verb_reject)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (approve) R.string.appr_confirm_approve_title else R.string.appr_confirm_reject_title)
            .setMessage(getString(R.string.appr_confirm_message, verb, scope))
            .setNegativeButton(R.string.appr_cancel, null)
            .setPositiveButton(if (approve) R.string.appr_approve else R.string.appr_reject) { _, _ ->
                if (item != null) viewModel.act(item, approve) else viewModel.actOnVisible(approve)
            }
            .show()
    }

    private fun showDetails(item: ApprovalItem) {
        sheetItem = item
        val ctx = requireContext()
        val actions = if (item.isPending) {
            listOf(
                WsDetailSheet.Action(ACTION_REJECT, getString(R.string.appr_reject), WsDetailSheet.STYLE_DANGER),
                WsDetailSheet.Action(ACTION_APPROVE, getString(R.string.appr_approve), WsDetailSheet.STYLE_PRIMARY)
            )
        } else {
            emptyList()
        }
        WsDetailSheet.show(
            childFragmentManager,
            title = item.displayName,
            subtitle = item.displayType,
            status = item.displayStatus,
            fields = listOf(
                WsDetailSheet.Field(getString(R.string.appr_field_employee), item.displayName, half = true),
                WsDetailSheet.Field(getString(R.string.appr_field_type), item.displayType, half = true),
                WsDetailSheet.Field(
                    getString(R.string.appr_field_effective_on),
                    WsFormat.dateTime(ctx, item.effective_on_iso ?: item.effective_on),
                    half = true
                ),
                WsDetailSheet.Field(getString(R.string.appr_field_location), item.location, half = true, accent = true),
                WsDetailSheet.Field(getString(R.string.appr_field_reference), item.reference_name),
                WsDetailSheet.Field(
                    getString(R.string.appr_field_notes),
                    item.subtitle?.takeIf { it.isNotBlank() } ?: getString(R.string.appr_no_details)
                )
            ),
            actions = actions,
            requestKey = SHEET_KEY
        )
    }

    override fun onDestroyView() {
        loading?.release()
        loading = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SHEET_KEY = "approvalSheet"
        const val ACTION_APPROVE = "approve"
        const val ACTION_REJECT = "reject"
    }
}
