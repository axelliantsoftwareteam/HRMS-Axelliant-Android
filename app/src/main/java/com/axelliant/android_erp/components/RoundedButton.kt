package com.axelliant.android_erp.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.axelliant.android_erp.R
import com.axelliant.android_erp.databinding.RoundedButtonBinding

class RoundedButton(
    context: Context,
    attributeSet: AttributeSet
) : ConstraintLayout(context, attributeSet) {

    private val binding: RoundedButtonBinding

    init {
        binding = RoundedButtonBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray = context.obtainStyledAttributes(attributeSet, R.styleable.RoundedButton)

        typedArray.getString(R.styleable.RoundedButton_text).let {
            it?.let { setText(text = it) }
        }
        typedArray.getBoolean(R.styleable.RoundedButton_iconVisibility, false).let {
            binding.root.getViewById(R.id.backButton).isVisible= it
        }

        typedArray.getDrawable(R.styleable.RoundedButton_buttonIcon).let { drawable ->
            drawable?.let { binding.backButton.setImageDrawable(drawable) }
        }

        typedArray.recycle()
    }

    fun setText(text: String) {
        binding.title.text = text
    }


    private fun setTextColor(color: Int) {
        binding.title.setTextColor(color)
    }


}