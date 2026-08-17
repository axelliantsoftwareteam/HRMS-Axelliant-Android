package com.axelliant.hris.features.calendar.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentCalendarBinding
import com.axelliant.hris.features.calendar.domain.model.CalendarEventUiModel
import com.axelliant.hris.features.calendar.domain.model.CalendarUiModel
import com.axelliant.hris.features.calendar.domain.model.CalendarViewMode
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CalendarFragment : Fragment() {
    private val viewModel: CalendarViewModel by viewModels()
    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!
    private lateinit var dayAdapter: CalendarDayAdapter
    private lateinit var eventAdapter: CalendarEventAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLists()
        setupInteractions()
        observeState()
        if (viewModel.uiState.value is UiState.Idle) {
            viewModel.loadCalendar()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setupLists() = with(binding) {
        dayAdapter = CalendarDayAdapter { day -> viewModel.selectDate(day.date) }
        eventAdapter = CalendarEventAdapter { showEventDetails(it) }
        monthDaysRecycler.layoutManager = GridLayoutManager(requireContext(), 7)
        monthDaysRecycler.adapter = dayAdapter
        eventsRecycler.layoutManager = LinearLayoutManager(requireContext())
        eventsRecycler.adapter = eventAdapter
    }

    private fun setupInteractions() = with(binding) {
        appTopBar.setOnBackClickListener {
            InternalAppsNavigator.returnToHomeShell(findNavController())
        }
        retryButton.setOnClickListener { viewModel.loadCalendar() }
        refreshButton.setOnClickListener { viewModel.loadCalendar() }
        todayButton.setOnClickListener { viewModel.goToToday() }
        previousMonthButton.setOnClickListener { viewModel.goToPreviousMonth() }
        nextMonthButton.setOnClickListener { viewModel.goToNextMonth() }
        monthButton.setOnClickListener { viewModel.selectMode(CalendarViewMode.Month) }
        weekButton.setOnClickListener { viewModel.selectMode(CalendarViewMode.Week) }
        dayButton.setOnClickListener { viewModel.selectMode(CalendarViewMode.Day) }
        appTopBar.setOnSearchClickListener {
            searchInput.requestFocus()
            contentScroll.smoothScrollTo(0, searchInput.top)
        }
        searchInput.doAfterTextChanged { text -> viewModel.updateSearch(text?.toString().orEmpty()) }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        UiState.Idle -> Unit
                        UiState.Loading -> renderLoading()
                        UiState.Empty -> renderEmpty()
                        is UiState.Success -> renderCalendar(state.data)
                        is UiState.Error -> renderError(state.message)
                        UiState.Unauthorized -> renderError("Outlook calendar permission is missing.")
                    }
                }
            }
        }
    }

    private fun renderLoading() = with(binding) {
        progressBar.isVisible = true
        errorBanner.isVisible = false
        emptyState.isVisible = false
        eventsRecycler.isVisible = false
        syncStatusText.text = getString(R.string.calendar_syncing)
    }

    private fun renderEmpty() = with(binding) {
        progressBar.isVisible = false
        errorBanner.isVisible = false
        emptyState.isVisible = true
        eventsRecycler.isVisible = false
        syncStatusText.text = getString(R.string.calendar_synced_outlook)
    }

    private fun renderCalendar(model: CalendarUiModel) = with(binding) {
        progressBar.isVisible = model.isSyncing
        errorBanner.isVisible = false
        headerDate.text = model.headerTitle
        selectedDateTitle.text = model.selectedDateTitle
        updatedText.text = model.lastUpdatedText
        syncStatusText.text = getString(
            if (model.isSyncing) R.string.calendar_syncing else R.string.calendar_synced_outlook
        )
        monthDaysRecycler.isVisible = model.viewMode == CalendarViewMode.Month
        dayAdapter.submitList(model.monthDays)
        eventAdapter.submitList(model.visibleEvents)
        eventsRecycler.isVisible = model.visibleEvents.isNotEmpty()
        emptyState.isVisible = model.visibleEvents.isEmpty()
        renderMode(model.viewMode)
    }

    private fun renderError(message: String) = with(binding) {
        progressBar.isVisible = false
        errorBanner.isVisible = true
        errorMessage.text = message
        syncStatusText.text = getString(R.string.calendar_not_synced)
    }

    private fun renderMode(mode: CalendarViewMode) = with(binding) {
        val selectedText = ContextCompat.getColor(requireContext(), R.color.ds_neutral_white)
        val normalText = ContextCompat.getColor(requireContext(), R.color.ds_text_primary)
        listOf(monthButton, weekButton, dayButton).forEach { button ->
            button.background = null
            button.setTextColor(normalText)
        }
        val selectedButton = when (mode) {
            CalendarViewMode.Month -> monthButton
            CalendarViewMode.Week -> weekButton
            CalendarViewMode.Day -> dayButton
        }
        selectedButton.setBackgroundResource(R.drawable.bg_calendar_segment_selected)
        selectedButton.setTextColor(selectedText)
    }

    private fun showEventDetails(event: CalendarEventUiModel) {
        EventDetailsBottomSheet.newInstance(event).show(parentFragmentManager, EventDetailsBottomSheet.TAG)
    }
}
