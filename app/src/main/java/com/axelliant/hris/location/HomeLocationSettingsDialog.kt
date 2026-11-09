package com.axelliant.hris.location

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.provider.Settings
import com.axelliant.hris.R
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

/** Checks native location settings; reports satisfied settings, a resolution intent, or fallback. */
internal fun checkHomeLocationSettings(
    context: Context,
    satisfied: () -> Unit,
    resolutionRequired: (IntentSender) -> Unit,
    unavailable: () -> Unit
) {
    val request = LocationSettingsRequest.Builder().addLocationRequest(
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
    ).setAlwaysShow(true).build()
    LocationServices.getSettingsClient(context).checkLocationSettings(request)
        .addOnSuccessListener { satisfied() }
        .addOnFailureListener { error ->
            if (error is ResolvableApiException) resolutionRequired(error.resolution.intentSender)
            else unavailable()
        }
}

/** Shows a permission/device-settings fallback for [action]; reports the chosen intent or cancellation. */
internal fun showHomeLocationSettingsDialog(
    context: Context,
    permissionDenied: Boolean,
    action: String,
    openSettings: (Intent) -> Unit,
    cancel: () -> Unit
): AlertDialog = AlertDialog.Builder(context)
    .setMessage(context.getString(if (permissionDenied) R.string.home_location_permission_settings
        else R.string.home_location_enable, action))
    .setPositiveButton(R.string.home_location_open_settings) { _, _ ->
        val intent = if (permissionDenied) Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)) else Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        openSettings(intent)
    }.setNegativeButton(android.R.string.cancel) { _, _ -> cancel() }
    .setOnCancelListener { cancel() }.show()
