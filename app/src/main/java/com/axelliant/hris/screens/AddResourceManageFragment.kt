package com.axelliant.hris.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.adapter.AddResourceManageAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentAddResourceManageBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.resourceManage.CreateResourceHour
import com.axelliant.hris.model.resourceManage.DeleteProject
import com.axelliant.hris.model.resourceManage.ProjectHour
import com.axelliant.hris.model.resourceManage.ProjectType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.getServerFormat
import com.axelliant.hris.viewmodel.ResourceManageViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.android.ext.android.inject


class AddResourceManageFragment : BaseFragment(), AddResourceManageAdapter.OnUpdateList {

    private var currentIndex = 0

    val ResourceType = "None"
    private var isUpdate = false
    private var docId = ""
    private var _binding: FragmentAddResourceManageBinding? = null
    private val binding get() = _binding
    private var addProjectHoursList: ArrayList<ProjectHour> = arrayListOf()
    private val resourceManageViewModel: ResourceManageViewModel by inject()

    var addResourceManageAdapter: AddResourceManageAdapter? = null
    private var projectTypeList: ArrayList<ProjectType> = arrayListOf()

    private var forUpdateList: ArrayList<ProjectHour> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddResourceManageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (arguments != null && requireArguments().containsKey(AppConst.HoursRequestParam)) {
            val parsedData = arguments?.getString(AppConst.HoursRequestParam, "")
            val docID = arguments?.getString(AppConst.HoursRequestIDParam, "")

            if (parsedData != null) {
                forUpdateList =
                    Gson().fromJson(parsedData, object : TypeToken<List<ProjectHour>>() {}.type)

                isUpdate = true
                docId = docID.toString()

            }


        }

        binding?.tvReject?.isVisible = isUpdate

        if (isUpdate) {
            binding?.btnApply?.setText("Update")
        } else {
            binding?.btnApply?.setText("Create")
        }

        binding?.tvDate?.text = getServerFormat()

        resourceManageViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        resourceManageViewModel.deleteExpenseResponse.observe(viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg(response.status_message)

                    Handler(Looper.getMainLooper()).postDelayed({
                        // do stuff
                        AppNavigator.moveBackToPreviousFragment()
                    }, 200)
                } else
                    requireContext().showErrorMsg(response?.meta?.message)

            })


        resourceManageViewModel.myPostExpenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    if (!isUpdate) {
                        docId = response.resource_hour_data?.name.toString()
                        requireContext().showSuccessMsg(response.status_message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            // do stuff
                            AppNavigator.moveBackToPreviousFragment()
                        }, 200)
                    }
                    else{
                        requireContext().showSuccessMsg(response.status_message)
                        Handler(Looper.getMainLooper()).postDelayed({
                            // do stuff
                            AppNavigator.moveBackToPreviousFragment()
                        }, 200)
                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }
            })
        resourceManageViewModel.getProjectTypeList()
        resourceManageViewModel.projectTypeResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {


                    if (response.project_list != null) {

                        projectTypeList.add(0, ProjectType().apply {
                            this.project = null
                            this.name = ResourceType
                        })
                        projectTypeList.addAll(response.project_list!!)

                        if (isUpdate) {
                            for (counter in 0..<forUpdateList.size) {
                                forUpdateList[counter].expenseTypeList = projectTypeList
                            }

                            addProjectHoursList = forUpdateList

                        } else {
                            addProjectHoursList.add(ProjectHour().apply {
                                this.project = null
                                this.name = ResourceType
                                this.date = null
                                this.working_hours = 0.0
                                this.expenseTypeList = projectTypeList
                            })
                        }

                        binding?.rvLeaveCount?.layoutManager =
                            LinearLayoutManager(requireActivity())
                        addResourceManageAdapter = AddResourceManageAdapter(
                            addProjectHoursList, requireContext(), object : AdapterItemClick {
                                override fun onItemClick(customObject: Any, position: Int) {
                                    // Handle item click if needed
                                }
                            },
                            this // Pass the fragment as the OnUpdateList implementation
                        )
                        binding?.rvLeaveCount?.adapter = addResourceManageAdapter

                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.btnApply?.setOnClickListener {

            Log.d("addExpenseListSize", addProjectHoursList.size.toString())

            for (expenseItem in addProjectHoursList) {

                if (expenseItem.name == ResourceType) {
                    requireContext().showErrorMsg("Please select the type")
                    return@setOnClickListener
                } else if (expenseItem.date == null) {
                    requireContext().showErrorMsg("Please choose date")
                    return@setOnClickListener
                } else if (expenseItem.working_hours == null || expenseItem.working_hours == 0.0) {
                    requireContext().showErrorMsg("Please enter working hours")
                    return@setOnClickListener
                }

            }

            // assume all good
            if (isUpdate) {
                resourceManageViewModel.postResourceHour(isUpdate, CreateResourceHour().apply {
                    this.name = docId
                    this.project_hours = addProjectHoursList
                })
            } else {
                resourceManageViewModel.postResourceHour(isUpdate, CreateResourceHour().apply {
                    this.project_hours = addProjectHoursList
                })
            }


        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.tvReject?.setOnClickListener {
            resourceManageViewModel.deleteExpense(DeleteProject().apply {
                this.name = docId
            })
        }


        // Initial item list with one item

        binding?.tvAddNew?.setOnClickListener {
            addProjectHoursList.add(ProjectHour().apply {
                this.project = null
                this.name = ResourceType
                this.date = null
                this.working_hours = 0.0
                this.expenseTypeList = projectTypeList
            })
            addResourceManageAdapter?.notifyItemInserted(addProjectHoursList.size - 1)
            binding?.rvLeaveCount?.scrollToPosition(addProjectHoursList.size - 1)
        }
    }


    override fun onListUpdated(updatedList: ArrayList<ProjectHour>) {
        addProjectHoursList = updatedList
        binding?.tvAmount?.text = addResourceManageAdapter?.grandTotalCalculation().toString()
    }


}
