package com.axelliant.hris.location

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.axelliant.hris.extention.showErrorMsg
import com.axelliant.hris.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Owns foreground location updates, permission prompts and device-location settings for Home. */
internal class HomeLocationController(
    private val fragment: Fragment,
    private val changed: (Location?) -> Unit,
    private val fetchingChanged: (Boolean) -> Unit = {}
) : DefaultLifecycleObserver {
    private val context = fragment.requireContext()
    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val preferences = context.getSharedPreferences("home_location_permission", Context.MODE_PRIVATE)
    private var location: Location? = null
    private var registered = false
    private var action = ""
    private var dialog: AlertDialog? = null
    private var resolving = false
    private var resolutionDeclined = false
    private val fetch = HomeLocationFetch()
    private var timeout: Job? = null
    private var permissionInFlight = false
    private var settingsInFlight = false
    private val settings = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        settingsInFlight = false
        if (fetch.active) {
            if (hasPermission() && enabled()) continueFind() else finishFind()
        }
    }
    private val permission = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        permissionInFlight = false
        refresh()
        if (!hasPermission()) {
            if (registered && !fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                showSettings(true)
            } else {
                finishFind()
                if (registered) context.showErrorMsg(context.getString(R.string.home_location_permission_needed))
            }
        }
        if (hasPermission() && fetch.active) continueFind()
    }
    private val resolution = fragment.registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        resolving = false
        refresh()
        resolutionDeclined = result.resultCode != Activity.RESULT_OK && !enabled()
        if (fetch.active) {
            if (enabled()) continueFind() else finishFind()
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
            if (fetch.active && !resolving && !permissionInFlight && !settingsInFlight) {
                if (enabled() && hasPermission()) continueFind() else finishFind()
            }
        }
    }
    private val listener = object : LocationListener {
        override fun onLocationChanged(value: Location) {
            if (!enabled() || !hasPermission()) { invalidate(); return }
            val now = SystemClock.elapsedRealtimeNanos()
            if (!isRecentAttendanceFix(now, value.elapsedRealtimeNanos) ||
                (fetch.active && !fetch.accepts(now, value.elapsedRealtimeNanos))) return
            location = value
            changed(value)
            if (fetch.accepts(SystemClock.elapsedRealtimeNanos(), value.elapsedRealtimeNanos)) finishFind()
        }
        override fun onProviderDisabled(provider: String) { refresh() }
        override fun onProviderEnabled(provider: String) { refresh() }
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
    }

    /** Registers system switch observation and starts permitted updates while [owner] is visible. */
    override fun onStart(owner: LifecycleOwner) {
        ContextCompat.registerReceiver(context, receiver, IntentFilter().apply {
            addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
            addAction(LocationManager.MODE_CHANGED_ACTION)
        }, ContextCompat.RECEIVER_EXPORTED)
        registered = true
        refresh()
        if (fetch.active && !permissionInFlight && !settingsInFlight && !resolving) continueFind()
    }

    /** Releases receiver/GPS resources and discards the current fix when [owner] stops. */
    override fun onStop(owner: LifecycleOwner) {
        if (registered) context.unregisterReceiver(receiver)
        registered = false
        manager.removeUpdates(listener)
        invalidate()
        timeout?.cancel()
        if (!permissionInFlight && !settingsInFlight && !resolving) finishFind()
    }

    /** Dismisses any Home-owned settings dialog when [owner]'s view is destroyed. */
    override fun onDestroy(owner: LifecycleOwner) {
        dialog?.dismiss()
        permission.unregister()
        resolution.unregister()
        settings.unregister()
        finishFind()
    }

    /** Finds a fresh fix after permission/settings checks; never submits attendance or repeats active work. */
    fun findLocation() {
        if (!registered || !fetch.begin()) return
        fetchingChanged(true)
        continueFind()
    }

    private fun continueFind() {
        if (!registered || !fetch.active) return
        if (!requirements(context.getString(R.string.home_location_find_action))) return
        if (fetch.startFetching(SystemClock.elapsedRealtimeNanos())) {
            timeout = fragment.viewLifecycleOwner.lifecycleScope.launch {
                delay(20_000L)
                invalidate()
                finishFind()
                context.showErrorMsg(context.getString(R.string.home_location_timeout))
            }
        }
        refresh()
    }

    private fun finishFind() {
        timeout?.cancel()
        timeout = null
        fetch.finish()
        fetchingChanged(false)
    }

    /** Returns true only for permitted, enabled and recent location; otherwise prompts for [label]. */
    fun ready(label: String): Boolean {
        if (fetch.active || !requirements(label)) return false
        refresh()
        if (location == null) {
            findLocation()
            context.showErrorMsg(context.getString(R.string.home_location_fetching))
            return false
        }
        return true
    }

    private fun requirements(label: String): Boolean {
        if (!registered || permissionInFlight || settingsInFlight || resolving) return false
        action = label
        if (!hasPermission()) {
            invalidate()
            val canRequest = canRequestHomeLocationPermission(preferences.getBoolean("asked", false),
                fragment.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION))
            if (canRequest) {
                preferences.edit().putBoolean("asked", true).apply()
                permissionInFlight = true
                permission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            } else showSettings(true)
            return false
        }
        if (!enabled()) {
            invalidate()
            requestDeviceLocation()
            return false
        }
        return true
    }

    private fun hasPermission() = ContextCompat.checkSelfPermission(context,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun enabled() = manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
        manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    private fun invalidate() { location = null; changed(null) }

    @SuppressLint("MissingPermission")
    private fun refresh() {
        manager.removeUpdates(listener)
        if (!registered || !hasPermission() || !enabled()) { invalidate(); return }
        val fixes = mutableListOf<Location>()
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).forEach { provider ->
            try {
                if (manager.isProviderEnabled(provider)) {
                    manager.getLastKnownLocation(provider)?.let { fixes.add(it) }
                    manager.requestLocationUpdates(provider, 5000L, 0f, listener)
                }
            } catch (_: SecurityException) {
                // Permission can be revoked between the check and the provider call.
                invalidate()
                finishFind()
                return
            } catch (_: IllegalArgumentException) {
                // A provider can disappear while device settings change; try the other one.
            }
        }
        location?.let { fixes.add(it) }
        val now = SystemClock.elapsedRealtimeNanos()
        location = fixes.filter { isRecentAttendanceFix(now, it.elapsedRealtimeNanos) &&
            (!fetch.active || fetch.accepts(now, it.elapsedRealtimeNanos)) }
            .maxByOrNull { it.elapsedRealtimeNanos }
        changed(location)
        if (location != null && fetch.accepts(now, location!!.elapsedRealtimeNanos)) finishFind()
    }

    private fun requestDeviceLocation() {
        if (resolutionDeclined) { showSettings(false); return }
        if (resolving) return
        resolving = true
        checkHomeLocationSettings(context, satisfied = {
                resolving = false
                if (!registered) finishFind()
                else { refresh(); if (!enabled()) showSettings(false) else if (fetch.active) continueFind() }
            }, resolutionRequired = { sender ->
                if (!registered) { resolving = false; finishFind() }
                else {
                    try { resolution.launch(IntentSenderRequest.Builder(sender).build()) }
                    catch (_: android.content.ActivityNotFoundException) { resolving = false; showSettings(false) }
                }
            }, unavailable = { resolving = false; if (registered) showSettings(false) else finishFind() })
    }

    private fun showSettings(permissionDenied: Boolean) {
        if (dialog?.isShowing == true || !registered) return
        dialog = showHomeLocationSettingsDialog(context, permissionDenied, action,
            openSettings = { intent ->
                settingsInFlight = true
                settings.launch(intent)
            }, cancel = ::finishFind)
    }
}
