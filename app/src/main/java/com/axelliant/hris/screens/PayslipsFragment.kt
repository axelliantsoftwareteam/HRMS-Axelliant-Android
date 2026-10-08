package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.axelliant.hris.R
import com.axelliant.hris.adapter.FooterListAdapter
import com.axelliant.hris.adapter.PayslipAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentPayslipsBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.viewmodel.PayslipError
import com.axelliant.hris.viewmodel.PayslipsViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** The employee's salary slips: header, paged list of cards and a full-screen PDF viewer behind "View". */
class PayslipsFragment : BaseFragment() {

    private var _binding: FragmentPayslipsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PayslipsViewModel by viewModel()

    private val adapter = PayslipAdapter { slip -> viewModel.open(slip, requireContext().cacheDir) }
    private var renderedCount = -1

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPayslipsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        renderedCount = -1

        binding.payList.layoutManager = LinearLayoutManager(requireContext())
        binding.payList.adapter = adapter
        (binding.payList.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        binding.payList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                loadMoreIfLastVisible()
            }
        })

        binding.payRefresh.setColorSchemeResources(R.color.ws_primary)
        binding.payRefresh.setOnChildScrollUpCallback { _, _ -> binding.payList.canScrollVertically(-1) }
        binding.payRefresh.setOnRefreshListener { viewModel.reload(pull = true) }

        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })
        viewModel.state.observe(viewLifecycleOwner) { render(it) }
        viewModel.errors.observe(viewLifecycleOwner, EventObserver { error ->
            val message = when (error) {
                PayslipError.LOAD_FAILED -> R.string.pay_error_load
                PayslipError.PREVIEW_FAILED -> R.string.pay_error_preview
            }
            requireContext().showErrorMsg(getString(message))
        })
        viewModel.preview.observe(viewLifecycleOwner, EventObserver { preview ->
            PayslipPreviewDialog.show(childFragmentManager, preview.title, preview.path)
        })

        if (viewModel.state.value?.hasLoaded != true) viewModel.reload()
    }

    private fun render(state: PayslipsViewModel.UiState) {
        binding.payCount.isVisible = state.total > 0
        binding.payCount.text = state.total.toString()
        binding.payCount.contentDescription = getString(R.string.pay_count_cd, state.total)

        adapter.submit(state.items)
        adapter.openingName = state.openingName
        adapter.footer = if (state.loadingMore) FooterListAdapter.Footer.LOADING else FooterListAdapter.Footer.NONE
        binding.payRefresh.isRefreshing = state.refreshing

        if (state.items.isEmpty() && state.hasLoaded) {
            binding.payList.visibility = View.GONE
            if (state.loadFailed) {
                binding.payEmpty.show(R.drawable.ws_ic_description, getString(R.string.pay_failed_title), getString(R.string.pay_failed_message))
            } else {
                binding.payEmpty.show(R.drawable.ws_ic_description, getString(R.string.pay_empty_title), getString(R.string.pay_empty_message))
            }
        } else {
            binding.payEmpty.hide()
            binding.payList.visibility = View.VISIBLE
        }

        // A new page arrived: if the list is still short enough to show its last row, keep going.
        if (state.items.size != renderedCount) {
            renderedCount = state.items.size
            binding.payList.post { _binding?.let { loadMoreIfLastVisible() } }
        }
    }

    private fun loadMoreIfLastVisible() {
        val layoutManager = binding.payList.layoutManager as? LinearLayoutManager ?: return
        val count = adapter.items.size
        if (count > 0 && layoutManager.findLastVisibleItemPosition() >= count - 1) viewModel.loadMore()
    }

    override fun onDestroyView() {
        binding.payList.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
