package com.axelliant.hris.extention

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.pm.PackageInfoCompat
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
import com.axelliant.hris.utils.SessionManager
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SUPPORT_EMAIL = "hris-support@axelliant.com"

private data class UserErrorPresentation(
    val userMessage: String,
    val rawMessage: String,
    val shouldOfferSupport: Boolean
)

fun String?.valueQualifier(): String {
    if (this == null)
        return "--"
    else if (this == "")
        return "--"
    else if (this == "null")
        return "--"
    else
        return this
}


fun String?.nullToEmpty(): String {
    if (this == null)
        return ""
    else if (this == "")
        return ""
    else
        return this
}

fun Context.showSuccessMsg(message: String? = "Feature in progress") {
    if (message == null)
        Toast.makeText(this, "Feature in progress", Toast.LENGTH_SHORT).show()
    else
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

}

private fun String?.toUserErrorPresentation(): UserErrorPresentation {
    val rawMessage = this?.trim().orEmpty()

    if (rawMessage.isBlank() || rawMessage.equals("null", ignoreCase = true)) {
        return UserErrorPresentation(
            userMessage = "Something went wrong. Please try again.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    if (rawMessage.contains("list index out of range", ignoreCase = true)) {
        return UserErrorPresentation(
            userMessage = "Some HR data is not configured correctly for this account yet. Please contact HR or try again later.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    if (rawMessage.contains("doctype access", ignoreCase = true) ||
        rawMessage.contains("no permission", ignoreCase = true) ||
        rawMessage.contains("not permitted", ignoreCase = true)
    ) {
        return UserErrorPresentation(
            userMessage = "Your account does not have access to this action yet. Please contact HR or your administrator.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    if (rawMessage.contains("timeout", ignoreCase = true) ||
        rawMessage.contains("unable to resolve host", ignoreCase = true) ||
        rawMessage.contains("failed to connect", ignoreCase = true) ||
        rawMessage.contains("connection reset", ignoreCase = true) ||
        rawMessage.contains("network", ignoreCase = true)
    ) {
        return UserErrorPresentation(
            userMessage = "We could not connect to HRIS right now. Please check your internet connection and try again.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    if (rawMessage.contains("exception", ignoreCase = true) ||
        rawMessage.contains("traceback", ignoreCase = true) ||
        rawMessage.contains("<html", ignoreCase = true) ||
        rawMessage.contains("server error", ignoreCase = true)
    ) {
        return UserErrorPresentation(
            userMessage = "We could not complete this request. Please try again. If it keeps happening, report the issue to HRIS support.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    return UserErrorPresentation(
        userMessage = rawMessage,
        rawMessage = rawMessage,
        shouldOfferSupport = false
    )
}

fun String?.toUserSafeErrorMessage(): String {
    return this.toUserErrorPresentation().userMessage
}

fun String?.shouldOfferSupportForError(): Boolean {
    return this.toUserErrorPresentation().shouldOfferSupport
}

private fun Context.buildIssueReportBody(screenName: String?, presentation: UserErrorPresentation): String {
    val sessionManager = SessionManager(applicationContext)
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionName = packageInfo.versionName ?: "Unknown"
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    val userIdentifier = sessionManager.getUserEmail()
        ?.takeIf { it.isNotBlank() }
        ?: sessionManager.getRememberUserName().takeIf { it.isNotBlank() }
        ?: "Unknown"
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())

    return """
        Please review the following HRIS issue.

        App: Android
        Version: $versionName ($versionCode)
        Screen: ${screenName ?: "Unknown"}
        User: $userIdentifier
        Time: $timestamp

        Issue:
        ${presentation.userMessage}

        Technical details:
        ${presentation.rawMessage.ifBlank { "N/A" }}
    """.trimIndent()
}

private fun Context.openIssueReporter(screenName: String?, presentation: UserErrorPresentation) {
    val subject = "HRIS Android issue - ${screenName ?: "Unknown screen"}"
    val body = buildIssueReportBody(screenName, presentation)
    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:$SUPPORT_EMAIL")
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, body)
    }

    try {
        if (emailIntent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(emailIntent, "Report issue"))
        } else {
            Toast.makeText(
                this,
                "No email app is installed. Please contact $SUPPORT_EMAIL.",
                Toast.LENGTH_LONG
            ).show()
        }
    } catch (exception: ActivityNotFoundException) {
        Toast.makeText(
            this,
            "No email app is installed. Please contact $SUPPORT_EMAIL.",
            Toast.LENGTH_LONG
        ).show()
    }
}

fun Context.showErrorMsg(message: String? = "Error", screenName: String? = null) {
    val presentation = message.toUserErrorPresentation()
    if (!presentation.shouldOfferSupport) {
        Toast.makeText(this, presentation.userMessage, Toast.LENGTH_SHORT).show()
        return
    }

    AlertDialog.Builder(this)
        .setTitle("We couldn't complete this request")
        .setMessage("${presentation.userMessage}\n\nIf this keeps happening, report it to HRIS support.")
        .setPositiveButton("Report Issue") { _, _ ->
            openIssueReporter(screenName, presentation)
        }
        .setNegativeButton("Dismiss", null)
        .show()
}

@SuppressLint("CheckResult")
fun ImageView.setLocalImage(uri: Uri, context: Context?) {

    if (context != null) {
        Glide.with(context)
            .load(uri) // image url
            .placeholder(R.drawable.ic_place_holder) // any placeholder to load at start
            .error(R.drawable.ic_place_holder)  // any image in case of error
            .centerCrop()
            .into(this)
    }
}

fun ImageView.setUrlImage(url: String?, context: Context? = null) {

    if (url == null) {
        if (context == null) {
            return
        } else {
            this.setImageDrawable(
                AppCompatResources.getDrawable(
                    context,
                    R.drawable.ic_place_holder
                )
            )
//            this.setImageDrawable(context?.getDrawable(R.drawable.ic_place_holder))
            return
        }

    }


    if (context != null) {
        Glide.with(context)
            .load(url) // image url
            .placeholder(R.drawable.ic_place_holder) // any placeholder to load at start
            .error(R.drawable.ic_place_holder)  // any image in case of error
            .centerCrop()
            .into(this)
    } else {
        Glide.with(this)
            .load(url) // image url
            .placeholder(R.drawable.ic_place_holder) // any placeholder to load at start
            .error(R.drawable.ic_place_holder)  // any image in case of error
            .centerCrop()
            .into(this)
    }


}

suspend fun getNtpTimeFormatted(): String {
    return withContext(Dispatchers.IO) {
        val ntpTime = getNtpTime()
        Log.d("ntpTime",ntpTime.toString())
        if (ntpTime != null) {
            val dateFormat = SimpleDateFormat(AppConst.ATTENDANCE_DATE_FORMAT, Locale.getDefault())
            "${dateFormat.format(ntpTime)}"
        } else {
            "Failed to retrieve NTP time."
        }
    }
}

private fun getNtpTime(): Date? {
    val ntpClient = NTPUDPClient()
    ntpClient.defaultTimeout = 5000 // Set timeout
    return try {
        val inetAddress = InetAddress.getByName("time.google.com")
        val timeInfo = ntpClient.getTime(inetAddress)
        timeInfo.computeDetails()
        if (timeInfo.offset != null) {
            val currentTime = System.currentTimeMillis() + timeInfo.offset
            Date(currentTime)
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } finally {
        ntpClient.close()
    }
}
