package com.axelliant.hris.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import com.axelliant.hris.core.session.HrisSessionStore
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SUPPORT_EMAIL = "hris-support@axelliant.com"

private data class UserErrorPresentation(
    val userMessage: String,
    val rawMessage: String,
    val shouldOfferSupport: Boolean
)

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface AppUiFeedbackEntryPoint {
    fun hrisSessionStore(): HrisSessionStore
}

fun Context.showAppSuccessMessage(message: String? = "Feature in progress") {
    Toast.makeText(this, message ?: "Feature in progress", Toast.LENGTH_SHORT).show()
}

fun Context.showAppErrorMessage(message: String? = "Error", screenName: String? = null) {
    val presentation = message.toUserErrorPresentation()
    if (!presentation.shouldOfferSupport) {
        Toast.makeText(this, presentation.userMessage, Toast.LENGTH_SHORT).show()
        return
    }

    MaterialAlertDialogBuilder(this)
        .setTitle("We couldn't complete this request")
        .setMessage("${presentation.userMessage}\n\nIf this keeps happening, report it to support.")
        .setPositiveButton("Report Issue") { _, _ ->
            openIssueReporter(screenName, presentation)
        }
        .setNegativeButton("Dismiss", null)
        .show()
}

fun String?.toAppUserSafeErrorMessage(): String {
    return this.toUserErrorPresentation().userMessage
}

fun String?.shouldOfferAppSupportForError(): Boolean {
    return this.toUserErrorPresentation().shouldOfferSupport
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
            userMessage = "Some account data is not configured correctly yet. Please contact your administrator or try again later.",
            rawMessage = rawMessage,
            shouldOfferSupport = true
        )
    }

    if (rawMessage.contains("doctype access", ignoreCase = true) ||
        rawMessage.contains("no permission", ignoreCase = true) ||
        rawMessage.contains("not permitted", ignoreCase = true)
    ) {
        return UserErrorPresentation(
            userMessage = "Your account does not have access to this action yet. Please contact your administrator.",
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
            userMessage = "We could not connect right now. Please check your internet connection and try again.",
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
            userMessage = "We could not complete this request. Please try again. If it keeps happening, report the issue to support.",
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

private fun Context.buildIssueReportBody(screenName: String?, presentation: UserErrorPresentation): String {
    val hrisSessionStore = EntryPointAccessors.fromApplication(
        applicationContext,
        AppUiFeedbackEntryPoint::class.java
    ).hrisSessionStore()
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val versionName = packageInfo.versionName ?: "Unknown"
    val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    val userIdentifier = hrisSessionStore.currentSession()?.email
        ?.takeIf { it.isNotBlank() }
        ?: "Unknown"
    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())

    return """
        Please review the following app issue.

        App: Axelliant Android
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
    val subject = "Axelliant Android issue - ${screenName ?: "Unknown screen"}"
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
