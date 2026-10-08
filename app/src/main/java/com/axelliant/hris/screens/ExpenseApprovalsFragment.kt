package com.axelliant.hris.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ExpenseApprovalsDetailAdapter
import com.axelliant.hris.adapter.ExpenseDetailBinder
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentExpenseApprovalsBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Calendar

/** The manager's team expense claims, each expandable to its lines and, while pending, Approve / Reject. */
class ExpenseApprovalsFragment : BaseFragment() {

    private var _binding: FragmentExpenseApprovalsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModel()
    private lateinit var adapter: ExpenseApprovalsDetailAdapter

    /** The decision being sent, so the success toast matches it. */
    private var lastDecisionApprove = true
    private var hasLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpenseApprovalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ExpenseApprovalsDetailAdapter(
            onDecision = ::confirmDecision,
            onPreview = { claim, index -> ExpenseDetailBinder.openPreview(claim, index) }
        )
        binding.expApprList.layoutManager = LinearLayoutManager(requireContext())
        binding.expApprList.adapter = adapter
        binding.expApprCount.text = "0"

        binding.expApprRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.expApprRefresh.setOnRefreshListener { load(showLoading = false) }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.approvals.observeResult(
            viewLifecycleOwner,
            onError = { message ->
                binding.expApprRefresh.isRefreshing = false
                requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_load_failed) })
            }
        ) { payload, _ ->
            binding.expApprRefresh.isRefreshing = false
            hasLoaded = true
            val claims = payload.expenses.orEmpty()
            adapter.submitList(claims)
            binding.expApprCount.text = claims.size.toString()
            if (claims.isEmpty()) binding.emptyState.show() else binding.emptyState.hide()
        }
        viewModel.decisionResult.observeResult(
            viewLifecycleOwner,
            onError = { message ->
                val fallback = if (lastDecisionApprove) R.string.exp_approve_failed else R.string.exp_reject_failed
                requireContext().showErrorMsg(message.ifBlank { getString(fallback) })
            }
        ) { payload, _ ->
            val fallback = if (lastDecisionApprove) R.string.exp_approved_toast else R.string.exp_rejected_toast
            requireContext().showSuccessMsg(payload.status_message?.takeIf { it.isNotBlank() } ?: getString(fallback))
            load(showLoading = false)
        }
    }

    override fun onResume() {
        super.onResume()
        load(showLoading = !hasLoaded)
    }

    /** Claims wait for a manager past a month end, so the list starts a year back. */
    private fun load(showLoading: Boolean) {
        val today = Calendar.getInstance()
        val yearAgo = (today.clone() as Calendar).apply { add(Calendar.YEAR, -1) }
        viewModel.loadApprovals(WsFormat.apiDay(yearAgo), WsFormat.apiDay(today), showLoading)
    }

    private fun confirmDecision(claim: ExpenseClaim, approve: Boolean) {
        val name = claim.name?.takeIf { it.isNotBlank() }
        if (name == null) {
            requireContext().showErrorMsg(getString(R.string.exp_missing_reference))
            return
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (approve) R.string.exp_approve_title else R.string.exp_reject_title)
            .setMessage(getString(if (approve) R.string.exp_approve_message else R.string.exp_reject_message, name))
            .setPositiveButton(if (approve) R.string.exp_approve else R.string.exp_reject) { _, _ ->
                lastDecisionApprove = approve
                viewModel.decide(name, approve)
            }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
        if (!approve) {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setTextColor(requireContext().getColor(R.color.ws_red))
        }
    }

    override fun onDestroyView() {
        hideDialog()
        _binding = null
        super.onDestroyView()
    }
}
