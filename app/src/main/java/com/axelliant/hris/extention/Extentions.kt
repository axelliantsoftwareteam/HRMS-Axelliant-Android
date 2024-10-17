package com.axelliant.hris.extention

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import com.axelliant.hris.R
import com.bumptech.glide.Glide

fun String?.valueQualifier():String {
    if(this==null)
        return "--"
    else if(this == "")
        return "--"
    else if(this == "null")
        return "--"
    else
        return this
}


fun String?.nullToEmpty():String {
    if(this==null)
        return ""
    else if(this == "")
        return ""
    else
        return this
}

fun Context.showSuccessMsg(message: String? = "Feature in progress") {
    if(message==null)
        Toast.makeText(this, "Feature in progress", Toast.LENGTH_SHORT).show()
    else
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

}


fun Context.showErrorMsg(message: String?="Error") {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

@SuppressLint("CheckResult")
 fun ImageView.setLocalImage(uri: Uri,context: Context?) {

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




