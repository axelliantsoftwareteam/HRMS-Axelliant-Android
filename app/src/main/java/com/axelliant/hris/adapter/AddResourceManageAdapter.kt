package com.axelliant.hris.adapter

import android.annotation.SuppressLint
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
import com.axelliant.hris.databinding.LyAddNewResourceBinding
import com.axelliant.hris.model.resourceManage. ProjectHour
import com.axelliant.hris.utils.Utils
import com.google.gson.Gson
import java.util.Calendar

class AddResourceManageAdapter(
    private val list: ArrayList<ProjectHour>,
    private val mContext: Context,
    private val itemClick: AdapterItemClick,
    private val onUpdateList: OnUpdateList // Add this parameter
) : RecyclerView.Adapter<AddResourceManageAdapter.AccountsVH>() {

    interface OnUpdateList {
        fun onListUpdated(updatedList: ArrayList<ProjectHour>)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountsVH {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = LyAddNewResourceBinding.inflate(layoutInflater, parent, false)
        return AccountsVH(binding)
    }

    override fun onBindViewHolder(holder: AccountsVH, @SuppressLint("RecyclerView") position: Int) {
        holder.bind(list[position], mContext)
        Log.d("updatedListJson", Gson().toJson(list[position]))
        // Populate spinner for expense types
        spinnerLeavePopulations(mContext, holder.binding.spAttendType, position)

        // Show/Hide delete button for the first item
        holder.binding.ivDelete.visibility = if (position == 0) View.GONE else View.VISIBLE

        // Remove any previous TextWatchers before adding new ones
        holder.binding.etAmount.removeTextChangedListener(holder.amountTextWatcher)

        // Set text in the EditText fields
        holder.binding.etAmount.setText(list[position].working_hours?.toString() ?: "")

//        // Add new TextWatchers for description and amount
//        holder.reasonTextWatcher = object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//                val newDescription = s.toString()
//                list[position].description = newDescription
//                onUpdateList.onListUpdated(list)
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
//        }

        holder.amountTextWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newAmount = try {
                    s.toString().toDouble()
                } catch (e: NumberFormatException) {
                    0.0
                }
                list[position].working_hours = newAmount
                onUpdateList.onListUpdated(list)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        // Re-add the new TextWatchers
        holder.binding.etAmount.addTextChangedListener(holder.amountTextWatcher)

        // Handle the delete action
        holder.binding.tvDelete.setOnClickListener {
            list.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, list.size)
            onUpdateList.onListUpdated(list)
            Log.d("removeList", list.size.toString())
        }

        // Handle the date picker
        holder.binding.lyDate.setOnClickListener {
            pickDate(position)
        }


    }

    override fun getItemCount(): Int = list.size

    class AccountsVH(val binding: LyAddNewResourceBinding) : RecyclerView.ViewHolder(binding.root) {
        var reasonTextWatcher: TextWatcher? = null
        var amountTextWatcher: TextWatcher? = null

        fun bind(item:  ProjectHour, mContext: Context) {
            binding.tvDateTxt.text = item.date ?: ""
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

                val selectedProjectName = list[position].name

                // Format the date and update the list
                list[position].date = Utils.getServerFormat(
                    SERVER_DATE_FORMAT_ATTENDANCE, selectedDate.time
                )
                list[position].name = selectedProjectName

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
        mainItemPosition: Int
    ) {
        val adapter = ProjectTypeSpinnerAdapter(mContext, list[mainItemPosition].expenseTypeList)
        spinner.adapter = adapter

      /*  val selectedType = list[mainItemPosition].project_name
        for (counter in 0 until list[mainItemPosition].expenseTypeList.size) {
            if (list[mainItemPosition].expenseTypeList[counter].name == selectedType) {
                spinner.setSelection(counter)
                break
            }
        }*/
        spinner.setSelection(list[mainItemPosition].expenseTypeList.indexOfFirst {
            it.name == list[mainItemPosition].name
        }.takeIf { it != -1 } ?: 0)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                list[mainItemPosition].name = list[mainItemPosition].expenseTypeList[pos].name
                list[mainItemPosition].project = list[mainItemPosition].expenseTypeList[pos].project
                onUpdateList.onListUpdated(list)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    fun grandTotalCalculation(): Double {
        return list.sumOf { it.working_hours ?: 0.0 }
    }
}
