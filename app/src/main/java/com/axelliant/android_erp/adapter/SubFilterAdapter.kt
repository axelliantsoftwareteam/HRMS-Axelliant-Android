package com.axelliant.android_erp.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.android_erp.R
import com.axelliant.android_erp.Test
import com.axelliant.android_erp.callback.AdapterItemClick
import com.axelliant.android_erp.databinding.MyAttendanceDetailRowBinding
import com.axelliant.android_erp.databinding.MyTeamAttendRowBinding
import com.axelliant.android_erp.databinding.SubFilterRowBinding
import com.axelliant.android_erp.utils.Utils

class SubFilterAdapter(
    private val list: List<Test>,
    private val context: Context
) :
    RecyclerView.Adapter<SubFilterAdapter.AccountsVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = SubFilterRowBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        holder.bind(list[position], position, context)

    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: SubFilterRowBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Test, position: Int, context: Context) {
            if (position == 3) {
                binding.tvWorkFrom.setTextColor(context.getColor(R.color.white))
                binding.lyWorkHome.background =
                    context.resources.getDrawable(R.drawable.enable_rounded_bgg)
            } else {
                binding.tvWorkFrom.setTextColor(context.getColor(R.color.black))

                binding.lyWorkHome.background =
                    context.resources.getDrawable(R.drawable.rounded_bgg)
            }

            binding.tvWorkFrom.text = item.testString

            binding.tvWorkTxt.text = Utils.getRandomString()

        }
    }

}