package com.axelliant.hris.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.FooterListAdapter
import com.axelliant.hris.adapter.NotificationAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentNotificationsBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.notification.WorkflowNotification
import com.axelliant.hris.model.payslip.cleaned
import com.axelliant.hris.ui.WsDetailSheet
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.NotificationEvent
import com.axelliant.hris.viewmodel.NotificationsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Workflow notifications: paged list, "Mark all read", and a details sheet with an "Open in HRIS" link. */
class NotificationsFragment : BaseFragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: NotificationsViewModel by viewModel()

    private val adapter = NotificationAdapter { showDetails(it) }
    private var renderedCount = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderedCount = -1
        adapter.endText = getString(R.string.notif_caught_up)

        binding.notifList.layoutManager = LinearLayoutManager(requireContext())
        binding.notifList.adapter = adapter
        binding.notifList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                loadMoreIfLastVisible()
            }
        })

        binding.notifRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.notifRefresh.setOnChildScrollUpCallback { _, _ -> binding.notifList.canScrollVertically(-1) }
        binding.notifRefresh.setOnRefreshListener { viewModel.reload(pull = true) }
        binding.notifMarkAll.setOnClickListener { viewModel.markAllRead() }
        binding.notifTryAgain.setOnClickListener { viewModel.reload() }

        // "Open in HRIS" from the details sheet; the action id carries the URL.
        childFragmentManager.setFragmentResultListener(SHEET_KEY, viewLifecycleOwner) { _, result ->
            val action = result.getString(WsDetailSheet.RESULT_ACTION).orEmpty()
            if (action.startsWith(ACTION_OPEN)) openUrl(action.removePrefix(ACTION_OPEN))
        }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.state.observe(viewLifecycleOwner) { render(it) }
        viewModel.events.observe(viewLifecycleOwner, EventObserver { event ->
            val context = requireContext()
            when (event) {
                NotificationEvent.LoadFailed -> context.showErrorMsg(getString(R.string.notif_error_load))
                NotificationEvent.UpdateFailed -> context.showErrorMsg(getString(R.string.notif_error_update))
                is NotificationEvent.MarkedRead ->
                    context.showSuccessMsg(event.message.cleaned() ?: getString(R.string.notif_marked_read))
            }
        })

        if (viewModel.state.value?.hasLoaded != true) viewModel.reload()
    }

    private fun render(state: NotificationsViewModel.UiState) {
        binding.notifSummary.isVisible = state.total > 0
        binding.notifSummaryText.text = getString(R.string.notif_summary, state.total, state.unread)
        binding.notifMarkAll.isVisible = state.unread > 0
        binding.notifMarkAll.isEnabled = !state.updating

        adapter.submit(state.items)
        adapter.footer = when {
            state.loadingMore -> FooterListAdapter.Footer.LOADING
            state.hasLoaded && !state.canLoadMore && state.items.isNotEmpty() -> FooterListAdapter.Footer.END
            else -> FooterListAdapter.Footer.NONE
        }
        binding.notifRefresh.isRefreshing = state.refreshing

        if (state.items.isEmpty() && state.hasLoaded) {
            binding.notifList.visibility = View.GONE
            binding.notifEmptyGroup.visibility = View.VISIBLE
            if (state.loadFailed) {
                binding.notifEmpty.show(R.drawable.notif_ic_bell_off, getString(R.string.notif_failed_title), getString(R.string.notif_error_load))
            } else {
                binding.notifEmpty.show(R.drawable.ws_ic_bell, getString(R.string.notif_empty_title), getString(R.string.notif_empty_message))
            }
            binding.notifTryAgain.isVisible = state.loadFailed
        } else {
            binding.notifEmptyGroup.visibility = View.GONE
            binding.notifList.visibility = View.VISIBLE
        }

        if (state.items.size != renderedCount) {
            renderedCount = state.items.size
            binding.notifList.post { _binding?.let { loadMoreIfLastVisible() } }
        }
    }

    private fun loadMoreIfLastVisible() {
        val layoutManager = binding.notifList.layoutManager as? LinearLayoutManager ?: return
        val count = adapter.items.size
        if (count > 0 && layoutManager.findLastVisibleItemPosition() >= count - 1) viewModel.loadMore()
    }

    /** Opens the details sheet only; a notification is not marked read by looking at it. */
    private fun showDetails(item: WorkflowNotification) {
        val context = requireContext()
        val link = actionLink(item)
        WsDetailSheet.show(
            childFragmentManager,
            title = getString(R.string.notif_sheet_title),
            subtitle = item.reference_document_type.cleaned(),
            status = getString(if (item.isRead) R.string.notif_sheet_read else R.string.notif_sheet_unread),
            fields = listOf(
                WsDetailSheet.Field(getString(R.string.notif_f_message), item.message.cleaned()),
                WsDetailSheet.Field(getString(R.string.notif_f_from), item.from_user_name.cleaned()),
                WsDetailSheet.Field(getString(R.string.notif_f_received), WsFormat.dateAt(context, item.created_on)),
                WsDetailSheet.Field(getString(R.string.notif_f_document), item.reference_document_type.cleaned()),
                WsDetailSheet.Field(getString(R.string.notif_f_reference), item.reference_document_name.cleaned())
            ),
            actions = if (link != null) {
                listOf(WsDetailSheet.Action(ACTION_OPEN + link, getString(R.string.notif_open_in_hris)))
            } else {
                emptyList()
            },
            requestKey = SHEET_KEY
        )
    }

    /** Relative paths from HRIS resolve against the HRIS host. */
    private fun actionLink(item: WorkflowNotification): String? =
        item.action_url.cleaned()?.let { WsFormat.mediaUrl(it) }?.takeIf { it.isNotEmpty() }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            requireContext().showErrorMsg(getString(R.string.notif_open_failed))
        }
    }

    override fun onDestroyView() {
        binding.notifList.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SHEET_KEY = "notificationSheet"
        const val ACTION_OPEN = "open:"
    }
}
