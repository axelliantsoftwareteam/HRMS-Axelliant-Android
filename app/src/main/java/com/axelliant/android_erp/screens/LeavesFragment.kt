package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.LeaveAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.FragmentLeavesBinding
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.screens.LeaveEvents.*

enum class LeaveEvents {
    Upcoming,
    Past,
    TeamLeave
}

class LeavesFragment : BaseFragment() {

    private var leaveEvent = Upcoming

    private var _binding: FragmentLeavesBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentLeavesBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataPopulate()
        eventSelection()
    }

    private fun eventSelection() {
        binding?.tvLikeReceived?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvLikeSent?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvLikeMatches?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvLikeReceived?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvLikeSent?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvLikeMatches?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        when (leaveEvent) {
            Upcoming -> {
                binding?.tvLikeReceived?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvLikeReceived?.setTextColor(requireContext().getColor(R.color.white))

            }

            Past -> {

                binding?.tvLikeSent?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvLikeSent?.setTextColor(requireContext().getColor(R.color.white))
            }

            TeamLeave -> {

                binding?.tvLikeMatches?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvLikeMatches?.setTextColor(requireContext().getColor(R.color.white))
            }
        }

        binding?.tvLikeReceived?.setOnClickListener {
            leaveEvent = Upcoming
            eventSelection()
        }


        binding?.tvLikeSent?.setOnClickListener {
            leaveEvent = Past
            eventSelection()
        }


        binding?.tvLikeMatches?.setOnClickListener {
            leaveEvent = TeamLeave
            eventSelection()
        }

        binding?.imgAdd?.setOnClickListener{
            AppNavigator.navigateToApplyLeaves()

        }

    }

    private fun dataPopulate() {
        binding?.rvLeave?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = LeaveAdapter(
            listOf(
                Test("item1"),
                Test("item2"),
                Test("item3"),
                Test("item4"),
                Test("item5"),
                Test("item6")
            ),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Test
                    requireContext().showSuccessMsg(
                        currentObject.testString
                    )

                }

            })
        binding?.rvLeave?.adapter = weeklyAdapter


    }
}