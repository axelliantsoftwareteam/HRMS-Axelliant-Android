package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.RequestRowFormat
import com.axelliant.hris.adapter.RequestsOverviewAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentRequestsOverviewBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.requests.RequestRow
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.RequestsOverviewViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

/** The Requests screen: a summary of the latest activity, a period toggle and one card per request. */
class RequestsOverviewFragment : BaseFragment() {

    private var _binding: FragmentRequestsOverviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RequestsOverviewViewModel by viewModel()
    private lateinit var adapter: RequestsOverviewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentRequestsOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = RequestsOverviewAdapter { showDetail(it) }
        binding.rqList.layoutManager = LinearLayoutManager(requireContext())
        binding.rqList.adapter = adapter

        binding.rqRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.rqRefresh.setOnRefreshListener { viewModel.load(showLoading = false) }

        // Restore the toggle before listening, so restoring it does not trigger a load.
        binding.rqSegment.check(
            if (viewModel.period == RequestsOverviewViewModel.Period.THIS_MONTH) R.id.rq_btn_month else R.id.rq_btn_week
        )
        binding.rqSegment.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            viewModel.period = if (checkedId == R.id.rq_btn_month) {
                RequestsOverviewViewModel.Period.THIS_MONTH
            } else {
                RequestsOverviewViewModel.Period.LAST_7_DAYS
            }
            viewModel.load(showLoading = adapter.itemCount == 0)
        }

        binding.rqFab.setOnClickListener {
            AppNavigator.navigateToRequest(Bundle().apply {
                putString(AppConst.RequestType, RequestFilter.ATTENDANCE.name)
            })
        }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.rows.observe(viewLifecycleOwner) { rows ->
            binding.rqRefresh.isRefreshing = false
            if (rows != null) render(rows)
        }
        viewModel.failed.observe(viewLifecycleOwner, EventObserver {
            requireContext().showErrorMsg(getString(R.string.rq_load_failed))
        })
        render(viewModel.rows.value.orEmpty())
    }

    override fun onResume() {
        super.onResume()
        // Coming back from the request form shows the new request.
        viewModel.load(showLoading = viewModel.rows.value.isNullOrEmpty())
    }

    private fun render(rows: List<RequestRow>) {
        val ctx = requireContext()
        adapter.submitList(rows)

        val loaded = viewModel.rows.value != null
        if (loaded && rows.isEmpty()) binding.emptyState.show() else binding.emptyState.hide()

        binding.rqTvShown.text = resources.getQuantityString(R.plurals.rq_requests_shown, rows.size, rows.size)
        val statuses = rows.map { RequestRowFormat.status(ctx, it).lowercase(Locale.ROOT) }
        binding.rqTvPendingCount.text =
            statuses.count { it.contains("pend") || it.contains("open") || it.contains("wait") }.toString()
        binding.rqTvApprovedCount.text = statuses.count { it.contains("approv") || it.contains("process") }.toString()
        binding.rqTvRejectedCount.text = statuses.count { it.contains("reject") }.toString()
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
                WsDetailSheet.Field(getString(R.string.rq_f_submitted), WsFormat.dateTime(ctx, row.submittedOn)),
                WsDetailSheet.Field(getString(R.string.rq_f_location), row.location, half = true, accent = true),
                WsDetailSheet.Field(getString(R.string.rq_f_hours), WsFormat.hours(row.workingHours), half = true),
                WsDetailSheet.Field(getString(R.string.rq_f_reason), row.reason),
                WsDetailSheet.Field(getString(R.string.rq_f_reference), row.reference)
            )
        )
    }

    override fun onDestroyView() {
        hideDialog()
        _binding = null
        super.onDestroyView()
    }
}
