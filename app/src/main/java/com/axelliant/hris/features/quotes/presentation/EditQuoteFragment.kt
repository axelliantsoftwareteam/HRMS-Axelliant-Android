package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentEditQuoteBinding
import com.axelliant.hris.features.quotes.domain.model.QuoteModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditQuoteFragment : Fragment() {

    private val viewModel: EditQuoteViewModel by viewModels()
    private var _binding: FragmentEditQuoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditQuoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { saveQuote() }
        observeQuote()
        observeSaveState()
    }

    private fun observeQuote() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.quoteState.collect { state ->
                    if (state is UiState.Success) {
                        bindQuote(state.data)
                    }
                }
            }
        }
    }

    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.quote_saved,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().navigateUp()
                        }
                        is UiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun bindQuote(quote: QuoteModel) {
        val statusUi = quote.status?.let { QuoteStatusUiMapper.map(it) }
            ?: QuoteStatusUiMapper.getStatusUi(quote.approvalStatus)
        binding.quoteIdInput.setText(quote.quoteId)
        binding.customerInput.setText(quote.customerName)
        binding.statusInput.setText(getString(statusUi.labelRes))
        binding.createdByInput.setText(quote.createdBy)
        binding.dateInput.setText(quote.date)
        binding.totalAmountInput.setText(quote.totalAmount)
        binding.notesInput.setText(quote.notes)
    }

    private fun saveQuote() {
        viewModel.saveQuote(
            customerName = binding.customerInput.text?.toString().orEmpty().trim(),
            statusLabel = binding.statusInput.text?.toString().orEmpty().trim(),
            createdBy = binding.createdByInput.text?.toString().orEmpty().trim(),
            date = binding.dateInput.text?.toString().orEmpty().trim(),
            totalAmount = binding.totalAmountInput.text?.toString().orEmpty().trim(),
            notes = binding.notesInput.text?.toString().orEmpty().trim()
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
