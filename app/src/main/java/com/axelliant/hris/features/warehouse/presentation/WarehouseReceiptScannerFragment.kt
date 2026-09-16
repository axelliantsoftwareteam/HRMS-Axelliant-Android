package com.axelliant.hris.features.warehouse.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentWarehouseReceiptScannerBinding
import com.axelliant.hris.extention.showSuccessMsg
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WarehouseReceiptScannerFragment : Fragment() {
    private var _binding: FragmentWarehouseReceiptScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var analysisExecutor: ExecutorService
    private var hasScannedResult = false
    private var pendingCameraImageUri: Uri? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showMessage(getString(R.string.receiving_scanner_permission_denied), showRetry = true)
        }
    }

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { showSelectedImage(it) }
    }

    private val attachmentCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCameraImageUri
        if (success && uri != null) {
            showSelectedImage(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWarehouseReceiptScannerBinding.inflate(inflater, container, false)
        barcodeScanner = BarcodeScanning.getClient()
        analysisExecutor = Executors.newSingleThreadExecutor()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.retryButton.setOnClickListener { requestCameraAndStart() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.createReceiptButton.setOnClickListener {
            requireContext().showSuccessMsg(getString(R.string.receiving_create_save_not_connected))
        }
        binding.clearSignatureButton.setOnClickListener { binding.signaturePad.clear() }
        binding.signaturePad.onSignatureChanged = { hasSignature ->
            binding.signatureHintText.isVisible = !hasSignature
        }
        binding.galleryButton.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
        binding.cameraButton.setOnClickListener {
            val uri = createAttachmentImageUri()
            pendingCameraImageUri = uri
            attachmentCameraLauncher.launch(uri)
        }
        requestCameraAndStart()
    }

    private fun requestCameraAndStart() {
        showMessage(getString(R.string.receiving_scanner_align_code), showRetry = false)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        hasScannedResult = false
        binding.scannedReceiptPanel.isVisible = false
        binding.scannerStatusContainer.isVisible = true
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { previewUseCase ->
                    previewUseCase.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { analysisUseCase ->
                        analysisUseCase.setAnalyzer(analysisExecutor, ::analyzeFrame)
                    }

                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                }.onFailure {
                    showMessage(getString(R.string.receiving_scanner_error), showRetry = true)
                }
            },
            ContextCompat.getMainExecutor(requireContext())
        )
    }

    @OptIn(ExperimentalGetImage::class)
    private fun analyzeFrame(imageProxy: ImageProxy) {
        if (hasScannedResult) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                val scannedValue = barcodes
                    .asSequence()
                    .mapNotNull { barcode -> barcode.rawValue ?: barcode.displayValue }
                    .map { value -> value.trim() }
                    .firstOrNull { value -> value.isNotBlank() }
                if (!scannedValue.isNullOrBlank() && !hasScannedResult) {
                    hasScannedResult = true
                    showScannedReceipt(scannedValue)
                }
            }
            .addOnFailureListener {
                if (!hasScannedResult) {
                    showMessage(getString(R.string.receiving_scanner_error), showRetry = false)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun showScannedReceipt(value: String) {
        val receipt = ScannedReceiptUi.dummy(value)
        binding.scannerStatusContainer.isVisible = false
        binding.scannedReceiptPanel.isVisible = true
        binding.scannedSummaryText.text = getString(
            R.string.receiving_scanned_summary_format,
            receipt.warehouse,
            receipt.receiptType,
            receipt.purchaseOrder,
            receipt.expectedDate
        )
        binding.signaturePad.clear()
        binding.signatureHintText.isVisible = true
        binding.selectedImagePreview.isVisible = false
        binding.attachmentStatusText.text = getString(R.string.receiving_scanned_no_photo)
    }

    private fun returnScanResult(value: String) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            WarehouseReceiptScannerResult.REQUEST_KEY,
            bundleOf(WarehouseReceiptScannerResult.RESULT_VALUE to value)
        )
        findNavController().popBackStack()
    }

    private fun createAttachmentImageUri(): Uri {
        val directory = File(requireContext().cacheDir, ATTACHMENT_CACHE_DIR).apply {
            mkdirs()
        }
        val imageFile = File.createTempFile(ATTACHMENT_FILE_PREFIX, ATTACHMENT_FILE_SUFFIX, directory)
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }

    private fun showSelectedImage(uri: Uri) {
        binding.selectedImagePreview.setImageURI(uri)
        binding.selectedImagePreview.isVisible = true
        binding.attachmentStatusText.text = getString(R.string.receiving_scanned_photo_selected)
    }

    private fun showMessage(message: String, showRetry: Boolean) {
        if (_binding == null) return
        binding.statusText.text = message
        binding.retryButton.isVisible = showRetry
    }

    private data class ScannedReceiptUi(
        val scannedValue: String,
        val warehouse: String,
        val receiptType: String,
        val purchaseOrder: String,
        val expectedDate: String
    ) {
        companion object {
            fun dummy(scannedValue: String): ScannedReceiptUi {
                return ScannedReceiptUi(
                    scannedValue = scannedValue.ifBlank { "WR-QR-DUMMY" },
                    warehouse = "LA",
                    receiptType = "Purchase Order",
                    purchaseOrder = "PO-00000028",
                    expectedDate = "09/10/2026"
                )
            }
        }
    }

    private companion object {
        const val ATTACHMENT_CACHE_DIR = "ask_ai_scan/warehouse_receipts"
        const val ATTACHMENT_FILE_PREFIX = "receipt_attachment_"
        const val ATTACHMENT_FILE_SUFFIX = ".jpg"
    }

    override fun onDestroyView() {
        if (::barcodeScanner.isInitialized) {
            barcodeScanner.close()
        }
        if (::analysisExecutor.isInitialized) {
            analysisExecutor.shutdown()
        }
        super.onDestroyView()
        _binding = null
    }
}
