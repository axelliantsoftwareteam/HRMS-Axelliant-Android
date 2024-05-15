package com.axelliant.android_erp.screens

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.WeeklyAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.FragmentHomeBinding
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.utils.Utils.getCurrentDate

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater).also { _binding = it }
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

                if (allAreGranted) {
//                    requireContext().showSuccessMsg("Location permission granted")
                } else {
//                    requireContext().showSuccessMsg("Ask Location permission")

                }
            }

        val appPerms = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        activityResultLauncher.launch(appPerms)

        // data population
        dataPopulate()

        binding?.tvDateTxt?.text = getCurrentDate()

    }


    private fun dataPopulate() {
        binding?.rvWeekly?.layoutManager = LinearLayoutManager(requireActivity())
        val weeklyAdapter = WeeklyAdapter(
            listOf(
                Test("item1"),
                Test("item2"),
                Test("item3"),
                Test("item4"),
                Test("item5"),
                Test("item6")
            ),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Test
                    requireContext().showSuccessMsg(
                        currentObject.testString
                    )

                }

            })
        binding?.rvWeekly?.adapter = weeklyAdapter


    }

}