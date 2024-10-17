package com.axelliant.hris.screens

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.axelliant.hris.dialog.LoadingDialog

open class BaseActivity : AppCompatActivity() {
    lateinit var loadingDialog: LoadingDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadingDialog = LoadingDialog(this)
        // Set dialog properties
        loadingDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        loadingDialog.setCanceledOnTouchOutside(false)

    }


    fun showDialog() {

        if (!this.isFinishing) {
            if (!loadingDialog.isShowing)
                loadingDialog.show()

        }

    }

    fun hideDialog() {
        if (loadingDialog.isShowing)
            loadingDialog.dismiss()
    }


}