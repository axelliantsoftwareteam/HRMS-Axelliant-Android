package com.axelliant.hris.screens

import android.Manifest
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
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
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.model.resourceManage.AddResourceType
import com.axelliant.hris.model.resourceManage.CreateResourceHour
import com.axelliant.hris.model.resourceManage.ProjectType
import com.axelliant.hris.model.resourceManage.UpdateResourceHour
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.getServerFormat
import com.axelliant.hris.viewmodel.ResourceManageViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.android.ext.android.inject


class AddResourceManageFragment: BaseFragment(), AddResourceManageAdapter.OnUpdateList {

    private val multiPartArray = ArrayList<ImageType>()
    private var currentIndex = 0

    val expenseType = "None"
    private var isUpdate = false
    private var expenseId = ""
    private var _binding: FragmentAddResourceManageBinding? = null
    private val binding get() = _binding
    private var addExpenseList: ArrayList<AddResourceType> = arrayListOf()
    private val resourceManageViewModel: ResourceManageViewModel by inject()

    var addResourceManageAdapter: AddResourceManageAdapter? = null
    private var expenseList: ArrayList<ProjectType> = arrayListOf()


    private var forUpdateList: ArrayList<UpdateResourceHour> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddResourceManageBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val activityResultLauncher: ActivityResultLauncher<Array<String>> =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                var allAreGranted = true
                for (b in result.values) {
                    allAreGranted = allAreGranted && b
                }

            }

        val appPerms = arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        activityResultLauncher.launch(appPerms)

        if (arguments != null && requireArguments().containsKey(AppConst.ExpenseRequestParam)) {
            val parsedData = arguments?.getString(AppConst.ExpenseRequestParam, "")
            val expenseID = arguments?.getString(AppConst.ExpenseRequestIDParam, "")
            val attachments = arguments?.getString(AppConst.ExpenseRequestAttachments, "")

            if (parsedData != null) {
                forUpdateList =
                    Gson().fromJson(parsedData, object : TypeToken<List<AddResourceType>>() {}.type)
                var attachments: List<Attachments> =
                    Gson().fromJson(attachments, object : TypeToken<List<Attachments>>() {}.type)


                if (attachments.isNotEmpty()) {

                    for (item in attachments) {
                        multiPartArray.add(ImageType().apply {
                            this.isUploaded = true
                            this.isMediaQuery = false
                            this.uri = null
                            this.imageUrl = item.file_url
                            this.file_id = item.name

                        })
                    }

                }

                isUpdate = true
                expenseId = expenseID.toString()

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

                    Handler().postDelayed({
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
                    if (!isUpdate)
                        expenseId = response.expense_detail?.name.toString()

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

                        expenseList.add(0, ProjectType().apply {
                            this.name = expenseType
                        })
                        expenseList.addAll(response.project_list!!)



                        if (isUpdate) {
                            for (counter in 0..<forUpdateList.size) {
                                forUpdateList[counter].expenseTypeList = expenseList
                            }

                            addExpenseList = forUpdateList

                        } else {
                            addExpenseList.add(AddResourceType().apply {
//                                this.expense_type = null
//                                this.expense_date = null
//                                this.amount = 0.0
//                                this.description = ""
//                                this.expenseTypeList = expenseList
                                this.project_id = expenseType
                                this.date = null
                                this.working_hours = 0.0
                            })
                        }

                        binding?.rvLeaveCount?.layoutManager =
                            LinearLayoutManager(requireActivity())
                        addResourceManageAdapter = AddResourceManageAdapter(
                            addExpenseList, requireContext(), object : AdapterItemClick {
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

            Log.d("addExpenseListSize",addExpenseList.size.toString())

            for (expenseItem in addExpenseList) {

                if (expenseItem.project_id == expenseType) {
                    requireContext().showErrorMsg("Please select the type")
                    return@setOnClickListener
                } else if (expenseItem.date == null) {
                    requireContext().showErrorMsg("Please choose expense date")
                    return@setOnClickListener
                } else if (expenseItem.working_hours == null || expenseItem.working_hours == 0.0) {
                    requireContext().showErrorMsg("Please enter expense amount")
                    return@setOnClickListener
                }

            }

            // assume all good
            if (isUpdate) {
                resourceManageViewModel.postResourceHour(isUpdate, CreateResourceHour().apply {
//                    this.expense_id = expenseId
                    this.employee_hours = addExpenseList
//                    this.posting_date = getServerFormat()
//                    this.total_amount = addResourceManageAdapter?.grandTotalCalculation().toString()
                })
            } else {
                resourceManageViewModel.postResourceHour(isUpdate, CreateResourceHour().apply {
                    this.employee_hours = addExpenseList
//                    this.posting_date = getServerFormat()
//                    this.total_amount = addResourceManageAdapter?.grandTotalCalculation().toString()
                })
            }


        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.tvReject?.setOnClickListener {

            resourceManageViewModel.deleteExpense(CreateExpense().apply {
                this.expense_id = expenseId
            })

        }


        // Initial item list with one item

        binding?.tvAddNew?.setOnClickListener {
            addExpenseList.add(AddResourceType().apply {
                this.project_id = expenseType
                this.date = null
                this.working_hours = 0.0
//                this.expenseTypeList = expenseList

            })
            addResourceManageAdapter?.notifyItemInserted(addExpenseList.size - 1)
            binding?.rvLeaveCount?.scrollToPosition(addExpenseList.size - 1)
        }
    }
    override fun onListUpdated(updatedList: ArrayList<AddResourceType>) {
        addExpenseList = updatedList
        binding?.tvAmount?.text = addResourceManageAdapter?.grandTotalCalculation().toString()
    }


}
