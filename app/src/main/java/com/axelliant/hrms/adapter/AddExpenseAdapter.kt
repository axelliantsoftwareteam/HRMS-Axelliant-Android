package com.axelliant.hrms.adapter

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hrms.R
import com.axelliant.hrms.callback.AdapterItemClick
import com.axelliant.hrms.config.AppConst.SERVER_DATE_FORMAT_ATTENDANCE
import com.axelliant.hrms.databinding.LyAddNewExpenseBinding
import com.axelliant.hrms.model.expense.AddExpense
import com.axelliant.hrms.utils.Utils
import java.util.Calendar
class AddExpenseAdapter(
    private val list: ArrayList<AddExpense>,
    private val mContext: Context,
    private val itemClick: AdapterItemClick,
    private val onUpdateList: OnUpdateList // Add this parameter
) : RecyclerView.Adapter<AddExpenseAdapter.AccountsVH>() {

    interface OnUpdateList {
        fun onListUpdated(updatedList: ArrayList<AddExpense>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LyAddNewExpenseBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, @SuppressLint("RecyclerView") position: Int) {
        holder.bind(list[position], mContext)

        holder.binding.tvDateTxt.text = list[position].expense_date
        // Set text change listeners to update the list
        holder.binding.etAttendanceReason.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                list[position].description = s.toString()
                onUpdateList.onListUpdated(list) // Notify the fragment
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.binding.etAmount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                list[position].amount = try {
                    s.toString().toInt()
                } catch (e: NumberFormatException) {
                    null
                }
                onUpdateList.onListUpdated(list) // Notify the fragment
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
        holder.binding.lyDate.setOnClickListener {
            pickDate(position)
        }

        holder.binding.tvDelete.setOnClickListener {
            list.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, list.size)
            onUpdateList.onListUpdated(list) // Notify the fragment
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class AccountsVH(val binding: LyAddNewExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AddExpense, mContext: Context) {
            // You can bind other view elements here if needed
        }
    }
    private fun pickDate(position: Int) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            mContext, R.style.my_dialog_theme, // Apply the theme here
            { view, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                // Format the date using SimpleDateFormat
                list[position].expense_date = Utils.getServerFormat(
                    dateFormat = SERVER_DATE_FORMAT_ATTENDANCE, date = selectedDate.time
                )
                notifyItemChanged(position) // Update the specific item
                onUpdateList.onListUpdated(list) // Notify the fragment
            },
            year,
            month,
            day
        )
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }
}

