package com.axelliant.hris.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.adapter.AddExpenseAdapter
import com.axelliant.hris.adapter.AttachmentsAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentAddExpenseBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.ImagePath
import com.axelliant.hris.model.expense.AddExpense
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.DeleteAttachment
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.model.leave.SpinnerType
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.Utils.getServerFormat
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.koin.android.ext.android.inject
import java.io.File


class AddExpenseFragment : BaseFragment(), AddExpenseAdapter.OnUpdateList {

    private var pickMultipleImages = 103
    private val cameraPermissionRequest = 101
    private var galleryPermissionRequest = 102
    private val multiPartArray = ArrayList<ImageType>()
    private var photosJsonArray = arrayListOf<String>()
    private var currentIndex = 0

    private var attachmentsAdapter: AttachmentsAdapter? = null


    val expenseType = "None"
    private var isUpdate = false
    private var expenseId = ""
    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding
    private var addExpenseList: ArrayList<AddExpense> = arrayListOf()
    private val expenseViewModel: ExpenseViewModel by inject()

    var addExpenseAdapter: AddExpenseAdapter? = null
    private var expenseList: ArrayList<SpinnerType> = arrayListOf()


    private var forUpdateList: ArrayList<AddExpense> = arrayListOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
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
                    Gson().fromJson(parsedData, object : TypeToken<List<AddExpense>>() {}.type)
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
                attachmentVisibility()


