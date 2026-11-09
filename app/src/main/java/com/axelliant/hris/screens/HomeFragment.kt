package com.axelliant.hris.screens

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.util.Locale
import java.util.Date
import java.text.SimpleDateFormat
import androidx.lifecycle.lifecycleScope

import androidx.core.view.isVisible
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.axelliant.hris.R
import com.axelliant.hris.adapter.BirthdayAdapter

import com.axelliant.hris.adapter.ModulesAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.bottomSheet.TodayTeamAttendanceDetailBottomSheet
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppState
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
import com.axelliant.hris.ui.WsFormat
import com.axelliant.hris.ui.setAvatar
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
import com.axelliant.hris.viewmodel.NotificationsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.android.ext.android.inject

class HomeFragment : BaseFragment() {

    private var employProfileResponse: EmployProfile? = null
    private var isManager: Boolean = false
    private var gridList: ArrayList<Modules>? = null
    private var renderedManager: Boolean? = null
    private var teamattend: TodayTeamResponse? = null
    private var ntpTimeString: String? = null
    private var currentStatus: String? = null
    private var loc: String? = null
    private lateinit var frontAnimation: AnimatorSet
    private lateinit var backAnimation: AnimatorSet
    private val radiusInMeters: Double = 200.0
    private var _binding: FragmentHomeBinding? = null


    private var isCheckIn: Boolean = true

    // Create an ArrayList to store the converted time strings
    private var targetLocList = ArrayList<BranchDataResponse>()
    private var employeeTodayList = ArrayList<EmployTeamProfile>()
    private var homeLocation: com.axelliant.hris.location.HomeLocationController? = null
    private var locationFetching = false
    private var internetDialog: AlertDialog? = null


    private val binding get() = _binding

    private var currentLocation: Location? = null
    private var checkInInfoResponse: CheckInInfoResponse? = null


    private val homeViewModel: HomeViewModel by viewModel()
    private val notificationsViewModel: NotificationsViewModel by viewModel()
    private val sessionManager: SessionManager by inject()

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



        homeLocation = com.axelliant.hris.location.HomeLocationController(
            this,
            changed = { location -> currentLocation = location; setCurrentLocationText() },
            fetchingChanged = { busy ->
                locationFetching = busy
                binding?.tvCurrentLocs?.isEnabled = !busy
                if (busy) binding?.tvCurrentLocs?.setText(R.string.home_location_fetching_short)
                else setCurrentLocationText()
            }
        ).also { viewLifecycleOwner.lifecycle.addObserver(it) }
        binding?.tvCurrentLocs?.setOnClickListener { homeLocation?.findLocation() }
/*
       AppConst.TOKEN = "8b87d8a458a89e9:a1705cecb80d093"
*/
       AppConst.TOKEN = sessionManager.getToken()

        // data population
        dataPopulate()
        homeViewModel.dashboardResponse.observe(
            viewLifecycleOwner,
            { response ->

                if (response?.meta?.status == true) {
                    // success

                    employProfileResponse = response.employee_profile
                    GlobalConfig.setCurrentEmployee(employProfileResponse)
                    isManager = GlobalConfig.isCurrentManager()

                    birthdayPopulate(response.birthday_data ?: arrayListOf())
                    response.shift_detail?.let { dashBoardShiftPopulate(it) }
                    response.checkin_info?.let { checkInInfoPopulate(it) }
                    checkInInfoResponse = response.checkin_info

                    targetLocList = response.branch_data ?: arrayListOf()
                    setCurrentLocationText()
                    response.employee_profile?.let { dashBoardPopulate(it) }

                    dataPopulate()
                    if (isManager) homeViewModel.getTodayTeamInfo() else binding?.lyMyTeam?.isVisible = false
                } else {
                    checkInInfoResponse = null
                    binding?.btnCheckIn?.isEnabled = false
                    binding?.lyMyTeam?.isVisible = false
                }

            })

        homeViewModel.checkInResponse.observe(
            viewLifecycleOwner,
            EventObserver { response ->

                if (response?.meta?.status == true) {
                    requireContext().showSuccessMsg(response.status_message)

                } else {
                    requireContext().showErrorMsg(response?.meta?.message.toString())
                }

            })

