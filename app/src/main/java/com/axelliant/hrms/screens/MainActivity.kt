package com.axelliant.hrms.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.axelliant.hrms.R
import com.axelliant.hrms.config.AppConst
import com.axelliant.hrms.di.Components
import com.axelliant.hrms.extention.showErrorMsg
import com.axelliant.hrms.navigation.AppNavigator
import com.axelliant.hrms.utils.SessionManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import org.koin.android.ext.android.inject


class MainActivity : BaseActivity() {

    private lateinit var diComponents: Components
    private var lastBackPressedTime: Long = 0
    private val exitThreshold: Long = 2000 // Time threshold in milliseconds
    private val sessionManager: SessionManager by inject()
    private var mSingleAccountApp: ISingleAccountPublicClientApplication? = null
    private var mAccount: IAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        diComponents = Components()

        val bottomNavigation: BottomNavigationView = findViewById(R.id.bottomNavigation)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // above will assign it
        diComponents.globalConfig.navController = navHostFragment.navController
        diComponents.globalConfig.navController.addOnDestinationChangedListener { controller, destination, arguments ->

            Handler(Looper.getMainLooper()).postDelayed({
                hideDialog()
            }, 200)

            when (destination.id) {
                R.id.homeFragment, R.id.profileFragment -> {
                    bottomNavigation.visibility = View.VISIBLE
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

        bottomNavigation.setupWithNavController(diComponents.globalConfig.navController)

        AppConst.observableCode.observe(this) { code ->
            if (code == 401) {
                AppNavigator.navigateToLogin()
            }
        }
    }

    override fun onBackPressed() {
        if (diComponents.globalConfig.navController.currentDestination?.id == R.id.homeFragment) {
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

}