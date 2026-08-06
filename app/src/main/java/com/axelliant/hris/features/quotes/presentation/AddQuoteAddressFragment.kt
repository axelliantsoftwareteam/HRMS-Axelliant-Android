package com.axelliant.hris.features.quotes.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.databinding.FragmentAddQuoteAddressBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddQuoteAddressFragment : Fragment() {

    private var _binding: FragmentAddQuoteAddressBinding? = null
    private val binding get() = _binding!!
    private val addressType: AddressType by lazy {
        AddressType.fromNavValue(arguments?.getString(AddQuoteViewModel.ARG_ADDRESS_TYPE))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddQuoteAddressBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.backButton.setOnClickListener { findNavController().navigateUp() }
        binding.saveAddressButton.setOnClickListener { saveAddress() }
    }

    private fun saveAddress() {
        val address = binding.addressInput.text?.toString().orEmpty().trim()
        val country = binding.countryInput.text?.toString().orEmpty().trim()
        val state = binding.stateInput.text?.toString().orEmpty().trim()
        val city = binding.cityInput.text?.toString().orEmpty().trim()
        val zip = binding.zipInput.text?.toString().orEmpty().trim()
        if (listOf(address, country, state, city, zip).any { it.isBlank() }) {
            Toast.makeText(requireContext(), R.string.add_quote_address_required_error, Toast.LENGTH_SHORT).show()
            return
        }

        findNavController().previousBackStackEntry?.savedStateHandle?.set(
            addressType.resultKey,
            bundleOf(
                KEY_ID to "address-${System.currentTimeMillis()}",
                KEY_ADDRESS to address,
                KEY_COUNTRY to country,
                KEY_STATE to state,
                KEY_CITY to city,
                KEY_ZIP to zip
            )
        )
        findNavController().navigateUp()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_ADDRESS = "address"
        const val KEY_COUNTRY = "country"
        const val KEY_STATE = "state"
        const val KEY_CITY = "city"
        const val KEY_ZIP = "zip"
    }
}
