package com.axelliant.hris.features.warehouse.presentation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddWarehouseBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddWarehouseFragment : Fragment() {

    private val viewModel: AddWarehouseViewModel by viewModels()
    private var _binding: FragmentAddWarehouseBinding? = null
    private val binding get() = _binding!!
    private lateinit var screenMode: WarehouseFormMode

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddWarehouseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screenMode = WarehouseFormMode.from(arguments?.getString(AddWarehouseViewModel.ARG_MODE))
        binding.activeCheckbox.isChecked = true
        populateWarehouseIfNeeded()
        applyScreenMode()
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveButton.setOnClickListener { saveWarehouse() }
        if (screenMode != WarehouseFormMode.View) {
            clearValidationOnInput()
        }
        observeSaveState()
    }

    private fun saveWarehouse() {
        if (screenMode == WarehouseFormMode.View) return
        clearValidation()
        viewModel.saveWarehouse(
            AddWarehouseInput(
                id = arguments?.getString(AddWarehouseViewModel.ARG_WAREHOUSE_ID),
                code = binding.codeInput.text?.toString().orEmpty(),
                name = binding.nameInput.text?.toString().orEmpty(),
                addressLine1 = binding.addressLine1Input.text?.toString().orEmpty(),
                addressLine2 = binding.addressLine2Input.text?.toString().orEmpty(),
                city = binding.cityInput.text?.toString().orEmpty(),
                state = binding.stateInput.text?.toString().orEmpty(),
                postalCode = binding.postalCodeInput.text?.toString().orEmpty(),
                country = binding.countryInput.text?.toString().orEmpty(),
                isActive = binding.activeCheckbox.isChecked
            )
        )
    }

    private fun populateWarehouseIfNeeded() {
        if (screenMode == WarehouseFormMode.Add) return
        val args = arguments ?: return
        binding.codeInput.setText(args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_CODE).formValue())
        binding.nameInput.setText(args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_NAME).formValue())
        binding.addressLine1Input.setText(
            args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_ADDRESS_LINE_1).formValue()
        )
        binding.addressLine2Input.setText(
            args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_ADDRESS_LINE_2).formValue()
        )
        binding.cityInput.setText(args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_CITY).formValue())
        binding.stateInput.setText(args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_STATE).formValue())
        binding.postalCodeInput.setText(
            args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_POSTAL_CODE).formValue()
        )
        binding.countryInput.setText(
            args.getString(AddWarehouseViewModel.ARG_WAREHOUSE_COUNTRY).formValue()
        )
        binding.activeCheckbox.isChecked =
            args.getBoolean(AddWarehouseViewModel.ARG_WAREHOUSE_IS_ACTIVE, true)
    }

    private fun applyScreenMode() {
        binding.appTopBar.setTitle(
            when (screenMode) {
                WarehouseFormMode.Add -> R.string.add_warehouse_title
                WarehouseFormMode.Edit -> R.string.edit_warehouse_title
                WarehouseFormMode.View -> R.string.view_warehouse_title
            }
        )
        binding.saveButton.setText(
            if (screenMode == WarehouseFormMode.Edit) R.string.update else R.string.save
        )
        binding.actionRow.isVisible = screenMode != WarehouseFormMode.View
        setFormEditable(screenMode != WarehouseFormMode.View)
    }

    private fun setFormEditable(editable: Boolean) {
        listOf(
            binding.codeInputLayout,
            binding.nameInputLayout,
            binding.addressLine1InputLayout,
            binding.addressLine2InputLayout,
            binding.cityInputLayout,
            binding.stateInputLayout,
            binding.postalCodeInputLayout,
            binding.countryInputLayout,
            binding.codeInput,
            binding.nameInput,
            binding.addressLine1Input,
            binding.addressLine2Input,
            binding.cityInput,
            binding.stateInput,
            binding.postalCodeInput,
            binding.countryInput,
            binding.activeCheckbox
        ).forEach { it.isEnabled = editable }
    }

    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    binding.progress.isVisible = state is UiState.Loading
                    binding.saveButton.isEnabled = state !is UiState.Loading
                    binding.cancelButton.isEnabled = state !is UiState.Loading
                    binding.saveButton.alpha = if (state is UiState.Loading) DISABLED_ALPHA else 1f
                    binding.cancelButton.alpha = if (state is UiState.Loading) DISABLED_ALPHA else 1f
                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.warehouse_save_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().previousBackStackEntry?.savedStateHandle
                                ?.set(AddWarehouseViewModel.RESULT_WAREHOUSE_CREATED, true)
                            findNavController().navigateUp()
                            viewModel.resetSaveState()
                        }
                        is UiState.Error -> {
                            showError(state.message)
                            viewModel.resetSaveState()
                        }
                        UiState.Unauthorized -> {
                            showError(getString(R.string.warehouses_error))
                            viewModel.resetSaveState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun clearValidationOnInput() {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = clearValidation()
            override fun afterTextChanged(s: Editable?) = Unit
        }
        binding.codeInput.addTextChangedListener(watcher)
        binding.nameInput.addTextChangedListener(watcher)
    }

    private fun showError(message: String) {
        when (message) {
            AddWarehouseViewModel.VALIDATION_CODE -> {
                binding.codeInputLayout.error = getString(R.string.warehouse_validation_code)
            }
            AddWarehouseViewModel.VALIDATION_NAME -> {
                binding.nameInputLayout.error = getString(R.string.warehouse_validation_name)
            }
            else -> Toast.makeText(
                requireContext(),
                message.ifBlank { getString(R.string.warehouse_save_error) },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun clearValidation() {
        binding.codeInputLayout.error = null
        binding.nameInputLayout.error = null
    }

    private fun String?.formValue(): String {
        val cleanValue = orEmpty().takeIf { it.isNotBlank() && it != "-" }
        return cleanValue ?: if (screenMode == WarehouseFormMode.View) {
            getString(R.string.warehouse_not_available)
        } else {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val DISABLED_ALPHA = 0.56f
    }
}

private enum class WarehouseFormMode {
    Add,
    Edit,
    View;

    companion object {
        fun from(value: String?): WarehouseFormMode {
            return when (value) {
                AddWarehouseViewModel.MODE_EDIT -> Edit
                AddWarehouseViewModel.MODE_VIEW -> View
                else -> Add
            }
        }
    }
}
