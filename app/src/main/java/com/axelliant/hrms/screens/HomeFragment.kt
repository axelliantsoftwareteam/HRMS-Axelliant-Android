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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.hrms.R
import com.axelliant.hrms.adapter.BirthdayAdapter

import com.axelliant.hrms.adapter.ModulesAdapter
import com.axelliant.hrms.base.BaseFragment
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.config.AppConst.inputFormat
import com.axelliant.hrms.config.AppConst.outputFormat
import com.axelliant.hrms.config.GlobalConfig
import com.axelliant.hrms.databinding.FragmentHomeBinding
import com.axelliant.hrms.enums.CheckRequestFilter
import com.axelliant.hrms.enums.LeaveStatus
import com.axelliant.hrms.enums.LocationFilter
import com.axelliant.hrms.event.EventObserver
import com.axelliant.hrms.extention.setUrlImage
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.extention.showSuccessMsg
import com.axelliant.hrms.extention.valueQualifier
import com.axelliant.hrms.model.Modules
import com.axelliant.hrms.model.dashboard.BranchDataResponse
import com.axelliant.hrms.model.dashboard.Birthday
import com.axelliant.hrms.model.dashboard.CheckInInfoResponse
import com.axelliant.hrms.model.dashboard.EmployProfile
import com.axelliant.hrms.model.login.CheckInRequest
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.SessionManager
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

    private var loc: String? = null
    private lateinit var frontAnimation: AnimatorSet
    private lateinit var backAnimation: AnimatorSet
    private var isFront = true
    private var checkIn: String? = null
    private var checkOut: String? = null
    private val radiusInMeters: Double = 200.0
    private var _binding: FragmentHomeBinding? = null


    private var isCheckIn: Boolean = true

    // Create an ArrayList to store the converted time strings
    private var targetLocList = ArrayList<BranchDataResponse>()


    private val binding get() = _binding

    private var currentLocation: Location? = null
    private lateinit var locationManager: LocationManager

    private val homeViewModel: HomeViewModel by inject()
    private val sessionManager: SessionManager by inject()

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
        AppConst.TOKEN = sessionManager.getToken()

        homeViewModel.getDashboardInformation()
        homeViewModel.dashboardResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    GlobalConfig.setCurrentEmployee(response.employee_profile!!)
                    birthdayPopulate(response.birthday_data!!)
                    checkInInfoPopulate(response.checkin_info!!)
                    if (response.branch_data != null)
                        targetLocList = response.branch_data

                    dashBoardPopulate(response.employee_profile)
                    dataPopulate()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        homeViewModel.checkInResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg("Attendance marked successfully")
                    homeViewModel.getDashboardInformation()

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })
        homeViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
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
                    sessionManager.logoutUser()
                    AppNavigator.navigateToLogin()
                }

                override fun onError(exception: MsalException) {
                    requireContext().showErrorMsg(exception.toString())
                    sessionManager.logoutUser()
                    AppNavigator.navigateToLogin()
                }
            })
        })

