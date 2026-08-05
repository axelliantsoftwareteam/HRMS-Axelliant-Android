package com.axelliant.hris.screens

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.config.GlobalConfig
import com.axelliant.hris.core.contracts.navigation.AppNavControllerStore
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.navigation.AppNavigator
import com.axelliant.hris.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : BaseActivity() {

    private val appUpdateManager: AppUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private val appUpdatedListener: InstallStateUpdatedListener by lazy {
        object : InstallStateUpdatedListener {
            override fun onStateUpdate(installState: InstallState) {
                when {
                    installState.installStatus() == InstallStatus.DOWNLOADED -> popupSnackbarForCompleteUpdate()
                    installState.installStatus() == InstallStatus.INSTALLED -> appUpdateManager.unregisterListener(this)
                    else -> Log.d("MainActivity","InstallStateUpdatedListener: state: %s"+installState.installStatus())
                }
            }
        }
    }
    @Inject
    lateinit var globalConfig: GlobalConfig
    private var lastBackPressedTime: Long = 0
    private val exitThreshold: Long = 2000 // Time threshold in milliseconds
    @Inject
    lateinit var sessionManager: SessionManager
    @Inject
    lateinit var appNavControllerStore: AppNavControllerStore
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // above will assign it
        globalConfig.navController = navHostFragment.navController
        appNavControllerStore.attach(navHostFragment.navController)
        globalConfig.navController.addOnDestinationChangedListener { controller, destination, arguments ->

            Handler(Looper.getMainLooper()).postDelayed({
                hideDialog()
            }, 200)

            when (destination.id) {
                R.id.homeFragment, R.id.profileFragment -> {
                    bottomNavigation.visibility = View.GONE
                }

                else -> {
                    bottomNavigation.visibility = View.GONE
                }
            }

        }


        if (sessionManager.checkLogin()) {

            AppConst.observableCode.observe(this) { code ->
                if (code == 401) {
                    mSingleAccountApp!!.signOut(object :
                        ISingleAccountPublicClientApplication.SignOutCallback {
                        override fun onSignOut() {
                            mAccount = null
                        }

                        override fun onError(exception: MsalException) {
                            this@MainActivity.showErrorMsg(exception.toString())
                        }
                    })
                    AppNavigator.navigateToLogin()
                }

            }
        }


//        R.id.homeFragment, R.id.leavesFragment, R.id.profileFragment -> {
//            bottomNavigation.visibility = View.VISIBLE
//        }

        bottomNavigation.setupWithNavController(globalConfig.navController)

        AppConst.observableCode.observe(this) { code ->
            if (code == 401) {
                AppNavigator.navigateToLogin()
            }
        }
        checkForAppUpdate()
    }
    private fun checkForAppUpdate() {
        // Returns an intent object that you use to check for an update.
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                // Request the update.
                try {
                    val installType = when {
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                        appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                        else -> null
                    }
                    if (installType == AppUpdateType.FLEXIBLE) appUpdateManager.registerListener(appUpdatedListener)

                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        installType!!,
                        this,
                        APP_UPDATE_REQUEST_CODE)
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == APP_UPDATE_REQUEST_CODE) {
            if (resultCode != Activity.RESULT_OK) {
                Toast.makeText(this,
                    "App Update failed, please try again on the next app launch.",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
    private fun popupSnackbarForCompleteUpdate() {
        val snackbar = Snackbar.make(
            findViewById(R.id.rootLayout),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction("RESTART") { appUpdateManager.completeUpdate() }
        snackbar.setActionTextColor(ContextCompat.getColor(this, R.color.colorApp))
        snackbar.show()
    }
    override fun onResume() {
        super.onResume()
        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->

                // If the update is downloaded but not installed,
                // notify the user to complete the update.
                if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                    popupSnackbarForCompleteUpdate()
                }

                //Check if Immediate update is required
                try {
                    if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                        // If an in-app update is already running, resume the update.
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            this,
                            APP_UPDATE_REQUEST_CODE)
                    }
                } catch (e: IntentSender.SendIntentException) {
                    e.printStackTrace()
                }
            }
    }

    override fun onBackPressed() {
        if (globalConfig.navController.currentDestination?.id == R.id.homeFragment) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressedTime < exitThreshold) {
                finish()
            } else {
                Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()
                lastBackPressedTime = currentTime
            }
        } else
            super.onBackPressed()

    }
    companion object {
        private const val APP_UPDATE_REQUEST_CODE = 1991
    }
}