                isUpdate = true
                expenseId = expenseID.toString()

            }


        }


        binding?.rvAttachments?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

        attachmentsAdapter =
            AttachmentsAdapter(true, requireContext(), multiPartArray, object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {

                    val expenseObject = customObject as ImageType
                    // here call the delete expense photo API

                    if (customObject.file_id != null) {
                        expenseViewModel.deleteAttachment(DeleteAttachment().apply {
                            this.file_id = expenseObject.file_id
                        })
                    }

                    multiPartArray.removeAt(position)
                    attachmentVisibility()
                    attachmentsAdapter?.notifyDataSetChanged()

                }

            })

        binding?.rvAttachments?.adapter = attachmentsAdapter

        binding?.tvReject?.isVisible = isUpdate

        if (isUpdate) {
            binding?.btnApply?.setText("Update")
        } else {
            binding?.btnApply?.setText("Create")
        }

        binding?.tvDate?.text = getServerFormat()

        expenseViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })

        expenseViewModel.postExpenseResponse.observe(viewLifecycleOwner,
            EventObserver { response ->
                for (multiPart in multiPartArray) {
                    if (multiPart.isUploaded && multiPart.imageUrl == null && !multiPart.isMediaQuery) {
                        multiPart.imageUrl = response?.url
                    }
                }
                uploadImages(response?.meta?.message.toString())

            })

        expenseViewModel.deleteExpenseResponse.observe(viewLifecycleOwner,
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


        expenseViewModel.myPostExpenseResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    if (!isUpdate)
                        expenseId = response.expense_detail?.name.toString()

                    uploadImages(response.status_message.toString())

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })


        expenseViewModel.deleteAttachmentResponse.observe(viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg(response.meta.message)

                } else
                    requireContext().showErrorMsg(response?.meta?.message)
            })


        expenseViewModel.getExpenseTypeList()
        expenseViewModel.expenseTypeResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {


                    if (response.expenses != null) {

                        expenseList.add(0, SpinnerType().apply {
                            this.type = expenseType
                        })
                        expenseList.addAll(response.expenses!!)



                        if (isUpdate) {
                            for (counter in 0..<forUpdateList.size) {
                                forUpdateList[counter].expenseTypeList = expenseList
                            }

                            addExpenseList = forUpdateList

                        } else {
                            addExpenseList.add(AddExpense().apply {
                                this.expense_type = null
                                this.expense_date = null
                                this.amount = 0.0
                                this.description = ""
                                this.expenseTypeList = expenseList
                            })
                        }

                        binding?.rvLeaveCount?.layoutManager =
                            LinearLayoutManager(requireActivity())
                        addExpenseAdapter = AddExpenseAdapter(
                            addExpenseList, requireContext(), object : AdapterItemClick {
                                override fun onItemClick(customObject: Any, position: Int) {
                                    // Handle item click if needed
                                }
                            },
                            this // Pass the fragment as the OnUpdateList implementation
                        )
                        binding?.rvLeaveCount?.adapter = addExpenseAdapter

                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })




        binding?.btnApply?.setOnClickListener {

            Log.d("addExpenseListSize",addExpenseList.size.toString())

            for (expenseItem in addExpenseList) {



                if (expenseItem.expense_type == expenseType) {
                    requireContext().showErrorMsg("Please select the type")
                    return@setOnClickListener
                } else if (expenseItem.expense_date == null) {
                    requireContext().showErrorMsg("Please choose expense date")
                    return@setOnClickListener
                } else if (expenseItem.amount == null || expenseItem.amount == 0.0) {
                    requireContext().showErrorMsg("Please enter expense amount")
                    return@setOnClickListener
                } else if (expenseItem.description == null || expenseItem.description.equals("")) {
                    requireContext().showErrorMsg("Please add expense reason")
                    return@setOnClickListener
                }

            }

            // assume all good
            if (isUpdate) {
                expenseViewModel.postExpense(isUpdate, CreateExpense().apply {
                    this.expense_id = expenseId
                    this.expense_details = addExpenseList
                    this.posting_date = getServerFormat()
                    this.total_amount = addExpenseAdapter?.grandTotalCalculation().toString()
                })
            } else {
                expenseViewModel.postExpense(isUpdate, CreateExpense().apply {
                    this.expense_details = addExpenseList
                    this.posting_date = getServerFormat()
                    this.total_amount = addExpenseAdapter?.grandTotalCalculation().toString()
                })
            }


        }

        binding?.ivBack?.setOnClickListener {
            AppNavigator.moveBackToPreviousFragment()
        }

        binding?.tvReject?.setOnClickListener {

            expenseViewModel.deleteExpense(CreateExpense().apply {
                this.expense_id = expenseId
            })

        }


        // Initial item list with one item

        binding?.tvAddNew?.setOnClickListener {
            addExpenseList.add(AddExpense().apply {
                this.expense_type = expenseType
                this.expense_date = null
                this.description = ""
                this.amount = 0.0
                this.expenseTypeList = expenseList

            })
            addExpenseAdapter?.notifyItemInserted(addExpenseList.size - 1)
            binding?.rvLeaveCount?.scrollToPosition(addExpenseList.size - 1)
        }

        binding?.ivAttachments?.setOnClickListener {
            selectImage()
        }


    }

    private fun attachmentVisibility() {
        binding?.tvAttachments?.isVisible = multiPartArray.size > 0
    }

    private fun uploadImages(statusMessage: String) {

        if (multiPartArray.size > 0) {
            var isAnyFound = false
            for (multiPart in multiPartArray) {
                if (!multiPart.isUploaded && multiPart.isMediaQuery && multiPart.uri != null) {
                    multiPart.isUploaded = true
                    multiPart.isMediaQuery = false
                    expenseViewModel.getMyExpenseFile(ImagePath().apply {
                        this.file = uriToMultiPart(multiPart.uri)
                        this.docname = expenseId
                        this.is_private = 0
                        this.folder = "Home/Attachments"
                        this.doctype = "Expense Claim"
                    })
                    isAnyFound = true
                    break
                }

            }
            if (!isAnyFound) {
                requireActivity().showSuccessMsg(statusMessage)
                Handler(Looper.getMainLooper()).postDelayed({
                    // do stuff
                    AppNavigator.moveBackToPreviousFragment()
                }, 200)
            }

        } else {
            requireActivity().showSuccessMsg(statusMessage)
            Handler(Looper.getMainLooper()).postDelayed({
                // do stuff
                AppNavigator.moveBackToPreviousFragment()
            }, 200)
        }
    }

    private fun selectImage() {

        val options = arrayOf("Camera", "Gallery")
        val builder = androidx.appcompat.app.AlertDialog.Builder(requireActivity())
        builder.setTitle("Select File Source")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> pickCameraImage()
                1 -> pickGalleryImage()

            }
        }
        builder.show()
    }

    override fun onListUpdated(updatedList: ArrayList<AddExpense>) {
        // Handle the updated list here
        addExpenseList = updatedList
        binding?.tvAmount?.text = addExpenseAdapter?.grandTotalCalculation().toString()
    }

    ////////////////////// image working //////////////////////


    private fun pickCameraImage() {
        ImagePicker.with(this)
            // User can only capture image from Camera
            .cameraOnly()
//            .crop(8f, 5f)
            .cropSquare()
            // Image size will be less than 1024 KB
            .compress(1024)
            //  Path: /storage/sdcard0/Android/data/package/files
            .saveDir(requireActivity().getExternalFilesDir(null)!!)
            //  Path: /storage/sdcard0/Android/data/package/files/ImagePicker
            .saveDir(requireActivity().getExternalFilesDir("ImagesDirectory")!!)
            .start(cameraPermissionRequest)
    }

    private fun pickGalleryImage() {
        ImagePicker.with(this)
            // Crop Image(User can choose Aspect Ratio)
            .cropSquare()
            // User can only select image from Gallery
            .galleryOnly()
            .galleryMimeTypes( // no gif images at all
                mimeTypes = arrayOf(
                    "image/png",
                    "image/jpg",
                    "image/jpeg"
                )
            )
            .compress(1024)
            // Image resolution will be less than 1080 x 1920
            .maxResultSize(1080, 1920)
            // .saveDir(getExternalFilesDir(null)!!)
            .start(galleryPermissionRequest)
    }

    private fun uriToMultiPart(uri: Uri?): MultipartBody.Part? {

        if (uri == null)
            return null

        val path = getPathFromUR(requireActivity(), uri) ?: return null

        return getMultiPart(path)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        photosJsonArray.clear()

        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> {
                multiPartArray.add(ImageType().apply {
                    this.imageUrl = null
                    this.uri = data?.data
                    this.isUploaded = false
                    this.isMediaQuery = true

                })
                attachmentVisibility()
                attachmentsAdapter?.notifyDataSetChanged()

            }

            ImagePicker.RESULT_ERROR -> {
                requireContext().showErrorMsg(ImagePicker.getError(data))
            }

            else -> {
                requireContext().showErrorMsg("You haven't picked Image")
            }
        }

    }


    private fun getMultiPart(filePath: String): MultipartBody.Part? {
        val pFile = File(filePath)
        if (pFile.exists()) {
            val requestBody: RequestBody =
                pFile.asRequestBody("*/*".toMediaTypeOrNull())
            return MultipartBody.Part.createFormData(
                "file",
                pFile.name,
                requestBody
            )
        }
        return null
    }


    @SuppressLint("NewApi")
    fun getPathFromUR(context: Context, uri: Uri): String? {
        var filePath: String? = null

        if (DocumentsContract.isDocumentUri(
                context,
                uri
            )
        ) {
            when {
                isExternalStorageDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()
                    val type = split[0]

                    if ("primary".equals(type, ignoreCase = true)) {
                        filePath = "${context.getExternalFilesDir(null)}/${split[1]}"
                    }
                }

                isDownloadsDocument(uri) -> {
                    val fileName = getFilePath(context, uri)
                    if (fileName != null) {
                        filePath = "${context.getExternalFilesDir(null)}/$fileName"
                    }
                }

                isMediaDocument(uri) -> {
                    val docId = DocumentsContract.getDocumentId(uri)
                    val split = docId.split(":").toTypedArray()

                    val contentUri: Uri = when (split[0]) {
                        "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                        "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                        else -> return null
                    }

                    val selection = "_id=?"
                    val selectionArgs = arrayOf(split[1])

                    filePath = getDataColumn(context, contentUri, selection, selectionArgs)
                }
            }
        } else if ("content".equals(uri.scheme, ignoreCase = true)) {
            filePath = getDataColumn(context, uri, null, null)
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            filePath = uri.path
        }

        return filePath
    }

    private fun getDataColumn(
        context: Context, uri: Uri, selection: String?,
        selectionArgs: Array<String>?
    ): String? {
        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, selection, selectionArgs, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun getFilePath(context: Context, uri: Uri): String? {
        var cursor: Cursor? = null
        val column = "_display_name"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

}
