package com.axelliant.hris.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.res.use
import com.axelliant.hris.R
import com.intuit.sdp.R as SdpR
import com.axelliant.hris.databinding.ViewWsToolbarBinding
import com.axelliant.hris.navigation.AppNavigator

/**
 * Screen toolbar used by every redesigned screen: back arrow, centred title and an optional trailing
 * action (for example the filter icon with an "active" dot).
 *
 * XML: `<com.axelliant.hris.ui.WsToolbar app:wsTitle="@string/..." app:wsActionIcon="@drawable/ws_ic_filter" />`
 */
class WsToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewWsToolbarBinding.inflate(LayoutInflater.from(context), this)

    init {
        minimumHeight = resources.getDimensionPixelSize(SdpR.dimen._40sdp)
        context.obtainStyledAttributes(attrs, R.styleable.WsToolbar).use { a ->
            binding.wsTvTitle.text = a.getString(R.styleable.WsToolbar_wsTitle).orEmpty()
            binding.wsBtnBack.visibility =
                if (a.getBoolean(R.styleable.WsToolbar_wsShowBack, true)) VISIBLE else INVISIBLE
            val actionIcon = a.getResourceId(R.styleable.WsToolbar_wsActionIcon, 0)
            if (actionIcon != 0) {
                binding.wsBtnAction.setImageResource(actionIcon)
                binding.wsActionContainer.visibility = VISIBLE
            }
            binding.wsBtnAction.contentDescription = a.getString(R.styleable.WsToolbar_wsActionDescription)
        }
        binding.wsBtnBack.setOnClickListener { AppNavigator.moveBackToPreviousFragment() }
    }

    var title: CharSequence
        get() = binding.wsTvTitle.text
        set(value) {
            binding.wsTvTitle.text = value
        }

    fun setOnBackClickListener(listener: OnClickListener) = binding.wsBtnBack.setOnClickListener(listener)

    fun setOnActionClickListener(listener: OnClickListener) = binding.wsBtnAction.setOnClickListener(listener)

    fun setActionVisible(visible: Boolean) {
        binding.wsActionContainer.visibility = if (visible) VISIBLE else GONE
    }

    /** Shows the small red dot on the action icon (used while filters are active). */
    fun setActionDotVisible(visible: Boolean) {
        binding.wsActionDot.visibility = if (visible) VISIBLE else GONE
    }
}
