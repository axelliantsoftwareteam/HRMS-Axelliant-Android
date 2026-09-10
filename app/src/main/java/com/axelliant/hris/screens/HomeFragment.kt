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
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.isActive
import com.axelliant.hris.extention.showShimmer
import com.axelliant.hris.extention.hideShimmer
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.axelliant.hris.R

import com.axelliant.hris.adapter.ModulesAdapter
import com.axelliant.hris.base.BaseFragment
import com.axelliant.hris.bottomSheet.TodayTeamAttendanceDetailBottomSheet
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.core.AppDrawerAction
import com.axelliant.hris.core.auth.GlobalLogoutCoordinator
import com.axelliant.hris.core.contracts.navigation.WorkspaceKey
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentHomeBinding
import com.axelliant.hris.databinding.ItemAppDrawerMenuBinding
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
import com.axelliant.hris.model.dashboard.CheckInInfoResponse
import com.axelliant.hris.model.dashboard.EmployProfile
import com.axelliant.hris.model.login.CheckInRequest
import com.axelliant.hris.model.todayTeam.EmployTeamProfile
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.viewmodel.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.databinding.LayoutTodayCardContentBinding
import com.axelliant.hris.features.dashboard.presentation.AppDrawerMenuItem
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlin.math.abs
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone


@AndroidEntryPoint
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
    private var isCheckInButtonEnabled: Boolean = true

    //animations
    private var statusEchoAnimatorSet: android.animation.AnimatorSet? = null
    private var statusEchoAnimatorSet2: android.animation.AnimatorSet? = null

    //fab state
    private var isFabCollapsed = false
    private var isDraggingFab = false
    private var isFabRightEdge = true
    private var fabWiggleJob: Job? = null
    private var autoCollapseJob: Job? = null
    private var fabBubbleJob: Job? = null


    // Create an ArrayList to store the converted time strings
    private var targetLocList = ArrayList<BranchDataResponse>()
    private var employeeTodayList = ArrayList<EmployTeamProfile>()
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

    //Pop scope

    private var backPressedOnce = false
    private var backPressedResetJob: Job? = null
    private var exitBackCallback: OnBackPressedCallback? = null


    private val binding get() = _binding
    private var _todayCardBinding: LayoutTodayCardContentBinding? = null
    private val todayCardBinding get() = _todayCardBinding

    private var currentLocation: Location? = null
    private var checkInInfoResponse: CheckInInfoResponse? = null
    private lateinit var locationManager: LocationManager

    private val homeViewModel: HomeViewModel by viewModels()

    @Inject
    lateinit var workspaceSessionProvider: WorkspaceSessionProvider
    @Inject
    lateinit var globalLogoutCoordinator: GlobalLogoutCoordinator

    private var drawerBackCallback: OnBackPressedCallback? = null
    private var selectedDrawerAction: AppDrawerAction? = null
    private val drawerMenuBindings = mutableMapOf<AppDrawerAction, ItemAppDrawerMenuBinding>()
    private var drawerTouchStartX = 0f
    private var drawerTouchStartY = 0f
    private var allowedDrawerActions: Set<AppDrawerAction> = AppDrawerAction.entries.toSet()
    private var businessModulesAdapter: ModulesAdapter? = null
    private var hasHrisHomeAccess: Boolean = true
    private var workingTimeJob: Job? = null
    private var currentShiftStartTime: Date? = null
    private var currentShiftEndTime: Date? = null
    private val drawerScrim: View?
        get() = binding?.root?.findViewById(R.id.drawerScrim)
    private val appDrawer: View?
        get() = binding?.root?.findViewById(R.id.appDrawer)
    private val drawerMenuContainer: LinearLayout?
        get() = binding?.root?.findViewById(R.id.drawerMenuContainer)
    private val drawerHomeContainer: LinearLayout?
        get() = binding?.root?.findViewById(R.id.drawerHomeContainer)
    private val drawerHrisMenuContainer: LinearLayout?
        get() = binding?.root?.findViewById(R.id.drawerHrisMenuContainer)
    private val drawerCommonMenuContainer: LinearLayout?
        get() = binding?.root?.findViewById(R.id.drawerCommonMenuContainer)
    private val drawerFooterMenuContainer: LinearLayout?
        get() = binding?.root?.findViewById(R.id.drawerFooterMenuContainer)



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
        setupExitOnBackPress()
        setupDraggableFab()
        setupInternalAppsDrawer()
        startStatusEchoAnimation()
        setupLogoutAction()
        observeDrawerPermissions()
        homeViewModel.loadDrawerPermissions()
        hasHrisHomeAccess = workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)

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
        if (hasHrisHomeAccess) {
            activityResultLauncher.launch(appPerms)
        }

        if (employProfileResponse == null && hasHrisHomeAccess) {
            binding?.shimmerLayout?.showShimmer(binding?.contentGroup!!)
        }
        if (hasHrisHomeAccess) {
            homeViewModel.getDashboardInformation()
        } else {
            renderInternalAppsOnlyDashboard()
            renderHomePermissionSections()
        }
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

                    response.shift_detail?.let { dashBoardShiftPopulate(it) }
                    checkInInfoPopulate(response.checkin_info!!)
                    checkInInfoResponse = response.checkin_info

                    if (response.branch_data != null) {
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
                    if (workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)) {
                        homeViewModel.getDashboardInformation()
                    }

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
                        teamattend = response
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
        binding?.askAiFloatingButton?.setOnClickListener {
            if (!workspaceSessionProvider.hasValidSession(WorkspaceKey.INTERNAL_APPS)) {
                requireContext().showErrorMsg("Internal Apps session is not available.")
                return@setOnClickListener
            }
            openInternalAppsDestination(R.id.iaAgentConsoleFragment)
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
        _todayCardBinding = binding?.todayCardContent
        todayCardBinding?.btnCheckIn?.setOnClickListener {
            onCheckInButtonClicked()
        }
        updateCheckInButton()

        binding?.askAiFloatingButton?.apply {
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setRoundRect(
                        0, 0, view.width, view.height,
                        resources.getDimension(R.dimen.ds_radius_xl)
                    )
                }
            }
        }
    }

    private fun setupLogoutAction() {
        binding?.ivNotification?.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.logout_message))
                .setTitle(getString(R.string.info))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    logoutAndOpenLogin()
                }
                .setNegativeButton(getString(R.string.no)) { _, _ -> }
                .show()
        }
    }

    private fun renderInternalAppsOnlyDashboard() {
        binding?.shimmerLayout?.stopShimmer()
        binding?.shimmerLayout?.isVisible = false
        binding?.headerBg?.isVisible = true
        binding?.lyInfo?.isVisible = true
        binding?.cardMain?.isVisible = false
        binding?.tvEmployeName?.text = getString(R.string.entry_internal_apps)
        binding?.tvEmployeDesignation?.text = getString(R.string.drawer_navigation)
        binding?.tvEmployeId?.text = getString(R.string.menu)
    }

    private fun setupExitOnBackPress() {
        exitBackCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedOnce) {
                    requireActivity().finish()
                    return
                }
                backPressedOnce = true
                com.google.android.material.snackbar.Snackbar.make(
                    binding?.root ?: return,
                    getString(R.string.press_back_again_to_exit),
                    com.google.android.material.snackbar.Snackbar.LENGTH_SHORT
                ).show()

                backPressedResetJob?.cancel()
                backPressedResetJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(2000L)
                    backPressedOnce = false
                }
            }
        }.also { callback ->
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupInternalAppsDrawer() {
        drawerBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeDrawer()
            }
        }.also { callback ->
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }

        binding?.ivMenu?.setOnClickListener { openDrawer() }
        drawerScrim?.setOnClickListener { closeDrawer() }
        appDrawer?.setOnTouchListener { _, event -> handleDrawerTouch(event) }

        drawerMenuBindings.clear()
        drawerHomeContainer?.removeAllViews()
        drawerHrisMenuContainer?.removeAllViews()
        drawerMenuContainer?.removeAllViews()
        drawerCommonMenuContainer?.removeAllViews()
        drawerFooterMenuContainer?.removeAllViews()
        bindDrawerProfileHeader()
        drawerHomeContainer?.let { bindDrawerMenu(it, homeDrawerItems()) }
        drawerHrisMenuContainer?.let { bindDrawerMenu(it, hrisDrawerItems()) }
        drawerMenuContainer?.let { bindDrawerMenu(it, primaryDrawerItems(allowedDrawerActions)) }
        drawerCommonMenuContainer?.let { bindDrawerMenu(it, commonDrawerItems()) }
        drawerFooterMenuContainer?.let { bindDrawerMenu(it, footerDrawerItems()) }
        updateDrawerSectionVisibility()
        if (selectedDrawerAction == null) {
            selectedDrawerAction = AppDrawerAction.Home
        }
        refreshDrawerSelection()
    }

    private fun bindDrawerProfileHeader() {
        binding?.root?.findViewById<TextView>(R.id.drawerUserName)?.text =
            employProfileResponse?.employee_name ?: getString(R.string.drawer_user_name)
        binding?.root?.findViewById<TextView>(R.id.drawerUserRole)?.text =
            employProfileResponse?.designation ?: getString(R.string.drawer_user_role)
        binding?.root?.findViewById<TextView>(R.id.drawerUserEmpId)?.text =
            employProfileResponse?.custom_employee_code?.let { "Emp ID: $it" } ?: "Emp ID: --"
        binding?.root?.findViewById<ImageView>(R.id.drawerProfileImage)?.let { imageView ->
            imageView.setUrlImage(employProfileResponse?.image, requireContext())
        }
        binding?.root?.findViewById<View>(R.id.drawerViewProfile)?.setOnClickListener {
            handleDrawerItemClick(AppDrawerAction.Profiles)
        }
    }

    private fun updateDrawerSectionVisibility() {
        val hasHrisAccess = workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)
        val hasBusinessAccess = primaryDrawerItems(allowedDrawerActions).isNotEmpty()
        binding?.root?.findViewById<View>(R.id.drawerHrisLabel)?.isVisible = hasHrisAccess
        drawerHrisMenuContainer?.isVisible = hasHrisAccess
        binding?.root?.findViewById<View>(R.id.drawerBusinessDivider)?.isVisible = hasBusinessAccess
        binding?.root?.findViewById<View>(R.id.drawerBusinessLabel)?.isVisible = hasBusinessAccess
        drawerMenuContainer?.isVisible = hasBusinessAccess
    }

    private fun bindDrawerMenu(
        container: LinearLayout,
        items: List<AppDrawerMenuItem>
    ) {
        items.forEach { item ->
            val itemBinding = ItemAppDrawerMenuBinding.inflate(layoutInflater, container, false)
            itemBinding.drawerMenuTitle.setText(item.titleRes)
            itemBinding.drawerMenuIcon.setImageResource(item.iconRes)
            itemBinding.drawerMenuRoot.setOnClickListener {
                handleDrawerItemClick(item.action)
            }
            drawerMenuBindings[item.action] = itemBinding
            container.addView(itemBinding.root)
        }
    }

    private fun refreshDrawerSelection() {
        selectedDrawerAction?.let { selectedAction ->
            if (selectedAction !in visibleDrawerActions()) {
                selectedDrawerAction = AppDrawerAction.Home
            }
        }
        drawerMenuBindings.forEach { (action, itemBinding) ->
            val isSelected = action == selectedDrawerAction
            val textColor = when {
                isSelected -> R.color.ds_primary
                else -> R.color.ds_text_primary
            }
            val iconColor = when {
                isSelected -> R.color.ds_primary
                action == AppDrawerAction.Settings || action == AppDrawerAction.Logout -> R.color.ds_text_primary
                else -> R.color.ds_primary
            }
            itemBinding.drawerMenuRoot.setBackgroundResource(
                if (isSelected) {
                    R.drawable.bg_app_drawer_item_selected
                } else {
                    R.drawable.bg_app_drawer_item_default
                }
            )
            itemBinding.drawerMenuTitle.setTextColor(ContextCompat.getColor(requireContext(), textColor))
            itemBinding.drawerMenuIcon.setColorFilter(ContextCompat.getColor(requireContext(), iconColor))
        }
    }

    private fun visibleDrawerActions(): Set<AppDrawerAction> {
        return buildSet {
            addAll(homeDrawerItems().map { it.action })
            if (workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)) {
                addAll(hrisDrawerItems().map { it.action })
            }
            addAll(primaryDrawerItems(allowedDrawerActions).map { it.action })
            addAll(commonDrawerItems().map { it.action })
            addAll(footerDrawerItems().map { it.action })
        }
    }

    private fun openDrawer() {
        drawerScrim?.isVisible = true
        appDrawer?.isVisible = true
        drawerScrim?.bringToFront()
        appDrawer?.bringToFront()
        drawerBackCallback?.isEnabled = true
        val drawerWidth = resources.getDimensionPixelSize(R.dimen.drawer_width).toFloat()
        appDrawer?.translationX = -drawerWidth
        appDrawer?.animate()
            ?.translationX(0f)
            ?.setDuration(DRAWER_ANIMATION_DURATION_MS)
            ?.start()
    }

    private fun closeDrawer(onClosed: (() -> Unit)? = null) {
        drawerBackCallback?.isEnabled = false
        val drawerWidth = resources.getDimensionPixelSize(R.dimen.drawer_width).toFloat()
        appDrawer?.animate()
            ?.translationX(-drawerWidth)
            ?.setDuration(DRAWER_ANIMATION_DURATION_MS)
            ?.withEndAction {
                appDrawer?.isVisible = false
                drawerScrim?.isVisible = false
                onClosed?.invoke()
            }
            ?.start()
    }

    private fun handleDrawerItemClick(action: AppDrawerAction) {
        val requiresInternalAppsSession = action in setOf(
            AppDrawerAction.Products,
            AppDrawerAction.Quotes,
            AppDrawerAction.SaleOrders,
            AppDrawerAction.PurchaseOrders,
            AppDrawerAction.Subscriptions,
            AppDrawerAction.Settings
        )
        if (requiresInternalAppsSession &&
            !workspaceSessionProvider.hasValidSession(WorkspaceKey.INTERNAL_APPS)
        ) {
            requireContext().showErrorMsg("Internal Apps session is not available.")
            return
        }

        if (action != AppDrawerAction.Logout) {
            selectedDrawerAction = action
            refreshDrawerSelection()
        }

        closeDrawer {
            if (!isAdded) return@closeDrawer
            when (action) {
                AppDrawerAction.Home -> Unit
                AppDrawerAction.AgentConsole -> openInternalAppsDestination(R.id.iaAgentConsoleFragment)
                AppDrawerAction.Calendar -> openInternalAppsDestination(R.id.iaCalendarFragment)
                AppDrawerAction.Attendance -> AppNavigator.navigateToAttendanceStats()
                AppDrawerAction.Requests -> AppNavigator.navigateToRequest()
                AppDrawerAction.Leaves -> AppNavigator.navigateToLeaves()
                AppDrawerAction.CheckInRequests -> AppNavigator.navigateToCheckInFragment()
                AppDrawerAction.Expenses -> AppNavigator.navigateToExpenseFragment()
                AppDrawerAction.DocumentVault -> AppNavigator.navigateToDocumentManageFragment()
                AppDrawerAction.ResourceManagement -> AppNavigator.navigateToResourceManageFragment()
                AppDrawerAction.Products -> openInternalAppsDestination(R.id.iaProductsFragment)
                AppDrawerAction.Quotes -> openInternalAppsDestination(R.id.iaQuotesFragment)
                AppDrawerAction.SaleOrders -> openInternalAppsDestination(R.id.iaSaleOrdersFragment)
                AppDrawerAction.PurchaseOrders -> openInternalAppsDestination(R.id.iaPurchaseOrdersFragment)
                AppDrawerAction.Subscriptions -> openInternalAppsDestination(R.id.iaSubscriptionsFragment)
                AppDrawerAction.Profiles -> openInternalAppsDestination(R.id.iaProfilesFragment)
                AppDrawerAction.Settings -> openInternalAppsDestination(R.id.iaSettingsFragment)
                AppDrawerAction.Logout -> showLogoutConfirmationDialog()
            }
        }
    }

    private fun observeDrawerPermissions() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                homeViewModel.drawerPermissionState.collect { state ->
                    if (state is UiState.Success) {
                        allowedDrawerActions = state.data + footerActions
                        setupInternalAppsDrawer()
                        renderHomePermissionSections()
                    }
                }
            }
        }
    }

    private fun openInternalAppsDestination(destinationId: Int) {
        InternalAppsNavigator.open(findNavController(), destinationId)
    }

    private fun renderHomePermissionSections() {
        val hasHrisAccess = workspaceSessionProvider.hasValidSession(WorkspaceKey.HRIS)
        val businessActions = primaryDrawerItems(allowedDrawerActions).map { it.action }
        val hasBusinessAccess = businessActions.isNotEmpty()
        hasHrisHomeAccess = hasHrisAccess

        binding?.tvTodayTitle?.isVisible = hasHrisAccess
        binding?.tvCurrentLocs?.isVisible = hasHrisAccess
        binding?.composeTodayCard?.isVisible = hasHrisAccess
        binding?.cardNote?.isVisible = hasHrisAccess
        binding?.tvShift?.isVisible = hasHrisAccess && isManager
        binding?.lyMyTeam?.isVisible = hasHrisAccess && isManager
        if (!hasHrisAccess) {
            binding?.lyMyShift?.isVisible = false
        }

        binding?.tvBusinessModules?.isVisible = hasBusinessAccess
        binding?.tvBusinessViewAll?.isVisible = hasBusinessAccess
        binding?.rvBusinessModule?.isVisible = hasBusinessAccess
        binding?.tvWeekly?.isVisible = hasHrisAccess
        binding?.rvModule?.isVisible = hasHrisAccess
        if (hasBusinessAccess) {
            bindBusinessModules(businessActions)
        }
    }

    private fun bindBusinessModules(actions: List<AppDrawerAction>) {
        val modules = actions.mapNotNull { action -> action.toBusinessModule() }
        val adapter = ModulesAdapter(modules, object : AdapterItemClick {
            override fun onItemClick(customObject: Any, position: Int) {
                when ((customObject as Modules).id) {
                    BUSINESS_MODULE_SALES_ORDERS -> openInternalAppsDestination(R.id.iaSaleOrdersFragment)
                    BUSINESS_MODULE_QUOTES -> openInternalAppsDestination(R.id.iaQuotesFragment)
                    BUSINESS_MODULE_PRODUCTS -> openInternalAppsDestination(R.id.iaProductsFragment)
                    BUSINESS_MODULE_PURCHASE_ORDERS -> openInternalAppsDestination(R.id.iaPurchaseOrdersFragment)
                    BUSINESS_MODULE_SUBSCRIPTIONS -> openInternalAppsDestination(R.id.iaSubscriptionsFragment)
                }
            }
        })
        businessModulesAdapter = adapter
        binding?.rvBusinessModule?.layoutManager = GridLayoutManager(requireContext(), 2)
        binding?.rvBusinessModule?.adapter = adapter
        binding?.rvBusinessModule?.isNestedScrollingEnabled = false
    }

    private fun AppDrawerAction.toBusinessModule(): Modules? {
        return when (this) {
            AppDrawerAction.SaleOrders -> Modules(
                id = BUSINESS_MODULE_SALES_ORDERS,
                name = getString(R.string.drawer_sale_orders),
                description = "Manage sales orders",
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_sales_orders)
            )
            AppDrawerAction.Quotes -> Modules(
                id = BUSINESS_MODULE_QUOTES,
                name = getString(R.string.drawer_quotes),
                description = "Create and track quotes",
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_quotes)
            )
            AppDrawerAction.Products -> Modules(
                id = BUSINESS_MODULE_PRODUCTS,
                name = getString(R.string.products),
                description = "Manage product catalog",
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_products)
            )
            AppDrawerAction.PurchaseOrders -> Modules(
                id = BUSINESS_MODULE_PURCHASE_ORDERS,
                name = getString(R.string.drawer_purchase_orders),
                description = "Manage purchase orders",
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_purchase_orders)
            )
            AppDrawerAction.Subscriptions -> Modules(
                id = BUSINESS_MODULE_SUBSCRIPTIONS,
                name = getString(R.string.drawer_subscriptions),
                description = "Manage subscriptions",
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_dashboard_box)
            )
            else -> null
        }
    }

    private fun handleDrawerTouch(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                drawerTouchStartX = event.rawX
                drawerTouchStartY = event.rawY
                false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - drawerTouchStartX
                val deltaY = event.rawY - drawerTouchStartY
                val swipeThreshold = ViewConfiguration.get(requireContext()).scaledTouchSlop * 4
                val isLeftSwipe = deltaX < -swipeThreshold && abs(deltaX) > abs(deltaY)
                if (isLeftSwipe) {
                    closeDrawer()
                    true
                } else {
                    false
                }
            }

            else -> false
        }
    }

    private fun logoutAndOpenLogin() {
        viewLifecycleOwner.lifecycleScope.launch {
            globalLogoutCoordinator.logout()
            if (!isAdded) return@launch
            findNavController().navigate(
                R.id.commonLoginFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.main_nav_graph, true)
                    .build()
            )
        }
    }

    private fun primaryDrawerItems(
        allowedActions: Set<AppDrawerAction> = allowedDrawerActions
    ) = allPrimaryDrawerItems.filter { it.action in allowedActions }

    private fun homeDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.drawer_home, R.drawable.ic_bt_home, AppDrawerAction.Home),
        AppDrawerMenuItem(R.string.agent_console_title, R.drawable.ic_ai_sparkles, AppDrawerAction.AgentConsole)
    )

    private fun hrisDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.drawer_attendance, R.drawable.ic_home_detail_clock, AppDrawerAction.Attendance),
        AppDrawerMenuItem(R.string.drawer_requests, R.drawable.ic_home_sales_orders, AppDrawerAction.Requests),
        AppDrawerMenuItem(R.string.leaves, R.drawable.ic_home_leave, AppDrawerAction.Leaves),
        AppDrawerMenuItem(R.string.drawer_check_in_requests, R.drawable.ic_home_check_requests, AppDrawerAction.CheckInRequests),
        AppDrawerMenuItem(R.string.drawer_expenses, R.drawable.ic_home_expenses, AppDrawerAction.Expenses),
        AppDrawerMenuItem(R.string.drawer_document_vault, R.drawable.ic_home_document_vault, AppDrawerAction.DocumentVault),
        AppDrawerMenuItem(R.string.drawer_resource_management, R.drawable.ic_home_resource_management, AppDrawerAction.ResourceManagement)
    )

    private val allPrimaryDrawerItems
        get() = listOf(
        AppDrawerMenuItem(R.string.drawer_sale_orders, R.drawable.ic_home_purchase_orders, AppDrawerAction.SaleOrders),
        AppDrawerMenuItem(R.string.drawer_quotes, R.drawable.ic_home_quotes, AppDrawerAction.Quotes),
        AppDrawerMenuItem(R.string.products, R.drawable.ic_home_products, AppDrawerAction.Products),
        AppDrawerMenuItem(R.string.drawer_purchase_orders, R.drawable.ic_home_sales_orders, AppDrawerAction.PurchaseOrders),
        AppDrawerMenuItem(R.string.drawer_subscriptions, R.drawable.ic_dashboard_box, AppDrawerAction.Subscriptions)
    )

    private val footerActions = setOf(AppDrawerAction.Settings, AppDrawerAction.Logout)

    private fun commonDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.drawer_profile, R.drawable.ic_home_check_requests, AppDrawerAction.Profiles),
        AppDrawerMenuItem(R.string.calendar_title, R.drawable.ic_calendar, AppDrawerAction.Calendar)
    )

    private fun footerDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.drawer_settings, R.drawable.ic_settings, AppDrawerAction.Settings),
        AppDrawerMenuItem(
            R.string.drawer_sign_out,
            R.drawable.ic_logout,
            AppDrawerAction.Logout
        )
    )
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
        todayCardBinding?.tvOfficeStatus?.text = checkInInfo.location.valueQualifier()
        bindAttendanceTimes(checkInInfo)
        bindWorkingTime(checkInInfo)

        if (checkInInfo.is_check_in_button == false && checkInInfo.is_check_out_button == false) {
            isCheckInButtonEnabled = false

        } else {
            isCheckInButtonEnabled = true

            if (checkInInfo.is_check_in_button == true && checkInInfo.is_check_out_button == true) {
                isCheckIn = true
            } else {
                if (checkInInfo.is_check_in_button == true) {
                    isCheckIn = true

                } else if (checkInInfo.is_check_out_button == true) {
                    isCheckIn = false
                }
            }
        }
        updateCheckInButton()
    }

    private fun bindAttendanceTimes(checkInInfo: CheckInInfoResponse) {
        todayCardBinding?.tvCheckInTxt?.text = "${checkInInfo.check_in.valueQualifier()} Check in"
        todayCardBinding?.tvCheckOutTxt?.text = "${checkInInfo.check_out.valueQualifier()} Shift end"
    }

    private fun bindWorkingTime(checkInInfo: CheckInInfoResponse) {
        workingTimeJob?.cancel()
        val checkInTime = parseAttendanceTime(checkInInfo.check_in)
        val isCurrentlyCheckedIn = checkInInfo.is_check_out_button == true
        val checkOutTime = if (isCurrentlyCheckedIn) {
            null
        } else {
            parseAttendanceTime(checkInInfo.check_out)
        }

        if (checkInTime == null) {
            todayCardBinding?.tvWorkingTime?.text = formatWorkingDuration(0L)
            updateShiftProgress(null)
            return
        }

        if (checkOutTime != null) {
            val duration = if (checkOutTime.time >= checkInTime.time) {
                checkOutTime.time - checkInTime.time
            } else {
                checkOutTime.time + ONE_DAY_IN_MILLIS - checkInTime.time
            }
            todayCardBinding?.tvWorkingTime?.text = formatWorkingDuration(duration)
            updateShiftProgress(checkOutTime)
            return
        }

        todayCardBinding?.tvWorkingTime?.text =
            formatWorkingDuration(System.currentTimeMillis() - checkInTime.time)
        updateShiftProgress(Date())
        workingTimeJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                todayCardBinding?.tvWorkingTime?.text =
                    formatWorkingDuration(System.currentTimeMillis() - checkInTime.time)
                updateShiftProgress(Date())
                delay(1000L)
            }
        }
    }
    private fun parseAttendanceTime(value: String?): Date? {
        val timeText = value?.trim().orEmpty()
        if (timeText.isBlank() || timeText == "--" || timeText.equals("null", ignoreCase = true)) {
            return null
        }

        val zone = TimeZone.getTimeZone("Asia/Karachi")

        val dateTimeFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm"
        )
        dateTimeFormats.forEach { pattern ->
            parseDate(timeText, pattern)?.let { return it }
        }

        val today = Calendar.getInstance(zone)
        val timeFormats = listOf("HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a")
        timeFormats.forEach { pattern ->
            parseDate(timeText, pattern)?.let { parsed ->
                return Calendar.getInstance(zone).apply {
                    time = parsed
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                    set(Calendar.MONTH, today.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
                }.time
            }
        }
        return null
    }
    private fun parseDate(value: String, pattern: String): Date? {
        return runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
                timeZone = TimeZone.getTimeZone("Asia/Karachi") // match backend/office timezone
            }.parse(value)
        }.getOrNull()
    }


    /*private fun parseAttendanceTime(value: String?): Date? {
        val timeText = value?.trim().orEmpty()
        if (timeText.isBlank() || timeText == "--" || timeText.equals("null", ignoreCase = true)) {
            return null
        }

        val dateTimeFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy HH:mm"
        )
        dateTimeFormats.forEach { pattern ->
            parseDate(timeText, pattern)?.let { return it }
        }

        val today = Calendar.getInstance()
        val timeFormats = listOf("HH:mm:ss", "HH:mm", "hh:mm:ss a", "hh:mm a")
        timeFormats.forEach { pattern ->
            parseDate(timeText, pattern)?.let { parsed ->
                return Calendar.getInstance().apply {
                    time = parsed
                    set(Calendar.YEAR, today.get(Calendar.YEAR))
                    set(Calendar.MONTH, today.get(Calendar.MONTH))
                    set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))
                }.time
            }
        }
        return null
    }

    private fun parseDate(value: String, pattern: String): Date? {
        return runCatching {
            SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }.parse(value)
        }.getOrNull()
    }*/

    private fun formatWorkingDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun updateShiftProgress(referenceTime: Date?) {
        val shiftStart = currentShiftStartTime
        val shiftEnd = currentShiftEndTime
        if (referenceTime == null || shiftStart == null || shiftEnd == null) {
            todayCardBinding?.progressShift?.progress = 0
            return
        }

        val normalizedEnd = if (shiftEnd.time >= shiftStart.time) {
            shiftEnd.time
        } else {
            shiftEnd.time + ONE_DAY_IN_MILLIS
        }
        val normalizedReference = if (referenceTime.time >= shiftStart.time) {
            referenceTime.time
        } else {
            referenceTime.time + ONE_DAY_IN_MILLIS
        }
        val shiftDuration = (normalizedEnd - shiftStart.time).coerceAtLeast(1L)
        val elapsed = (normalizedReference - shiftStart.time).coerceIn(0L, shiftDuration)
        todayCardBinding?.progressShift?.progress = ((elapsed * 100L) / shiftDuration).toInt()
    }

    private fun updateCheckInButton() {
        todayCardBinding?.btnCheckIn?.text =
            if (isCheckIn) getString(R.string.check_in) else getString(R.string.check_out)
        todayCardBinding?.btnCheckIn?.isEnabled = isCheckInButtonEnabled
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
        gridList = arrayListOf(
            Modules(
                id = 0,
                name = HomeMenu.Attendance.gridName,
                description = HomeMenu.Attendance.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_attendance)
            ),
            Modules(
                id = 1,
                name = HomeMenu.Request.gridName,
                description = HomeMenu.Request.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_sales_orders),
                progressValue = 2,
                progressMax = 5
            ),
            Modules(
                id = 2,
                name = HomeMenu.Leaves.gridName,
                description = HomeMenu.Leaves.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_leave),
                progressValue = 4,
                progressMax = 10

            ),
            Modules(
                id = 4,
                name = HomeMenu.CheckIN.gridName,
                description = HomeMenu.CheckIN.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_check_requests)


            ),
            Modules(
                id = 5,
                name = HomeMenu.Expense.gridName,
                description = HomeMenu.Expense.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_expenses)
            ),
            Modules(
                id = 6,
                name = HomeMenu.DocumentManagement.gridName,
                description = HomeMenu.DocumentManagement.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_document_vault)
            ),
            Modules(
                id = 7,
                name = HomeMenu.ResourceManagement.gridName,
                description = HomeMenu.ResourceManagement.description,
                color = null,
                drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_home_resource_management)
            ),
        )
        if (isManager) {
            homeViewModel.getTodayTeamInfo()
            gridList?.add(
                Modules(
                    id = 3,
                    name = HomeMenu.Approval.gridName,
                    description = HomeMenu.Approval.description,
                    color = null,
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
                            AppNavigator.navigateToLeaves()
                        }

                        HomeMenu.Request.gridName -> {
                            AppNavigator.navigateToRequest()
                        }

                        HomeMenu.Approval.gridName -> {
                            showDialog()
                            AppNavigator.navigateToApprovals()
                        }

                        HomeMenu.CheckIN.gridName -> {
                            AppNavigator.navigateToCheckInFragment()
                        }

                        HomeMenu.Expense.gridName -> {
                            AppNavigator.navigateToExpenseFragment()
                        }
                        HomeMenu.DocumentManagement.gridName -> {
                            AppNavigator.navigateToDocumentManageFragment()
                        }
                        HomeMenu.ResourceManagement.gridName -> {
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
        renderHomePermissionSections()


    }

    private fun dashBoardShiftPopulate(shiftData: ShiftData) {

        binding?.tvShiftNote?.text =
            "Your shift ${shiftData.name} is ${shiftData.location}"
        currentShiftStartTime = parseAttendanceTime(shiftData.actual_start)
        currentShiftEndTime = parseAttendanceTime(shiftData.actual_end)
        todayCardBinding?.tvShiftChip?.text = buildShiftChipText(shiftData)
        todayCardBinding?.tvShiftEndHint?.text = buildShiftEndHintText(shiftData)
        updateShiftProgress(Date())
    }

    private fun buildShiftChipText(shiftData: ShiftData): String {
        val shiftStart = shiftData.actual_start.valueQualifier()
        val shiftEnd = shiftData.actual_end.valueQualifier()
        return "Shift $shiftStart - $shiftEnd"
    }

    private fun buildShiftEndHintText(shiftData: ShiftData): String {
        return "Your shift ends at ${shiftData.actual_end.valueQualifier()}"
    }


    private fun dashBoardPopulate(employProfile: EmployProfile) {
        binding?.tvEmployeName?.text = employProfile.employee_name
        binding?.tvEmployeDesignation?.text = employProfile.designation
        binding?.tvEmployeId?.text = "Emp ID: ${employProfile.custom_employee_code}"
        binding?.profileImg?.setUrlImage(employProfile.image, requireContext())
        bindDrawerProfileHeader()


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


    private fun startStatusEchoAnimation() {
        val echoView1 = binding?.root?.findViewById<View>(R.id.status_dot_echo) ?: return
        val echoView2 = binding?.root?.findViewById<View>(R.id.status_dot_echo2)

        echoView1.post {
            if (!isAdded || _binding == null) return@post
            statusEchoAnimatorSet = buildEchoAnimator(echoView1, startDelay = 0L)
            statusEchoAnimatorSet?.start()

            echoView2?.let {
                statusEchoAnimatorSet2 = buildEchoAnimator(it, startDelay = 750L)
                statusEchoAnimatorSet2?.start()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableFab() {
        val fabContainer = binding?.askAiFloatingButton ?: return
        val fabOrb = binding?.askAiFloatingButton ?: return

        var dX = 0f
        var dY = 0f
        var downRawX = 0f
        var downRawY = 0f
        val clickSlop = ViewConfiguration.get(requireContext()).scaledTouchSlop

        fabOrb.setOnTouchListener { _, event ->
            val parent = fabContainer.parent as? ViewGroup ?: return@setOnTouchListener false

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    if (isFabCollapsed) {
                        return@setOnTouchListener false
                    }
                    isDraggingFab = false
                    dX = fabContainer.x - event.rawX
                    dY = fabContainer.y - event.rawY
                    downRawX = event.rawX
                    downRawY = event.rawY
                    parent.requestDisallowInterceptTouchEvent(true)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isFabCollapsed) return@setOnTouchListener false

                    val movedX = abs(event.rawX - downRawX)
                    val movedY = abs(event.rawY - downRawY)
                    if (movedX > clickSlop || movedY > clickSlop) isDraggingFab = true

                    var newX = event.rawX + dX
                    var newY = event.rawY + dY
                    newX = newX.coerceIn(0f, (parent.width - fabContainer.width).toFloat())
                    newY = newY.coerceIn(0f, (parent.height - fabContainer.height).toFloat())

                    fabContainer.x = newX
                    fabContainer.y = newY
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    parent.requestDisallowInterceptTouchEvent(false)

                    if (isFabCollapsed) {
                        if (event.actionMasked == MotionEvent.ACTION_UP) {
                            expandFab()
                        }
                        return@setOnTouchListener true
                    }

                    val wasDragging = isDraggingFab
                    isDraggingFab = false // reset immediately so auto-collapse isn't stuck blocked

                    if (!wasDragging) {
                        fabOrb.performClick()
                    } else {
                        // dropped after a drag — stays put, auto-collapses after a delay
                        scheduleAutoCollapse()
                    }
                    true
                }

                else -> false
            }
        }

        // on screen load: visible first, then auto-collapse after a delay
        fabContainer.post {
            if (isAdded && _binding != null) {
                scheduleAutoCollapse()
            }
        }
    }

    private fun scheduleAutoCollapse() {
        val fabContainer = binding?.askAiFloatingButton ?: return

        isFabCollapsed = false
        fabContainer.alpha = 1f

        autoCollapseJob?.cancel()
        autoCollapseJob = viewLifecycleOwner.lifecycleScope.launch {
            delay(1800L)
            if (!isAdded || _binding == null) return@launch
            if (isDraggingFab) return@launch
            val parent = fabContainer.parent as? ViewGroup ?: return@launch
            snapAndCollapseFab(parent)
        }
    }

    private fun snapAndCollapseFab(parent: ViewGroup) {
        val fabContainer = binding?.askAiFloatingButton ?: return
        val edgeX = (parent.width - fabContainer.width).toFloat()

        fabContainer.animate()
            .x(edgeX)
            .setDuration(200L)
            .withEndAction {
                collapseFab(isRightEdge = true)
            }
            .start()
    }

    private fun collapseFab(isRightEdge: Boolean, animate: Boolean = true) {
        val fabContainer = binding?.askAiFloatingButton ?: return
        val arrow = binding?.root?.findViewById<ImageView>(R.id.ivFabArrow)
        val parent = fabContainer.parent as? ViewGroup

        isFabCollapsed = true
        isFabRightEdge = isRightEdge

        val peekPx = 22 * resources.displayMetrics.density
        val hiddenOffset = fabContainer.width - peekPx

        val baseX = if (isRightEdge) {
            (parent?.width ?: 0) - fabContainer.width
        } else {
            0
        }.toFloat()

        val targetX = if (isRightEdge) baseX + hiddenOffset else baseX - hiddenOffset

        arrow?.apply {
            isVisible = true
            scaleX = if (isRightEdge) 1f else -1f
        }

        if (animate) {
            fabContainer.animate()
                .x(targetX)
                .alpha(0.9f)
                .setDuration(220L)
                .start()
        } else {
            fabContainer.x = targetX
            fabContainer.alpha = 0.9f
        }

        startFabWiggle()
    }

    private fun expandFab() {
        val fabContainer = binding?.askAiFloatingButton ?: return
        val arrow = binding?.root?.findViewById<ImageView>(R.id.ivFabArrow)
        val parent = fabContainer.parent as? ViewGroup ?: return

        autoCollapseJob?.cancel()
        isFabCollapsed = false
        stopFabWiggle()

        val edgeGap = 16 * resources.displayMetrics.density
        val targetX = (parent.width - fabContainer.width) - edgeGap

        arrow?.isVisible = false
        fabContainer.animate()
            .x(targetX)
            .alpha(1f)
            .setDuration(220L)
            .start()

        // re-collapse automatically if left untouched
        scheduleAutoCollapse()
    }

    private fun startFabWiggle() {
        fabWiggleJob?.cancel()
        val fabContainer = binding?.askAiFloatingButton ?: return

        fabWiggleJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                delay(3500L)
                if (!isFabCollapsed || _binding == null) continue
                playWiggle(fabContainer)
            }
        }
    }

    private fun stopFabWiggle() {
        fabWiggleJob?.cancel()
        fabWiggleJob = null
        binding?.askAiFloatingButton?.rotation = 0f
    }

    private fun playWiggle(view: View) {
        val pivotAdjust = if (isFabRightEdge) 1f else 0f
        view.pivotX = view.width * pivotAdjust
        view.pivotY = view.height / 2f

        val tiltAngle = if (isFabRightEdge) -12f else 12f

        android.animation.ObjectAnimator.ofFloat(
            view, "rotation", 0f, tiltAngle, 0f, tiltAngle * 0.6f, 0f
        ).apply {
            duration = 500L
            interpolator = android.view.animation.DecelerateInterpolator()
            start()
        }
    }


    private fun buildEchoAnimator(
        echoView: View,
        startDelay: Long
    ): android.animation.AnimatorSet {
        val scaleX = android.animation.ObjectAnimator.ofFloat(echoView, "scaleX", 1f, 2.6f)
        val scaleY = android.animation.ObjectAnimator.ofFloat(echoView, "scaleY", 1f, 2.6f)
        val alpha = android.animation.ObjectAnimator.ofFloat(echoView, "alpha", 0.55f, 0f)

        return android.animation.AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1500L
            this.startDelay = startDelay
            interpolator = android.view.animation.DecelerateInterpolator()
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    echoView.scaleX = 1f
                    echoView.scaleY = 1f
                    echoView.alpha = 0.55f
                    if (isAdded && _binding != null) {
                        this@apply.startDelay = 0L
                        start()
                    }
                }
            })
        }
    }

    private fun stopStatusEchoAnimation() {
        statusEchoAnimatorSet?.cancel()
        statusEchoAnimatorSet2?.cancel()
        statusEchoAnimatorSet = null
        statusEchoAnimatorSet2 = null
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
    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.info))
            .setMessage(getString(R.string.logout_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                logoutAndOpenLogin()
            }
            .setNegativeButton(getString(R.string.no)) { _, _ -> }
            .show()
    }
    override fun onDestroyView() {
        workingTimeJob?.cancel()
        workingTimeJob = null
        backPressedResetJob?.cancel()
        backPressedResetJob = null
        fabWiggleJob?.cancel()
        fabWiggleJob = null
        autoCollapseJob?.cancel()
        autoCollapseJob = null
        binding?.shimmerLayout?.stopShimmer()
        super.onDestroyView()
        stopStatusEchoAnimation()
        _binding = null
        _todayCardBinding = null
    }
    companion object {
        private val TAG = HomeFragment::class.java.simpleName
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val DRAWER_ANIMATION_DURATION_MS = 180L
        private const val BUSINESS_MODULE_PRODUCTS = 100
        private const val BUSINESS_MODULE_QUOTES = 101
        private const val BUSINESS_MODULE_SALES_ORDERS = 102
        private const val BUSINESS_MODULE_PURCHASE_ORDERS = 103
        private const val BUSINESS_MODULE_SUBSCRIPTIONS = 104
        private const val ONE_DAY_IN_MILLIS = 24 * 60 * 60 * 1000L
    }
}
