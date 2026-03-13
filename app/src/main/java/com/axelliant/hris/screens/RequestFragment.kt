package com.axelliant.hris.screens

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.axelliant.hris.R
import com.axelliant.hris.adapter.LeaveSpinnerAdapter
import com.axelliant.hris.adapter.LeaveWithCountSpinnerAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst.AttendanceRequestParam
import com.axelliant.hris.config.AppConst.LeaveRequestParam
import com.axelliant.hris.config.AppConst.RequestType
import com.axelliant.hris.config.AppConst.SERVER_DATE_FORMAT_ATTENDANCE
import com.axelliant.hris.databinding.FragmentRequestBinding
import com.axelliant.hris.enums.RequestFilter
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.nullToEmpty
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.checkin.CheckInDetail
import com.axelliant.hris.model.leave.LeaveDetail
import com.axelliant.hris.model.leave.SpinnerType
import com.axelliant.hris.model.leave.leaveCount.LeaveAllocation
import com.axelliant.hris.model.leave.leaveCount.LeaveCountByDaysRequest
import com.axelliant.hris.model.leave.leaveCount.Leaves
import com.axelliant.hris.model.post.AttendanceRequest
import com.axelliant.hris.model.post.LeaveRequest
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.RequestViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


const val leaveType = "Select leave type"
const val attendanceType = "Select attendance type"
const val locationType = "Select location"

class RequestFragment : BaseFragment() {

    private var halfDayCount: Double? = null
    private var ishalfday: Boolean? = null
    private var daysCount: Long? = null
    private var currentFilter = RequestFilter.LEAVE
    private var currentDateString: String? = null
    private var currentTimeString: String? = null

    private var halfDateString: String? = null
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var selectedDateRange: String? = null

    private var _binding: FragmentRequestBinding? = null
    private val binding get() = _binding

    private val requestViewModel: RequestViewModel by inject()

    private var isUpdate: Boolean = false
    private var leaveId: String = ""
    private var checkInId: String = ""

    private var selectedLeaveType: String? = ""
    private var preLeaveType: String = leaveType
    private var preAttendanceType: String = attendanceType
    private var preLocationType: String = locationType

    private fun selectedLeaveBalance(): Double {
        return binding?.tvRemainingLeaveTxt?.text?.toString()?.toDoubleOrNull() ?: 0.0
    }

    private fun requestedLeaveDays(): Double {
        return binding?.tvLeaveCountTxt?.text?.toString()?.toDoubleOrNull() ?: 0.0
    }


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

                binding?.etLeaveReason?.setText(leaveDetail.leave_reason.nullToEmpty())
                preLeaveType = leaveDetail.leave_type
                leaveId = leaveDetail.name
                setDateView()

