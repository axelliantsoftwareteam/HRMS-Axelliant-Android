package com.axelliant.hris.extention

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import com.axelliant.hris.R
import com.axelliant.hris.core.constants.AppDateFormats
import com.axelliant.hris.core.ui.shouldOfferAppSupportForError
import com.axelliant.hris.core.ui.showAppErrorMessage
import com.axelliant.hris.core.ui.showAppSuccessMessage
import com.axelliant.hris.core.ui.toAppUserSafeErrorMessage
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ntp.NTPUDPClient
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    showAppSuccessMessage(message)
}

fun String?.toUserSafeErrorMessage(): String {
    return this.toAppUserSafeErrorMessage()
}

fun String?.shouldOfferSupportForError(): Boolean {
    return this.shouldOfferAppSupportForError()
}

fun Context.showErrorMsg(message: String? = "Error", screenName: String? = null) {
    showAppErrorMessage(message, screenName)
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
            val dateFormat = SimpleDateFormat(AppDateFormats.ATTENDANCE_DATE_TIME, Locale.getDefault())
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
