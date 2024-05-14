package com.axelliant.android_erp.screens

import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        diComponents = Components()

        Toast.makeText(
            this,
            "componoent= ${diComponents.testModelInjection.testInjection}",
            Toast.LENGTH_SHORT
        ).show()


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // above will assign it
        diComponents.globalConfig.navController = navHostFragment.navController
        diComponents.globalConfig.navController.addOnDestinationChangedListener { _, _, _ ->

        }


        AppConst.observableCode.observe(this, Observer { code ->
            if (code == 401) {
                AppNavigator.navigateToLogin()
            }
        })

        val navView: BottomNavigationView = findViewById(R.id.bottomNavigation)
        navView.setupWithNavController(diComponents.globalConfig.navController)
    }


}