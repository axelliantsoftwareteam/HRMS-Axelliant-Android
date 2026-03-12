package com.axelliant.hris.adapter

import android.app.DatePickerDialog
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.recyclerview.widget.RecyclerView
import com.axelliant.hris.R
import com.axelliant.hris.callback.AdapterItemClick
import com.axelliant.hris.config.AppConst.SERVER_DATE_FORMAT_ATTENDANCE
import com.axelliant.hris.databinding.LyAddNewExpenseBinding
import com.axelliant.hris.model.expense.AddExpense
import com.axelliant.hris.utils.Utils
import com.google.gson.Gson
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

    override fun onBindViewHolder(holder: AccountsVH, position: Int) {
        val currentItem = list[position]
        holder.bind(currentItem, mContext)
        Log.d("updatedListJson", Gson().toJson(currentItem))
        // Populate spinner for expense types
        spinnerLeavePopulations(mContext, holder.binding.spAttendType, currentItem, holder)

        // Show/Hide delete button for the first item
        holder.binding.ivDelete.visibility = if (position == 0) View.GONE else View.VISIBLE

        // Remove any previous TextWatchers before adding new ones
        holder.binding.etAttendanceReason.removeTextChangedListener(holder.reasonTextWatcher)
        holder.binding.etAmount.removeTextChangedListener(holder.amountTextWatcher)

        // Set text in the EditText fields
        holder.binding.etAttendanceReason.setText(currentItem.description)
        holder.binding.etAmount.setText(currentItem.amount?.toString() ?: "")

        // Add new TextWatchers for description and amount
        holder.reasonTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    list[adapterPosition].description = s.toString()
                    onUpdateList.onListUpdated(list)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        holder.amountTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val newAmount = try {
                        s.toString().toDouble()
                    } catch (e: NumberFormatException) {
                        0.0
                    }
                    list[adapterPosition].amount = newAmount
                    onUpdateList.onListUpdated(list)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        // Re-add the new TextWatchers
        holder.binding.etAttendanceReason.addTextChangedListener(holder.reasonTextWatcher)
        holder.binding.etAmount.addTextChangedListener(holder.amountTextWatcher)

        // Handle the delete action
        holder.binding.tvDelete.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                list.removeAt(adapterPosition)
                notifyItemRemoved(adapterPosition)
                notifyItemRangeChanged(adapterPosition, list.size)
                onUpdateList.onListUpdated(list)
                Log.d("removeList", list.size.toString())
            }
        }

        // Handle the date picker
        holder.binding.lyDate.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                pickDate(adapterPosition)
            }
        }


    }

    override fun getItemCount(): Int = list.size

    class AccountsVH(val binding: LyAddNewExpenseBinding) : RecyclerView.ViewHolder(binding.root) {
        var reasonTextWatcher: TextWatcher? = null
        var amountTextWatcher: TextWatcher? = null

        fun bind(item: AddExpense, mContext: Context) {
            binding.tvDateTxt.text = item.expense_date ?: ""
//            binding.etAttendanceReason.setText(item.description ?: "")
//            binding.etAmount.setText(item.amount?.toString() ?: "")
        }
    }

    private fun pickDate(position: Int) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            mContext, R.style.my_dialog_theme,
            { _, year, monthOfYear, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, monthOfYear, dayOfMonth)

                // Format the date and update the list
                list[position].expense_date = Utils.getServerFormat(
                    SERVER_DATE_FORMAT_ATTENDANCE, selectedDate.time
                )
                notifyItemChanged(position)
                onUpdateList.onListUpdated(list)
            },
            year, month, day
        )
        datePickerDialog.datePicker.maxDate = c.timeInMillis
        datePickerDialog.show()
    }

    private fun spinnerLeavePopulations(
        mContext: Context,
        spinner: Spinner,
        item: AddExpense,
        holder: AccountsVH
    ) {
        val adapter = LeaveSpinnerAdapter(mContext, item.expenseTypeList)
        spinner.adapter = adapter

        val selectedType = item.expense_type
        for (counter in 0 until item.expenseTypeList.size) {
            if (item.expenseTypeList[counter].type == selectedType) {
                spinner.setSelection(counter)
                break
            }
        }

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                val adapterPosition = holder.adapterPosition
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    val currentItem = list[adapterPosition]
                    currentItem.expense_type = currentItem.expenseTypeList[pos].type
                    onUpdateList.onListUpdated(list)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun grandTotalCalculation(): Double {
        return list.sumOf { it.amount ?: 0.0 }
    }
}
