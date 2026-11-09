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
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.ui.WsListLoading
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

    private var loading: WsListLoading? = null
    private var hasResumed = false
    private var period: Period
        get() = Period.valueOf(viewModel.selectedPeriod)
        set(value) { viewModel.selectedPeriod = value.name }
    private var statusFilter: String
        get() = viewModel.selectedStatus
        set(value) { viewModel.selectedStatus = value }
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
        binding.expRefresh.setOnRefreshListener { load(ListLoadKind.PULL_REFRESH) }

        binding.expSegment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || ignoreSegment) return@addOnButtonCheckedListener
            period = if (checkedId == R.id.exp_btn_month) Period.THIS_MONTH else Period.LAST_7_DAYS
            claimsAdapter.collapseAll()
            load(ListLoadKind.FILTER)
        }

        binding.expFab.setOnClickListener { AppNavigator.navigateToAddExpenseFragment() }

        viewModel.blockingLoading.observe(viewLifecycleOwner) { busy ->
            if (busy) showDialog() else hideDialog()
        }
        loading = WsListLoading.attach(viewLifecycleOwner, binding.expRefresh, { load(ListLoadKind.REFRESH) },
            emptyTitle = getString(R.string.exp_empty_title), emptyMessage = getString(R.string.exp_empty_message))
            .also { it.bind(viewModel.listLoading("claims")) }
        binding.emptyState.hide()
        viewModel.listLoading("claims").observe(viewLifecycleOwner) {
            binding.expRefresh.isRefreshing = it.pullRefreshing
            renderDates()
        }
        viewModel.claims.observe(viewLifecycleOwner) { payload ->
            payload?.let { showClaims(it.expenses.orEmpty(), it.expense_status.orEmpty()) }
        }
        viewModel.deleteClaimResult.observeResult(
            viewLifecycleOwner,
            onError = { message -> requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_delete_failed) }) }
        ) { payload, _ ->
            requireContext().showSuccessMsg(payload.status_message?.takeIf { it.isNotBlank() } ?: getString(R.string.exp_deleted_toast))
            claimsAdapter.collapseAll()
            load(ListLoadKind.REFRESH)
        }
    }

    override fun onResume() {
        super.onResume()
        ignoreSegment = true
        binding.expSegment.check(if (period == Period.THIS_MONTH) R.id.exp_btn_month else R.id.exp_btn_week)
        ignoreSegment = false
        if (hasResumed || (viewModel.claims.value == null && viewModel.listLoading("claims").value?.error == null &&
                viewModel.listLoading("claims").value?.isLoading != true)) load(ListLoadKind.REFRESH)
        hasResumed = true
    }
    private fun load(kind: ListLoadKind) {
        val (start, end) = dateRange()
        renderDates()
        viewModel.loadClaims(start, end, statusFilter, kind)
    }

    private fun renderDates() {
        val (start, end) = dateRange()
        val text = getString(R.string.exp_dates_fmt, WsFormat.reversedDay(start), WsFormat.reversedDay(end))
        val previous = viewModel.claims.value != null && viewModel.displayedClaimsQuery != Triple(start, end, statusFilter)
        binding.expTvDates.text = if (previous) text + "\n" + getString(R.string.list_previous_results) else text
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
        binding.emptyState.hide()
        renderDates()
    }

    private fun selectStatus(chip: ExpenseStatusChip) {
        val id = chip.id.orEmpty()
        if (id == statusFilter) return
        statusFilter = id
        chipAdapter.selectedId = id
        claimsAdapter.collapseAll()
        load(ListLoadKind.FILTER)
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
        loading?.release()
        loading = null
        binding.expList.adapter = null
        binding.expChips.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
