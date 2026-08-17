package com.axelliant.hris.features.calendar.presentation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.axelliant.hris.databinding.BottomSheetCalendarEventDetailsBinding
import com.axelliant.hris.features.calendar.domain.model.CalendarEventUiModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class EventDetailsBottomSheet : BottomSheetDialogFragment() {
    private var _binding: BottomSheetCalendarEventDetailsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCalendarEventDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.eventTitle.text = args.getString(ARG_TITLE).orEmpty()
        binding.eventSubtitle.text = args.getString(ARG_SUBTITLE).orEmpty()
        binding.dateValue.text = args.getString(ARG_DATE).orEmpty()
        binding.timeValue.text = args.getString(ARG_TIME).orEmpty()
        binding.locationValue.text = args.getString(ARG_LOCATION).orEmpty()
        binding.organizerValue.text = args.getString(ARG_ORGANIZER).orEmpty()
        binding.attendeesValue.text = args.getString(ARG_ATTENDEES).orEmpty()
        binding.descriptionValue.text = args.getString(ARG_DESCRIPTION).orEmpty()
        binding.teamsBadge.isVisible = args.getBoolean(ARG_TEAMS)
        binding.eventAccent.backgroundTintList =
            android.content.res.ColorStateList.valueOf(args.getInt(ARG_COLOR))
        val teamsUrl = args.getString(ARG_TEAMS_URL).orEmpty()
        val outlookUrl = args.getString(ARG_OUTLOOK_URL).orEmpty()
        binding.joinTeamsButton.isVisible = teamsUrl.isNotBlank()
        binding.openOutlookButton.isVisible = outlookUrl.isNotBlank()
        binding.joinTeamsButton.setOnClickListener { openUrl(teamsUrl) }
        binding.openOutlookButton.setOnClickListener { openUrl(outlookUrl) }
        binding.closeButton.setOnClickListener { dismiss() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "EventDetailsBottomSheet"
        private const val ARG_TITLE = "title"
        private const val ARG_SUBTITLE = "subtitle"
        private const val ARG_DATE = "date"
        private const val ARG_TIME = "time"
        private const val ARG_LOCATION = "location"
        private const val ARG_ORGANIZER = "organizer"
        private const val ARG_ATTENDEES = "attendees"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_TEAMS = "teams"
        private const val ARG_COLOR = "color"
        private const val ARG_TEAMS_URL = "teams_url"
        private const val ARG_OUTLOOK_URL = "outlook_url"

        fun newInstance(event: CalendarEventUiModel): EventDetailsBottomSheet {
            return EventDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, event.title)
                    putString(ARG_SUBTITLE, event.subtitle)
                    putString(ARG_DATE, event.dateLabel)
                    putString(ARG_TIME, event.timeRange)
                    putString(ARG_LOCATION, event.location)
                    putString(ARG_ORGANIZER, listOf(event.organizerName, event.organizerEmail)
                        .filter { it.isNotBlank() && it != "--" }
                        .joinToString("\n")
                        .ifBlank { "--" })
                    putString(ARG_ATTENDEES, event.attendeesSummary)
                    putString(ARG_DESCRIPTION, event.description)
                    putBoolean(ARG_TEAMS, event.isTeamsMeeting)
                    putInt(ARG_COLOR, event.accentColor)
                    putString(ARG_TEAMS_URL, event.onlineMeetingUrl.orEmpty())
                    putString(ARG_OUTLOOK_URL, event.webLink.orEmpty())
                }
            }
        }
    }

    private fun openUrl(url: String) {
        if (url.isBlank()) return
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
