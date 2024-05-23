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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.android_erp.R
import com.axelliant.android_erp.adapter.BirthdayAdapter

import com.axelliant.android_erp.adapter.ModulesAdapter
import com.axelliant.android_erp.base.BaseFragment
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.databinding.FragmentHomeBinding
import com.axelliant.android_erp.event.EventObserver
import com.axelliant.android_erp.extention.setUrlImage
import com.axelliant.android_erp.extention.showErrorMsg
import com.axelliant.android_erp.extention.showSuccessMsg
import com.axelliant.android_erp.model.Modules
import com.axelliant.android_erp.model.dashboard.Birthday
import com.axelliant.android_erp.model.dashboard.EmployProfile
import com.axelliant.android_erp.navigation.AppNavigator
import com.axelliant.android_erp.viewmodel.HomeViewModel
import org.koin.android.ext.android.inject
import java.io.IOException
import java.util.Locale

class HomeFragment : BaseFragment() {


    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager

    private val homeViewModel: HomeViewModel by inject()


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

    private fun setCurrentLocationText() {
//        binding?.tvLocTxt?.text = getLocationAddress(currentLocation)

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

        homeViewModel.getBirthdayList()
        homeViewModel.birthdayResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    birthdayPopulate(response.birthday_data!!)
                    dashBoardPopulate(response.employee_profile!!)
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.ivQr?.setOnClickListener {
            requireContext().showSuccessMsg()
        }
        binding?.ivNotification?.setOnClickListener {
            requireContext().showSuccessMsg()
        }

        binding?.btnCheckIn?.setOnClickListener {
            requireContext().showSuccessMsg()
        }

    }


    private fun dataPopulate() {
        binding?.rvModule?.layoutManager = GridLayoutManager(requireContext(), 2)
        val modulesAdapter = ModulesAdapter(
            listOf(
                Modules(
                    id = 0,
                    name = "Attendance",
                    color = requireContext().getColor(R.color.color_secondry),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_loc_pin)
                ),
                Modules(
                    id = 1,
                    name = "Leaves",
                    color = requireContext().getColor(R.color.yellow),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home)
                ),
                Modules(
                    id = 2,
                    name = "Expense",
                    color = requireContext().getColor(R.color.greeny),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_cake_tone)
                ),
                Modules(
                    id = 3,
                    name = "Pay Roll",
                    color = requireContext().getColor(R.color.colorApp),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_calendar)
                ),
                Modules(
                    id = 4,
                    name = "Employs",
                    color = requireContext().getColor(R.color.purple),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_loc_pin)
                ),

                ),
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Modules

                    when (currentObject.name) {
                        "Attendance" -> {

                            showDialog()
                            AppNavigator.navigateToAttendanceStats()
                        }

                        "Leaves" -> {
                            showDialog()
                            AppNavigator.navigateToLeaves()
                        }

                        else -> {
                            requireContext().showSuccessMsg()
                        }
                    }

                }

            })
        binding?.rvModule?.adapter = modulesAdapter
        binding?.rvModule?.isNestedScrollingEnabled = false


    }

    private fun dashBoardPopulate(employProfile: EmployProfile) {
        binding?.tvEmployeName?.text = employProfile.employee_name
        binding?.tvEmployeDesignation?.text = employProfile.designation
        binding?.profileImg?.setUrlImage(employProfile.image, requireContext())
        GlobalConfig.isManager = employProfile.is_manager

    }

    private fun birthdayPopulate(birthdayList: List<Birthday>) {


        binding?.rvBirthdays?.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val birthdayAdapter = BirthdayAdapter(
            requireContext(),
            birthdayList,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Birthday
                    requireContext().showSuccessMsg(
                        currentObject.name
                    )

                }

            })
        binding?.rvBirthdays?.adapter = birthdayAdapter


    }

}