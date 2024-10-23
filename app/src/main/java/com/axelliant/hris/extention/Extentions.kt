package com.axelliant.hris.extention

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.axelliant.hris.R
import com.axelliant.hris.config.AppConst
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


fun Context.showErrorMsg(message: String? = "Error") {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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




