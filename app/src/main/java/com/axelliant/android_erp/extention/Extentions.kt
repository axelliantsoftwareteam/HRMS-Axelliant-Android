package com.axelliant.android_erp.extention

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import com.google.android.material.datepicker.MaterialDatePicker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.showSuccessMsg( message: String?="Feature in progress"){
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}


fun Context.showErrorMsg( context: Context? = null,message: String){
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
/* fun datePickerDialog(fragmentManager: FragmentManager): String? {
     var rangedate:String?=null
    // Creating a MaterialDatePicker builder for selecting a date range
    val builder = MaterialDatePicker.Builder.dateRangePicker()
    builder.setTitleText("Select a date range")

    // Building the date picker dialog
    val datePicker = builder.build()
    datePicker.addOnPositiveButtonClickListener { selection ->
        // Retrieving the selected start and end dates
        val startDate = selection.first
        val endDate = selection.second

        // Formatting the selected dates as strings
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateString = sdf.format(Date(startDate))
        val endDateString = sdf.format(Date(endDate))

        // Creating the date range string
        rangedate = "$startDateString - $endDateString"
    }

    // Showing the date picker dialog
    datePicker.show(fragmentManager, "DATE_PICKER")
      return rangedate
}*/



