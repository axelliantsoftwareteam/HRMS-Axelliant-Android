package com.axelliant.hrms.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.BirthdayAdapter

import com.axelliant.hrms.adapter.ModulesAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.databinding.FragmentHomeBinding
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.model.Modules
import com.axelliant.hrms.model.dashboard.Birthday
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.viewmodel.HomeViewModel
import org.koin.android.ext.android.inject
import java.io.IOException
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
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

        homeViewModel.getDashboardInformation()
        homeViewModel.dashboardResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    GlobalConfig.setCurrentEmployee(response.employee_profile!!)
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
        // KeyHash generated
        try {
            val info: PackageInfo = requireActivity().packageManager.getPackageInfo(
                requireActivity().packageName,
                PackageManager.GET_SIGNATURES
            )

            for (signature: Signature in info.signatures) {
                val messageDigest: MessageDigest = MessageDigest.getInstance("SHA")
                messageDigest.update(signature.toByteArray())
                Log.d(
                    "KeyHash:",
                    "KeyHash=" + android.util.Base64.encodeToString(messageDigest.digest(), android.util.Base64.DEFAULT)
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Handle NameNotFoundException
        } catch (e: NoSuchAlgorithmException) {
            // Handle NoSuchAlgorithmException
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
                Modules(
                    id = 5,
                    name = "Request",
                    color = requireContext().getColor(R.color.red),
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
                        "Request" -> {
                            showDialog()
                            AppNavigator.navigateToRequest()
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