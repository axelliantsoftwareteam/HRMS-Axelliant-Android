package com.axelliant.hris.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.adapter.AddResourceManageAdapter
import com.axelliant.hris.adapter.ResourceHoursAdapter
import com.axelliant.hris.adapter.SubFilterAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.callback.CheckBoxAdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentResourceManagmentBinding
import com.axelliant.hris.enums.AttendanceFilter
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.ResourceStatus
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.attendance.AttendanceInput
import com.axelliant.hris.model.dashboard.FilterModel
import com.axelliant.hris.model.documentRequest.SubmitDocument
import com.axelliant.hris.model.resourceManage.DocumentHours
import com.axelliant.hris.model.resourceManage.ProjectHour
import com.axelliant.hris.model.resourceManage.ProjectType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.ErrorMessages
import com.axelliant.hris.utils.Utils
import com.axelliant.hris.viewmodel.ResourceManageViewModel
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.gson.Gson
import org.koin.android.ext.android.inject
import java.util.Date

class ResourceManagementFragment : BaseFragment() {

    private var resourceHours: ArrayList<DocumentHours>? = null
    private lateinit var resourceHoursAdapter: ResourceHoursAdapter
    private var _binding: FragmentResourceManagmentBinding? = null
    private val binding get() = _binding
    private var currentFilter = AttendanceFilter.WEEK
    private val resourceManageViewModel: ResourceManageViewModel by inject()
    private var startDateString: String? = null
    private var endDateString: String? = null
    private var filterId = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentResourceManagmentBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resourceManageViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })



        binding?.ivBack?.setOnClickListener {
            previousFragmentNavigation()
        }
        eventSelection()
        resourceManageViewModel.getMyHoursDetail(getCurrentObject())
        resourceManageViewModel.hoursResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true && response.resource_hour_data != null) {
//                    subFilterPopulations(response.expense_status)

                    if (response.resource_hour_data.size > 0) {
                        binding?.rvExpense?.visibility = View.VISIBLE
                        binding?.tvNoRecord?.visibility = View.GONE
                        resourceHours = response.resource_hour_data
                        dataPopulate()
                        binding?.viewExpand?.isVisible=true
                        binding?.btnApply?.isVisible=true


                    } else {
                        binding?.rvExpense?.visibility = View.GONE
                        binding?.tvNoRecord?.visibility = View.VISIBLE
                        binding?.viewExpand?.isVisible=false
                        binding?.btnApply?.isVisible=false

                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        resourceManageViewModel.postResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg(response.status_message.toString())
                    resourceManageViewModel.getMyHoursDetail(getCurrentObject())

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })

        binding?.addExpense?.setOnClickListener {
            AppNavigator.navigateToAddResourceManageFragment()
        }
        binding?.btnApply?.setOnClickListener {
            Log.d("Approved", getMultiSelectedDocument().toString())
            resourceManageViewModel.submitResourceHour(SubmitDocument().apply {
                this.resource_hour_id_list = getMultiSelectedDocument()
            })
        }
        binding?.viewExpand?.setOnCheckedChangeListener { _, isChecked ->
            resourceHours?.forEachIndexed { index, documentHour ->
                if (documentHour.docstatus == ResourceStatus.DRAFT.value) {
                    documentHour.isSelected = isChecked
                    resourceHoursAdapter.notifyItemChanged(index)
                }
            }
        }
    }

    private fun getMultiSelectedDocument(): ArrayList<String> {

        val arrayList: ArrayList<String> = arrayListOf()

        for (item in 0..<(resourceHours?.size ?: 0)) {
            if (resourceHours?.get(item)?.isSelected == true)
                arrayList.add(resourceHours!![item].name.toString())
        }

        return arrayList
    }

    private fun dataPopulate() {
        binding?.rvExpense?.layoutManager = LinearLayoutManager(requireActivity())
        resourceHoursAdapter = ResourceHoursAdapter(
            resourceHours!!, requireContext(), object : CheckBoxAdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val documentHours = customObject as DocumentHours
                    if (documentHours.status == LeaveStatus.PENDING.value)
                    {
                        AppNavigator.navigateToAddResourceManageFragment(Bundle().apply {
                            this.putString(AppConst.HoursRequestIDParam, documentHours.name)
                            this.putString(
                                AppConst.HoursRequestParam,
                                Gson().toJson(documentHours.resource_detail)
                            )
                        })
                    } else {
                        requireContext().showErrorMsg(
                            ErrorMessages.DRAFT_EXPENSE_ONLY.errorString.plus(
                                documentHours.status
                            )
                        )
                    }
                }

                override fun onCheckBoxItemClick(customObject: Any, position: Int) {
                    val documentHour = customObject as DocumentHours
                    documentHour.isSelected = !documentHour.isSelected
                    resourceHoursAdapter.notifyItemChanged(position)
                }
            }
        )
        binding?.rvExpense?.adapter = resourceHoursAdapter
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
            resourceManageViewModel.getMyHoursDetail(getCurrentObject())
            eventSelection()
        }
        binding?.tvMonth?.setOnClickListener {
            currentFilter = AttendanceFilter.MONTH
            resourceManageViewModel.getMyHoursDetail(getCurrentObject())
            eventSelection()
        }

        binding?.tvCustom?.setOnClickListener {
            datePickerDialog()
            currentFilter = AttendanceFilter.Custom
            resourceManageViewModel.getMyHoursDetail(getCurrentObject())
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

    private fun getCurrentObject(): AttendanceInput {


        when (currentFilter) {
            AttendanceFilter.WEEK -> {


                startDateString = Utils.getServerFormat(date = Utils.getLastWeek())
                endDateString = Utils.getServerFormat()

                setDateView()
            }

            AttendanceFilter.MONTH -> {
                startDateString = Utils.getServerFormat(date = Utils.getFirstDayOfMonth())
                endDateString =
                    Utils.getServerFormat(date = Utils.getLastDayOfMonth())

                setDateView()
            }

            else -> {}

        }
        return AttendanceInput().apply {
            this.startDate = startDateString!!
            this.endDate = endDateString!!
            this.filter = currentFilter
            this.filters = filterId

        }

    }

    private fun setDateView() {
        if (startDateString != null && endDateString != null) {
            binding?.tvStartDateTxt?.text = startDateString
            binding?.tvEndDateTxt?.text = endDateString
        }

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

            setDateView()

            currentFilter = AttendanceFilter.Custom
            resourceManageViewModel.getMyHoursDetail(getCurrentObject())
            eventSelection()
        }

        // Showing the date picker dialog
        datePicker.show(activity?.supportFragmentManager!!, "DATE_PICKER")
    }

    private fun subFilterPopulations(leaveStatus: ArrayList<FilterModel>?) {

        leaveStatus?.add(0, FilterModel().apply {
            this.id = ""
            this.title = "All"
            this.count = "0"
        })

        binding?.rvSubFilter?.layoutManager =
            LinearLayoutManager(requireActivity(), RecyclerView.HORIZONTAL, false)
        val weeklyAdapter = leaveStatus?.let {
            SubFilterAdapter(
                filterId,
                it, requireContext(),
                object : AdapterItemClick {
                    override fun onItemClick(customObject: Any, position: Int) {
                        val filterObject = customObject as FilterModel


                        filterId = filterObject.id.toString()
                        resourceManageViewModel.getMyHoursDetail(getCurrentObject())
                    }

                }
            )
        }
        binding?.rvSubFilter?.adapter = weeklyAdapter

    }

}