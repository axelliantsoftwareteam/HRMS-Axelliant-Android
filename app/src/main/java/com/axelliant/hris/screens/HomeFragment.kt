package com.axelliant.hris.screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.extention.hideShimmer
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.compose.ui.res.vectorResource
import com.axelliant.hris.R
import com.axelliant.hris.adapter.BirthdayAdapter

import com.axelliant.hris.adapter.ModulesAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.bottomSheet.TodayTeamAttendanceDetailBottomSheet
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.databinding.FragmentHomeBinding
import com.axelliant.hris.enums.CheckRequestFilter
import com.axelliant.hris.enums.HomeMenu
import com.axelliant.hris.enums.LeaveStatus
import com.axelliant.hris.enums.LocationFilter
import com.axelliant.hris.enums.TodayTeamStatus
import com.axelliant.hris.event.EventObserver
import com.axelliant.hris.extention.getNtpTimeFormatted
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.extention.showSuccessMsg
import com.axelliant.hris.extention.valueQualifier
import com.axelliant.hris.model.Modules
import com.axelliant.hris.model.todayTeam.TodayTeamResponse
import com.axelliant.hris.model.attendance.ShiftData
import com.axelliant.hris.model.dashboard.BranchDataResponse
import com.axelliant.hris.model.dashboard.Birthday
import com.axelliant.hris.model.dashboard.CheckInInfoResponse
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.todayTeam.EmployTeamProfile
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.SessionManager
import com.axelliant.hris.viewmodel.HomeViewModel
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.microsoft.fluentui.theme.FluentTheme
import com.axelliant.hris.components.AppButton
import com.axelliant.hris.components.CheckInButtonTokens
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import com.axelliant.hris.databinding.LayoutTodayCardContentBinding
import com.axelliant.hris.components.AppCard
import com.axelliant.hris.components.TodayCardTokens


class HomeFragment : BaseFragment() {

    private var employProfileResponse: EmployProfile? = null
    private var isManager: Boolean = false
    private var gridList: ArrayList<Modules>? = null
    private var teamattend: TodayTeamResponse? = null
    private var ntpTimeString: String? = null
    private var currentStatus: String? = null
    private var loc: String? = null
    private val radiusInMeters: Double = 200.0
    private var _binding: FragmentHomeBinding? = null

    private var isCheckIn: Boolean = true
    private val isCheckInState = mutableStateOf(true)
    private val checkInEnabledState = mutableStateOf(true)

    // Create an ArrayList to store the converted time strings
    private var targetLocList = ArrayList<BranchDataResponse>()
    private var employeeTodayList = ArrayList<EmployTeamProfile>()
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>


    private val binding get() = _binding
    private var _todayCardBinding: LayoutTodayCardContentBinding? = null
    private val todayCardBinding get() = _todayCardBinding