                binding?.btnApply?.isVisible = false
                binding?.btnUpdate?.isVisible = true
                binding?.btnDeleted?.isVisible = true
                binding?.tvMonth?.isVisible = false


            } else {
                currentFilter = RequestFilter.ATTENDANCE


                val checkInDetail = Gson().fromJson(
                    arguments?.getString(AttendanceRequestParam),
                    CheckInDetail::class.java
                )
                checkInId = checkInDetail.name
                val dateTimeParts = checkInDetail.time
                    ?.trim()
                    ?.split(Regex("\\s+"), limit = 2)
                    .orEmpty()

                currentDateString = dateTimeParts.getOrNull(0)
                currentTimeString = dateTimeParts.getOrNull(1)

                binding?.etAttendanceReason?.setText(checkInDetail.reason)
                preAttendanceType = checkInDetail.log_type
                preLocationType = checkInDetail.location
                setCurrentDate()
                setCurrentTime()

                binding?.btnApply?.isVisible = false
                binding?.btnUpdate?.isVisible = true
                binding?.btnDeleted?.isVisible = true
                binding?.tvWeek?.isVisible = false

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

        requestViewModel.updateLeaveResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    requireActivity().showSuccessMsg(response.status_message)
                    if (currentFilter == RequestFilter.LEAVE)
                        clearLeaveForm()
                    else
                        clearAttendanceForm()

                    Handler(Looper.getMainLooper()).postDelayed({
                        // do stuff
                        AppNavigator.moveBackToPreviousFragment()
                    }, 200)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })
        requestViewModel.leaveCountByDate.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    halfDayCount = response.days
                    binding?.tvLeaveCountTxt?.text = halfDayCount.toString()

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        requestViewModel.deleteLeaveResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {

                    requireActivity().showSuccessMsg(response.status_message)
                    if (currentFilter == RequestFilter.LEAVE)
                        clearLeaveForm()
                    else
                        clearAttendanceForm()


                    Handler(Looper.getMainLooper()).postDelayed({
                        // do stuff
                        AppNavigator.moveBackToPreviousFragment()
                    }, 200)

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


//        requestViewModel.getLeaves()
//        requestViewModel.leaveTypes.observe(
//            viewLifecycleOwner,
//            EventObserver { response ->
//
//                requestViewModel.getAttendanceRequestInfo()
//                if (response?.meta?.status == true) {
//                    spinnerLeavePopulations(response.leaves)
//                } else {
//                    requireContext().showErrorMsg(response?.meta?.message.toString())
//                }
//
//            })


        requestViewModel.getLeavesCount()
        requestViewModel.getLeaveTypesCount.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                requestViewModel.getAttendanceRequestInfo()
                if (response?.meta?.status == true) {
                    spinnerLeavePopulations(response.leaves)
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

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

        binding?.halfDay?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                ishalfday = isChecked
                binding?.lyHalfday?.isVisible = (halfDayCount?.toInt() ?: 0) > 1
            } else {
                ishalfday = isChecked
                binding?.lyHalfday?.isVisible = false
                halfDateString = null
                binding?.tvHalfDateTxt?.text = halfDateString
            }
            if (startDateString != null && endDateString != null)
                getDayCount()
        }

        binding?.btnApply?.setOnClickListener {

            addUpdateCall()

        }

        binding?.btnUpdate?.setOnClickListener {
            addUpdateCall()
        }

        binding?.btnDeleted?.setOnClickListener {

            if (currentFilter == RequestFilter.LEAVE) {
                requestViewModel.deleteLeaveQuest(LeaveRequest().apply {
                    this.leave_id = leaveId

                })

            } else {
                requestViewModel.deleteAttendanceQuest(LeaveRequest().apply {
                    this.checkin_id = checkInId

                })

            }

        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.lyDate?.setOnClickListener {
            pickDate()
        }

        binding?.lyTime?.setOnClickListener {
            pickTime()
        }
        binding?.lyStartDate?.setOnClickListener {
            binding?.halfDay?.isChecked = false
            datePickerDialog()
        }

        binding?.lyHalfdayDate?.setOnClickListener {

            showDatePicker()
        }


    }

    private fun addUpdateCall() {
        when (currentFilter) {
            RequestFilter.LEAVE -> {

                if (binding?.spLeaveType?.selectedItemPosition == 0) {
                    requireContext().showErrorMsg("Please select leave type")
                } else if (startDateString == null) {
                    requireContext().showErrorMsg("Please select start date")
                } else if (endDateString == null) {
                    requireContext().showErrorMsg("Please select end date")
                } else if (binding?.etLeaveReason?.text?.isEmpty() == true) {
                    requireContext().showErrorMsg("Please add reason for leave")
                } else if (requestedLeaveDays() > selectedLeaveBalance()) {
                    requireContext().showErrorMsg("Requested leave exceeds your remaining quota for this leave type")
                } else {
                    val leaveItem = binding?.spLeaveType?.selectedItem as LeaveAllocation

                    if (isUpdate) {
                        requestViewModel.updateLeaveQuest(LeaveRequest().apply {
                            this.start_date = startDateString
                            this.end_date = endDateString
                            this.leave_reason = binding?.etLeaveReason?.text.toString()
                            this.leave_type = leaveItem.name
                            this.post_date = Utils.getServerFormat()
                            this.leave_id = leaveId
                            this.half_day_date = halfDateString
                            this.half_day = ishalfday

                        })
                    } else {
                        requestViewModel.postLeaveQuest(LeaveRequest().apply {
                            this.start_date = startDateString
                            this.end_date = endDateString
                            this.leave_reason = binding?.etLeaveReason?.text.toString()
                            this.leave_type = leaveItem.name
                            this.post_date = Utils.getServerFormat()
                            this.half_day_date = halfDateString
                            this.half_day = ishalfday

                        })
                    }


                }

            }

            RequestFilter.ATTENDANCE -> {

                if (binding?.spAttendType?.selectedItemPosition == 0) {
                    requireContext().showErrorMsg("Please select attendance type")
                } else if (binding?.spLocType?.selectedItemPosition == 0) {
                    requireContext().showErrorMsg("Please select location")
                } else if (currentDateString == null) {
                    requireContext().showErrorMsg("Please select date")
                } else if (currentTimeString == null) {
                    requireContext().showErrorMsg("Please select time")
                } else if (binding?.etAttendanceReason?.text?.isEmpty() == true) {
                    requireContext().showErrorMsg("Please add reason for attendance")
                } else {

                    val attendanceType = binding?.spAttendType?.selectedItem as SpinnerType
                    val locationType = binding?.spLocType?.selectedItem as SpinnerType
                    if (isUpdate) {

                        requestViewModel.updateAttendanceQuest(AttendanceRequest().apply {
                            this.date_time = currentDateString.plus(" ").plus(currentTimeString)
                            this.location = locationType.type
                            this.log_type = attendanceType.type
                            this.attendance_reason = binding?.etAttendanceReason?.text.toString()
                            this.request_status = "Pending"
                            this.checkin_id = checkInId

                        })

                    } else {
                        requestViewModel.postAttendanceQuest(AttendanceRequest().apply {
                            this.date_time = currentDateString.plus(" ").plus(currentTimeString)
                            this.location = locationType.type
                            this.log_type = attendanceType.type
                            this.attendance_reason = binding?.etAttendanceReason?.text.toString()
                            this.request_status = "Pending"

                        })
                    }


                }

            }

            else -> {}
        }
    }

    private fun clearLeaveForm() {
        isUpdate = false
        startDateString = null
        endDateString = null
        leaveId = ""
        binding?.spLeaveType?.setSelection(0)
        binding?.etLeaveReason?.text?.clear()
        setDateView()

    }

    private fun clearAttendanceForm() {
        currentDateString = null
        currentTimeString = null
        binding?.spAttendType?.setSelection(0)
        binding?.spLocType?.setSelection(0)
        binding?.etAttendanceReason?.text?.clear()
        binding?.tvDateTxt?.text = null
        binding?.tvDateTxt?.hint = requireContext().getString(R.string.date)

        binding?.tvTimeTxt?.text = null
        binding?.tvTimeTxt?.hint = requireContext().getString(R.string.time)


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

    private fun pickTime() {
        val c = Calendar.getInstance()

        val hour = c[Calendar.HOUR_OF_DAY]
        val minutes = c[Calendar.MINUTE]
        val timePickerDialog = TimePickerDialog(
            requireActivity(), R.style.my_dialog_theme,
            { view, hourOfDay, minute ->

                currentTimeString =
                    hourOfDay.toString().plus(":").plus(minute)

                setCurrentTime()

            }, hour, minutes, false

        )

        timePickerDialog.show()
    }


    private fun setCurrentTime() {
        if (currentTimeString != null)
            binding?.tvTimeTxt?.text = currentTimeString
        else
            binding?.tvTimeTxt?.text = ""
    }

    private fun setCurrentDate() {
        binding?.tvDateTxt?.text = currentDateString

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                // Set the selected date in a Calendar instance
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }

                // Format the date as "yyyy-MM-dd"
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                halfDateString = dateFormat.format(selectedCalendar.time)

                binding?.tvHalfDateTxt?.text = halfDateString
                getDayCount()
            }, year, month, day
        )

        datePickerDialog.show()
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

            // Calculate the difference in days
            val differenceInMillis = endDate - startDate
            daysCount = differenceInMillis / (1000 * 60 * 60 * 24) // Convert milliseconds to days

            Log.d("DateRange", "Number of days: $daysCount")
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
            getDayCount()

        } else {
            binding?.tvStartDateTxt?.text = null
            binding?.tvEndDateTxt?.text = null
            binding?.tvLeaveCountTxt?.text = "0"
            binding?.tvStartDateTxt?.hint = requireContext().getString(R.string.start_date)
            binding?.tvEndDateTxt?.hint = requireContext().getString(R.string.end_date)

        }

    }

    private fun getDayCount() {


        requestViewModel.getLeaveCountOnDate(LeaveCountByDaysRequest().apply {
            this.leave_type = selectedLeaveType
            this.from_date = startDateString
            this.to_date = endDateString
            this.half_day = ishalfday
            this.half_day_date = halfDateString
        })
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

            else -> {}
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun spinnerLeavePopulations(leaves: Leaves?) {

        val finalLeavesArray = arrayListOf<LeaveAllocation>()

        finalLeavesArray.add(0, LeaveAllocation().apply {
            this.name = leaveType
            this.remaining_leaves = 0.0
        })
        if (leaves?.leave_allocation != null) {
            finalLeavesArray.addAll(
                leaves.leave_allocation.filter { !it.name.isNullOrBlank() }
            )
        }

        val adapter = LeaveWithCountSpinnerAdapter(
            requireContext(), finalLeavesArray
        )
        binding?.spLeaveType?.adapter = adapter

        if (isUpdate) {

            for (index in 0..<finalLeavesArray.size) {
                if (finalLeavesArray[index].name.equals(preLeaveType)) {
                    binding?.spLeaveType?.setSelection(index)
                    break
                }
            }

            binding?.spLeaveType?.isClickable = false
            binding?.spLeaveType?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    //Your code
                    requireContext().showErrorMsg(ErrorMessages.UNABLE_TO_EDIT_LEAVE.errorString)
                }
                true
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

                selectedLeaveType = finalLeavesArray[position].name.toString()

                binding?.tvRemainingLeaveTxt?.text =
                    finalLeavesArray[position].remaining_leaves.toString()

                if (startDateString != null && endDateString != null && !selectedLeaveType.isNullOrEmpty()) {
                    requestViewModel.getLeaveCountOnDate(LeaveCountByDaysRequest().apply {
                        this.leave_type = selectedLeaveType
                        this.from_date = startDateString
                        this.to_date = endDateString
                    })
                }

            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}

        }
    }

