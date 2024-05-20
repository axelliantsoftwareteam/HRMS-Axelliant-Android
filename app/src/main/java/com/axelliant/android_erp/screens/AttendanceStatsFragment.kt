package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.axelliant.android_erp.R
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.databinding.FragmentAttendanceStatsBinding
import com.axelliant.android_erp.navigation.AppNavigator

enum class AttendanceFilter {
    WEEK,
    MONTH,
    Custom
}



class AttendanceStatsFragment : BaseFragment() {
    private var currentFilter = AttendanceFilter.WEEK

    private var _binding: FragmentAttendanceStatsBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentAttendanceStatsBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        eventSelection()


        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()

        }

        binding?.tvMyAttendance?.setOnClickListener {
            showDialog()
            AppNavigator.navigateToMyAttendanceDetail()

        }
    }

    private fun eventSelection() {
        randomData()
        binding?.tvWeek?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvMonth?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            eventSelection()
        }

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))

            }

            AttendanceFilter.MONTH -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }
    }

    private fun randomData(){
        binding?.tvAbsentTxt?.text =getRandomString()
        binding?.tvHalfDayTxt?.text =getRandomString()
        binding?.tvMissPunchOutTxt?.text =getRandomString()
        binding?.tvLeavesTxt?.text =getRandomString()
        binding?.tvHolidayTxt?.text =getRandomString()
        binding?.tvWeeklyOffsTxt?.text =getRandomString()

    }

    private fun getRandomString():String{
      return   (0..10).random().toString()
    }




}