package com.axelliant.hris.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.databinding.AttendanceCountRowBinding
import com.axelliant.hris.extention.setUrlImage
import com.axelliant.hris.model.attendance.AttendanceValues

class ValuesAdapter(
    private val list: List<AttendanceValues>,
    private val mContext: Context
) :
    RecyclerView.Adapter<ValuesAdapter.AccountsVH>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = AttendanceCountRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }


    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], position, mContext)


    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: AttendanceCountRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AttendanceValues, position: Int, mContext: Context) {

            binding.tvAttend.text = item.name.toString()
            binding.tvPresentTxt.text = item.count.toString()
            binding.ivAttend.setUrlImage(item.icon, mContext)



        }
    }

}