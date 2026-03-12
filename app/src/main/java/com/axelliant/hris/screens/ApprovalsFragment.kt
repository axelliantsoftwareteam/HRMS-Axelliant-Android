package com.axelliant.hris.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.ApprovalsDetailAdapter
import com.axelliant.hris.adapter.ExpenseApprovalsDetailAdapter
import com.axelliant.hris.adapter.PersonSpinnerAdapter
import com.axelliant.hris.adapter.TeamLeaveDetailAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentApprovalsBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.model.attendance.AttendanceApprovalObject
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.expense.Expense
import com.axelliant.hris.model.leave.ExpenseApprovalStatus
import com.axelliant.hris.model.leave.LeaveApproval
import com.axelliant.hris.model.leave.TeamLeaveDetail
import com.axelliant.hris.viewmodel.AttendanceViewModel
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.axelliant.hris.viewmodel.LeaveViewModel
import org.koin.android.ext.android.inject


class ApprovalsFragment : BaseFragment() {


    private var _binding: FragmentApprovalsBinding? = null


    private val binding get() = _binding!!
    private var currentFilter = RequestFilter.LEAVE
    private var currentLeaveApprovals: ArrayList<TeamLeaveDetail> = arrayListOf()
    private var currentAttendanceApprovals: ArrayList<AttendanceApprovalObject> = arrayListOf()
    private var currentExpenseApprovals: ArrayList<Expense> = arrayListOf()
    private val attendanceViewModel: AttendanceViewModel by inject()
    private val leaveViewModel: LeaveViewModel by inject()
    private val expenseViewModel: ExpenseViewModel by inject()

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
        binding.btnBulkApprove.setOnClickListener {
            bulkUpdateVisibleApprovals("Approved")
        }
        binding.btnBulkReject.setOnClickListener {
            bulkUpdateVisibleApprovals("Rejected")
        }

        spinnerPopulations()
        binding.rvAttend.visibility=View.VISIBLE
        eventSelection()

        leaveViewModel.getTeamLeaveDetail(getCurrentObject())

        leaveViewModel.teamLeaveDetailResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    if (response.leaves?.size ?: 0 > 0)
                    {
                        currentLeaveApprovals = ArrayList(response.leaves ?: arrayListOf())
                        binding.rvAttend.visibility=View.VISIBLE
                        binding.tvNoRecord.visibility=View.GONE

                        dataPopulate(response.leaves)
                    } else {
                        currentLeaveApprovals = arrayListOf()
                        binding.rvAttend.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }
                    updateBulkActionsState()

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
                    if (response.checkin?.size ?: 0 > 0)
                    {
                        currentAttendanceApprovals = ArrayList(response.checkin ?: arrayListOf())
                        binding.rvAttend.visibility=View.VISIBLE
                        binding.tvNoRecord.visibility=View.GONE

                        attendanceDataPopulate(response.checkin!!)
                    } else {
                        currentAttendanceApprovals = arrayListOf()
                        binding.rvAttend.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }
                    updateBulkActionsState()

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        expenseViewModel.expenseApproval.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response.expenses?.size ?: 0 > 0)
                    {
                        currentExpenseApprovals = ArrayList(response.expenses ?: arrayListOf())
                        binding.rvExpense.visibility=View.VISIBLE
                        binding.tvNoRecord.visibility=View.GONE

                        expenseDataPopulate(response.expenses!!)
                    } else {
                        currentExpenseApprovals = arrayListOf()
                        binding.rvExpense.visibility = View.GONE
                        binding.tvNoRecord.visibility = View.VISIBLE
                    }
                    updateBulkActionsState()



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

    }

    private fun eventSelection() {
        binding.tvWeek.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding.tvMonth.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)
        binding.tvExpense.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)


        binding.tvWeek.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvMonth.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding.tvExpense.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding.tvWeek.setOnClickListener {
            currentFilter = RequestFilter.LEAVE
                    leaveViewModel.getTeamLeaveDetail(getCurrentObject())
                    binding.tvTeamMember.text = "Leave Requests"
                    binding.rvAttend.visibility=View.VISIBLE
                    binding.rvExpense.visibility=View.GONE
                    eventSelection()
                    updateBulkActionsState()
        }
        binding.tvMonth.setOnClickListener {
            currentFilter = RequestFilter.ATTENDANCE
            attendanceViewModel.getAttendanceApproval(getCurrentObject())
            binding.tvTeamMember.text = "Check In Requests"
            binding.rvAttend.visibility=View.VISIBLE
            binding.rvExpense.visibility=View.GONE
            eventSelection()
            updateBulkActionsState()
        }

        binding.tvExpense.setOnClickListener {
            currentFilter = RequestFilter.APPROVAL
            expenseViewModel.getExpenseApproval(getCurrentObject())
            binding.tvTeamMember.text = "Approval Requests"
            binding.rvAttend.visibility=View.GONE
            binding.rvExpense.visibility=View.VISIBLE
            eventSelection()
            updateBulkActionsState()

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
            RequestFilter.APPROVAL -> {

                binding.tvExpense.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding.tvExpense.setTextColor(requireContext().getColor(R.color.white))
            }


            else -> {}
        }


    }

    private fun bulkUpdateVisibleApprovals(status: String) {
        when (currentFilter) {
            RequestFilter.LEAVE -> {
                val ids = currentLeaveApprovals.mapNotNull { it.name }.filter { it.isNotBlank() }
                if (ids.isEmpty()) {
                    requireContext().showErrorMsg("No leave approvals are visible right now.")
                    return
                }
                leaveViewModel.bulkLeaveApprovalStatus(ids, status)
            }
            RequestFilter.ATTENDANCE -> {
                val ids = currentAttendanceApprovals.mapNotNull { it.name }.filter { it.isNotBlank() }
                if (ids.isEmpty()) {
                    requireContext().showErrorMsg("No attendance approvals are visible right now.")
                    return
                }
                attendanceViewModel.bulkAttendanceApprovalStatus(ids, status)
            }
            RequestFilter.APPROVAL -> {
                val ids = currentExpenseApprovals.mapNotNull { it.name }.filter { it.isNotBlank() }
                if (ids.isEmpty()) {
                    requireContext().showErrorMsg("No expense approvals are visible right now.")
                    return
                }
                expenseViewModel.bulkExpenseApprovalStatus(ids, status)
            }
            else -> Unit
        }
    }

    private fun updateBulkActionsState() {
        val hasBulkItems = when (currentFilter) {
            RequestFilter.LEAVE -> currentLeaveApprovals.isNotEmpty()
            RequestFilter.ATTENDANCE -> currentAttendanceApprovals.isNotEmpty()
            RequestFilter.APPROVAL -> currentExpenseApprovals.isNotEmpty()
            else -> false
        }

        binding.lyBulkActions.visibility = if (hasBulkItems) View.VISIBLE else View.GONE
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
                    RequestFilter.APPROVAL ->expenseViewModel.getExpenseApproval(getCurrentObject()
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
                        this.status = "Approved"
                    })
                }
            }, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val attObject = customObject as Expense
                    expenseViewModel.expenseApprovalStatus(ExpenseApprovalStatus().apply {
                        this.expense_id = attObject.name.toString()
                        this.status = "Rejected"
                    })
                }
            }
        )
        binding.rvExpense.adapter = weeklyAdapter
    }

}
