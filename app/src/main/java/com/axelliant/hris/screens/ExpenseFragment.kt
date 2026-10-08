package com.axelliant.hris.screens

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ExpChipAdapter
import com.axelliant.hris.adapter.ExpenseAdapter
import com.axelliant.hris.adapter.ExpenseDetailBinder
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentExpenseBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.requests.ExpenseClaim
import com.axelliant.hris.model.requests.ExpenseStatusChip
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Date

/**
 * The expense claim list: the period and status filters stay at the top, each claim is a card below
 * that opens to its line items and, while it is a draft, to Edit and Delete.
 */
class ExpenseFragment : BaseFragment() {

    private enum class Period { LAST_7_DAYS, THIS_MONTH }

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModel()
    private lateinit var claimsAdapter: ExpenseAdapter
    private lateinit var chipAdapter: ExpChipAdapter

    private var period = Period.LAST_7_DAYS
    private var statusFilter = ""
    private var ignoreSegment = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        claimsAdapter = ExpenseAdapter(
            onEdit = ::editClaim,
            onDelete = ::confirmDelete,
            onPreview = { claim, index -> ExpenseDetailBinder.openPreview(claim, index) }
        )
        binding.expList.layoutManager = LinearLayoutManager(requireContext())
        binding.expList.adapter = claimsAdapter

        chipAdapter = ExpChipAdapter { chip -> selectStatus(chip) }
        binding.expChips.adapter = chipAdapter

        binding.expRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.expRefresh.setOnRefreshListener { load(showLoading = false) }

        binding.expSegment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || ignoreSegment) return@addOnButtonCheckedListener
            period = if (checkedId == R.id.exp_btn_month) Period.THIS_MONTH else Period.LAST_7_DAYS
            claimsAdapter.collapseAll()
            load(showLoading = claimsAdapter.itemCount == 0)
        }

        binding.expFab.setOnClickListener { AppNavigator.navigateToAddExpenseFragment() }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.claims.observeResult(
            viewLifecycleOwner,
            onError = { message ->
                binding.expRefresh.isRefreshing = false
                requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_load_failed) })
            }
        ) { payload, _ ->
            binding.expRefresh.isRefreshing = false
            showClaims(payload.expenses.orEmpty(), payload.expense_status.orEmpty())
        }
        viewModel.deleteClaimResult.observeResult(
            viewLifecycleOwner,
            onError = { message -> requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_delete_failed) }) }
        ) { payload, _ ->
            requireContext().showSuccessMsg(payload.status_message?.takeIf { it.isNotBlank() } ?: getString(R.string.exp_deleted_toast))
            claimsAdapter.collapseAll()
            load(showLoading = false)
        }
    }

    override fun onResume() {
        super.onResume()
        // Like iOS, the list opens on the last 7 days every time the screen appears (this also
        // refreshes it after a claim was created, edited or deleted).
        period = Period.LAST_7_DAYS
        ignoreSegment = true
        binding.expSegment.check(R.id.exp_btn_week)
        ignoreSegment = false
        claimsAdapter.collapseAll()
        load(showLoading = claimsAdapter.itemCount == 0)
    }

    private fun load(showLoading: Boolean) {
        val (start, end) = dateRange()
        binding.expTvDates.text = getString(
            R.string.exp_dates_fmt,
            WsFormat.reversedDay(start),
            WsFormat.reversedDay(end)
        )
        viewModel.loadClaims(start, end, statusFilter, showLoading)
    }

    /** Last 7 days (the same window as the other lists) or the current calendar month, as "yyyy-MM-dd". */
    private fun dateRange(): Pair<String, String> = when (period) {
        Period.LAST_7_DAYS -> WsFormat.apiDay(Utils.getLastWeek()) to WsFormat.apiDay(Date())
        Period.THIS_MONTH -> WsFormat.apiDay(Utils.getFirstDayOfMonth()) to WsFormat.apiDay(Utils.getLastDayOfMonth())
    }

    private fun showClaims(claims: List<ExpenseClaim>, statuses: List<ExpenseStatusChip>) {
        val chips = listOf(ExpenseStatusChip(id = "", title = getString(R.string.exp_all), count = null)) + statuses
        chipAdapter.selectedId = statusFilter
        chipAdapter.submitList(chips)
        claimsAdapter.submitList(claims)
        if (claims.isEmpty()) binding.emptyState.show() else binding.emptyState.hide()
    }

    private fun selectStatus(chip: ExpenseStatusChip) {
        val id = chip.id.orEmpty()
        if (id == statusFilter) return
        statusFilter = id
        chipAdapter.selectedId = id
        claimsAdapter.collapseAll()
        load(showLoading = claimsAdapter.itemCount == 0)
    }

    private fun editClaim(claim: ExpenseClaim) {
        AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
            putString(AppConst.ExpenseRequestIDParam, claim.name)
            putString(AppConst.ExpenseRequestParam, Gson().toJson(claim.editLines()))
            putString(AppConst.ExpenseRequestAttachments, Gson().toJson(claim.attachmentList()))
        })
    }

    private fun confirmDelete(claim: ExpenseClaim) {
        val name = claim.name?.takeIf { it.isNotBlank() }
        if (name == null) {
            requireContext().showErrorMsg(getString(R.string.exp_missing_id))
            return
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exp_delete_title)
            .setMessage(getString(R.string.exp_delete_message, name))
            .setPositiveButton(R.string.exp_delete) { _, _ -> viewModel.deleteClaim(name) }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            ?.setTextColor(requireContext().getColor(R.color.ws_red))
    }

    override fun onDestroyView() {
        hideDialog()
        _binding = null
        super.onDestroyView()
    }
}