//    @SuppressLint("ClickableViewAccessibility")
//    private fun spinnerLeavePopulations(leaves: ArrayList<SpinnerType>?) {
//
//        val finalLeavesArray = arrayListOf<SpinnerType>()
//
//        finalLeavesArray.add(0, SpinnerType().apply {
//            this.type = leaveType
//        })
//        if (leaves != null) {
//            finalLeavesArray.addAll(leaves)
//        }
//
//        val adapter = LeaveSpinnerAdapter(
//            requireContext(), finalLeavesArray
//        )
//        binding?.spLeaveType?.adapter = adapter
//
//        if (isUpdate) {
//
//            for (index in 0..<finalLeavesArray.size) {
//                if (finalLeavesArray[index].type.equals(preLeaveType)) {
//                    binding?.spLeaveType?.setSelection(index)
//                    break
//                }
//            }
//
//            binding?.spLeaveType?.isClickable = false
//            binding?.spLeaveType?.setOnTouchListener { _, event ->
//                if (event.action == MotionEvent.ACTION_UP) {
//                    //Your code
//                    requireContext().showErrorMsg(ErrorMessages.UNABLE_TO_EDIT_LEAVE.errorString)
//                }
//                true
//            }
//        }
//
//
//        binding?.spLeaveType?.onItemSelectedListener = object :
//            AdapterView.OnItemSelectedListener {
//            override fun onItemSelected(
//                parent: AdapterView<*>?,
//                view: View?,
//                position: Int,
//                id: Long
//            ) {
//            }
//
//            override fun onNothingSelected(parent: AdapterView<*>?) {}
//
//        }
//    }

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
                    binding?.spLocType?.setSelection(index)
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

