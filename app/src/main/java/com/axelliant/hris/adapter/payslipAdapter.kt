package com.axelliant.hris.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.model.PayslipModel

class PayslipAdapter(
    private var list: List<PayslipModel>
) : RecyclerView.Adapter<PayslipAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonth: TextView = view.findViewById(R.id.tvMonth)
        val tvSize: TextView = view.findViewById(R.id.tvSize)
        val ivDownload: ImageView = view.findViewById(R.id.ivDownload)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.row_payslip, parent, false)
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val payslip = list[position]

        holder.tvMonth.text = payslip.month
        holder.tvSize.text = payslip.size

        holder.ivDownload.setOnClickListener {
            // TODO: Download/Open payslip PDF

            // Example:
            // Toast.makeText(
            //     holder.itemView.context,
            //     "Downloading ${payslip.month}",
            //     Toast.LENGTH_SHORT
            // ).show()
        }
    }

    fun updateData(newList: List<PayslipModel>) {
        list = newList
        notifyDataSetChanged()
    }
}