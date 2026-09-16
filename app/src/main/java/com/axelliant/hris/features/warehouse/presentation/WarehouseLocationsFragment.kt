package com.axelliant.hris.features.warehouse.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentWarehouseLocationsBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationModel
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehouseLocationsFragment : Fragment() {

    private val viewModel: WarehouseLocationsViewModel by viewModels()
    private var _binding: FragmentWarehouseLocationsBinding? = null
    private val binding get() = _binding!!
    private val adapter = WarehouseLocationsAdapter(::showLocationActions)
    private val shimmerHelper = ShimmerAnimatorHelper()
    private var warehouseOptions = emptyList<WarehouseModel>()
    private var selectedWarehouse: WarehouseModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseLocationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupList()
        setupInteractions()
        observeLocationSavedResult()
        observeLocations()
        viewModel.loadIfNeeded()
    }

    private fun setupList() {
        binding.locationsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WarehouseLocationsFragment.adapter
            setHasFixedSize(false)
        }
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        binding.appTopBar.actionButton.contentDescription =
            getString(R.string.add_location_title)
        binding.appTopBar.setOnActionClickListener {
            openAddLocation()
        }
        binding.warehouseSelectorButton.setOnClickListener { anchor ->
            showWarehouseSelector(anchor)
        }
    }

    private fun observeLocations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationsState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.warehouse_locations_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showWarehouseSelector(anchor: View) {
        if (warehouseOptions.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.warehouse_selector_empty,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val popupMenu = PopupMenu(anchor.context, anchor)
        warehouseOptions.forEachIndexed { index, warehouse ->
            popupMenu.menu.add(0, index, index, warehouse.selectorLabel())
        }
        popupMenu.setOnMenuItemClickListener { item ->
            warehouseOptions.getOrNull(item.itemId)?.let { warehouse ->
                viewModel.selectWarehouse(warehouse)
                true
            } ?: false
        }
        popupMenu.show()
    }

    private fun observeLocationSavedResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>(AddWarehouseLocationViewModel.RESULT_LOCATION_SAVED)
            ?.observe(viewLifecycleOwner) { saved ->
                if (!saved) return@observe
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Boolean>(AddWarehouseLocationViewModel.RESULT_LOCATION_SAVED)
                viewModel.refresh()
            }
    }

    private fun showLocationActions(location: WarehouseLocationModel, anchor: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_warehouse_location_actions, popupMenu.menu)
        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_location_view -> {
                    openLocationForm(location, AddWarehouseLocationViewModel.MODE_VIEW)
                    true
                }
                R.id.action_location_edit -> {
                    openLocationForm(location, AddWarehouseLocationViewModel.MODE_EDIT)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun showLoading() {
        binding.locationsRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = false
        binding.shimmerContainer.isVisible = true
        shimmerHelper.populate(
            container = binding.shimmerContainer,
            inflater = layoutInflater,
            itemLayoutRes = R.layout.item_sale_order_shimmer,
            count = SHIMMER_ITEM_COUNT
        )
    }

    private fun showSuccess(data: WarehouseLocationsUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        warehouseOptions = data.warehouseOptions
        selectedWarehouse = data.selectedWarehouse
        binding.warehouseSelectorText.text = data.selectedWarehouse.selectorLabel()
        binding.totalEntriesText.text =
            resources.getQuantityString(
                R.plurals.warehouse_locations_count,
                data.locations.size,
                data.locations.size
            )
        val isEmpty = data.locations.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        binding.locationsRecyclerView.isVisible = !isEmpty
        adapter.submitList(data.locations)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.locationsRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        binding.totalEntriesText.text = getString(R.string.warehouse_locations_empty)
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.locationsRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text =
            message.ifBlank { getString(R.string.warehouse_locations_error) }
        adapter.submitList(emptyList())
    }

    private fun openAddLocation() {
        val warehouse = selectedWarehouse
        if (warehouse == null) {
            Toast.makeText(
                requireContext(),
                R.string.warehouse_selector_empty,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        findNavController().navigate(
            R.id.iaAddWarehouseLocationFragment,
            bundleOf(
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_ID to warehouse.id,
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_NAME to warehouse.name,
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_CODE to warehouse.code,
                AddWarehouseLocationViewModel.ARG_MODE to AddWarehouseLocationViewModel.MODE_ADD
            )
        )
    }

    private fun openLocationForm(location: WarehouseLocationModel, mode: String) {
        findNavController().navigate(
            R.id.iaAddWarehouseLocationFragment,
            bundleOf(
                AddWarehouseLocationViewModel.ARG_MODE to mode,
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_ID to location.warehouseId,
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_NAME to location.warehouseName,
                AddWarehouseLocationViewModel.ARG_WAREHOUSE_CODE to "",
                AddWarehouseLocationViewModel.ARG_LOCATION_ID to location.id,
                AddWarehouseLocationViewModel.ARG_LOCATION_CODE to location.code,
                AddWarehouseLocationViewModel.ARG_LOCATION_NAME to location.name,
                AddWarehouseLocationViewModel.ARG_LOCATION_LEVEL_TYPE to location.levelType,
                AddWarehouseLocationViewModel.ARG_LOCATION_IS_ACTIVE to location.isActive
            )
        )
    }

    private fun WarehouseModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val codeValue = code.takeIf { it.isNotBlank() && it != "-" }
        return listOfNotNull(nameValue, codeValue?.let { "($it)" }).joinToString(" ")
            .ifBlank { getString(R.string.warehouse_not_available) }
    }

    override fun onDestroyView() {
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val SHIMMER_ITEM_COUNT = 5
    }
}
