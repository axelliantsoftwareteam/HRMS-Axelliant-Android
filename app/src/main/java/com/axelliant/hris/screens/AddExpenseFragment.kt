package com.axelliant.hris.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.AddExpenseAdapter
import com.axelliant.hris.adapter.AttachmentsAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.databinding.FragmentAddExpenseBinding
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.model.expense.AddExpense
import com.axelliant.hris.model.expense.Attachments
import com.axelliant.hris.model.expense.CreateExpense
import com.axelliant.hris.model.expense.ImageType
import com.axelliant.hris.model.requests.ExpenseLineDraft
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.network.observeResult
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.viewmodel.ExpenseViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Create or update an expense claim: one card per expense line, a live grand total and photo
 * attachments. Saving first stores the claim, then uploads each new photo one after the other.
 */
class AddExpenseFragment : BaseFragment(), AddExpenseAdapter.Listener {

    private var _binding: FragmentAddExpenseBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExpenseViewModel by viewModel()

    private lateinit var linesAdapter: AddExpenseAdapter
    private lateinit var attachmentsAdapter: AttachmentsAdapter

    private var initialised = false
    private var isUpdate = false
    private var expenseId = ""

    private val lines = ArrayList<ExpenseLineDraft>()
    private var nextUid = 1L
    private var expenseTypes: List<String> = emptyList()
    private val attachments = ArrayList<ImageType>()

    /** The uploaded attachment whose delete call is in flight. */
    private var pendingRemoval: ImageType? = null

    // Save flow: claim saved -> upload the new photos one by one -> report.
    private var claimName = ""
    private val uploadQueue = ArrayDeque<ImageType>()
    private var uploadFailures = 0

