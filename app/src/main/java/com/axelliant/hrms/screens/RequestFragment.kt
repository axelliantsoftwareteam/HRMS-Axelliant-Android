package com.axelliant.hrms.screens

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.LeaveSpinnerAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.config.AppConst.AttendanceRequestParam
import com.axelliant.hrms.config.AppConst.LeaveRequestParam
import com.axelliant.hrms.config.AppConst.RequestType
import com.axelliant.hrms.config.AppConst.SERVER_DATE_FORMAT_ATTENDANCE
import com.axelliant.hrms.databinding.FragmentRequestBinding
import com.axelliant.hrms.enums.RequestFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.attendance.AttendanceDetail
import com.axelliant.hrms.model.leave.LeaveDetail
import com.axelliant.hrms.model.leave.SpinnerType
import com.axelliant.hrms.model.post.AttendanceRequest
import com.axelliant.hrms.model.post.LeaveRequest
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils
import com.axelliant.hrms.viewmodel.RequestViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import java.util.Calendar
import java.util.Date


const val leaveType = "Select leave type"
const val attendanceType = "Select attendance type"
const val locationType = "Select location"

class RequestFragment : BaseFragment() {

    private var currentFilter = RequestFilter.LEAVE
    private var currentDateString: String? = null

    private var startDateString: String? = null
    private var endDateString: String? = null
    private var selectedDateRange: String? = null

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding

    private val requestViewModel: RequestViewModel by inject()

    private var isUpdate: Boolean = false

    private var preLeaveType: String = leaveType
    private var preAttendanceType: String = attendanceType
    private var preLocationType: String = locationType


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        _binding = FragmentRequestBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (arguments != null && requireArguments().containsKey(RequestType)) {
            isUpdate = true
            val type = arguments?.getString(RequestType, RequestFilter.LEAVE.name)
            if (type == RequestFilter.LEAVE.name) {
                currentFilter = RequestFilter.LEAVE
                val leaveDetail = Gson().fromJson(
                    arguments?.getString(LeaveRequestParam),
                    LeaveDetail::class.java
                )
                startDateString = leaveDetail.from_date
                endDateString = leaveDetail.to_date
                binding!!.etLeaveReason.setText(leaveDetail.leave_reason.toString())
                preLeaveType = leaveDetail.leave_type

                setDateView()
                binding?.btnApply?.setText(requireContext().getString(R.string.update))


            } else {
                currentFilter = RequestFilter.ATTENDANCE


                val attendanceDetail = Gson().fromJson(
                    arguments?.getString(AttendanceRequestParam),
                    AttendanceDetail::class.java
                )
                currentDateString = attendanceDetail.date
                binding!!.etAttendanceReason.setText(attendanceDetail.attendance_reason)
                preAttendanceType = attendanceDetail.attendance_type
                preLocationType = attendanceDetail.attendance_location
                setCurrentDate()
                binding?.btnApply?.setText(requireContext().getString(R.string.update))

            }
        }


        requestViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        requestViewModel.postLeaveResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireActivity().showSuccessMsg(response.status_message)
                    clearLeaveForm()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        requestViewModel.attendanceRequestResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireActivity().showSuccessMsg(response.status_message)
                    clearAttendanceForm()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        requestViewModel.getLeaves()
        requestViewModel.leaveTypes.observe(
            viewLifecycleOwner,
            EventObserver { response ->


                if (response?.meta?.status == true) {
                    spinnerLeavePopulations(response.leaves)

                    //parse leave spinner here


                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        requestViewModel.getAttendanceRequestInfo()
        requestViewModel.attendanceRequestInfo.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
//                    spinnerLeavePopulations(response.leaves)

                    spinnerAttendTypePopulations(response.checkin)
                    spinnerLocTypePopulations(response.location)
                    //parse leave spinner here


                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        eventSelection()

        binding?.btnApply?.setOnClickListener {

            when (currentFilter) {
                RequestFilter.LEAVE -> {

                    if (binding!!.spLeaveType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select leave type")
                    } else if (startDateString == null) {
                        requireContext().showErrorMsg("Please select start date")
                    } else if (endDateString == null) {
                        requireContext().showErrorMsg("Please select end date")
                    } else {
                        val leaveItem = binding!!.spLeaveType.selectedItem as SpinnerType


                        requestViewModel.postLeaveQuest(LeaveRequest().apply {
                            this.start_date = startDateString
                            this.end_date = endDateString
                            this.leave_reason = binding!!.etLeaveReason.text.toString()
                            this.leave_type = leaveItem.type
                            this.post_date = Utils.getServerFormat()

                        })

                    }

                }

                RequestFilter.ATTENDANCE -> {

                    if (binding!!.spAttendType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select attendance type")
                    } else if (binding!!.spLocType.selectedItemPosition == 0) {
                        requireContext().showErrorMsg("Please select location")
                    } else if (currentDateString == null) {
                        requireContext().showErrorMsg("Please select date")
                    } else {

                        val attendanceType = binding!!.spAttendType.selectedItem as SpinnerType
                        val locationType = binding!!.spLocType.selectedItem as SpinnerType

                        requestViewModel.postAttendanceQuest(AttendanceRequest().apply {
                            this.date_time = currentDateString
                            this.location = locationType.type
                            this.log_type = attendanceType.type
                            this.attendance_reason = binding!!.etAttendanceReason.text.toString()
                            this.request_status = "Pending"

                        })


                    }

                }
            }

        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.lyDate?.setOnClickListener {
            pickDate()
        }
        binding?.lyStartDate?.setOnClickListener {
            datePickerDialog()
        }
    }

    private fun clearLeaveForm() {
        startDateString = null
        endDateString = null
        binding?.spLeaveType?.setSelection(0)
        binding?.etLeaveReason?.text?.clear()
        setDateView()

    }

    private fun clearAttendanceForm() {
        currentDateString = null
        binding?.spAttendType?.setSelection(0)
        binding?.spLocType?.setSelection(0)
        binding?.etAttendanceReason?.text?.clear()
        binding?.tvDateTxt?.text = null
        binding?.tvDateTxt?.hint = requireContext().getString(R.string.date)


    }


    private fun pickDate() {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            requireActivity(), R.style.my_dialog_theme, // Apply the theme here
            { view, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                // Format the date using SimpleDateFormat
                currentDateString = Utils.getServerFormat(
                    dateFormat = SERVER_DATE_FORMAT_ATTENDANCE, date = selectedDate.time
                )
                setCurrentDate()
            },
            year,
            month,
            day
        )
        // Set the maximum date to today
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }

    private fun setCurrentDate(){
        binding?.tvDateTxt?.text = currentDateString

    }

    private fun datePickerDialog() {
        // Creating a MaterialDatePicker builder for selecting a date range
        val builder = MaterialDatePicker.Builder.dateRangePicker()
        builder.setTitleText("Select a date range")

        // Building the date picker dialog
        val datePicker = builder.build()
        datePicker.addOnPositiveButtonClickListener { selection ->
            // Retrieving the selected start and end dates
            val startDate = selection.first
            val endDate = selection.second

            // Formatting the selected dates as strings

            startDateString = Utils.getServerFormat(date = Date(startDate))
            endDateString = Utils.getServerFormat(date = Date(endDate))

            // Creating the date range string
            selectedDateRange = "$startDateString - $endDateString"
            setDateView()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            binding?.tvStartDateTxt?.text = startDateString
            binding?.tvEndDateTxt?.text = endDateString
        } else {
            binding?.tvStartDateTxt?.text = null
            binding?.tvEndDateTxt?.text = null
            binding?.tvStartDateTxt?.hint = requireContext().getString(R.string.start_date)
            binding?.tvEndDateTxt?.hint = requireContext().getString(R.string.end_date)

        }

    }

    private fun eventSelection() {
        binding?.tvWeek?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvMonth?.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.rounded_disabled)

        binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.btn_text_color))
        binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.btn_text_color))

        binding?.tvWeek?.setOnClickListener {
            currentFilter = RequestFilter.LEAVE
            eventSelection()
        }

        binding?.tvMonth?.setOnClickListener {
            currentFilter = RequestFilter.ATTENDANCE
            eventSelection()
        }

        when (currentFilter) {
            RequestFilter.LEAVE -> {
                binding?.tvWeek?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvWeek?.setTextColor(requireContext().getColor(R.color.white))
                binding?.lyCreateLeave!!.visibility = View.VISIBLE
                binding?.lyCreateAttend!!.visibility = View.GONE

            }

            RequestFilter.ATTENDANCE -> {

                binding?.tvMonth?.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.rounded_enabled)
                binding?.tvMonth?.setTextColor(requireContext().getColor(R.color.white))
                binding?.lyCreateAttend!!.visibility = View.VISIBLE
                binding?.lyCreateLeave!!.visibility = View.GONE
            }
        }
    }


    private fun spinnerLeavePopulations(leaves: ArrayList<SpinnerType>?) {

        val finalLeavesArray = arrayListOf<SpinnerType>()

        finalLeavesArray.add(0, SpinnerType().apply {
            this.type = leaveType
        })
        if (leaves != null) {
            finalLeavesArray.addAll(leaves)
        }

        val adapter = LeaveSpinnerAdapter(
            requireContext(), finalLeavesArray
        )
        binding?.spLeaveType?.adapter = adapter

        if (isUpdate) {

            for (index in 0..<finalLeavesArray.size) {
                if (finalLeavesArray[index].type.equals(preLeaveType)) {
                    binding?.spLeaveType?.setSelection(index)
                    break
                }
            }
        }




        binding?.spLeaveType?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }
    }

    private fun spinnerAttendTypePopulations(leaves: ArrayList<SpinnerType>?) {


        val finalLeavesArray = arrayListOf<SpinnerType>()

        finalLeavesArray.add(0, SpinnerType().apply {
            this.type = attendanceType
        })
        if (leaves != null) {
            finalLeavesArray.addAll(leaves)
        }

        val adapter = LeaveSpinnerAdapter(
            requireContext(), finalLeavesArray
        )
        binding?.spAttendType?.adapter = adapter

        if (isUpdate) {

            for (index in 0..<finalLeavesArray.size) {
                if (finalLeavesArray[index].type.equals(preAttendanceType)) {
                    binding?.spAttendType?.setSelection(index)
                    break
                }
            }
        }



        binding?.spAttendType?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }


    }

    private fun spinnerLocTypePopulations(leaves: ArrayList<SpinnerType>?) {


        val finalLeavesArray = arrayListOf<SpinnerType>()

        finalLeavesArray.add(0, SpinnerType().apply {
            this.type = locationType
        })
        if (leaves != null) {
            finalLeavesArray.addAll(leaves)
        }

        val adapter = LeaveSpinnerAdapter(
            requireContext(), finalLeavesArray
        )
        binding?.spLocType?.adapter = adapter

        if (isUpdate) {

            for (index in 0..<finalLeavesArray.size) {
                if (finalLeavesArray[index].type.equals(preLocationType)) {
                    binding?.spAttendType?.setSelection(index)
                    break
                }
            }
        }


        binding?.spLocType?.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }


    }
}




