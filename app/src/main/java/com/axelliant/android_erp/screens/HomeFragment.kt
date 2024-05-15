package com.axelliant.android_erp.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat.getSystemService
import androidx.recyclerview.widget.LinearLayoutManager
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.adapter.WeeklyAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.FragmentHomeBinding
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.utils.Utils.getCurrentDate
import java.io.IOException
import java.util.Locale

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager


    private val gpsLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            setCurrentLocationText()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    //------------------------------------------------------//
    private val networkLocationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            currentLocation = location
            setCurrentLocationText()
        }

        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    private fun setCurrentLocationText(){
        binding?.tvLocTxt?.text = getLocationAddress(currentLocation)

    }

    private fun getLocationAddress(location: Location?): String {
        if (location == null) {
            return "Location not available"
        }
        if (!Geocoder.isPresent()) {
            return "Geocoding not supported"
        }
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses: List<Address> =
                geocoder.getFromLocation(location.latitude, location.longitude, 1)!!
            if (addresses.isNotEmpty()) {
                val address: Address = addresses[0]

                val sb = StringBuilder()
                sb.append(address.thoroughfare ?: "").append(address.subThoroughfare ?: "")
                for (i in 0 until address.maxAddressLineIndex) {
                    sb.append(address.getAddressLine(i)).append(", ")
                }
                sb.append(address.locality).append(", ")
                sb.append(address.countryName)
                return sb.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "NA"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater).also { _binding = it }
        return binding?.root
    }


    @SuppressLint("MissingPermission")
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
                    locationManager =
                        requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager

                    val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    val hasNetwork =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

                    if (hasGps) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            5000,
                            0F,
                            gpsLocationListener
                        )
                    }
                    if (hasNetwork) {
                        locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            5000,
                            0F,
                            networkLocationListener
                        )
                    }



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
        binding?.textClock?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                if (editable.toString().length >= 3) {
                    binding?.tvHour?.text = editable?.split(":")?.get(0)
                    binding?.tvMin?.text = editable?.split(":")?.get(1)
                    binding?.tvSec?.text = editable?.split(":")?.get(2)

                }

            }

        })

    }

    override fun onPause() {
        super.onPause()
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