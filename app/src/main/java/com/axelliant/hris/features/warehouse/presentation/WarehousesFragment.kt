package com.axelliant.hris.features.warehouse.presentation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
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
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.showKeyboard
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentWarehousesBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.warehouse.domain.model.WarehouseModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class WarehousesFragment : Fragment() {

    private val viewModel: WarehousesViewModel by viewModels()
    private var _binding: FragmentWarehousesBinding? = null
    private val binding get() = _binding!!
    private val adapter = WarehousesAdapter(::showWarehouseActions)
    private val shimmerHelper = ShimmerAnimatorHelper()
    private var isSearchVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehousesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupList()
        setupInteractions()
        observeWarehouseCreatedResult()
        observeWarehouses()
        viewModel.loadWarehousesIfNeeded()
    }

    private fun setupSearchUi() {
        isSearchVisible = false
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.warehouses_search_open)
        binding.appTopBar.actionButton.contentDescription =
            getString(R.string.add_warehouse_title)
    }

    private fun setupList() {
        binding.warehousesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@WarehousesFragment.adapter
            setHasFixedSize(false)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (dy <= 0) return
                    val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                    val lastVisible = layoutManager.findLastVisibleItemPosition()
                    val totalItems = layoutManager.itemCount
                    if (lastVisible >= totalItems - PAGINATION_THRESHOLD_ITEMS) {
                        viewModel.loadNextPage()
                    }
                }
            })
        }
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        binding.appTopBar.setOnSearchClickListener { toggleSearchField() }
        binding.appTopBar.setOnActionClickListener {
            findNavController().navigate(
                R.id.iaAddWarehouseFragment,
                bundleOf(AddWarehouseViewModel.ARG_MODE to AddWarehouseViewModel.MODE_ADD)
            )
        }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                if (!isSearchVisible) return
                viewModel.onSearchQueryChanged(editable?.toString().orEmpty())
            }
        })
        binding.searchEditText.setOnEditorActionListener { _, actionId, event ->
            val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH ||
                actionId == EditorInfo.IME_ACTION_DONE
            val isEnterKey = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                event.action == KeyEvent.ACTION_UP
            if (isSubmitAction || isEnterKey) {
                submitSearch()
                true
            } else {
                false
            }
        }
    }

    private fun observeWarehouses() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.warehousesState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.warehouses_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeWarehouseCreatedResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>(AddWarehouseViewModel.RESULT_WAREHOUSE_CREATED)
            ?.observe(viewLifecycleOwner) { created ->
                if (!created) return@observe
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Boolean>(AddWarehouseViewModel.RESULT_WAREHOUSE_CREATED)
                binding.warehousesRecyclerView.scrollToPosition(0)
                viewModel.loadWarehouses()
            }
    }

    private fun showWarehouseActions(warehouse: WarehouseModel, anchor: View) {
        val popupMenu = PopupMenu(anchor.context, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_warehouse_actions, popupMenu.menu)
        popupMenu.setForceShowIcon(true)
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_warehouse_view -> {
                    navigateToWarehouseForm(warehouse, AddWarehouseViewModel.MODE_VIEW)
                    true
                }
                R.id.action_warehouse_edit -> {
                    navigateToWarehouseForm(warehouse, AddWarehouseViewModel.MODE_EDIT)
                    true
                }
                R.id.action_warehouse_configure_operations -> {
                    Toast.makeText(
                        requireContext(),
                        R.string.warehouse_configure_operations_coming_soon,
                        Toast.LENGTH_SHORT
                    ).show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun navigateToWarehouseForm(warehouse: WarehouseModel, mode: String) {
        findNavController().navigate(
            R.id.iaAddWarehouseFragment,
            bundleOf(
                AddWarehouseViewModel.ARG_MODE to mode,
                AddWarehouseViewModel.ARG_WAREHOUSE_ID to warehouse.id,
                AddWarehouseViewModel.ARG_WAREHOUSE_CODE to warehouse.code,
                AddWarehouseViewModel.ARG_WAREHOUSE_NAME to warehouse.name,
                AddWarehouseViewModel.ARG_WAREHOUSE_ADDRESS_LINE_1 to warehouse.addressLine1,
                AddWarehouseViewModel.ARG_WAREHOUSE_ADDRESS_LINE_2 to warehouse.addressLine2,
                AddWarehouseViewModel.ARG_WAREHOUSE_CITY to warehouse.city,
                AddWarehouseViewModel.ARG_WAREHOUSE_STATE to warehouse.state,
                AddWarehouseViewModel.ARG_WAREHOUSE_POSTAL_CODE to warehouse.postalCode,
                AddWarehouseViewModel.ARG_WAREHOUSE_COUNTRY to warehouse.country,
                AddWarehouseViewModel.ARG_WAREHOUSE_IS_ACTIVE to warehouse.isActive
            )
        )
    }

    private fun toggleSearchField() {
        if (isSearchVisible) {
            closeSearchField(clearText = true, reload = true)
        } else {
            openSearchField()
        }
    }

    private fun openSearchField() {
        isSearchVisible = true
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_filter_close)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.warehouses_search_close)
        binding.searchInputLayout.isVisible = true
        binding.searchInputLayout.alpha = 0f
        binding.searchInputLayout.animate()
            .alpha(1f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .start()
        binding.searchEditText.post {
            if (_binding == null || !isSearchVisible) return@post
            binding.searchEditText.requestFocus()
            binding.searchEditText.showKeyboard()
        }
    }

    private fun closeSearchField(clearText: Boolean, reload: Boolean) {
        isSearchVisible = false
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription =
            getString(R.string.warehouses_search_open)
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        binding.searchInputLayout.animate()
            .alpha(0f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.searchInputLayout.isVisible = false
            }
            .start()
        if (clearText) {
            binding.searchEditText.setText("")
            viewModel.clearSearchQuery()
        }
        if (reload) {
            binding.warehousesRecyclerView.scrollToPosition(0)
            viewModel.submitSearch("")
        }
    }

    private fun submitSearch() {
        if (!isSearchVisible) return
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        binding.warehousesRecyclerView.scrollToPosition(0)
        viewModel.submitSearch(query)
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        binding.warehousesRecyclerView.isVisible = false
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

    private fun showSuccess(data: WarehouseListUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        binding.totalEntriesText.text =
            resources.getQuantityString(
                R.plurals.warehouses_count,
                data.totalCount,
                data.totalCount
            )
        val isEmpty = data.warehouses.isEmpty()
        binding.emptyStateText.isVisible = isEmpty
        binding.warehousesRecyclerView.isVisible = !isEmpty
        adapter.submitList(data.warehouses)
        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmpty() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.warehousesRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.emptyStateText.isVisible = true
        adapter.submitList(emptyList())
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.warehousesRecyclerView.isVisible = false
        binding.emptyStateText.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.warehouses_error) }
        adapter.submitList(emptyList())
    }

    private fun showPaginationShimmer(show: Boolean) {
        binding.paginationShimmerContainer.isVisible = show
        if (show) {
            shimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_sale_order_shimmer,
                count = 1
            )
        } else {
            shimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    override fun onDestroyView() {
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val PAGINATION_THRESHOLD_ITEMS = 2
        const val SEARCH_ANIMATION_DURATION_MS = 180L
        const val SHIMMER_ITEM_COUNT = 5
    }
}
