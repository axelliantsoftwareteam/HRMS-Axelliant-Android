package com.axelliant.hris.features.dashboard.presentation

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewConfiguration
import android.widget.LinearLayout
import java.text.NumberFormat
import java.util.Locale
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.axelliant.hris.R
import com.axelliant.hris.core.AppDrawerAction
import com.axelliant.hris.core.auth.GlobalLogoutCoordinator
import com.axelliant.hris.core.contracts.session.WorkspaceSessionProvider
import com.axelliant.hris.core.ui.BottomNavigationHost
import com.axelliant.hris.core.ui.UiState
import com.axelliant.hris.databinding.FragmentDashboardBinding
import com.axelliant.hris.databinding.ItemAppDrawerMenuBinding
import com.axelliant.hris.features.internalapps.navigation.InternalAppsNavigator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : Fragment() {
    @Inject
    lateinit var workspaceSessionProvider: WorkspaceSessionProvider
    @Inject
    lateinit var globalLogoutCoordinator: GlobalLogoutCoordinator

    private val viewModel: DashboardViewModel by viewModels()
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private var drawerBackCallback: OnBackPressedCallback? = null
    private var selectedDrawerAction = AppDrawerAction.Products
    private val drawerMenuBindings = mutableMapOf<AppDrawerAction, ItemAppDrawerMenuBinding>()
    private var drawerTouchStartX = 0f
    private var drawerTouchStartY = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedInstanceState?.getString(KEY_SELECTED_DRAWER_ACTION)?.let { actionName ->
            runCatching { AppDrawerAction.valueOf(actionName) }
                .getOrNull()
                ?.let { selectedDrawerAction = it }
        }
        setupDrawer()
        observeDashboard()
        viewModel.loadSummary()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_SELECTED_DRAWER_ACTION, selectedDrawerAction.name)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDrawer() {
        drawerBackCallback = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                closeDrawer()
            }
        }.also { callback ->
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, callback)
        }

        binding.menuButton.setOnClickListener { openDrawer() }
        binding.drawerScrim.setOnClickListener { closeDrawer() }
        binding.appDrawer.setOnTouchListener { _, event -> handleDrawerTouch(event) }

        drawerMenuBindings.clear()
        binding.drawerMenuContainer.removeAllViews()
        binding.drawerFooterMenuContainer.removeAllViews()
        bindDrawerMenu(binding.drawerMenuContainer, primaryDrawerItems())
        bindDrawerMenu(binding.drawerFooterMenuContainer, footerDrawerItems())
        refreshDrawerSelection()
    }

    private fun bindDrawerMenu(
        container: LinearLayout,
        items: List<AppDrawerMenuItem>
    ) {
        items.forEach { item ->
            val itemBinding = ItemAppDrawerMenuBinding.inflate(
                layoutInflater,
                container,
                false
            )
            bindDrawerMenuItem(itemBinding, item)
            drawerMenuBindings[item.action] = itemBinding
            container.addView(itemBinding.root)
        }
    }

    private fun bindDrawerMenuItem(
        itemBinding: ItemAppDrawerMenuBinding,
        item: AppDrawerMenuItem
    ) = with(itemBinding) {
        drawerMenuTitle.setText(item.titleRes)
        drawerMenuIcon.setImageResource(item.iconRes)
        drawerMenuRoot.setOnClickListener {
            handleDrawerItemClick(item.action)
        }
    }

    private fun refreshDrawerSelection() {
        drawerMenuBindings.forEach { (action, itemBinding) ->
            val isSelected = action == selectedDrawerAction
            val isDestructive = action == AppDrawerAction.Logout
            applyDrawerMenuItemStyle(itemBinding, isSelected, isDestructive)
        }
    }

    private fun applyDrawerMenuItemStyle(
        itemBinding: ItemAppDrawerMenuBinding,
        isSelected: Boolean,
        isDestructive: Boolean
    ) = with(itemBinding) {
        val textColor = when {
            isSelected -> R.color.ia_white
            isDestructive -> R.color.drawer_logout
            else -> R.color.ds_text_secondary
        }
        val iconColor = when {
            isSelected -> R.color.ia_white
            isDestructive -> R.color.drawer_logout
            else -> R.color.drawer_icon_tint
        }

        drawerMenuRoot.setBackgroundResource(
            if (isSelected) {
                R.drawable.bg_app_drawer_item_selected
            } else {
                R.drawable.bg_app_drawer_item_default
            }
        )
        drawerMenuTitle.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        drawerMenuIcon.setColorFilter(ContextCompat.getColor(requireContext(), iconColor))
    }

    private fun openDrawer() {
        setBottomNavigationVisible(false)
        binding.drawerScrim.isVisible = true
        binding.appDrawer.isVisible = true
        binding.drawerScrim.bringToFront()
        binding.appDrawer.bringToFront()
        drawerBackCallback?.isEnabled = true
        val drawerWidth = resources.getDimensionPixelSize(R.dimen.drawer_width).toFloat()
        binding.appDrawer.translationX = -drawerWidth
        binding.appDrawer.animate()
            .translationX(0f)
            .setDuration(DRAWER_ANIMATION_DURATION_MS)
            .start()
    }

    private fun closeDrawer(onClosed: (() -> Unit)? = null) {
        drawerBackCallback?.isEnabled = false
        val drawerWidth = resources.getDimensionPixelSize(R.dimen.drawer_width).toFloat()
        binding.appDrawer.animate()
            .translationX(-drawerWidth)
            .setDuration(DRAWER_ANIMATION_DURATION_MS)
            .withEndAction {
                if (_binding == null) return@withEndAction
                binding.appDrawer.isVisible = false
                binding.drawerScrim.isVisible = false
                setBottomNavigationVisible(true)
                onClosed?.invoke()
            }
            .start()
    }

    private fun handleDrawerItemClick(action: AppDrawerAction) {
        if (action != AppDrawerAction.Logout) {
            selectedDrawerAction = action
            refreshDrawerSelection()
        }

        closeDrawer {
            if (!isAdded) return@closeDrawer
            when (action) {
                AppDrawerAction.Products -> openProducts()
                AppDrawerAction.Quotes -> openQuotes()
                AppDrawerAction.SaleOrders -> openSaleOrders()
                AppDrawerAction.PurchaseOrders -> openPurchaseOrders()
                AppDrawerAction.Profiles -> openProfiles()
                AppDrawerAction.Settings -> openSettings()
                AppDrawerAction.Logout -> logoutAndOpenLogin()
                AppDrawerAction.Home,
                AppDrawerAction.AgentConsole,
                AppDrawerAction.Attendance,
                AppDrawerAction.Requests,
                AppDrawerAction.Leaves,
                AppDrawerAction.CheckInRequests,
                AppDrawerAction.Expenses,
                AppDrawerAction.DocumentVault,
                AppDrawerAction.ResourceManagement -> Unit
            }
        }
    }

    private fun openProducts() {
        InternalAppsNavigator.open(findNavController(), R.id.iaProductsFragment)
    }

    private fun openQuotes() {
        InternalAppsNavigator.open(findNavController(), R.id.iaQuotesFragment)
    }

    private fun openSaleOrders() {
        InternalAppsNavigator.open(findNavController(), R.id.iaSaleOrdersFragment)
    }

    private fun openPurchaseOrders() {
        InternalAppsNavigator.open(findNavController(), R.id.iaPurchaseOrdersFragment)
    }

    private fun openProfiles() {
        InternalAppsNavigator.open(findNavController(), R.id.iaProfilesFragment)
    }

    private fun openSettings() {
        InternalAppsNavigator.open(findNavController(), R.id.iaSettingsFragment)
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

    private fun setBottomNavigationVisible(visible: Boolean) {
        (activity as? BottomNavigationHost)?.setBottomNavigationVisible(visible)
    }

    private fun observeDashboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.dashboardState.collect { state ->
                    binding.progress.isVisible = state is UiState.Loading
                    binding.errorText.isVisible = state is UiState.Error || state is UiState.Unauthorized
                    binding.metricsContainer.isVisible = state is UiState.Success
                    binding.summaryText.isVisible = false
                    when (state) {
                        is UiState.Success -> renderDashboard(state.data)
                        is UiState.Error -> binding.errorText.text = state.message
                        UiState.Unauthorized -> binding.errorText.text = "Session expired."
                        else -> Unit
                    }
                }
            }
        }
    }

    private fun renderDashboard(model: DashboardUiModel) {
        val numberFormat = NumberFormat.getIntegerInstance(Locale.US)
        with(binding.dashboardMetricsGrid) {
            totalProductsValue.text = numberFormat.format(model.totalProducts)
            activeQuotesValue.text = numberFormat.format(model.activeQuotes)
            assetHealthValue.text = model.assetHealthLabel
            openOrdersValue.text = model.openOrders.toString().padStart(2, '0')
        }

        with(binding.dashboardOverviewSections) {
            productsTrack.post {
                val maxValue = maxOf(
                    model.totalProducts,
                    model.totalProfiles,
                    model.activeModules,
                    1
                )
                updateBar(productsBar, productsTrack.width, model.totalProducts, maxValue)
                updateBar(profilesBar, profilesTrack.width, model.totalProfiles, maxValue)
                updateBar(modulesBar, modulesTrack.width, model.activeModules, maxValue)
            }
        }
    }

    private fun updateBar(bar: View, trackWidth: Int, value: Int, maxValue: Int) {
        val ratio = value.toFloat() / maxValue.toFloat()
        val minWidth = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._24sdp)
        val targetWidth = (trackWidth * ratio).toInt().coerceAtLeast(minWidth)
        bar.layoutParams = bar.layoutParams.apply {
            width = targetWidth.coerceAtMost(trackWidth)
        }
    }

    override fun onDestroyView() {
        setBottomNavigationVisible(true)
        drawerBackCallback = null
        drawerMenuBindings.clear()
        super.onDestroyView()
        _binding = null
    }

    private fun primaryDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.products, R.drawable.ic_bt_home, AppDrawerAction.Products),
        AppDrawerMenuItem(R.string.drawer_quotes, R.drawable.ic_document, AppDrawerAction.Quotes),
        AppDrawerMenuItem(R.string.drawer_sale_orders, R.drawable.ic_document, AppDrawerAction.SaleOrders),
        AppDrawerMenuItem(R.string.drawer_purchase_orders, R.drawable.ic_document, AppDrawerAction.PurchaseOrders),
        AppDrawerMenuItem(R.string.profiles, R.drawable.ic_bt_account, AppDrawerAction.Profiles)
    )

    private fun footerDrawerItems() = listOf(
        AppDrawerMenuItem(R.string.drawer_settings, R.drawable.ic_settings, AppDrawerAction.Settings),
        AppDrawerMenuItem(
            R.string.drawer_logout,
            R.drawable.ic_logout,
            AppDrawerAction.Logout,
            isDestructive = true
        )
    )

    companion object {
        private const val DRAWER_ANIMATION_DURATION_MS = 180L
        private const val KEY_SELECTED_DRAWER_ACTION = "selected_drawer_action"
    }
}