//        targetLocList.add(BranchDataResponse(LocationFilter.NTC_OFFICE.value, 31.5494, 74.3333))
//        targetLocList.add(
//            BranchDataResponse(
//                LocationFilter.NASTP_OFFICE.value,
//                targetLatitude,
//                targetLongitude
//            )
//        )


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
            setCurrentLocationText()

            if (loc != null) {
                setCurrentLocationText()

                if (isCheckIn)
                {

                    frontAnimation.setTarget(binding?.lyCheckIn)
                    backAnimation.setTarget(binding?.lyCheckOut)
                    frontAnimation.start()
                    backAnimation.start()


                } else {
                    frontAnimation.setTarget(binding?.lyCheckOut)
                    backAnimation.setTarget(binding?.lyCheckIn)
                    backAnimation.start()
                    frontAnimation.start()

                }

                var type = CheckRequestFilter.OUT.name

                if (isCheckIn)
                    type = CheckRequestFilter.IN.name

                homeViewModel.postCheckIn(CheckInRequest().apply {
                    this.log_type = type
                    this.date_time = getCurrentTime()
                    this.location = loc
                    this.request_status = LeaveStatus.APPROVED.value
                    this.attendance_reason = "Punch from application"
                })

            } else {
                requireContext().showErrorMsg("Please wait we are fetching your location")
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

    private fun checkInInfoPopulate(checkInInfo: CheckInInfoResponse) {
        if (checkInInfo.is_check_in_button == false && checkInInfo.is_check_out_button == false) {
            binding?.btnCheckIn?.isEnabled = false

            binding?.lyCheckIn?.visibility = View.VISIBLE
            binding?.lyCheckOut?.visibility = View.GONE

            binding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()
            binding?.tvCheckOutTxt?.text = checkInInfo.check_out.valueQualifier()


        } else {
            binding?.btnCheckIn?.isEnabled = true

            if (checkInInfo.is_check_in_button == true && checkInInfo.is_check_out_button == true) {

                binding?.lyCheckIn?.visibility = View.VISIBLE
                binding?.lyCheckOut?.visibility = View.GONE

                isCheckIn = true
            } else {
                if (checkInInfo.is_check_in_button == true) {

                    isCheckIn = true
                    binding?.lyCheckIn?.visibility = View.VISIBLE
                    binding?.lyCheckOut?.visibility = View.GONE

                    binding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()

                } else if (checkInInfo.is_check_out_button == true) {

                    binding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()
                    isCheckIn = false
                    binding?.lyCheckIn?.visibility = View.GONE
                    binding?.lyCheckOut?.visibility = View.VISIBLE
                    binding?.tvCheckOutTxt?.text = checkInInfo.check_out.valueQualifier()


                }
            }

        }
    }

    private fun setCurrentLocationText() {
//        binding?.tvLocTxt?.text = getLocationAddress(currentLocation)

        for (targetloc in targetLocList) {
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
                    loc = LocationFilter.OFFICE.value
                    return
                } else {
                    binding?.tvLocation?.text = LocationFilter.WHF.value
                    loc = LocationFilter.WHF.value
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

        val gridList = arrayListOf(
            Modules(
                id = 0,
                name = "Attendance",
                description = "Present of this month",
                color = requireContext().getColor(R.color.color_secondry),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_atten)
            ),
            Modules(
                id = 1,
                name = "Request",
                description = "Present of this month",
                color = requireContext().getColor(R.color.yellow),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_req)
            ),
            Modules(
                id = 2,
                name = "Leaves",
                description = "Leaves you have",
                color = requireContext().getColor(R.color.color_third),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_leaves)

            ),
            Modules(
                id = 3,
                name = "Approval",
                description = "View all requests",
                color = requireContext().getColor(R.color.colorApp),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_approv)
            ),
            Modules(
                id = 4,
                name = "Check IN",
                description = "View all the check-in requests",
                color = requireContext().getColor(R.color.blue_iris),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_checkin)


            ),
            Modules(
                id = 5,
                name = "Expense",
                description = "View all the expense requests",
                color = requireContext().getColor(R.color.violet),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_expe)
            ), Modules(
                id = 6,
                name = "Payslip",
                description = "View your all pay-slip",
                color = requireContext().getColor(R.color.greeny),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_payslip)
            )
        )

        val isManager = GlobalConfig.isCurrentManager()

        if (!isManager)
            gridList.removeAt(3)

        binding?.rvModule?.layoutManager = GridLayoutManager(requireContext(), 2)
        val modulesAdapter = ModulesAdapter(
            gridList,
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

                        "Approval" -> {
                            showDialog()
                            AppNavigator.navigateToApprovals()
                        }

                        "Check IN" -> {
                            showDialog()
                            AppNavigator.navigateToCheckInFragment()
                        }
                        "Expense" -> {
                            showDialog()
                            AppNavigator.navigateToExpenseFragment()
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

        val manager = GlobalConfig.isCurrentManager()
        binding?.rvBirthdays?.isVisible = manager
        binding?.tvBirthdays?.isVisible = manager

        if (birthdayList.isEmpty()) {
            binding?.rvBirthdays?.visibility = View.GONE
            binding?.tvBirthdays?.visibility = View.GONE
        }



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