    private val pickImage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        onImagePicked(result)
    }

    private val requestCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchPicker(camera = true) else showCameraDenied()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddExpenseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!initialised) {
            initialised = true
            readArguments()
            viewModel.loadExpenseTypes()
        }

        binding.expTvBanner.setText(if (isUpdate) R.string.exp_banner_update else R.string.exp_banner_create)
        binding.expTvPostingDate.text = WsFormat.reversedDay(WsFormat.apiDay())
        binding.expBtnSubmit.setText(if (isUpdate) R.string.exp_update else R.string.exp_create)
        binding.expBtnDelete.visibility = if (isUpdate) View.VISIBLE else View.GONE

        linesAdapter = AddExpenseAdapter(this)
        binding.expLines.layoutManager = LinearLayoutManager(requireContext())
        binding.expLines.itemAnimator = null
        binding.expLines.adapter = linesAdapter
        linesAdapter.submitList(ArrayList(lines))

        attachmentsAdapter = AttachmentsAdapter(showRemove = true, onRemove = { item, _ -> removeAttachment(item) })
        binding.expAttachList.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.expAttachList.adapter = attachmentsAdapter
        attachmentsAdapter.submitList(ArrayList(attachments))

        binding.expFabAttach.setOnClickListener { chooseImageSource() }
        binding.expFabAdd.setOnClickListener { addLine(scroll = true) }
        binding.expBtnSubmit.setOnClickListener { submit() }
        binding.expBtnDelete.setOnClickListener { confirmDeleteClaim() }

        updateTotal()
        updateAttachmentSection()
        observeViewModel()
    }

    // ---- arguments ----

    private fun readArguments() {
        val args = arguments ?: return
        if (!args.containsKey(AppConst.ExpenseRequestParam)) return
        isUpdate = true
        expenseId = args.getString(AppConst.ExpenseRequestIDParam).orEmpty()

        val gson = Gson()
        try {
            val saved: List<AddExpense>? = gson.fromJson(
                args.getString(AppConst.ExpenseRequestParam),
                object : TypeToken<List<AddExpense>>() {}.type
            )
            saved.orEmpty().forEach { line ->
                lines.add(
                    ExpenseLineDraft(
                        uid = nextUid++,
                        type = line.expense_type,
                        dateApi = line.expense_date?.take(10),
                        amountText = amountToText(line.amount ?: 0.0),
                        description = line.description
                    )
                )
            }
        } catch (e: Exception) {
            // An unreadable payload leaves the form empty; ensureFirstLine adds a blank line.
        }
        try {
            val files: List<Attachments>? = gson.fromJson(
                args.getString(AppConst.ExpenseRequestAttachments),
                object : TypeToken<List<Attachments>>() {}.type
            )
            files.orEmpty().forEach {
                attachments.add(ImageType(imageUrl = WsFormat.mediaUrl(it.file_url), file_id = it.name))
            }
        } catch (e: Exception) {
            // No attachments to show.
        }
    }

    // ---- observers ----

    private fun observeViewModel() {
        viewModel.getIsLoading().observe(viewLifecycleOwner, EventObserver { loading ->
            if (loading) showDialog() else hideDialog()
        })

        viewModel.expenseTypes.observeResult(
            viewLifecycleOwner,
            onError = { message ->
                requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_types_failed) })
                ensureFirstLine()
            }
        ) { payload, _ ->
            expenseTypes = payload.expenses.orEmpty().mapNotNull { it.type?.takeIf { type -> type.isNotBlank() } }
            ensureFirstLine()
        }

        viewModel.saveResult.observeResult(
            viewLifecycleOwner,
            onError = { message -> requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_save_failed) }) }
        ) { payload, _ ->
            val name = if (isUpdate) expenseId else payload.expense_detail?.name.orEmpty()
            if (name.isBlank()) {
                requireContext().showErrorMsg(getString(R.string.exp_save_failed))
            } else {
                claimName = name
                uploadQueue.clear()
                uploadQueue.addAll(attachments.filter { it.isLocal })
                uploadFailures = 0
                uploadNext()
            }
        }

        viewModel.uploadResult.observeResult(
            viewLifecycleOwner,
            onError = {
                uploadFailures++
                uploadNext()
            }
        ) { _, _ -> uploadNext() }

        viewModel.deleteAttachmentResult.observeResult(
            viewLifecycleOwner,
            onError = { message ->
                pendingRemoval = null
                requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_attach_remove_failed) })
            }
        ) { _, _ ->
            pendingRemoval?.let { attachments.remove(it) }
            pendingRemoval = null
            refreshAttachments()
        }

        viewModel.deleteClaimResult.observeResult(
            viewLifecycleOwner,
            onError = { message -> requireContext().showErrorMsg(message.ifBlank { getString(R.string.exp_delete_failed) }) }
        ) { _, _ ->
            requireContext().showSuccessMsg(getString(R.string.exp_deleted_simple))
            AppNavigator.moveBackToPreviousFragment()
        }
    }

    // ---- lines ----

    /** Create mode starts with one blank line once the expense types are in (or failed to load). */
    private fun ensureFirstLine() {
        if (lines.isEmpty()) addLine(scroll = false)
    }

    private fun addLine(scroll: Boolean) {
        lines.add(ExpenseLineDraft(uid = nextUid++))
        linesAdapter.submitList(ArrayList(lines))
        updateTotal()
        if (scroll) binding.expScroll.post { _binding?.let { it.expScroll.smoothScrollTo(0, it.expLines.bottom) } }
    }

    private fun updateTotal() {
        binding.expTvTotal.text = String.format(Locale.US, "%.2f", lines.sumOf { it.amount })
    }

    override fun onAmountChanged() = updateTotal()

    override fun onDelete(line: ExpenseLineDraft) {
        lines.remove(line)
        linesAdapter.submitList(ArrayList(lines))
        updateTotal()
    }

    override fun onPickType(line: ExpenseLineDraft) {
        if (expenseTypes.isEmpty()) {
            requireContext().showErrorMsg(getString(R.string.exp_types_failed))
            viewModel.loadExpenseTypes()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exp_select_type)
            .setSingleChoiceItems(expenseTypes.toTypedArray(), expenseTypes.indexOf(line.type)) { dialog, which ->
                line.type = expenseTypes[which]
                notifyLineChanged(line)
                dialog.dismiss()
            }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
    }

    override fun onPickDate(line: ExpenseLineDraft) {
        val utc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val selection = line.dateApi?.let { runCatching { utc.parse(it)?.time }.getOrNull() }
            ?.coerceAtMost(today) ?: today
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.exp_date_picker_title)
            .setCalendarConstraints(
                CalendarConstraints.Builder().setValidator(DateValidatorPointBackward.now()).build()
            )
            .setSelection(selection)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            line.dateApi = utc.format(Date(millis))
            notifyLineChanged(line)
        }
        picker.show(childFragmentManager, "exp_date_picker")
    }

    private fun notifyLineChanged(line: ExpenseLineDraft) {
        val index = lines.indexOf(line)
        if (index >= 0) linesAdapter.notifyItemChanged(index)
    }

    // ---- attachments ----

    private fun refreshAttachments() {
        attachmentsAdapter.submitList(ArrayList(attachments))
        updateAttachmentSection()
    }

    private fun updateAttachmentSection() {
        binding.expAttachSection.visibility = if (attachments.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun removeAttachment(item: ImageType) {
        val fileId = item.file_id
        if (item.isLocal || fileId.isNullOrBlank()) {
            // Not uploaded yet: just forget it.
            attachments.remove(item)
            refreshAttachments()
        } else {
            // Already on the claim: HRIS deletes it right away and the thumbnail goes once that succeeds.
            pendingRemoval = item
            viewModel.deleteAttachment(fileId)
        }
    }

    private fun chooseImageSource() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exp_choose_source)
            .setItems(arrayOf(getString(R.string.exp_camera), getString(R.string.exp_gallery))) { _, which ->
                if (which == 0) openCamera() else launchPicker(camera = false)
            }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
    }

    private fun openCamera() {
        val granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchPicker(camera = true) else requestCamera.launch(Manifest.permission.CAMERA)
    }

    private fun showCameraDenied() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exp_camera_perm_title)
            .setMessage(R.string.exp_camera_perm_message)
            .setPositiveButton(R.string.exp_open_settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", requireContext().packageName, null))
                )
            }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
    }

    private fun launchPicker(camera: Boolean) {
        val builder = ImagePicker.with(this).compress(1024).maxResultSize(1080, 1920)
        if (camera) {
            builder.cameraOnly()
        } else {
            builder.galleryOnly().galleryMimeTypes(arrayOf("image/png", "image/jpg", "image/jpeg"))
        }
        builder.createIntent { intent -> pickImage.launch(intent) }
    }

    private fun onImagePicked(result: ActivityResult) {
        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val uri = result.data?.data
                if (uri == null) {
                    requireContext().showErrorMsg(getString(R.string.exp_pick_unreadable))
                } else {
                    attachments.add(ImageType(uri = uri))
                    refreshAttachments()
                }
            }
            ImagePicker.RESULT_ERROR -> requireContext().showErrorMsg(ImagePicker.getError(result.data))
            else -> Unit // cancelled
        }
    }

    // ---- save ----

    private fun submit() {
        if (lines.isEmpty() || lines.any { !it.isValid }) {
            lines.forEach { it.showErrors = true }
            linesAdapter.notifyItemRangeChanged(0, lines.size)
            requireContext().showErrorMsg(getString(R.string.exp_validation))
            return
        }
        if (isUpdate && expenseId.isBlank()) {
            requireContext().showErrorMsg(getString(R.string.exp_missing_id))
            return
        }
        val body = CreateExpense(
            expense_id = if (isUpdate) expenseId else null,
            posting_date = WsFormat.apiDay(),
            expense_details = ArrayList(lines.map {
                AddExpense(
                    amount = it.amount,
                    description = it.description,
                    expense_date = it.dateApi,
                    expense_type = it.type
                )
            }),
            total_amount = String.format(Locale.US, "%.2f", lines.sumOf { it.amount })
        )
        viewModel.saveClaim(isUpdate, body)
    }

    /** Uploads the next new photo, or finishes when the queue is empty. */
    private fun uploadNext() {
        while (true) {
            val next = uploadQueue.removeFirstOrNull() ?: break
            val file = next.uri?.let { uriToFile(it) }
            if (file == null) {
                uploadFailures++
                continue
            }
            viewModel.uploadAttachment(file, claimName)
            return
        }
        finishSave()
    }

    private fun finishSave() {
        if (uploadFailures == 0) {
            requireContext().showSuccessMsg(getString(if (isUpdate) R.string.exp_updated else R.string.exp_created))
            AppNavigator.moveBackToPreviousFragment()
        } else {
            // The claim itself is saved; tell the user which part was lost instead of failing silently.
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.exp_uploads_partial_title)
                .setMessage(getString(R.string.exp_uploads_partial, uploadFailures))
                .setCancelable(false)
                .setPositiveButton(R.string.exp_ok) { _, _ -> AppNavigator.moveBackToPreviousFragment() }
                .show()
        }
    }

    /** The picker returns a file Uri; anything else is copied to the cache so it can be uploaded. */
    private fun uriToFile(uri: Uri): File? {
        val ctx = context ?: return null
        return try {
            if (uri.scheme == "file") {
                uri.path?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }
            } else {
                val out = File(ctx.cacheDir, "exp_upload_${System.nanoTime()}.jpg")
                val input = ctx.contentResolver.openInputStream(uri) ?: return null
                input.use { source -> out.outputStream().use { sink -> source.copyTo(sink) } }
                out
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun confirmDeleteClaim() {
        if (expenseId.isBlank()) {
            requireContext().showErrorMsg(getString(R.string.exp_missing_id))
            return
        }
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.exp_delete_title)
            .setMessage(getString(R.string.exp_delete_message, expenseId))
            .setPositiveButton(R.string.exp_delete) { _, _ -> viewModel.deleteClaim(expenseId) }
            .setNegativeButton(R.string.exp_cancel, null)
            .show()
        dialog.getButton(android.content.DialogInterface.BUTTON_POSITIVE)
            ?.setTextColor(requireContext().getColor(R.color.ws_red))
    }

    private fun amountToText(amount: Double): String =
        if (amount % 1.0 == 0.0) amount.toLong().toString() else String.format(Locale.US, "%.2f", amount)

    override fun onDestroyView() {
        hideDialog()
        _binding = null
        super.onDestroyView()
    }
}
