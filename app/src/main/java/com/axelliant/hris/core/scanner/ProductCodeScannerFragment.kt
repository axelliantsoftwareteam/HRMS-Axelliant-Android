package com.axelliant.hris.core.scanner

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentProductCodeScannerBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ProductCodeScannerFragment : Fragment() {
    private var _binding: FragmentProductCodeScannerBinding? = null
    private val binding get() = _binding!!
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var analysisExecutor: ExecutorService
    private var hasScannedResult = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startCamera()
        } else {
            showMessage(getString(R.string.ask_ai_scanner_permission_denied), showRetry = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductCodeScannerBinding.inflate(inflater, container, false)
        barcodeScanner = BarcodeScanning.getClient()
        analysisExecutor = Executors.newSingleThreadExecutor()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.retryButton.setOnClickListener { requestCameraAndStart() }
        requestCameraAndStart()
    }

    private fun requestCameraAndStart() {
        showMessage(getString(R.string.ask_ai_scanner_align_code), showRetry = false)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
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
                    showMessage(getString(R.string.ask_ai_scanner_error), showRetry = true)
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
                    returnScanResult(scannedValue)
                }
            }
            .addOnFailureListener {
                if (!hasScannedResult) {
                    showMessage(getString(R.string.ask_ai_scanner_error), showRetry = false)
                }
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    private fun returnScanResult(value: String) {
        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            ProductCodeScannerResult.REQUEST_KEY,
            value
        )
        findNavController().popBackStack()
    }

    private fun showMessage(message: String, showRetry: Boolean) {
        if (_binding == null) return
        binding.statusText.text = message
        binding.retryButton.isVisible = showRetry
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
