package com.axelliant.hris.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import com.axelliant.hris.R
import com.axelliant.hris.databinding.RoundedButtonBinding

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
        typedArray.getColor(R.styleable.RoundedButton_textColor,context.getColor(R.color.white)).let {
            it.let { setTextColor(color = it) }
        }
        typedArray.getBoolean(R.styleable.RoundedButton_iconVisibility, false).let {
            binding.root.getViewById(R.id.backButton).isVisible= it
        }
        typedArray.getBoolean(R.styleable.RoundedButton_isOutlineButton, false).let {
            if (it) {
                binding.root.background = ResourcesCompat.getDrawable(context.resources, R.drawable.outline_enabled,context.theme)
            } else {
                typedArray.getDrawable(R.styleable.RoundedButton_buttonBg).let { drawable ->
                    if(drawable==null)
                        binding.root.background = null
                    else
                        drawable.let { binding.root.background = drawable }


                }
            }
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