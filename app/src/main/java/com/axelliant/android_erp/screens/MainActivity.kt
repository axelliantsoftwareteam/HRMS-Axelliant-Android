package com.axelliant.android_erp.screens

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.axelliant.android_erp.config.GlobalConfig
import com.axelliant.android_erp.R
import com.axelliant.android_erp.di.Components


class MainActivity : AppCompatActivity() {

    private lateinit var components: Components

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        components = Components()

        Toast.makeText(
            this,
            "componoent= ${components.testModelInjection.testInjection}",
            Toast.LENGTH_SHORT
        ).show()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        // above will assign it
        GlobalConfig.getInstance().navController = navHostFragment.navController
        GlobalConfig.getInstance().navController.addOnDestinationChangedListener { _, _, _ ->

        }

    }


}