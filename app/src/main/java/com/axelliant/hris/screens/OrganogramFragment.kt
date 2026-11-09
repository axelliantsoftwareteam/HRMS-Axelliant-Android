package com.axelliant.hris.screens

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.axelliant.hris.R
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.databinding.FragmentOrganogramBinding
import com.axelliant.hris.loading.ListLoadKind
import com.axelliant.hris.ui.WsListLoading
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.viewmodel.OrganogramViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

/** Manager's team structure as a zoomable, pannable chart. Reloads every time the screen resumes. */
class OrganogramFragment : BaseFragment() {

    private var _binding: FragmentOrganogramBinding? = null
    private val binding get() = _binding!!
    private var loading: WsListLoading? = null

    private val viewModel: OrganogramViewModel by viewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrganogramBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.orgChart.onZoomChanged = { updateZoomUi() }
        binding.orgBtnZoomIn.setOnClickListener { binding.orgChart.zoomIn() }
        binding.orgBtnZoomOut.setOnClickListener { binding.orgChart.zoomOut() }
        binding.orgBtnReset.setOnClickListener { binding.orgChart.fit() }
        updateZoomUi()

        loading = WsListLoading.attach(viewLifecycleOwner, binding.orgContent, { viewModel.reload() },
            emptyTitle = getString(R.string.org_empty_title), emptyMessage = getString(R.string.org_empty_message))
            .also { it.bind(viewModel.listLoading()) }
        viewModel.changed.observe(viewLifecycleOwner) { render() }
        render()
    }

    override fun onResume() {
        super.onResume()
        viewModel.reload()
    }

    private fun render() {
        val b = _binding ?: return
        if (viewModel.hasTeam) {
            b.orgContent.visibility = View.VISIBLE
            b.orgEmptyState.hide()
            b.orgTeamCount.text = getString(R.string.org_team_count, viewModel.teamCount)
            if (!b.orgChart.isShowing(viewModel.nodes)) b.orgChart.setChart(viewModel.nodes)
        } else {
            b.orgContent.visibility = View.GONE
            b.orgEmptyState.hide()
        }
    }

    private fun updateZoomUi() {
        val b = _binding ?: return
        b.orgZoomPercent.text = getString(R.string.org_zoom_percent, b.orgChart.zoomPercent)
        tintZoom(b.orgBtnZoomOut, b.orgChart.canZoomOut)
        tintZoom(b.orgBtnZoomIn, b.orgChart.canZoomIn)
    }

    private fun tintZoom(button: android.widget.ImageButton, enabled: Boolean) {
        button.isEnabled = enabled
        val color = ContextCompat.getColor(requireContext(), if (enabled) R.color.ws_primary else R.color.ws_grey)
        ImageViewCompat.setImageTintList(button, ColorStateList.valueOf(color))
    }

    override fun onDestroyView() {
        binding.orgChart.onZoomChanged = null
        loading?.release()
        loading = null
        _binding = null
        super.onDestroyView()
    }
}
