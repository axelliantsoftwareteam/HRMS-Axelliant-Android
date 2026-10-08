package com.axelliant.hris.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.annotation.DrawableRes
import com.axelliant.hris.R
import com.axelliant.hris.databinding.ViewWsEmptyStateBinding

/**
 * Card shown when a list has nothing to show (or failed to load): icon, title and a short message.
 * Set the defaults in XML (`app:wsEmptyIcon/wsEmptyTitle/wsEmptyMessage`) or call [show] at runtime
 * to switch between "empty" and "could not load" copy.
 */
class WsEmptyState @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewWsEmptyStateBinding.inflate(LayoutInflater.from(context), this)

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.WsEmptyState)
        try {
            val icon = a.getResourceId(R.styleable.WsEmptyState_wsEmptyIcon, 0)
            if (icon != 0) binding.wsEmptyIcon.setImageResource(icon)
            binding.wsEmptyTitle.text = a.getString(R.styleable.WsEmptyState_wsEmptyTitle).orEmpty()
            binding.wsEmptyMessage.text = a.getString(R.styleable.WsEmptyState_wsEmptyMessage).orEmpty()
        } finally {
            a.recycle()
        }
    }

    fun show(@DrawableRes icon: Int? = null, title: CharSequence? = null, message: CharSequence? = null) {
        icon?.let { binding.wsEmptyIcon.setImageResource(it) }
        title?.let { binding.wsEmptyTitle.text = it }
        message?.let { binding.wsEmptyMessage.text = it }
        visibility = VISIBLE
    }

    fun hide() {
        visibility = GONE
    }
}
