package com.axelliant.android_erp.extention

import android.app.Activity
import android.content.Context
import android.widget.Toast

fun Context.showSuccessMsg( message: String?="Feature in progress"){
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}


fun Context.showErrorMsg( context: Context? = null,message: String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}



