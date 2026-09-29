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
import java.net.DatagramPacket
import java.net.DatagramSocket
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
            dateFormat.format(ntpTime)
        } else {
            "Failed to retrieve NTP time."
        }
    }
}

private fun getNtpTime(): Date? {
    return try {
        val buffer = ByteArray(NTP_PACKET_SIZE)
        buffer[0] = NTP_CLIENT_MODE
        val requestTime = System.currentTimeMillis()
        DatagramSocket().use { socket ->
            socket.soTimeout = NTP_TIMEOUT_MILLIS
            val inetAddress = InetAddress.getByName(NTP_HOST)
            socket.send(DatagramPacket(buffer, buffer.size, inetAddress, NTP_PORT))
            socket.receive(DatagramPacket(buffer, buffer.size))

            val responseTime = System.currentTimeMillis()
            val receiveTime = readNtpTimestamp(buffer, NTP_RECEIVE_TIME_OFFSET)
            val transmitTime = readNtpTimestamp(buffer, NTP_TRANSMIT_TIME_OFFSET)
            val adjustedTime = if (receiveTime > 0 && transmitTime > 0) {
                responseTime + ((receiveTime - requestTime) + (transmitTime - responseTime)) / 2
            } else {
                transmitTime
            }
            adjustedTime.takeIf { it > 0 }?.let(::Date)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun readNtpTimestamp(buffer: ByteArray, offset: Int): Long {
    val seconds = readUnsignedInt(buffer, offset)
    val fraction = readUnsignedInt(buffer, offset + 4)
    if (seconds == 0L && fraction == 0L) return 0L

    return ((seconds - NTP_EPOCH_DELTA_SECONDS) * 1000L) +
        ((fraction * 1000L) / NTP_FRACTION_DIVISOR)
}

private fun readUnsignedInt(buffer: ByteArray, offset: Int): Long {
    return ((buffer[offset].toLong() and 0xFFL) shl 24) or
        ((buffer[offset + 1].toLong() and 0xFFL) shl 16) or
        ((buffer[offset + 2].toLong() and 0xFFL) shl 8) or
        (buffer[offset + 3].toLong() and 0xFFL)
}

private const val NTP_HOST = "time.google.com"
private const val NTP_PORT = 123
private const val NTP_TIMEOUT_MILLIS = 5000
private const val NTP_PACKET_SIZE = 48
private const val NTP_CLIENT_MODE = 0x1B.toByte()
private const val NTP_RECEIVE_TIME_OFFSET = 32
private const val NTP_TRANSMIT_TIME_OFFSET = 40
private const val NTP_EPOCH_DELTA_SECONDS = 2_208_988_800L
private const val NTP_FRACTION_DIVISOR = 0x1_0000_0000L