        homeViewModel.todayTeamResponse.observe(
            viewLifecycleOwner,
            { response ->

                if (response?.meta?.status == true && isManager) {
                    // success
                    binding?.tvShift?.isVisible = true
                    binding?.lyMyTeam?.isVisible = true
                    teamattend = response
                    teamsToday(response)

                } else {
                    binding?.lyMyTeam?.isVisible = false
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
                if (isLoading) {
                    showDialog()
                } else {
                    hideDialog()
                }
            })


        homeViewModel.homeError.observe(viewLifecycleOwner, EventObserver { message ->
            requireContext().showErrorMsg(message)
        })
        homeViewModel.refreshing.observe(viewLifecycleOwner) { busy ->
            binding?.homeRefreshProgress?.isVisible = busy
        }
        binding?.tvTodayTitle?.setOnClickListener { homeViewModel.getDashboardInformation(force = true) }

        // Call the NTP time method in a coroutine
        viewLifecycleOwner.lifecycleScope.launch {
            ntpTimeString = getNtpTimeFormatted()
            Log.d("NTP", ntpTimeString.orEmpty())
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
        binding?.ivMenu?.setOnClickListener { (requireActivity() as MainActivity).openDrawer() }
        binding?.ivNotification?.setOnClickListener { AppNavigator.navigateToNotifications() }
        listOfNotNull(binding?.tvEmployeName, binding?.tvEmployeDesignation, binding?.tvEmployeId).forEach { profileView ->
            profileView.setOnClickListener { AppNavigator.navigateToProfile() }
            profileView.isFocusable = true
        }

        AppState.unreadNotifications.observe(viewLifecycleOwner) { count ->
            val badge = binding?.tvNotificationBadge ?: return@observe
            badge.isVisible = (count ?: 0) > 0
            badge.text = if ((count ?: 0) > 99) "99+" else (count ?: 0).toString()
        }
        notificationsViewModel.refreshUnreadCount()

//        targetLocList.add(BranchDataResponse(LocationFilter.NTC_OFFICE.value, 31.5494, 74.3333))
//        targetLocList.add(
//            BranchDataResponse(
//                LocationFilter.NASTP_OFFICE.value,
//                targetLatitude,
//                targetLongitude
//            )
//        )


        setCurrentLocationText()

        binding?.btnCheckIn?.setOnClickListener {
            if (!requireAttendanceInternet()) return@setOnClickListener
            if (checkInInfoResponse == null) {
                requireContext().showErrorMsg(getString(R.string.home_attendance_loading))
                return@setOnClickListener
            }
            val action = if (isCheckIn) LeaveStatus.CHECKIN.value else LeaveStatus.CHECKOUT.value
            if (homeLocation?.ready(action) == true) showAttendanceDialog(action)
        }
    }
    override fun onResume() {
        super.onResume()
        homeViewModel.getDashboardInformation()
    }

    override fun onDestroyView() {
        renderedManager = null
        hideDialog()
        internetDialog?.dismiss()
        internetDialog = null
        homeLocation = null
        _binding = null
        super.onDestroyView()
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

    fun showAttendanceDialog(action: String) {
        val title = "Confirm Action"
        val description = when (action) {
            "Check In" -> "Are you sure you want to 'Check In'? This will mark your attendance."
            "Check Out" -> "Are you sure you want to 'Check Out' ? This will complete your attendance."
            else -> "Are you sure you want to proceed?"
        }

        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(description)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                submitPunch()
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /** Sends the punch. The check-in widget only flips after the server confirms (dashboard reload). */
    private fun submitPunch() {
        if (!requireAttendanceInternet()) return
        if (checkInInfoResponse == null) {
            requireContext().showErrorMsg(getString(R.string.home_attendance_loading))
            return
        }
        val action = if (isCheckIn) LeaveStatus.CHECKIN.value else LeaveStatus.CHECKOUT.value
        if (homeLocation?.ready(action) != true) return
        setCurrentLocationText()
        val where = loc
        if (where == null) {
            requireContext().showErrorMsg(getString(R.string.home_location_unresolved))
            return
        }
        val type = if (isCheckIn) CheckRequestFilter.IN.name else CheckRequestFilter.OUT.name
        homeViewModel.postCheckIn(CheckInRequest().apply {
            this.log_type = type
            this.date_time = punchTimestamp()
            this.location = where
            this.request_status = LeaveStatus.APPROVED.value
            this.attendance_reason = "Punch from application"
        })
    }

    /** Requires working internet, including validation beyond merely being connected to Wi-Fi. */
    private fun requireAttendanceInternet(): Boolean {
        val connectivity = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) return true

        if (internetDialog?.isShowing != true) {
            internetDialog = AlertDialog.Builder(requireContext())
                .setTitle(R.string.home_internet_title)
                .setMessage(R.string.home_internet_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        return false
    }

    /** NTP time when it was fetched, otherwise the phone clock (never an error string). */
    private fun punchTimestamp(): String {
        val ntp = ntpTimeString
        if (!ntp.isNullOrBlank() && TIMESTAMP.matches(ntp)) return ntp
        return SimpleDateFormat(AppConst.ATTENDANCE_DATE_FORMAT, Locale.US).format(Date())
    }

    private fun checkOutAnimate() {
        frontAnimation.setTarget(binding?.lyCheckOut)
        backAnimation.setTarget(binding?.lyCheckIn)
        backAnimation.start()
        frontAnimation.start()
    }

    private fun checkInAnimate() {
        frontAnimation.setTarget(binding?.lyCheckIn)
        backAnimation.setTarget(binding?.lyCheckOut)
        frontAnimation.start()
        backAnimation.start()
    }

    /** "--" stays "--"; otherwise the server clock text without seconds, in the phone's 12/24 h style. */
    private fun clockOrDash(raw: String?): String {
        val text = raw.valueQualifier()
        return if (text == "--") text else WsFormat.clockText(requireContext(), text)
    }

    private fun checkInInfoPopulate(checkInInfo: CheckInInfoResponse) {

        binding?.tvLocation?.text = checkInInfo.location.valueQualifier()

        if (checkInInfo.is_check_in_button == false && checkInInfo.is_check_out_button == false) {
            // Day is complete: show a dimmed "Completed" button. Stop any flip animation first, it leaves
            // the button face transparent (alpha 0) which showed up as an empty white square.
            if (::frontAnimation.isInitialized) frontAnimation.cancel()
            if (::backAnimation.isInitialized) backAnimation.cancel()
            binding?.btnCheckIn?.isEnabled = false
            binding?.btnCheckIn?.alpha = 0.55f
            binding?.lyCheckIn?.apply {
                alpha = 1f
                rotationY = 0f
                visibility = View.VISIBLE
            }
            binding?.lyCheckOut?.visibility = View.GONE
            binding?.tvCheckInStatus?.setText(R.string.home_punch_done)

            binding?.tvCheckInTxt?.text = clockOrDash(checkInInfo.check_in)
            binding?.tvCheckOutTxt?.text = clockOrDash(checkInInfo.check_out)


        } else {
            binding?.btnCheckIn?.isEnabled = true
            binding?.btnCheckIn?.alpha = 1f
            binding?.tvCheckInStatus?.setText(R.string.check_in)

            if (checkInInfo.is_check_in_button == true && checkInInfo.is_check_out_button == true) {
                checkOutAnimate()
                binding?.lyCheckIn?.visibility = View.VISIBLE
                binding?.lyCheckOut?.visibility = View.GONE

                isCheckIn = true
            } else {
                if (checkInInfo.is_check_in_button == true) {
                    checkOutAnimate()
                    isCheckIn = true
                    binding?.lyCheckIn?.visibility = View.VISIBLE
                    binding?.lyCheckOut?.visibility = View.GONE

                    binding?.tvCheckInTxt?.text = clockOrDash(checkInInfo.check_in)

                } else if (checkInInfo.is_check_out_button == true) {
                    checkInAnimate()
                    binding?.tvCheckInTxt?.text = clockOrDash(checkInInfo.check_in)
                    isCheckIn = false
                    binding?.lyCheckIn?.visibility = View.GONE
                    binding?.lyCheckOut?.visibility = View.VISIBLE
                    binding?.tvCheckOutTxt?.text = clockOrDash(checkInInfo.check_out)


                }
            }

        }
    }

    private fun setCurrentLocationText() {
        loc = null
        binding?.tvCurrentLocs?.setText(if (locationFetching) R.string.home_location_fetching_short else R.string.home_location_find)
        val here = currentLocation ?: return
        if (targetLocList.none { it.latitude != null && it.longitude != null }) return

        val office = targetLocList.firstOrNull { branch ->
            val lat = branch.latitude
            val lng = branch.longitude
            lat != null && lng != null &&
                isLocationWithinRadius(here.latitude, here.longitude, lat, lng, radiusInMeters)
        }
        if (office != null) {
            binding?.tvCurrentLocs?.text = office.name
            loc = LocationFilter.OFFICE.value
        } else {
            binding?.tvCurrentLocs?.text = LocationFilter.WHF.value
            loc = LocationFilter.WHF.value
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
        if (renderedManager == isManager) return
        renderedManager = isManager

        gridList = arrayListOf(
            Modules(
                id = 0,
                name = HomeMenu.Attendance.gridName,
                description = HomeMenu.Attendance.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.attend_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_atten)
            ),
            Modules(
                id = 1,
                name = HomeMenu.Request.gridName,
                description = HomeMenu.Request.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.request_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_req)
            ),
            Modules(
                id = 2,
                name = HomeMenu.Leaves.gridName,
                description = HomeMenu.Leaves.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.leaves_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_leaves)

            ),
            Modules(
                id = 5,
                name = HomeMenu.Expense.gridName,
                description = HomeMenu.Expense.description,
                color = ContextCompat.getDrawable(requireContext(), R.drawable.expense_gradient),
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_expe)
            )
        )
        if (isManager) {

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
                            AppNavigator.navigateToMyAttendanceDetail()
                        }

                        HomeMenu.Leaves.gridName -> {
                            AppNavigator.navigateToLeaves()
                        }

                        HomeMenu.Request.gridName -> {
                            AppNavigator.navigateToRequestsOverview()
                        }

                        HomeMenu.Approval.gridName -> {
                            AppNavigator.navigateToApprovals()
                        }

                        HomeMenu.Expense.gridName -> {
                            AppNavigator.navigateToExpense()
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
        binding?.profileImg?.setAvatar(employProfile.image, employProfile.employee_name)


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

        private val TIMESTAMP = Regex("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")
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
