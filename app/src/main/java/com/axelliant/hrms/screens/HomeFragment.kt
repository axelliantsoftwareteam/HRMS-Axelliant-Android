package com.axelliant.hrms.screens

import android.Manifest
import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.annotation.SuppressLint
import android.content.Context
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
import com.axelliant.hrms.config.AppConst.inputFormat
import com.axelliant.hrms.config.AppConst.outputFormat
import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.databinding.FragmentHomeBinding
import com.axelliant.hrms.enums.LocationFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.Modules
import com.axelliant.hrms.model.TargetLocResponse
import com.axelliant.hrms.model.dashboard.Birthday
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.Utils.getCurrentTime
import com.axelliant.hrms.viewmodel.HomeViewModel
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.koin.android.ext.android.inject
import java.util.Date

class HomeFragment : BaseFragment() {

    private lateinit var frontAnimation: AnimatorSet
    private lateinit var backAnimation: AnimatorSet
    private var isFront = true
    private var checkIn: String? = null
    private var checkOut: String? = null
    private val radiusInMeters: Double = 200.0
    private val targetLongitude: Double = 74.389467
    private val targetLatitude: Double = 31.522359
    private var _binding: FragmentHomeBinding? = null

    // Create an ArrayList to store the converted time strings
    private val targetLocList = ArrayList<TargetLocResponse>()


    private val binding get() = _binding

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager

    private val homeViewModel: HomeViewModel by inject()

    /* Azure AD Variables */
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null


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
                    dashBoardPopulate(response.employee_profile)
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        binding?.ivQr?.setOnClickListener {
            requireContext().showSuccessMsg()
        }
        binding?.ivNotification?.setOnClickListener(View.OnClickListener {
            if (mSingleAccountApp == null) {
                return@OnClickListener
            }
            /*
             * Removes the signed-in account and cached tokens from this app (or device, if the device is in shared mode).
           */
            mSingleAccountApp!!.signOut(object :
                ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    mAccount = null
                    requireContext().showErrorMsg("Sign Out")
                    AppNavigator.navigateToLogin()
                }

                override fun onError(exception: MsalException) {
                    requireContext().showErrorMsg(exception.toString())
                }
            })
        })

        targetLocList.add(TargetLocResponse(LocationFilter.NTC_OFFICE.value,31.5494, 74.3333))
        targetLocList.add(TargetLocResponse(LocationFilter.NASTP_OFFICE.value,targetLatitude,targetLongitude))



        setCurrentLocationText()
        val scale = requireContext().resources.displayMetrics.density
        binding?.tvCheckInStatus?.cameraDistance = 8000 * scale
        binding?.tvCheckInStatus?.cameraDistance = 8000 * scale


        // Now we will set the front animation
        frontAnimation = AnimatorInflater.loadAnimator(
            requireContext(),
            R.animator.front_animator
        ) as AnimatorSet
        backAnimation =
            AnimatorInflater.loadAnimator(requireContext(), R.animator.back_animator) as AnimatorSet

        binding?.btnCheckIn?.setOnClickListener {
            if (isFront)
            {
                checkIn = getCurrentTime()
                val date: Date? = checkIn?.let { it1 -> inputFormat.parse(it1) }
                val formattedTime: String = date?.let { outputFormat.format(it) } ?: "Invalid date"
                binding?.tvCheckInTxt?.text = formattedTime.valueQualifier()
                binding?.tvCheckInStatus?.text = LocationFilter.CHECK_IN.value
                binding?.ivPunchIn?.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.ic_punch_in))
                setCurrentLocationText()

                frontAnimation.setTarget(binding?.lyCheckIn)
                backAnimation.setTarget(binding?.lyCheckOut)
                frontAnimation.start()
                backAnimation.start()
                isFront = false

            } else {
                checkOut = getCurrentTime()
                val date: Date? = checkOut?.let { it1 -> inputFormat.parse(it1) }
                val formattedTime: String = date?.let { outputFormat.format(it) } ?: "Invalid date"
                binding?.tvCheckOutTxt?.text = formattedTime.valueQualifier()
                binding?.tvCheckOutStatus?.text = LocationFilter.CHECK_OUT.value
                binding?.ivPunchOut?.setImageDrawable(ContextCompat.getDrawable(requireContext(),R.drawable.ic_absent))
                setCurrentLocationText()

                frontAnimation.setTarget(binding?.lyCheckOut)
                backAnimation.setTarget(binding?.lyCheckIn)
                backAnimation.start()
                frontAnimation.start()
                isFront = true


            }


        }

        PublicClientApplication.createSingleAccountPublicClientApplication(
            requireContext(),
            R.raw.auth_config_ciam_auth,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    mSingleAccountApp = application
                }

                override fun onError(exception: MsalException) {
                    // Handle the exception
                    requireContext().showErrorMsg(exception.toString())
                    Log.d(TAG, exception.toString())

                }
            })

    }
    private fun setCurrentLocationText() {
//        binding?.tvLocTxt?.text = getLocationAddress(currentLocation)

        for (targetloc in targetLocList)
        {
            if (currentLocation != null) {
                val isWithinRadius = isLocationWithinRadius(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude,
                    targetloc.latitude!!,
                    targetloc.longitude!!,
                    radiusInMeters
                )
                if (isWithinRadius) {
                    binding?.tvLocation?.text = targetloc.name
                    return
                }
                else{
                    binding?.tvLocation?.text=LocationFilter.REMOTE.value
                }
            }
        }

    }
    private fun isLocationWithinRadius(
        currentLat: Double,
        currentLng: Double,
        targetLat: Double,
        targetLng: Double,
        radius: Double
    ): Boolean {
        val currentLocation = Location("").apply {
            latitude = currentLat
            longitude = currentLng
        }

        val targetLocation = Location("").apply {
            latitude = targetLat
            longitude = targetLng
        }

        val distanceInMeters = currentLocation.distanceTo(targetLocation)
        return distanceInMeters <= radius
    }


    private fun dataPopulate() {
        binding?.rvModule?.layoutManager = GridLayoutManager(requireContext(), 2)
        val modulesAdapter = ModulesAdapter(
            listOf(
                Modules(
                    id = 0,
                    name = "Attendance",
                    description = "Present of this month",
                    color = requireContext().getColor(R.color.color_secondry),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_atten)
                ),
                Modules(
                    id = 1,
                    name = "Leaves",
                    description = "Leaves you have",
                    color = requireContext().getColor(R.color.color_third),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_leaves)
                ),
                Modules(
                    id = 2,
                    name = "Expense",
                    description = "Claim your expense",
                    color = requireContext().getColor(R.color.colorApp),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_expe)
                ),
                Modules(
                    id = 3,
                    name = "Payslip",
                    description = "View your all pay-slip",
                    color = requireContext().getColor(R.color.greeny),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_payslip)
                ),
                Modules(
                    id = 4,
                    name = "Request",
                    description = "Present of this month",
                    color = requireContext().getColor(R.color.yellow),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_req)
                ),
               /* Modules(
                    id = 4,
                    name = "Approval",
                    description = "View all the requests",
                    color = requireContext().getColor(R.color.purple),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_loc_pin)
                ),*/

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

    companion object {
        private val TAG = HomeFragment::class.java.simpleName
    }
}


/*
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
}*/
