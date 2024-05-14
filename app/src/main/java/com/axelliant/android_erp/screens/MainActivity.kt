package com.axelliant.android_erp.screens

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.axelliant.android_erp.R
import com.axelliant.android_erp.config.AppConst
import com.axelliant.android_erp.di.Components
import com.axelliant.android_erp.navigation.AppNavigator
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var diComponents: Components
    private var lastBackPressedTime: Long = 0
    private val exitThreshold: Long = 2000 // Time threshold in milliseconds


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

            when (destination.id) {
                R.id.homeFragment, R.id.leavesFragment, R.id.profileFragment -> {
                    bottomNavigation.visibility = View.VISIBLE
                }

                else -> {
                    bottomNavigation.visibility = View.GONE
                }
            }

        }

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