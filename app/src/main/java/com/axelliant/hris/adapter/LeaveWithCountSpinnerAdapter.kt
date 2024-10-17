package com.axelliant.hris.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.axelliant.hris.R
import com.axelliant.hris.model.leave.SpinnerType
import com.axelliant.hris.model.leave.leaveCount.LeaveAllocation
import com.axelliant.hris.model.leave.leaveCount.Leaves

class LeaveWithCountSpinnerAdapter(context: Context, private val leaves: ArrayList<LeaveAllocation>)
    : ArrayAdapter<LeaveAllocation>(context, 0, leaves) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.spinner_item)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return createViewFromResource(position, convertView, parent, R.layout.spinner_item)
    }

    private fun createViewFromResource(position: Int, convertView: View?, parent: ViewGroup, resource: Int): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(resource, parent, false)

        val item = getItem(position)
//        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.tv_spinner_text)

        item?.let {
//            imageView.setImageResource(it.imageResource)
            textView.text = it.name
        }

        return view
    }
}
