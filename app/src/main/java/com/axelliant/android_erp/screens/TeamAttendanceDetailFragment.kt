package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.PersonSpinnerAdapter
import com.axelliant.android_erp.adapter.SubFilterAdapter
import com.axelliant.android_erp.adapter.TeamAttendanceDetailAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.FragmentTeamAttendanceDetailBinding
import com.axelliant.android_erp.extention.showSuccessMsg


class TeamAttendanceDetailFragment : BaseFragment() {

    private var _binding: FragmentTeamAttendanceDetailBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentTeamAttendanceDetailBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }
        dataPopulate()
        eventSelection()
        subFilterPopulations()
        spinnerPopulations()

    }

    private fun spinnerPopulations() {

        val adapter = PersonSpinnerAdapter(
            requireContext(), listOf(
                Test("All Team"),
                Test("Adnan Maqbool"),
                Test("Muhammad Arslan"),
                Test("Munir Ahmad"),
                Test("Zeeshan Rasool"),
                Test("Amjad Ali"),
                Test("Ali Aslam")
            )
        )
        binding?.spTeamMember?.adapter = adapter

    }

    private fun subFilterPopulations() {
        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            listOf(
                Test("Pending"),
                Test("Approved"),
                Test("Work from home"),
                Test("In office"),
                Test("Remote"),
                Test("Rejected")
            ), requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Test
                    requireContext().showSuccessMsg(
                        currentObject.testString
                    )

                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }

    private fun dataPopulate() {
        binding?.rvAttend?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TeamAttendanceDetailAdapter(
            listOf(
                Test("item1"),
                Test("item2"),
                Test("item3"),
                Test("item4"),
                Test("item5"),
                Test("item6")
            )
        )
        binding?.rvAttend?.adapter = weeklyAdapter


    }

    private fun eventSelection() {
        binding?.tvWeek?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvMonth?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvCustom?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            eventSelection()
        }

        binding?.tvCustom?.setOnClickListener {
            currentFilter = AttendanceFilter.Custom
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

            AttendanceFilter.Custom -> {

                binding?.tvCustom?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvCustom?.setTextColor(requireContext().getColor(R.color.white))
            }

            else -> {}
        }
    }


}