    private var currentLocation: Location? = null
    private var checkInInfoResponse: CheckInInfoResponse? = null
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

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                // Permission granted, perform location-based task
                onCheckInButtonClicked()
            } else {
                // Permission denied, check if "Don't ask again" was selected
                if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // User clicked "Don't Allow", show rationale
                    showPermissionRationale()
                } else {
                    // User selected "Don't ask again", guide them to app settings
                    showSettingsDialog()
                }
            }
        }


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

        /*
               AppConst.TOKEN = "8b87d8a458a89e9:a1705cecb80d093"
        */
        AppConst.TOKEN = sessionManager.getToken()
        if (employProfileResponse == null) {
            binding?.shimmerLayout?.showShimmer(binding?.contentGroup!!)
        }
        homeViewModel.getDashboardInformation()
        // data population
        homeViewModel.dashboardResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                binding?.shimmerLayout?.hideShimmer(binding?.contentGroup!!)
                if (response?.meta?.status == true) {
                    // success

                    employProfileResponse = response.employee_profile
                    employProfileResponse?.let { GlobalConfig.setCurrentEmployee(it) }
                    isManager = GlobalConfig.isCurrentManager()

                    birthdayPopulate(response.birthday_data!!)
                    response.shift_detail?.let { dashBoardShiftPopulate(it) }
                    checkInInfoPopulate(response.checkin_info!!)
                    checkInInfoResponse = response.checkin_info

                    if (response.branch_data != null)
                    {
                        targetLocList = response.branch_data

                    }
                    response.employee_profile?.let { dashBoardPopulate(it) }

                    dataPopulate()
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        homeViewModel.checkInResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg(response.status_message)
                    homeViewModel.getDashboardInformation()

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        homeViewModel.todayTeamResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    // success
                    if (response != null) {
                        binding?.tvShift?.isVisible = true
                        binding?.lyMyTeam?.isVisible = true
                        teamattend=response
                        teamsToday(response)

                    }

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })
        homeViewModel.employListResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->
                if (response?.meta?.status == true) {
                    // success
                    if ((response.employee_list?.size ?: 0) > 0) {
                        employeeTodayList = response.employee_list!!
                        getEmployeeList()
                    }
                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        homeViewModel.getIsLoading()
            .observe(viewLifecycleOwner, EventObserver { isLoading ->
                if (employProfileResponse == null && isLoading) return@EventObserver
                if (isLoading) showDialog() else hideDialog()
            })


        // Call the NTP time method in a coroutine
        CoroutineScope(Dispatchers.Main).launch {
            ntpTimeString = getNtpTimeFormatted() // Call the method that returns a string
            Log.d("NTP", ntpTimeString!!)
        }


        binding?.lyTotalMembers?.setOnClickListener {
//            requireContext().showSuccessMsg(TodayTeamStatus.AllTeamMember.value)
//            homeViewModel.getTodayTeamList()
//            val todayTeamAttendance =
//                TodayTeamAttendanceDetailBottomSheet(TodayTeamStatus.AllTeamMember.value)
//            navigateToBottomSheet(todayTeamAttendance)
        }
        binding?.lyPresent?.setOnClickListener {
            currentStatus = TodayTeamStatus.CheckIn.value
            if ((teamattend?.checkin_count ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }
        }
        binding?.lyWorkHome?.setOnClickListener {
            currentStatus = TodayTeamStatus.CheckOut.value
            if ((teamattend?.checkout_count ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }

        }
        binding?.lyMisPunchOut?.setOnClickListener {
            currentStatus = TodayTeamStatus.OnLeave.value
            if ((teamattend?.leave_count ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }
            homeViewModel.getTodayTeamList(currentStatus)

        }
        binding?.lyTeamsAbsent?.setOnClickListener {
            currentStatus = TodayTeamStatus.InOffice.value
            if ((teamattend?.in_office ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }

        }
        binding?.lyOnLeave?.setOnClickListener {
            currentStatus = TodayTeamStatus.WFH.value
            if ((teamattend?.work_from_home ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }

        }
        binding?.lyWeeklyOffs?.setOnClickListener {
            currentStatus = TodayTeamStatus.MissedPunch.value
            if ((teamattend?.absent_count ?: 0) > 0) {
                homeViewModel.getTodayTeamList(currentStatus)
            }

        }

        binding?.ivQr?.setOnClickListener {
            requireContext().showSuccessMsg()
        }
        binding?.ivNotification?.setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.logout_message))
                .setTitle(getString(R.string.info))
                .setPositiveButton(getString(R.string.yes)) { dialog, which ->
                    if (mSingleAccountApp == null) {
                        return@setPositiveButton
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
                }
                .setNegativeButton(getString(R.string.no)) { dialog, which ->
                    // Do nothing
                }
                .show()


        }

//        targetLocList.add(BranchDataResponse(LocationFilter.NTC_OFFICE.value, 31.5494, 74.3333))
//        targetLocList.add(
//            BranchDataResponse(
//                LocationFilter.NASTP_OFFICE.value,
//                targetLatitude,
//                targetLongitude
//            )
//        )


        setCurrentLocationText()
        binding?.root?.findViewById<ComposeView>(R.id.compose_today_card)?.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                FluentTheme {
                    AppCard(
                        basicCardTokens = TodayCardTokens(
                            cornerRadiusRes = R.dimen.ds_radius_md
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        AndroidViewBinding(
                            factory = LayoutTodayCardContentBinding::inflate
                        ) {
                            _todayCardBinding = this
                            composeCheckIn.apply {
                                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                                setContent {
                                    FluentTheme {
                                        AppButton(
                                            text = if (isCheckInState.value) getString(R.string.check_in) else getString(R.string.check_out),
                                            onClick = { onCheckInButtonClicked() },
                                            enabled = checkInEnabledState.value,
                                            icon = ImageVector.vectorResource(id = R.drawable.ic_signout_fluent),
                                            buttonTokens = CheckInButtonTokens(
                                                fontSizeDimenName = "_13sdp",
                                                iconSizeDimenName = "_23sdp",
                                                bgRest = 0xFF0078D4,
                                                bgPressed = 0xFF106EBE,
                                                bgSelected = 0xFF106EBE,
                                                bgFocused = 0xFF272757,
                                                bgDisabled = 0xFFACACAC,
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
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
    private fun getEmployeeList() {
        if (employeeTodayList.size > 0) {
            val todayTeamAttendance =
                currentStatus?.let {
                    TodayTeamAttendanceDetailBottomSheet(
                        it,
                        employeeTodayList
                    )
                }
            if (todayTeamAttendance != null) {
                navigateToBottomSheet(todayTeamAttendance)
            }
        }
    }

    private fun teamsToday(response: TodayTeamResponse) {

        binding?.tvTotalMemberTxt?.text = response.team_member_count.toString().valueQualifier()
        binding?.tvPresentTxt?.text = response.checkin_count.toString().valueQualifier()
        binding?.tvWorkHomeTxt?.text = response.checkout_count.toString().valueQualifier()
        binding?.tvMissPunchOutTxt?.text = response.leave_count.toString().valueQualifier()
        binding?.tvTeamsAbsentTxt?.text = response.in_office.toString().valueQualifier()
        binding?.tvTeamsOnleaveTxt?.text = response.work_from_home.toString().valueQualifier()
        binding?.tvWeeklyOffsTxt?.text = response.absent_count.toString().valueQualifier()


    }

    // Show a rationale dialog explaining why the permission is needed
    private fun showPermissionRationale() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Needed")
            .setMessage("This app requires location permission to perform check-in and check-out. Please grant the permission.")
            .setPositiveButton("OK") { _, _ ->
                // Try requesting the permission again
                requestLocationPermission()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // Show a dialog guiding the user to the app's settings
    private fun showSettingsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("Location permission is denied. You need to enable it in the app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri: Uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun onCheckInButtonClicked() {
        if (checkLocationPermission()) {
            setCurrentLocationText()
            if (checkInInfoResponse?.is_check_in_button == true) {
                showAttendanceDialog(LeaveStatus.CHECKIN.value)
            } else {
                showAttendanceDialog(LeaveStatus.CHECKOUT.value)
            }
        } else {
            requireContext().showErrorMsg("Permission denied")
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun showAttendanceDialog(action: String) {
        val title = "Confirm Action"
        val description = when (action) {
            "Check In" -> "Are you sure you want to 'Check In'? This will mark your attendance."
            "Check Out" -> "Are you sure you want to 'Check Out' ? This will complete your attendance."
            else -> "Are you sure you want to proceed?"
        }

        // Example showing dialog
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(description)
            .setPositiveButton("Yes") { dialog, _ ->
                if (loc != null) {
                    var type = CheckRequestFilter.OUT.name
                    if (isCheckIn) type = CheckRequestFilter.IN.name

                    homeViewModel.postCheckIn(CheckInRequest().apply {
                        this.log_type = type
                        this.date_time = ntpTimeString
                        this.location = loc
                        this.request_status = LeaveStatus.APPROVED.value
                        this.attendance_reason = "Punch from application"
                    })
                } else {
                    requireContext().showErrorMsg("Please wait we are fetching your location")
                }
                dialog.dismiss()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }


    private fun checkInInfoPopulate(checkInInfo: CheckInInfoResponse) {

//        todayCardBinding?.tvLocation?.text = checkInInfo.location.valueQualifier()

        if (checkInInfo.is_check_in_button == false && checkInInfo.is_check_out_button == false) {
            checkInEnabledState.value = false

            todayCardBinding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()
            todayCardBinding?.tvCheckOutTxt?.text = checkInInfo.check_out.valueQualifier()

        } else {
            checkInEnabledState.value = true

            if (checkInInfo.is_check_in_button == true && checkInInfo.is_check_out_button == true) {
                isCheckIn = true
                isCheckInState.value = true
            } else {
                if (checkInInfo.is_check_in_button == true) {
                    isCheckIn = true
                    isCheckInState.value = true
                    todayCardBinding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()

                } else if (checkInInfo.is_check_out_button == true) {
                    isCheckIn = false
                    isCheckInState.value = false
                    todayCardBinding?.tvCheckInTxt?.text = checkInInfo.check_in.valueQualifier()
                    todayCardBinding?.tvCheckOutTxt?.text = checkInInfo.check_out.valueQualifier()
                }
            }
        }
    }
    private fun setCurrentLocationText() {
//        binding?.tvLocTxt?.text = getLocationAddress(currentLocation)

        /* currentLocation = Location("").apply {
             latitude = 31.5226884
             longitude = 74.3490491
         }*/
        for (targetloc in targetLocList) {
            if (currentLocation != null) {
                Log.d("loc", " ${currentLocation!!.latitude} ${currentLocation!!.longitude}")
                val isWithinRadius = isLocationWithinRadius(
                    currentLocation!!.latitude,
                    currentLocation!!.longitude,
                    targetloc.latitude!!,
                    targetloc.longitude!!,
                    radiusInMeters
                )
                if (isWithinRadius) {
                    binding?.tvCurrentLocs?.text = targetloc.name
                    loc = LocationFilter.OFFICE.value
                    return
                } else {
                    binding?.tvCurrentLocs?.text = LocationFilter.WHF.value
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

        if (isManager) {
            homeViewModel.getTodayTeamInfo()
            gridList?.add(
                Modules(
                    id = 3,
                    name = HomeMenu.Approval.gridName,
                    description = HomeMenu.Approval.description,
                    color = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_bg),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_approv)
                )
            )
        }

        gridList = arrayListOf(
            Modules(
                id = 0,
                name = HomeMenu.Attendance.gridName,
                description = HomeMenu.Attendance.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.attend_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_attendance)
            ),
            Modules(
                id = 1,
                name = HomeMenu.Request.gridName,
                description = HomeMenu.Request.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.request_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_requests),
                progressValue = 2,
                progressMax = 5
            ),
            Modules(
                id = 2,
                name = HomeMenu.Leaves.gridName,
                description = HomeMenu.Leaves.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.leaves_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_leaves),
                progressValue = 4,
                progressMax = 10

            ),
            Modules(
                id = 4,
                name = HomeMenu.CheckIN.gridName,
                description = HomeMenu.CheckIN.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.checkin_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_check_in)


            ),
            Modules(
                id = 5,
                name = HomeMenu.Expense.gridName,
                description = HomeMenu.Expense.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.expense_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_expense)
            ),
            Modules(
                id = 6,
                name = HomeMenu.PaySlips.gridName,
                description = HomeMenu.PaySlips.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.payslips_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.fluent_pay),
                progressValue = 4,
                progressMax = 10

            ),
        )
        if (isManager) {
            homeViewModel.getTodayTeamInfo()
            gridList?.add(
                Modules(
                    id = 3,
                    name = HomeMenu.Approval.gridName,
                    description = HomeMenu.Approval.description,
                    color = ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.approval_gradient
                    ),
                    drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_approv)
                )
            )
        }

        binding?.rvModule?.layoutManager = GridLayoutManager(requireContext(), 2)
        val modulesAdapter = ModulesAdapter(
            gridList!!,
            object : AdapterItemClick {
                override fun onItemClick(customObject: Any, position: Int) {
                    val currentObject = customObject as Modules


                    when (currentObject.name) {
                        HomeMenu.Attendance.gridName -> {
                            AppNavigator.navigateToAttendanceStats()
                        }

                        HomeMenu.Leaves.gridName -> {
                            showDialog()
                            AppNavigator.navigateToLeaves()
                        }

                        HomeMenu.Request.gridName -> {
                            showDialog()
                            AppNavigator.navigateToRequest()
                        }

                        HomeMenu.Approval.gridName -> {
                            showDialog()
                            AppNavigator.navigateToApprovals()
                        }

                        HomeMenu.CheckIN.gridName -> {
                            showDialog()
                            AppNavigator.navigateToCheckInFragment()
                        }

                        HomeMenu.Expense.gridName -> {
                            showDialog()
                            AppNavigator.navigateToExpenseFragment()
                        }
                        HomeMenu.DocumentManagement.gridName -> {
                            showDialog()
                            AppNavigator.navigateToDocumentManageFragment()
                        }
                        HomeMenu.ResourceManagement.gridName -> {
                            showDialog()
                            AppNavigator.navigateToResourceManageFragment()
                        }
                        HomeMenu.PaySlips.gridName -> {
                            showDialog()
                            AppNavigator.navigateToPaySlips()
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

    private fun dashBoardShiftPopulate(shiftData: ShiftData) {

        binding?.tvShiftNote?.text =
            "Your shift ${shiftData.name} is ${shiftData.location}"
    }


    private fun dashBoardPopulate(employProfile: EmployProfile) {
        binding?.tvEmployeName?.text = employProfile.employee_name
        binding?.tvEmployeDesignation?.text = employProfile.designation
        binding?.tvEmployeId?.text = "Emp ID: ${employProfile.custom_employee_code}"
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

    // Check if location permission is granted
    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            // Show rationale to the user
            requireContext().showErrorMsg("Location permission is needed to access your location for check-in/out.")
        } else {
            // User has permanently denied the permission, open settings
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", requireContext().packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                onCheckInButtonClicked()
            } else {
                requireContext().showErrorMsg("Location permission is required to check in/out.")
            }
        }
    }
    override fun onDestroyView() {
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        _binding = null
        _todayCardBinding = null
    }
    companion object {
        private val TAG = HomeFragment::class.java.simpleName
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }
}