package com.axelliant.hris.bottomSheet

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.TodayTeamAttendanceDetailAdapter
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.databinding.TeamAttendDetailBottomSheetFragmentBinding
import com.axelliant.hris.enums.TodayTeamStatus
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.todayTeam.EmployTeamProfile
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TodayTeamAttendanceDetailBottomSheet(
    private val currentStatus: String,
    private val getList: ArrayList<EmployTeamProfile>,
) : BottomSheetDialogFragment() {

    private lateinit var binding: TeamAttendDetailBottomSheetFragmentBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TeamAttendDetailBottomSheetFragmentBinding.inflate(inflater, container, false)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.ThemeOverlay_Demo_BottomSheetDialog)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvTeamMember.text = currentStatus


        if (getList.size>0)
        {
            binding.tvTeamMemberTxt.text = getList.size.toString().valueQualifier()
            binding.rvAttend.isVisible=true
            dataset()
        }
        else{

            binding.rvAttend.isVisible=false
        }





        binding.ivClose.setOnClickListener {
            this.dismiss() // Close the bottom sheet
        }

        when (currentStatus) {

            TodayTeamStatus.CheckIn.value -> {
//                binding.lyHeader.backgroundTintList =
//                    ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.blue_iris))

            }

            TodayTeamStatus.CheckOut.value -> {
            }

            TodayTeamStatus.OnLeave.value -> {
            }

            TodayTeamStatus.InOffice.value -> {
            }

            TodayTeamStatus.WFH.value -> {
            }

            TodayTeamStatus.MissedPunch.value -> {
            }

        }


    }

    private fun dataset() {
        binding.rvAttend.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TodayTeamAttendanceDetailAdapter(requireContext(),getList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as EmployTeamProfile

                }

            }
        )
        binding.rvAttend.adapter = weeklyAdapter

    }

}
