package com.axelliant.hris.features.quotes.presentation

import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.core.extensions.hideKeyboard
import com.axelliant.hris.core.extensions.showKeyboard
import com.axelliant.hris.core.ui.ShimmerAnimatorHelper
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentQuotesBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import com.axelliant.hris.features.quotes.domain.model.QuoteListUiModel
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import com.axelliant.hris.features.quotes.domain.model.QuoteStatus
import com.axelliant.hris.features.quotes.domain.model.QuotesEmptyStateUi
import com.axelliant.hris.features.quotes.domain.model.QuoteType
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class QuotesFragment : Fragment(), QuotesAdapter.QuoteItemListener {

    private val viewModel: QuotesViewModel by viewModels()
    private var _binding: FragmentQuotesBinding? = null
    private val binding get() = _binding!!

    private val shimmerHelper = ShimmerAnimatorHelper()
    private lateinit var quotesAdapter: QuotesAdapter
    private lateinit var filterChipAdapter: QuoteFilterChipAdapter
    private lateinit var actionMenuHandler: QuoteActionMenuHandler

    private var isSearchVisible = false
    private var currentSearchQuery = ""
    private var isQuoteActionMenuVisible = false
    private var cancelProgressDialog: androidx.appcompat.app.AlertDialog? = null


    private lateinit var dateFilterAdapter: FilterAdapter

    private var workflowBottomSheet: QuoteWorkflowBottomSheet? = null



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSearchUi()
        setupAdapters()
        setupInteractions()
        setupPagination()
        observeQuotes()
        observeQuoteCreationResult()
        observeQuoteSubmitResult()
        observeQuoteWorkflow()
        observeQuoteCancel()
        renderQuoteTypeTabs()
        setupDateFilterBar()
        observeQuoteDecisionWorkflow()

        viewModel.loadQuotesIfNeeded()
    }

    private fun setupDateFilterBar() {
        val items = listOf(
            FilterItem(getString(R.string.quotes_tab_standard), 0),
            FilterItem(getString(R.string.quotes_tab_quick), 1)
        )

        dateFilterAdapter = FilterAdapter(
            items,
            selectedPosition = 0
        ) { position, _ ->

            when (position) {
                0 -> viewModel.onQuoteTypeSelected(QuoteType.Standard)
                1 -> viewModel.onQuoteTypeSelected(QuoteType.Quick)
            }

            dateFilterAdapter.setSelected(position)
        }


        binding.rvDateFilters.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
        }
    }

    private fun setupSearchUi() {
        binding.searchInputLayout.isVisible = false
        binding.searchInputLayout.alpha = 0f
        binding.appTopBar.titleView.alpha = 1f
    }

    private fun setupAdapters() {
        actionMenuHandler = QuoteActionMenuHandler { actionId, quote ->
            when (actionId) {
                R.id.action_quote_view -> navigateToViewQuote(quote)
                R.id.action_quote_edit -> navigateToEditQuote(quote)
                R.id.action_quote_revise -> navigateToReviseQuote(quote)
                R.id.action_quote_duplicate -> navigateToDuplicateQuote(quote)
                R.id.action_quote_show_report -> navigateToQuoteReport(quote)
                R.id.action_quote_submit_workflow,
                R.id.action_quote_view_workflow -> openQuoteWorkflow(quote)

                R.id.action_quote_cancel -> showCancelQuoteConfirmation(quote)
                else -> showComingSoon()
            }
        }
        quotesAdapter = QuotesAdapter(this)
        binding.quotesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = quotesAdapter
            setHasFixedSize(false)
        }

        filterChipAdapter = QuoteFilterChipAdapter { chip ->
            if (isSearchVisible) {
                closeSearchField(clearText = true, reload = false)
            }
            viewModel.onFilterChipSelected(chip)
        }
        binding.statusFilterRecycler.apply {
            layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = filterChipAdapter
        }
    }

    private fun setupPagination() {
        binding.quotesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        binding.createQuoteFab.setOnClickListener { toggleQuoteActionMenu() }

        binding.quoteActionOverlay.setOnClickListener { hideQuoteActionMenu() }
        binding.createQuoteOption.setOnClickListener {
            hideQuoteActionMenu()
            findNavController().navigate(R.id.iaAddQuoteFragment)
        }
        binding.smartQuoteOption.setOnClickListener {
            hideQuoteActionMenu()
            findNavController().navigate(R.id.iaSmartQuoteFragment)
        }
        binding.appTopBar.setOnSearchClickListener { toggleSearchField() }

        /*  binding.standardTab.setOnClickListener {
              viewModel.onQuoteTypeSelected(QuoteType.Standard)
              renderQuoteTypeTabs()
          }
          binding.quickTab.setOnClickListener {
              viewModel.onQuoteTypeSelected(QuoteType.Quick)
              renderQuoteTypeTabs()
          }*/

        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(editable: Editable?) {
                if (!isSearchVisible) return
                currentSearchQuery = editable?.toString().orEmpty()
                viewModel.onSearchQueryChanged(currentSearchQuery)
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

    private fun toggleSearchField() {
        hideQuoteActionMenu()
        if (isSearchVisible) {
            closeSearchField(clearText = true, reload = true)
        } else {
            openSearchField()
        }
    }

    private fun openSearchField() {
        isSearchVisible = true
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_filter_close)
        binding.appTopBar.searchButton.contentDescription = getString(R.string.quotes_search_close)


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
        currentSearchQuery = ""
        binding.appTopBar.setSearchIconResource(R.drawable.ia_ic_search)
        binding.appTopBar.searchButton.contentDescription = getString(R.string.quotes_search_open)

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

        binding.appTopBar.titleView.isVisible = true
        binding.appTopBar.titleView.alpha = 0f
        binding.appTopBar.titleView.animate()
            .alpha(1f)
            .setDuration(SEARCH_ANIMATION_DURATION_MS)
            .start()

        if (clearText) {
            binding.searchEditText.setText("")
            viewModel.clearSearchQuery()
        }

        if (reload) {
            binding.quotesRecyclerView.scrollToPosition(0)
            viewModel.submitSearch("")
        }
    }

    private fun submitSearch() {
        if (!isSearchVisible) return
        val query = binding.searchEditText.text?.toString().orEmpty().trim()
        currentSearchQuery = query
        binding.searchEditText.clearFocus()
        binding.searchEditText.hideKeyboard()
        binding.quotesRecyclerView.scrollToPosition(0)
        viewModel.submitSearch(query)
    }

    private fun renderQuoteTypeTabs() {
        val isStandard = viewModel.getSelectedQuoteType() == QuoteType.Standard
        binding.standardTab.apply {
            setBackgroundResource(
                if (isStandard) R.drawable.bg_quote_tab_selected else 0
            )
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isStandard) R.color.ds_text_primary else R.color.ds_text_secondary
                )
            )
        }
        binding.quickTab.apply {
            setBackgroundResource(
                if (!isStandard) R.drawable.bg_quote_tab_selected else 0
            )
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (!isStandard) R.color.ds_text_primary else R.color.ds_text_secondary
                )
            )
        }
    }

    private fun toggleQuoteActionMenu() {
        if (isQuoteActionMenuVisible) {
            hideQuoteActionMenu()
        } else {
            showQuoteActionMenu()
        }
    }

    private fun applyContentBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding.contentContainer.setRenderEffect(
                if (enabled) {
                    RenderEffect.createBlurEffect(
                        CONTENT_BLUR_RADIUS,
                        CONTENT_BLUR_RADIUS,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    null
                }
            )
        }
    }


    private fun showQuoteActionMenu() {
        if (isQuoteActionMenuVisible) return
        isQuoteActionMenuVisible = true
        applyContentBlur(true)
        binding.quoteActionOverlay.isVisible = true
        binding.quoteActionOverlay.alpha = 0f
        binding.quoteActionOverlay.animate()
            .alpha(1f)
            .setDuration(QUOTE_ACTION_MENU_DIM_DURATION_MS)
            .start()

        binding.quoteActionMenuCard.apply {
            animate().cancel()
            pivotX = width.toFloat()
            pivotY = height.toFloat()
            isVisible = true
            alpha = 0f
            scaleX = QUOTE_ACTION_MENU_START_SCALE
            scaleY = QUOTE_ACTION_MENU_START_SCALE
            translationY = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp).toFloat()
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(QUOTE_ACTION_MENU_DURATION_MS)
                .setInterpolator(OvershootInterpolator(QUOTE_ACTION_MENU_OVERSHOOT))
                .start()
        }

    /*    binding.createQuoteFab.animate()
            .rotation(45f)
            .setDuration(QUOTE_ACTION_MENU_DURATION_MS)
            .start()*/

        binding.createQuoteFab.setImageResource(R.drawable.ic_close)
        binding.createQuoteFab.contentDescription = getString(R.string.purchase_orders_fab_close)
        binding.createQuoteFab.animate()
            .rotation(90f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()
    }

    private fun hideQuoteActionMenu() {
        if (!isQuoteActionMenuVisible && !binding.quoteActionMenuCard.isVisible) return
        isQuoteActionMenuVisible = false
        binding.quoteActionOverlay.animate()
            .alpha(0f)
            .setDuration(QUOTE_ACTION_MENU_DIM_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.quoteActionOverlay.isVisible = false
                applyContentBlur(false)
            }
            .start()
        binding.quoteActionMenuCard.animate()
            .alpha(0f)
            .scaleX(QUOTE_ACTION_MENU_START_SCALE)
            .scaleY(QUOTE_ACTION_MENU_START_SCALE)
            .translationY(resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._8sdp).toFloat())
            .setDuration(QUOTE_ACTION_MENU_DIM_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.quoteActionMenuCard.isVisible = false
            }
            .start()
       /* binding.createQuoteFab.animate()
            .rotation(0f)
            .setDuration(QUOTE_ACTION_MENU_DURATION_MS)
            .start()*/

        binding.createQuoteFab.setImageResource(R.drawable.ia_ic_add)
        binding.createQuoteFab.contentDescription = getString(R.string.purchase_orders_fab_open)
        binding.createQuoteFab.animate()
            .rotation(0f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .start()
    }

    private fun observeQuotes() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quotesState.collect { state ->
                    when (state) {
                        UiState.Loading -> showLoading()
                        is UiState.Success -> showSuccess(state.data)
                        UiState.Empty -> showEmptyLegacy()
                        is UiState.Error -> showError(state.message)
                        UiState.Unauthorized -> showError(getString(R.string.quotes_error))
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeQuoteSubmitResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>(ViewQuoteViewModel.RESULT_QUOTE_SUBMITTED)
            ?.observe(viewLifecycleOwner) { submitted ->
                if (!submitted) return@observe
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Boolean>(ViewQuoteViewModel.RESULT_QUOTE_SUBMITTED)
                binding.quotesRecyclerView.scrollToPosition(0)
                viewModel.loadQuotes()
            }
    }

    private fun observeQuoteCreationResult() {
        findNavController().currentBackStackEntry?.savedStateHandle
            ?.getLiveData<Boolean>(AddQuoteViewModel.RESULT_QUOTE_CREATED)
            ?.observe(viewLifecycleOwner) { created ->
                if (!created) return@observe
                findNavController().currentBackStackEntry?.savedStateHandle
                    ?.remove<Boolean>(AddQuoteViewModel.RESULT_QUOTE_CREATED)
                binding.quotesRecyclerView.scrollToPosition(0)
                viewModel.loadQuotes()
            }
    }

    private fun showLoading() {
        showPaginationShimmer(false)
        hideEmptyState()
        binding.quotesRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        binding.shimmerContainer.isVisible = true
        shimmerHelper.populate(
            container = binding.shimmerContainer,
            inflater = layoutInflater,
            itemLayoutRes = R.layout.item_quote_shimmer,
            count = SHIMMER_ITEM_COUNT
        )
    }

    private fun showSuccess(data: QuoteListUiModel) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        binding.errorStateText.isVisible = false
        filterChipAdapter.setSelectedFilterId(viewModel.getSelectedFilterId())
        filterChipAdapter.submitList(data.filterChips)

        val isEmpty = data.quotes.isEmpty()
        if (isEmpty) {
            binding.quotesRecyclerView.isVisible = false
            quotesAdapter.submitList(emptyList())
            showEmptyState(data.emptyState)
        } else {
            hideEmptyState()
            binding.quotesRecyclerView.isVisible = true
            quotesAdapter.submitList(data.quotes)
        }

        showPaginationShimmer(data.isLoadingNextPage)
    }

    private fun showEmptyLegacy() {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        binding.quotesRecyclerView.isVisible = false
        binding.errorStateText.isVisible = false
        quotesAdapter.submitList(emptyList())
        showEmptyState(
            QuotesEmptyStateUi(
                titleRes = R.string.quotes_empty_all_title,
                descriptionRes = R.string.quotes_empty_all_description
            )
        )
    }

    private fun showError(message: String) {
        shimmerHelper.clear(binding.shimmerContainer)
        binding.shimmerContainer.isVisible = false
        showPaginationShimmer(false)
        hideEmptyState()
        binding.quotesRecyclerView.isVisible = false
        binding.errorStateText.isVisible = true
        binding.errorStateText.text = message.ifBlank { getString(R.string.quotes_error) }
        quotesAdapter.submitList(emptyList())
    }

    private fun showEmptyState(emptyState: QuotesEmptyStateUi?) {
        if (emptyState == null) {
            hideEmptyState()
            return
        }
        binding.quotesEmptyState.emptyStateTitle.setText(emptyState.titleRes)
        binding.quotesEmptyState.emptyStateDescription.setText(emptyState.descriptionRes)
        binding.quotesEmptyState.root.isVisible = true
    }

    private fun hideEmptyState() {
        binding.quotesEmptyState.root.isVisible = false
    }

    private fun showPaginationShimmer(show: Boolean) {
        binding.paginationShimmerContainer.isVisible = show
        if (show) {
            shimmerHelper.populate(
                container = binding.paginationShimmerContainer,
                inflater = layoutInflater,
                itemLayoutRes = R.layout.item_quote_shimmer,
                count = PAGINATION_SHIMMER_COUNT
            )
        } else {
            shimmerHelper.clear(binding.paginationShimmerContainer)
        }
    }

    override fun onMenuClick(quote: QuoteModel, anchor: View) {
        hideQuoteActionMenu()
        actionMenuHandler.show(anchor, quote)
    }

    private fun openQuoteWorkflow(quote: QuoteModel) {
        viewModel.loadQuoteWorkflow(quote)
    }

    private fun observeQuoteWorkflow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.workflowState.collect { state ->
                    when (state) {
                        UiState.Loading -> Unit
                        is UiState.Success -> {
                            workflowBottomSheet?.dismiss()

                            workflowBottomSheet = QuoteWorkflowBottomSheet(
                                fragment = this@QuotesFragment,
                                workflow = state.data,
                                titleResId = R.string.quote_workflow_title,
                                onApproveClick = { step, comments ->

                                    viewModel.decideWorkflowNode(
                                        instanceId = state.data.instanceId,
                                        nodeId = step.workflowNodeId,
                                        approved = true,
                                        comments = comments
                                    )
                                },

                                onRejectClick = { step, comments ->

                                    viewModel.decideWorkflowNode(
                                        instanceId = state.data.instanceId,
                                        nodeId = step.workflowNodeId,
                                        approved = false,
                                        comments = comments
                                    )
                                }
                            )
                            workflowBottomSheet?.show()
                            viewModel.resetWorkflowState()
                        }


                        is UiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message.ifBlank {
                                    getString(R.string.quote_workflow_load_failed)
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.resetWorkflowState()
                        }

                        UiState.Unauthorized -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.quotes_error,
                                Toast.LENGTH_SHORT
                            ).show()
                            viewModel.resetWorkflowState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    private fun observeQuoteDecisionWorkflow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                viewModel.workflowDecisionState.collect { state ->

                    when (state) {

                        UiState.Loading -> Unit

                        is UiState.Success -> {
                            Toast.makeText(
                                requireContext(),
                                state.data.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            workflowBottomSheet?.dismiss()
                            workflowBottomSheet = null

                            viewModel.resetWorkflowDecisionState()

                            viewModel.loadQuotes()
                        }

                        is UiState.Error -> {
                            Toast.makeText(
                                requireContext(),
                                state.message,
                                Toast.LENGTH_SHORT
                            ).show()

                            viewModel.resetWorkflowDecisionState()

                        }

                        UiState.Unauthorized -> {
                            viewModel.resetWorkflowDecisionState()
                        }

                        else -> Unit
                    }
                }
            }
        }
    }


    private fun showCancelQuoteConfirmation(quote: QuoteModel) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quote_cancel_confirmation, null)
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<View>(R.id.cancelQuoteYesButton).setOnClickListener {
            dialog.dismiss()
            viewModel.cancelQuote(quote.id)
        }
        dialogView.findViewById<View>(R.id.cancelQuoteNoButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        dialog.show()
    }

    private fun observeQuoteCancel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.cancelState.collect { state ->
                    when (state) {
                        UiState.Loading -> showCancelProgressDialog()
                        is UiState.Success -> {
                            dismissCancelProgressDialog()
                            showCancelResultDialog(
                                message = state.data.message.ifBlank {
                                    getString(R.string.quote_cancel_success)
                                },
                                isSuccess = true
                            )
                        }

                        is UiState.Error -> {
                            dismissCancelProgressDialog()
                            showCancelResultDialog(
                                message = state.message.ifBlank {
                                    getString(R.string.quote_cancel_failed)
                                },
                                isSuccess = false
                            )
                        }

                        UiState.Unauthorized -> {
                            dismissCancelProgressDialog()
                            showCancelResultDialog(
                                message = getString(R.string.quotes_error),
                                isSuccess = false
                            )
                        }

                        else -> dismissCancelProgressDialog()
                    }
                }
            }
        }
    }

    private fun showCancelProgressDialog() {
        if (cancelProgressDialog?.isShowing == true) return
        cancelProgressDialog = MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.quote_cancel_in_progress)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun dismissCancelProgressDialog() {
        cancelProgressDialog?.dismiss()
        cancelProgressDialog = null
    }

    private fun showCancelResultDialog(message: String, isSuccess: Boolean) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                viewModel.resetCancelState()
                if (isSuccess) {
                    binding.quotesRecyclerView.scrollToPosition(0)
                    viewModel.loadQuotes()
                }
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToDuplicateQuote(quote: QuoteModel) {
        findNavController().navigate(
            R.id.iaAddQuoteFragment,
            bundleOf(AddQuoteViewModel.ARG_DUPLICATE_QUOTE_ID to quote.id)
        )
    }

    private fun navigateToReviseQuote(quote: QuoteModel) {
        findNavController().navigate(
            R.id.iaAddQuoteFragment,
            bundleOf(AddQuoteViewModel.ARG_REVISE_QUOTE_ID to quote.id)
        )
    }

    private fun navigateToViewQuote(quote: QuoteModel) {
        findNavController().navigate(
            R.id.iaViewQuoteFragment,
            bundleOf(ViewQuoteViewModel.ARG_QUOTE_ID to quote.id)
        )
    }

    private fun navigateToQuoteReport(quote: QuoteModel) {
        findNavController().navigate(
            R.id.iaQuoteReportFragment,
            bundleOf(QuoteReportViewModel.ARG_QUOTE_ID to quote.id)
        )
    }

    private fun navigateToEditQuote(quote: QuoteModel) {
        if (quote.status == QuoteStatus.Draft) {
            findNavController().navigate(
                R.id.iaAddQuoteFragment,
                bundleOf(AddQuoteViewModel.ARG_QUOTE_ID to quote.id)
            )
        } else {
            navigateToViewQuote(quote)
        }
    }

    private fun showComingSoon() {
        Toast.makeText(requireContext(), R.string.coming_soon, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        if (_binding != null) {
            applyContentBlur(false)
        }
        dismissCancelProgressDialog()
        shimmerHelper.release()
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SHIMMER_ITEM_COUNT = 5
        private const val PAGINATION_SHIMMER_COUNT = 1
        private const val PAGINATION_THRESHOLD_ITEMS = 2
        private const val SEARCH_ANIMATION_DURATION_MS = 180L
        private const val QUOTE_ACTION_MENU_DURATION_MS = 220L
        private const val QUOTE_ACTION_MENU_DIM_DURATION_MS = 140L
        private const val QUOTE_ACTION_MENU_START_SCALE = 0.88f
        private const val QUOTE_ACTION_MENU_OVERSHOOT = 1.05f

        private const val CONTENT_BLUR_RADIUS = 28f
        private const val FAB_MENU_ANIMATION_MS = 220L


    }
}


