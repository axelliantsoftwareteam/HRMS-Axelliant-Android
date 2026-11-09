package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.RequestRowFormat
import com.axelliant.hris.adapter.RequestsOverviewAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentRequestsOverviewBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.model.requests.RequestRow
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.WsListLoading
import com.axelliant.hris.viewmodel.RequestsOverviewViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

/** The Requests screen: a summary of the latest activity, a period toggle and one card per request. */
class RequestsOverviewFragment : BaseFragment() {

    private var _binding: FragmentRequestsOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestsOverviewViewModel by viewModel()
    private lateinit var adapter: RequestsOverviewAdapter
    private var loading: WsListLoading? = null
    private var hasResumed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRequestsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RequestsOverviewAdapter { showDetail(it) }
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rqList.layoutManager = LinearLayoutManager(requireContext())
        binding.rqList.adapter = adapter

        binding.rqRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.rqRefresh.setOnRefreshListener { viewModel.load(ListLoadKind.PULL_REFRESH, force = true) }

        loading = WsListLoading(viewLifecycleOwner, binding.rqListHost, { viewModel.load() },
            emptyTitle = getString(R.string.rq_empty_title), emptyMessage = getString(R.string.rq_empty_message))
            .also { it.bind(viewModel.listLoading()) }

        RequestsPeriodPicker(this, binding, viewModel).bind()

        binding.rqFab.setOnClickListener {
            AppNavigator.navigateToRequest(Bundle().apply {
                putString(AppConst.RequestType, RequestFilter.ATTENDANCE.name)
            })
        }

        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            if (rows != null) render(rows)
        }
        viewModel.listLoading().observe(viewLifecycleOwner) { state ->
            binding.rqRefresh.isRefreshing = state.pullRefreshing
            render(viewModel.rows.value.orEmpty())
        }
        render(viewModel.rows.value.orEmpty())
    }

    override fun onResume() {
        super.onResume()
        // Recreating the view retains data/errors/in-flight work; returning from a form refreshes it.
        if (hasResumed) viewModel.load(force = true)
        else if (viewModel.rows.value == null && viewModel.listLoading().value?.error == null)
            viewModel.load(ListLoadKind.INITIAL)
        hasResumed = true
    }

    private fun render(rows: List<RequestRow>) {
        val ctx = requireContext()
        adapter.submitList(rows)

        val loaded = viewModel.rows.value != null
        val count = resources.getQuantityString(R.plurals.rq_requests_shown, rows.size, rows.size)
        binding.rqTvShown.text = when {
            !loaded -> getString(if (viewModel.listLoading().value?.error != null) R.string.ws_list_error_title else R.string.ws_loading_list)
            viewModel.displayedRange != viewModel.currentRange() -> getString(R.string.rq_previous_count, count)
            else -> count
        }
        val statuses = rows.map { RequestRowFormat.status(ctx, it).lowercase(Locale.ROOT) }
        binding.rqTvPendingCount.text =
            if (loaded) statuses.count { it.contains("pend") || it.contains("open") || it.contains("wait") }.toString() else "--"
        binding.rqTvApprovedCount.text = if (loaded) statuses.count { it.contains("approv") || it.contains("process") }.toString() else "--"
        binding.rqTvRejectedCount.text = if (loaded) statuses.count { it.contains("reject") }.toString() else "--"
    }

    private fun showDetail(row: RequestRow) {
        val ctx = requireContext()
        val title = RequestRowFormat.title(ctx, row)
        val valueLabel = when (row.kind) {
            RequestRow.Kind.LEAVE -> R.string.rq_f_days
            RequestRow.Kind.EXPENSE -> R.string.rq_f_amount
            RequestRow.Kind.CHECKIN -> R.string.rq_f_time
        }
        WsDetailSheet.show(
            fm = childFragmentManager,
            title = title,
            subtitle = getString(R.string.rq_details_fmt, RequestRowFormat.typeLabel(ctx, row)),
            status = RequestRowFormat.status(ctx, row),
            fields = listOf(
                WsDetailSheet.Field(getString(R.string.rq_f_request_for), title),
                WsDetailSheet.Field(getString(R.string.rq_f_date), RequestRowFormat.dateLine(row), half = true),
                WsDetailSheet.Field(getString(valueLabel), RequestRowFormat.headline(ctx, row), half = true),
                WsDetailSheet.Field(getString(R.string.rq_f_submitted), RequestRowFormat.submittedOn(ctx, row)),
                WsDetailSheet.Field(getString(R.string.rq_f_location), row.location, half = true, accent = true),
                WsDetailSheet.Field(getString(R.string.rq_f_hours), WsFormat.hours(row.workingHours), half = true),
                WsDetailSheet.Field(getString(R.string.rq_f_reason), row.reason),
                WsDetailSheet.Field(getString(R.string.rq_f_reference), row.reference)
            )
        )
    }

    override fun onDestroyView() {
        loading?.release()
        loading = null
        binding.rqRefresh.isRefreshing = false
        binding.rqList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
