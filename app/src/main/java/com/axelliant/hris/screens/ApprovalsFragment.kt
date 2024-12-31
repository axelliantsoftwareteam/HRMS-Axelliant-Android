package com.axelliant.hris.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ApprovalsDetailAdapter
import com.axelliant.hris.adapter.ExpenseApprovalsDetailAdapter
import com.axelliant.hris.adapter.PersonSpinnerAdapter
import com.axelliant.hris.adapter.ResourcesApprovalsDetailAdapter
import com.axelliant.hris.adapter.TeamLeaveDetailAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentApprovalsBinding
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.enums.ResourceStatus
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceApprovalObject
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.documentRequest.SubmitDocument
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.TeamLeaveDetail
import com.axelliant.hris.model.resourceManage.DocumentHours
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.axelliant.hris.viewmodel.LeaveViewModel
import com.axelliant.hris.viewmodel.ResourceManageViewModel
import org.koin.android.ext.android.inject


class ApprovalsFragment : BaseFragment() {


    private var currentIndex: Int = 0
    private var actStatus: Boolean = false
    private var resourceHours: ArrayList<DocumentHours>? = null
    private lateinit var resourcesApprovalsDetailAdapter: ResourcesApprovalsDetailAdapter
    private var _binding: FragmentApprovalsBinding? = null


    private val binding get() = _binding!!
    private var currentFilter = RequestFilter.LEAVE
    private val attendanceViewModel: AttendanceViewModel by inject()
    private val leaveViewModel: LeaveViewModel by inject()
    private val expenseViewModel: ExpenseViewModel by inject()
    private val resourceManageViewModel: ResourceManageViewModel by inject()

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

        leaveViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })
        expenseViewModel.getIsLoading()
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
        binding.rvAttend.visibility = View.VISIBLE
        eventSelection()

        leaveViewModel.getTeamLeaveDetail(getCurrentObject())

        leaveViewModel.teamLeaveDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    if (response.leaves?.size ?: 0 > 0) {
                        binding.rvAttend.visibility = View.VISIBLE
                        binding.tvNoRecord.visibility = View.GONE

                        dataPopulate(response.leaves)
                    } else {
                        binding.rvAttend.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }

                    binding.tvTeamMemberTxt.text = response.team_count.toString()

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })



        leaveViewModel.leaveApprovalResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showErrorMsg(response.status_message)
                    leaveViewModel.getTeamLeaveDetail(getCurrentObject())

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        attendanceViewModel.attendanceApprovalResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showErrorMsg(response.status_message)
                    attendanceViewModel.getAttendanceApproval(getCurrentObject())
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        attendanceViewModel.attendanceApproval.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response.checkin?.size ?: 0 > 0) {
                        binding.rvAttend.visibility = View.VISIBLE
                        binding.tvNoRecord.visibility = View.GONE

                        attendanceDataPopulate(response.checkin!!)
                    } else {
                        binding.rvAttend.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        expenseViewModel.expenseApproval.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response.expenses?.size ?: 0 > 0) {
                        binding.rvExpense.visibility = View.VISIBLE
                        binding.tvNoRecord.visibility = View.GONE

                        expenseDataPopulate(response.expenses!!)
                    } else {
                        binding.rvExpense.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }


                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })
        expenseViewModel.expenseApprovalResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showErrorMsg(response.status_message)
                    expenseViewModel.getExpenseApproval(getCurrentObject())
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        resourceManageViewModel.hoursResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response.resource_hour_data?.size ?: 0 > 0) {
                        binding.rvResource.visibility = View.VISIBLE
                        binding.tvNoRecord.visibility = View.GONE

                        resourceHours = response.resource_hour_data

                        binding.viewExpand.isVisible = actionStatus()
                        binding.btnStatus.isVisible = actionStatus()

                        resourceDataPopulate()
                    } else {
                        binding.rvResource.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                        binding.viewExpand.isVisible = false
                        binding.btnStatus.isVisible = false
                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding.tvApproved.setOnClickListener {
            Log.d("Approved", getMultiSelectedDocument().toString())
            resourceManageViewModel.submitResourceHour(SubmitDocument().apply {
                this.resource_hour_id_list = getMultiSelectedDocument()
                this.status = LeaveStatus.APPROVED.value

            })
        }
        binding.tvReject.setOnClickListener {
            Log.d("Reject", getMultiSelectedDocument().toString())
            resourceManageViewModel.submitResourceHour(SubmitDocument().apply {
                this.resource_hour_id_list = getMultiSelectedDocument()
                this.status = LeaveStatus.REJECTED.value

            })
        }
        binding.viewExpand.setOnCheckedChangeListener { _, isChecked ->
            resourceHours?.forEachIndexed { index, documentHour ->
                if (documentHour.docstatus == ResourceStatus.DRAFT.value) {
                    documentHour.isSelected = isChecked
                    resourcesApprovalsDetailAdapter.notifyItemChanged(index)
                }
            }
        }

    }

    private fun actionStatus(): Boolean {
        resourceHours?.forEachIndexed { index, documentHour ->
            if (documentHour.docstatus == ResourceStatus.DRAFT.value) {
                return true
            }
        }
        return false
    }


    private fun eventSelection() {
        binding.tvWeek.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding.tvMonth.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)
        binding.tvExpense.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding.tvResources.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding.tvWeek.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvMonth.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvExpense.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvResources.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding.tvWeek.setOnClickListener {
            currentFilter = RequestFilter.LEAVE
            leaveViewModel.getTeamLeaveDetail(getCurrentObject())
            binding.tvTeamMember.text = "Leave Requests"
            binding.rvAttend.visibility = View.VISIBLE
            binding.rvExpense.visibility = View.GONE
            binding.rvResource.visibility = View.GONE
            binding.btnStatus.visibility = View.GONE
            binding.viewExpand.visibility = View.GONE
            eventSelection()
        }
        binding.tvMonth.setOnClickListener {
            currentFilter = RequestFilter.ATTENDANCE
            attendanceViewModel.getAttendanceApproval(getCurrentObject())
            binding.tvTeamMember.text = "Check-In Requests"
            binding.rvAttend.visibility = View.VISIBLE
            binding.rvExpense.visibility = View.GONE
            binding.rvResource.visibility = View.GONE
            binding.btnStatus.visibility = View.GONE
            binding.viewExpand.visibility = View.GONE

            eventSelection()
        }

        binding.tvExpense.setOnClickListener {
            currentFilter = RequestFilter.EXPENSE
            expenseViewModel.getExpenseApproval(getCurrentObject())
            binding.tvTeamMember.text = "Expense Requests"
            binding.rvAttend.visibility = View.GONE
            binding.rvExpense.visibility = View.VISIBLE
            binding.rvResource.visibility = View.GONE
            binding.btnStatus.visibility = View.GONE
            binding.viewExpand.visibility = View.GONE


            eventSelection()

        }
        binding.tvResources.setOnClickListener {
            currentFilter = RequestFilter.RESOURCES
            resourceManageViewModel.getResourcesApproval(getCurrentObject())
            binding.tvTeamMember.text = "Resource Hours Requests"
            binding.rvAttend.visibility = View.GONE
            binding.rvExpense.visibility = View.GONE
            binding.rvResource.visibility = View.VISIBLE

            eventSelection()
        }


        when (currentFilter) {
            RequestFilter.LEAVE -> {
                binding.tvWeek.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvWeek.setTextColor(requireContext().getColor(R.color.white))

            }

            RequestFilter.ATTENDANCE -> {

                binding.tvMonth.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvMonth.setTextColor(requireContext().getColor(R.color.white))
            }

            RequestFilter.EXPENSE -> {
                binding.tvExpense.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvExpense.setTextColor(requireContext().getColor(R.color.white))
            }

            RequestFilter.RESOURCES -> {
                binding.tvResources.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvResources.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }


    }


    private fun getCurrentObject(): AttendanceInput {

        val currentEmploy = binding.spTeamMember.selectedItem as EmployProfile
        return AttendanceInput().apply {
            if (currentEmploy.name == null)
                this.employeeId = listOf()
            else
                this.employeeId = listOf(currentEmploy.name.toString())

            this.for_approvals = 1
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
                when (currentFilter) {
                    RequestFilter.LEAVE -> leaveViewModel.getTeamLeaveDetail(getCurrentObject())
                    RequestFilter.ATTENDANCE -> attendanceViewModel.getAttendanceApproval(
                        getCurrentObject()
                    )

                    RequestFilter.EXPENSE -> expenseViewModel.getExpenseApproval(
                        getCurrentObject()
                    )

                    RequestFilter.RESOURCES -> resourceManageViewModel.getResourcesApproval(
                        getCurrentObject()
                    )
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }


    }

    private fun dataPopulate(leaves: ArrayList<TeamLeaveDetail>?) {
        binding.rvAttend.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = TeamLeaveDetailAdapter(requireContext(),
            leaves!!, true, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {

                    val leaveDetail = customObject as TeamLeaveDetail
                    // call approved APi here
                    leaveViewModel.leaveApprovalStatus(LeaveApproval().apply {
                        this.leave_id = leaveDetail.name
                        this.status = "Approved"
                    })


                }

            }, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {

                    val leaveDetail = customObject as TeamLeaveDetail
                    // call approved APi here
                    leaveViewModel.leaveApprovalStatus(LeaveApproval().apply {
                        this.leave_id = leaveDetail.name
                        this.status = "Rejected"
                    })

                }

            })
        binding.rvAttend.adapter = weeklyAdapter

    }

    private fun attendanceDataPopulate(detailArrayList: ArrayList<AttendanceApprovalObject>) {
        binding.rvAttend.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = ApprovalsDetailAdapter(
            requireContext(), detailArrayList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attObject = customObject as AttendanceApprovalObject
                    attendanceViewModel.attendanceApprovalStatus(LeaveApproval().apply {
                        this.checkin_id = attObject.name
                        this.status = "Approved"
                    })
                }
            }, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attObject = customObject as AttendanceApprovalObject
                    attendanceViewModel.attendanceApprovalStatus(LeaveApproval().apply {
                        this.checkin_id = attObject.name
                        this.status = "Rejected"
                    })
                }
            }
        )
        binding.rvAttend.adapter = weeklyAdapter
    }

    private fun expenseDataPopulate(detailArrayList: ArrayList<Expense>) {
        binding.rvExpense.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = ExpenseApprovalsDetailAdapter(
            requireContext(), detailArrayList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attObject = customObject as Expense
                    expenseViewModel.expenseApprovalStatus(ExpenseApprovalStatus().apply {
                        this.expense_id = attObject.name.toString()
                        this.status = LeaveStatus.APPROVED.value
                    })
                }
            }, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attObject = customObject as Expense
                    expenseViewModel.expenseApprovalStatus(ExpenseApprovalStatus().apply {
                        this.expense_id = attObject.name.toString()
                        this.status = LeaveStatus.REJECTED.value
                    })
                }
            }
        )
        binding.rvExpense.adapter = weeklyAdapter
    }

    private fun resourceDataPopulate() {


        binding.rvResource.layoutManager = LinearLayoutManager(requireActivity())
        resourcesApprovalsDetailAdapter = resourceHours?.let {
            ResourcesApprovalsDetailAdapter(
                requireContext(), it,
                object : AdapterItemClick {
                    override fun onItemClick(customObject: Any, position: Int) {
                        val documentHour = customObject as DocumentHours
                        documentHour.isSelected = !documentHour.isSelected
                        resourcesApprovalsDetailAdapter.notifyItemChanged(position)
                    }
                }
            )
        }!!
        binding.rvResource.adapter = resourcesApprovalsDetailAdapter
    }

    private fun getMultiSelectedDocument(): ArrayList<String> {

        val arrayList: ArrayList<String> = arrayListOf()

        for (item in 0..<(resourceHours?.size ?: 0)) {
            if (resourceHours?.get(item)?.isSelected == true)
                arrayList.add(resourceHours!![item].name.toString())
        }

        return arrayList
    }

}