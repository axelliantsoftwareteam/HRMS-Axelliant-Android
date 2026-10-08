package com.axelliant.hris.screens

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.hris.R
import com.axelliant.hris.adapter.PdfPagesAdapter
import com.axelliant.hris.databinding.PayDialogPreviewBinding
import com.axelliant.hris.extention.showErrorMsg
import java.io.File

/** Full-screen pay slip PDF: coloured header with share and close, pages below (PdfRenderer). */
class PayslipPreviewDialog : DialogFragment() {

    private var _binding: PayDialogPreviewBinding? = null
    private val binding get() = _binding!!
    private var pages: PdfPagesAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PayDialogPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.ws_payslip)
            WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val path = args.getString(ARG_PATH).orEmpty()
        binding.payPreviewTitle.text = args.getString(ARG_TITLE)

        val headerTop = binding.payPreviewHeader.paddingTop
        val listBottom = binding.payPages.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.payPreviewHeader.updatePadding(top = headerTop + bars.top)
            binding.payPages.updatePadding(bottom = listBottom + bars.bottom)
            insets
        }

        binding.payPreviewClose.setOnClickListener { dismiss() }
        binding.payPreviewShare.setOnClickListener { share(path) }

        binding.payPages.layoutManager = LinearLayoutManager(requireContext())
        binding.payPages.doOnLayout { list ->
            if (_binding == null) return@doOnLayout
            val width = (list.width - list.paddingLeft - list.paddingRight).coerceIn(1, MAX_PAGE_WIDTH)
            val adapter = PdfPagesAdapter(File(path), width) { page -> getString(R.string.pay_page_cd, page) }
            adapter.onReady = { _binding?.payPreviewProgress?.visibility = View.GONE }
            adapter.onError = {
                requireContext().showErrorMsg(getString(R.string.pay_error_preview))
                dismissAllowingStateLoss()
            }
            pages = adapter
            binding.payPages.adapter = adapter
            adapter.open()
        }
    }

    private fun share(path: String) {
        val context = requireContext()
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, getString(R.string.pay_share)))
        } catch (e: Exception) {
            context.showErrorMsg(getString(R.string.pay_error_share))
        }
    }

    override fun onDestroyView() {
        binding.payPages.adapter = null
        pages?.close()
        pages = null
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "PayslipPreviewDialog"
        private const val ARG_TITLE = "title"
        private const val ARG_PATH = "path"
        private const val MAX_PAGE_WIDTH = 2048

        fun show(fm: FragmentManager, title: String, path: String) {
            if (fm.findFragmentByTag(TAG) != null || fm.isStateSaved) return
            PayslipPreviewDialog().apply {
                arguments = bundleOf(ARG_TITLE to title, ARG_PATH to path)
            }.show(fm, TAG)
        }
    }
}
