package com.axelliant.hris.screens

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.MyExpenseAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.core.constants.AppRouteArgs
import com.axelliant.hris.databinding.FragmentExpenseBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.hideShimmer
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.ui.designsystem.adapters.FilterAdapter
import com.axelliant.hris.ui.designsystem.adapters.FilterItem
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpenseFragment : BaseFragment() {

    private var _binding: FragmentExpenseBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val expenseViewModel: ExpenseViewModel by viewModels()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""
    private var selectedFilterId = ""
    private var expenseList: List<Expense> = emptyList()
    private var filterList: List<FilterModel> = emptyList()

    private lateinit var dateFilterAdapter: FilterAdapter

    // --- FAB menu state ---
    private var isFabMenuOpen = false
    private lateinit var fabMenuBackCallback: OnBackPressedCallback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentExpenseBinding.inflate(inflater).also { _binding = it }
        binding?.lyContent?.isVisible = false
        binding?.shimmerLayout?.showShimmer(binding?.lyContent!!)
        return binding?.root
    }

    private var isDataLoaded = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Trigger dialog spinner ONLY after initial shimmer load completes
        expenseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isDataLoaded) {
                    if (isLoading) showDialog() else hideDialog()
                }
            })

        // 2. Start initial shimmer only on first launch
        if (!isDataLoaded) {
            toggleShimmer(true)
        }

        binding?.appTopBar?.setOnBackClickListener {
            if (isFabMenuOpen) closeFabMenu() else previousFragmentNavigation()
        }

        setupRecyclerViews()
        setupDateFilterBar()
        setupFabMenu()
        expenseViewModel.getMyExpenseDetail(getCurrentObject())

        expenseViewModel.expenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                // 3. Dismiss shimmer and mark initial load finished as soon as response arrives
                toggleShimmer(false)
                isDataLoaded = true

                if (response?.meta?.status == true && response.expenses != null) {
                    subFilterPopulations(response.expense_status)
                    expenseList = response.expenses
                    renderContent()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })
    }

    private fun toggleShimmer(show: Boolean) {
        if (show) {
            binding?.shimmerLayout?.showShimmer(binding?.lyContent!!)
        } else {
            binding?.shimmerLayout?.hideShimmer(binding?.lyContent!!)
        }
    }

    // --- FAB menu setup ---

    private fun setupFabMenu() {
        binding?.fabMenuScrim?.isVisible = false
        binding?.fabMenuScrim?.alpha = 0f
        binding?.addExpenseActionRow?.isVisible = false
        prepareClosedFabAction()

        fabMenuBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeFabMenu()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, fabMenuBackCallback)

        binding?.addExpense?.setOnClickListener { toggleFabMenu() }
        binding?.fabMenuScrim?.setOnClickListener { closeFabMenu() }
        binding?.addExpenseSubFab?.setOnClickListener {
            closeFabMenu()
            AppNavigator.navigateToAddExpenseFragment()
        }
        binding?.addExpenseLabel?.setOnClickListener { binding?.addExpenseSubFab?.performClick() }
    }

    private fun toggleFabMenu() {
        if (isFabMenuOpen) closeFabMenu() else openFabMenu()
    }

    private fun openFabMenu() {
        if (isFabMenuOpen) return
        isFabMenuOpen = true
        fabMenuBackCallback.isEnabled = true

        applyContentBlur(true)

        binding?.fabMenuScrim?.isVisible = true
        binding?.fabMenuScrim?.animate()
            ?.alpha(1f)
            ?.setDuration(FAB_MENU_ANIMATION_MS)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()

        binding?.addExpense?.setImageResource(R.drawable.ic_close)
        binding?.addExpense?.animate()
            ?.rotation(90f)
            ?.setDuration(FAB_MENU_ANIMATION_MS)
            ?.start()

        showFabAction()
    }

    private fun closeFabMenu() {
        if (!isFabMenuOpen) return
        isFabMenuOpen = false
        fabMenuBackCallback.isEnabled = false

        binding?.addExpense?.setImageResource(R.drawable.plus)
        binding?.addExpense?.animate()
            ?.rotation(0f)
            ?.setDuration(FAB_MENU_ANIMATION_MS)
            ?.start()

        hideFabAction()

        binding?.fabMenuScrim?.animate()
            ?.alpha(0f)
            ?.setDuration(FAB_MENU_ANIMATION_MS)
            ?.withEndAction {
                if (_binding == null) return@withEndAction
                binding?.fabMenuScrim?.isVisible = false
                applyContentBlur(false)
            }
            ?.start()
    }

    private fun prepareClosedFabAction() {
        binding?.addExpenseActionRow?.apply {
            alpha = 0f
            translationY = FAB_ACTION_TRANSLATION_Y
            scaleX = 0.85f
            scaleY = 0.85f
        }
    }

    private fun showFabAction() {
        val row = binding?.addExpenseActionRow ?: return
        row.animate().cancel()
        prepareClosedFabAction()
        row.isVisible = true
        row.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideFabAction() {
        val row = binding?.addExpenseActionRow ?: return
        row.animate().cancel()
        row.animate()
            .alpha(0f)
            .translationY(FAB_ACTION_TRANSLATION_Y)
            .scaleX(0.85f)
            .scaleY(0.85f)
            .setDuration(FAB_MENU_ANIMATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                row.isVisible = false
            }
            .start()
    }

    private fun applyContentBlur(enabled: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            binding?.contentContainer?.setRenderEffect(
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

    // --- existing screen logic (unchanged) ---

    private fun setupRecyclerViews() {
        binding?.rvExpenseFilters?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        binding?.rvExpenses?.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun renderContent() {
        binding?.tvFromDate?.text = startDateString.orEmpty()
        binding?.tvToDate?.text = endDateString.orEmpty()
        binding?.tvToDate?.setOnClickListener { datePickerDialog() }

        binding?.rvExpenseFilters?.isVisible = filterList.isNotEmpty()
        binding?.rvExpenseFilters?.adapter = SubFilterAdapter(
            selectedFilterId,
            filterList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filter = customObject as FilterModel
                    selectedFilterId = filter.id.toString()
                    filterId = selectedFilterId
                    expenseViewModel.getMyExpenseDetail(getCurrentObject())
                }
            }
        )

        binding?.rvExpenses?.isVisible = expenseList.isNotEmpty()
        binding?.tvNoRecord?.isVisible = expenseList.isEmpty()
        binding?.rvExpenses?.adapter = MyExpenseAdapter(
            expenseList,
            requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    onExpenseClicked(customObject as Expense)
                }
            }
        )
    }

    private fun onExpenseClicked(expense: Expense) {
        if (expense.status == "Draft") {
            AppNavigator.navigateToAddExpenseFragment(Bundle().apply {
                putString(AppRouteArgs.EXPENSE_REQUEST_ID, expense.name)
                putString(AppRouteArgs.EXPENSE_REQUEST, Gson().toJson(expense.expenses_detail))
                putString(AppRouteArgs.EXPENSE_REQUEST_ATTACHMENTS, Gson().toJson(expense.attachments))
            })
        } else {
            requireContext().showErrorMsg(
                ErrorMessages.DRAFT_EXPENSE_ONLY.errorString.plus(expense.approval_status)
            )
        }
    }

    private fun setupDateFilterBar() {
        val items = listOf(
            FilterItem(getString(R.string.last_seven), 0),
            FilterItem(getString(R.string.this_month), 1),
            FilterItem(getString(R.string.custom), 2)
        )
        dateFilterAdapter = FilterAdapter(items, selectedPosition = 0) { position, _ ->
            when (position) {
                0 -> {
                    currentFilter = AttendanceFilter.WEEK
                    dateFilterAdapter.setSelected(position)
                    expenseViewModel.getMyExpenseDetail(getCurrentObject())
                }

                1 -> {
                    currentFilter = AttendanceFilter.MONTH
                    dateFilterAdapter.setSelected(position)
                    expenseViewModel.getMyExpenseDetail(getCurrentObject())
                }

                2 -> datePickerDialog()
            }
        }
        binding?.rvDateFilters?.apply {
            layoutManager = GridLayoutManager(requireContext(), items.size)
            adapter = dateFilterAdapter
        }
    }

    private fun getCurrentObject(): AttendanceInput {
        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()
                setDateView()
            }
            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString = Utils.getServerFormat(date = Utils.getLastDayOfMonth())
                setDateView()
            }
            AttendanceFilter.Custom -> {}
        }
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter
            this.filters = filterId
        }
    }

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            binding?.tvFromDate?.text = startDateString.orEmpty()
            binding?.tvToDate?.text = endDateString.orEmpty()
        }
    }

    private fun datePickerDialog() {
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")
        builder.setTheme(R.style.MyDatePickerTheme)
        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            val startDate = Utils.utcToLocalDate(selection.first)
            val endDate = Utils.utcToLocalDate(selection.second)
            startDateString = Utils.getServerFormat(date = startDate)
            endDateString = Utils.getServerFormat(date = endDate)
            binding?.tvFromDate?.text = startDateString
            binding?.tvToDate?.text = endDateString
            currentFilter = AttendanceFilter.Custom
            dateFilterAdapter.setSelected(2)
            expenseViewModel.getMyExpenseDetail(getCurrentObject())
        }
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>?) {
        val list = ArrayList<FilterModel>().apply {
            add(FilterModel().apply {
                this.id = ""
                this.title = "All"
                this.count = "0"
            })
            leaveStatus?.let {
                addAll(it.filterNot { filter ->
                    filter.id.orEmpty().isBlank() && filter.title.equals("All", ignoreCase = true)
                })
            }
        }
        filterList = list
        if (selectedFilterId.isBlank()) selectedFilterId = ""
    }

    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        if (_binding != null) {
            applyContentBlur(false)
        }
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FAB_MENU_ANIMATION_MS = 220L
        private const val FAB_ACTION_TRANSLATION_Y = 28f
        private const val CONTENT_BLUR_RADIUS = 28f
    }
}