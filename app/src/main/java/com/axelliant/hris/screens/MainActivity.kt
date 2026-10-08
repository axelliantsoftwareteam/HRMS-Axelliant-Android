package com.axelliant.hris.screens

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.WindowCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.AppState
import com.axelliant.hris.databinding.ActivityMainBinding
import com.axelliant.hris.di.Components
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.ui.DrawerController
import com.axelliant.hris.utils.SessionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.koin.android.ext.android.inject


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerController: DrawerController
    private lateinit var diComponents: Components
    private val sessionManager: SessionManager by inject()

    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> popupSnackbarForCompleteUpdate()
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                    else -> Log.d("MainActivity", "InstallStateUpdatedListener: state: " + installState.installStatus())
                }
            }
        }
    }

    private var lastBackPressedTime: Long = 0
    private val exitThreshold: Long = 2000 // ms

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // dark clock/battery icons on the white status bar (also covers edge-to-edge on Android 15+)
        WindowCompat.getInsetsController(window, binding.root).isAppearanceLightStatusBars = true
        diComponents = Components()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        diComponents.globalConfig.navController = navHostFragment.navController
        diComponents.globalConfig.navController.addOnDestinationChangedListener { _, destination, _ ->
            Handler(Looper.getMainLooper()).postDelayed({ hideDialog() }, 200)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        setupDrawer()
        setupBackNavigation()
        observeSessionExpiry()
        checkForAppUpdate()
    }

    // ------------------------------------------------------------------ drawer

    private fun setupDrawer() {
        // Opened only by the hamburger on Home (openDrawer); never by an edge swipe.
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        binding.drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: android.view.View) {
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }
        })
        drawerController = DrawerController(binding.drawerLayout, binding.drawer) { confirmLogout() }
        AppState.currentEmployee.observe(this) { drawerController.bind(it) }
        AppState.unreadNotifications.observe(this) { drawerController.setUnread(it ?: 0) }
    }

    /** Called by the Home hamburger. */
    fun openDrawer() {
        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    // ------------------------------------------------------------------ sign out / session

    /** Asks for confirmation, then signs the user out of Microsoft and HRIS. */
    fun confirmLogout() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.drawer_logout_title)
            .setMessage(R.string.drawer_logout_message)
            .setPositiveButton(R.string.drawer_logout_title) { _, _ -> signOut() }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun observeSessionExpiry() {
        AppConst.observableCode.observe(this) { code ->
            if (code == 401) {
                AppConst.observableCode.set(0) // consume, the LiveData would otherwise replay it
                val current = AppNavigator.getCurrentDestinationId()
                if (current != R.id.loginFragment) signOut()
            }
        }
    }

    private fun signOut() {
        PublicClientApplication.createSingleAccountPublicClientApplication(
            this,
            R.raw.auth_config_ciam_auth,
            object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                override fun onCreated(application: ISingleAccountPublicClientApplication) {
                    application.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                        override fun onSignOut() = finishSignOut()
                        override fun onError(exception: MsalException) = finishSignOut()
                    })
                }

                override fun onError(exception: MsalException) = finishSignOut()
            })
    }

    private fun finishSignOut() {
        sessionManager.logoutUser()
        AppConst.TOKEN = ""
        AppState.reset()
        AppNavigator.navigateToLogin()
    }

    // ------------------------------------------------------------------ back

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) ->
                        binding.drawerLayout.closeDrawer(GravityCompat.START)

                    diComponents.globalConfig.navController.currentDestination?.id == R.id.homeFragment -> {
                        val now = System.currentTimeMillis()
                        if (now - lastBackPressedTime < exitThreshold) {
                            finish()
                        } else {
                            Toast.makeText(this@MainActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                            lastBackPressedTime = now
                        }
                    }

                    else -> if (!diComponents.globalConfig.navController.popBackStack()) finish()
                }
            }
        })
    }

    // ------------------------------------------------------------------ in-app updates

    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                try {
                    val installType = when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    } ?: return@addOnSuccessListener
                    if (installType == AppUpdateType.FLEXIBLE) appUpdateManager.registerListener(appUpdatedListener)

                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        installType,
                        this,
                        APP_UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE && resultCode != Activity.RESULT_OK) {
            Toast.makeText(this, "App Update failed, please try again on the next app launch.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(binding.rootLayout, "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE).apply {
            setAction("RESTART") { appUpdateManager.completeUpdate() }
            setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorApp))
            show()
        }
    }

    override fun onResume() {
        super.onResume()
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate()
            }
            try {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        AppUpdateType.IMMEDIATE,
                        this,
                        APP_UPDATE_REQUEST_CODE
                    )
                }
            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        private const val APP_UPDATE_REQUEST_CODE = 1991
    }
}
