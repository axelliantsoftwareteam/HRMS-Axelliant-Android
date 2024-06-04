package com.axelliant.hrms.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.ApprovalsDetailAdapter
import com.axelliant.hrms.adapter.PersonSpinnerAdapter
import com.axelliant.hrms.adapter.SubFilterAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.databinding.FragmentApprovalsBinding
import com.axelliant.hrms.enums.AttendanceFilter
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.model.attendance.AttendanceData
import com.axelliant.hrms.model.attendance.AttendanceInput
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.model.dashboard.FilterModel
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.utils.Utils.getRandomString
import com.axelliant.hrms.viewmodel.AttendanceViewModel
import org.koin.android.ext.android.inject
import kotlin.random.Random


class ApprovalsFragment : BaseFragment() {


    private var _binding: FragmentApprovalsBinding? = null

    private var startDateString: String? = null
    private var endDateString: String? = null
    private val binding get() = _binding!!
    private var currentFilter = AttendanceFilter.WEEK
    private val attendanceViewModel: AttendanceViewModel by inject()
    private var filterId = ""


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentApprovalsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attendanceViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        binding.ivBack.setOnClickListener {
            previousFragmentNavigation()
        }

        spinnerPopulations()
        eventSelection()
        attendanceViewModel.getTeamAttendance(getCurrentObject())

        attendanceViewModel.teamAttendanceResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success

                    val array :ArrayList<FilterModel> = arrayListOf()
                    array.add(FilterModel().apply {
                        this.title = "Approved"
                        this.id = "Approved"
                        this.count= getRandomString()
                    })
                    array.add(FilterModel().apply {
                        this.title = "Pending"
                        this.id = "Pending"
                        this.count= getRandomString()
                    })
                    array.add(FilterModel().apply {
                        this.title = "Cancelled"
                        this.id = "Cancelled"
                        this.count= getRandomString()
                    })
                    subFilterPopulations(
                     array
                    )

                    dataPopulate(response.attendance_data!!)
                    binding.tvTeamMemberTxt.text = response.team_count.toString()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

    }

    private fun eventSelection() {
        binding.tvWeek.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding.tvMonth.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding.tvWeek.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvMonth.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding.tvWeek.setOnClickListener {
            currentFilter = AttendanceFilter.WEEK
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }
        binding.tvMonth.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            attendanceViewModel.getTeamAttendance(getCurrentObject())
            eventSelection()
        }


        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                binding.tvWeek.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvWeek.setTextColor(requireContext().getColor(R.color.white))

            }

            AttendanceFilter.MONTH -> {

                binding.tvMonth.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvMonth.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }


    }

    private fun getCurrentObject(): AttendanceInput {

        when (currentFilter) {
            AttendanceFilter.WEEK -> {
                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()
            }

            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())
            }

            else -> {}
        }


        val currentEmploy = binding.spTeamMember.selectedItem as EmployProfile
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter
            if (currentEmploy.name == null)
                this.employeeId = listOf()
            else
                this.employeeId = listOf(currentEmploy.name.toString())


        }

    }

    private fun spinnerPopulations() {

        val adapter = PersonSpinnerAdapter(
            requireContext(), GlobalConfig.getReportingEmploys()
        )
        binding.spTeamMember.adapter = adapter


        binding.spTeamMember.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                attendanceViewModel.getTeamAttendance(getCurrentObject())

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }


    }

    private fun subFilterPopulations(attendanceStatusList: ArrayList<FilterModel>) {

        attendanceStatusList.add(0, FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "10"
        })
        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = SubFilterAdapter(
            filterId,
            attendanceStatusList, requireContext(),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val filterObject = customObject as FilterModel

                    filterId = filterObject.id.toString()
//                    leaveViewModel.getTeamLeaveDetail(getCurrentObject())

                }

            }
        )
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }

    private fun dataPopulate(detailArrayList: ArrayList<AttendanceData>) {
        binding.rvAttend.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = ApprovalsDetailAdapter(
            requireContext(), detailArrayList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as AttendanceData
                    AppNavigator.navigateToMyAttendanceDetail(Bundle().apply {
                        this.putString(AppConst.KEY_ID, currentObject.id)
                    })
                }
            }, RequestFilter.ATTENDANCE.name, true
        )
        binding.rvAttend.adapter = weeklyAdapter
    }

}