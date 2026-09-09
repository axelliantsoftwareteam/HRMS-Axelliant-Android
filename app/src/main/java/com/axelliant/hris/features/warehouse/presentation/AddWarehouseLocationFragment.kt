package com.axelliant.hris.features.warehouse.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentAddWarehouseLocationBinding
import com.axelliant.hris.features.warehouse.domain.model.WarehouseLocationOptionModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AddWarehouseLocationFragment : Fragment() {

    private val viewModel: AddWarehouseLocationViewModel by viewModels()
    private var _binding: FragmentAddWarehouseLocationBinding? = null
    private val binding get() = _binding!!

    private lateinit var screenMode: WarehouseLocationFormMode
    private var selectedLevel = WarehouseLocationLevel.Area
    private var selectedParentLocation: WarehouseLocationOptionModel? = null
    private var parentLocationOptions = emptyList<WarehouseLocationOptionModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddWarehouseLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screenMode = WarehouseLocationFormMode.from(
            arguments?.getString(AddWarehouseLocationViewModel.ARG_MODE)
        )
        binding.activeCheckbox.isChecked = true
        populateLocationIfNeeded()
        applyScreenMode()
        bindWarehouseContext()
        setupInteractions()
        observeParentLocations()
        observeSaveState()
        arguments?.getString(AddWarehouseLocationViewModel.ARG_WAREHOUSE_ID)
            ?.let(viewModel::loadParentLocations)
    }

    private fun bindWarehouseContext() {
        val warehouseName = arguments
            ?.getString(AddWarehouseLocationViewModel.ARG_WAREHOUSE_NAME)
            .displayValue()
        val warehouseCode = arguments
            ?.getString(AddWarehouseLocationViewModel.ARG_WAREHOUSE_CODE)
            .displayValue()
        binding.warehouseContextText.text = getString(
            R.string.location_warehouse_context,
            warehouseName,
            warehouseCode
        )
    }

    private fun setupInteractions() {
        binding.appTopBar.setOnBackClickListener { findNavController().navigateUp() }
        binding.cancelButton.setOnClickListener { findNavController().navigateUp() }
        binding.levelField.setOnClickListener { anchor -> showLevelSelector(anchor) }
        binding.parentLocationField.setOnClickListener { anchor -> showParentLocationSelector(anchor) }
        binding.saveButton.setOnClickListener { saveLocation() }
    }

    private fun observeParentLocations() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.parentLocationsState.collect { state ->
                    binding.progress.isVisible = state is UiState.Loading
                    binding.parentLocationField.isEnabled =
                        state !is UiState.Loading && screenMode != WarehouseLocationFormMode.View
                    when (state) {
                        is UiState.Success -> {
                            parentLocationOptions = state.data
                            if (selectedParentLocation == null) {
                                binding.parentLocationText.text =
                                    getString(R.string.location_parent_placeholder)
                            }
                        }
                        UiState.Empty -> {
                            parentLocationOptions = emptyList()
                            binding.parentLocationText.text =
                                getString(R.string.location_parent_none)
                        }
                        is UiState.Error -> showParentLocationError(state.message)
                        UiState.Unauthorized -> showParentLocationError(
                            getString(R.string.warehouse_locations_error)
                        )
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showLevelSelector(anchor: View) {
        if (screenMode == WarehouseLocationFormMode.View) return
        val popupMenu = PopupMenu(anchor.context, anchor)
        WarehouseLocationLevel.entries.forEachIndexed { index, level ->
            popupMenu.menu.add(0, index, index, level.label)
        }
        popupMenu.setOnMenuItemClickListener { item ->
            WarehouseLocationLevel.entries.getOrNull(item.itemId)?.let { level ->
                selectedLevel = level
                binding.levelText.text = level.label
                true
            } ?: false
        }
        popupMenu.show()
    }

    private fun showParentLocationSelector(anchor: View) {
        if (screenMode == WarehouseLocationFormMode.View) return
        if (parentLocationOptions.isEmpty()) {
            Toast.makeText(
                requireContext(),
                R.string.location_parent_empty,
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val popupMenu = PopupMenu(anchor.context, anchor)
        parentLocationOptions.forEachIndexed { index, option ->
            popupMenu.menu.add(0, index, index, option.selectorLabel())
        }
        popupMenu.setOnMenuItemClickListener { item ->
            parentLocationOptions.getOrNull(item.itemId)?.let { option ->
                selectedParentLocation = option
                binding.parentLocationText.text = option.selectorLabel()
                binding.parentLocationText.setTextColor(
                    ContextCompat.getColor(requireContext(), R.color.ds_text_primary)
                )
                true
            } ?: false
        }
        popupMenu.show()
    }

    private fun saveLocation() {
        if (screenMode == WarehouseLocationFormMode.View) return
        clearValidation()
        viewModel.saveLocation(
            AddWarehouseLocationInput(
                id = arguments?.getString(AddWarehouseLocationViewModel.ARG_LOCATION_ID),
                warehouseId = arguments
                    ?.getString(AddWarehouseLocationViewModel.ARG_WAREHOUSE_ID)
                    .orEmpty(),
                parentId = selectedParentLocation?.id
                    ?: arguments?.getString(AddWarehouseLocationViewModel.ARG_LOCATION_PARENT_ID),
                levelType = selectedLevel.apiValue,
                code = binding.codeInput.text?.toString().orEmpty(),
                name = binding.nameInput.text?.toString().orEmpty(),
                isActive = binding.activeCheckbox.isChecked
            )
        )
    }

    private fun showParentLocationError(message: String) {
        parentLocationOptions = emptyList()
        binding.parentLocationText.text = getString(R.string.location_parent_placeholder)
        Toast.makeText(
            requireContext(),
            message.ifBlank { getString(R.string.location_parent_load_error) },
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun observeSaveState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.saveState.collect { state ->
                    binding.progress.isVisible = state is UiState.Loading
                    binding.saveButton.isEnabled = state !is UiState.Loading
                    binding.cancelButton.isEnabled = state !is UiState.Loading
                    when (state) {
                        is UiState.Success -> {
                            Toast.makeText(
                                requireContext(),
                                R.string.location_save_success,
                                Toast.LENGTH_SHORT
                            ).show()
                            findNavController().previousBackStackEntry?.savedStateHandle
                                ?.set(AddWarehouseLocationViewModel.RESULT_LOCATION_SAVED, true)
                            findNavController().navigateUp()
                            viewModel.resetSaveState()
                        }
                        is UiState.Error -> {
                            showSaveError(state.message)
                            viewModel.resetSaveState()
                        }
                        UiState.Unauthorized -> {
                            showSaveError(getString(R.string.warehouse_locations_error))
                            viewModel.resetSaveState()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun showSaveError(message: String) {
        when (message) {
            AddWarehouseLocationViewModel.VALIDATION_WAREHOUSE -> Toast.makeText(
                requireContext(),
                R.string.warehouse_selector_empty,
                Toast.LENGTH_SHORT
            ).show()
            AddWarehouseLocationViewModel.VALIDATION_CODE -> {
                binding.codeInputLayout.error = getString(R.string.location_validation_code)
            }
            AddWarehouseLocationViewModel.VALIDATION_NAME -> {
                binding.nameInputLayout.error = getString(R.string.location_validation_name)
            }
            else -> Toast.makeText(
                requireContext(),
                message.ifBlank { getString(R.string.location_save_error) },
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun populateLocationIfNeeded() {
        if (screenMode == WarehouseLocationFormMode.Add) return
        val args = arguments ?: return
        binding.codeInput.setText(args.getString(AddWarehouseLocationViewModel.ARG_LOCATION_CODE).formValue())
        binding.nameInput.setText(args.getString(AddWarehouseLocationViewModel.ARG_LOCATION_NAME).formValue())
        val levelType = args.getInt(
            AddWarehouseLocationViewModel.ARG_LOCATION_LEVEL_TYPE,
            WarehouseLocationLevel.Area.apiValue
        )
        selectedLevel = WarehouseLocationLevel.from(levelType)
        binding.levelText.text = selectedLevel.label
        binding.activeCheckbox.isChecked =
            args.getBoolean(AddWarehouseLocationViewModel.ARG_LOCATION_IS_ACTIVE, true)
    }

    private fun applyScreenMode() {
        binding.appTopBar.setTitle(
            when (screenMode) {
                WarehouseLocationFormMode.Add -> R.string.add_location_title
                WarehouseLocationFormMode.Edit -> R.string.edit_location_title
                WarehouseLocationFormMode.View -> R.string.view_location_title
            }
        )
        binding.saveButton.setText(
            if (screenMode == WarehouseLocationFormMode.Edit) R.string.update else R.string.save
        )
        binding.actionRow.isVisible = screenMode != WarehouseLocationFormMode.View
        setFormEditable(screenMode != WarehouseLocationFormMode.View)
    }

    private fun setFormEditable(editable: Boolean) {
        listOf(
            binding.levelField,
            binding.parentLocationField,
            binding.codeInputLayout,
            binding.nameInputLayout,
            binding.codeInput,
            binding.nameInput,
            binding.activeCheckbox
        ).forEach { it.isEnabled = editable }
    }

    private fun clearValidation() {
        binding.codeInputLayout.error = null
        binding.nameInputLayout.error = null
    }

    private fun WarehouseLocationOptionModel.selectorLabel(): String {
        val nameValue = name.takeIf { it.isNotBlank() && it != "-" }
        val pathValue = path.takeIf { it.isNotBlank() && it != "-" && it != nameValue }
        return listOfNotNull(nameValue, pathValue).joinToString(" - ")
            .ifBlank { getString(R.string.warehouse_not_available) }
    }

    private fun String?.displayValue(): String {
        return orEmpty().takeIf { it.isNotBlank() && it != "-" }
            ?: getString(R.string.warehouse_not_available)
    }

    private fun String?.formValue(): String {
        val cleanValue = orEmpty().takeIf { it.isNotBlank() && it != "-" }
        return cleanValue ?: if (screenMode == WarehouseLocationFormMode.View) {
            getString(R.string.warehouse_not_available)
        } else {
            ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

private enum class WarehouseLocationFormMode {
    Add,
    Edit,
    View;

    companion object {
        fun from(value: String?): WarehouseLocationFormMode {
            return when (value) {
                AddWarehouseLocationViewModel.MODE_EDIT -> Edit
                AddWarehouseLocationViewModel.MODE_VIEW -> View
                else -> Add
            }
        }
    }
}
