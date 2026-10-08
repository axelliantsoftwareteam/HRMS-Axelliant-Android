package com.axelliant.hris.ui

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.axelliant.hris.R

/** Accent colours per request type (same tones as the iOS app). */
object WsTone {
    @ColorInt fun leave(c: Context) = ContextCompat.getColor(c, R.color.ws_red)
    @ColorInt fun attendance(c: Context) = ContextCompat.getColor(c, R.color.ws_secondary)
    @ColorInt fun expense(c: Context) = ContextCompat.getColor(c, R.color.ws_purple)
    @ColorInt fun request(c: Context) = ContextCompat.getColor(c, R.color.ws_yellow)
    @ColorInt fun approval(c: Context) = ContextCompat.getColor(c, R.color.ws_primary)
    @ColorInt fun payslip(c: Context) = ContextCompat.getColor(c, R.color.ws_payslip)
    @ColorInt fun notification(c: Context) = ContextCompat.getColor(c, R.color.ws_dark_purple)

    /** Quota/tile colours cycle in this order: red, blue, green, purple, yellow. */
    fun cycle(c: Context, index: Int): Int {
        val ids = intArrayOf(R.color.ws_red, R.color.ws_secondary, R.color.ws_light_green, R.color.ws_purple, R.color.ws_yellow)
        return ContextCompat.getColor(c, ids[Math.floorMod(index, ids.size)])
    }
}

/** Status text → colour and the tinted "badge" look (solid text on a 15 % background). */
object WsStatus {

    /** First match wins, on the lower-cased text. */
    @ColorInt
    fun colorFor(context: Context, status: String?): Int {
        val s = status.orEmpty().lowercase()
        val id = when {
            s.contains("reject") || s.contains("cancel") || s.contains("absent") || s.contains("expired") -> R.color.ws_red
            s.contains("approv") || s.contains("submit") || s.contains("paid") || s.contains("process") || s.contains("active") -> R.color.ws_green
            s.contains("pend") || s.contains("open") || s.contains("wait") -> R.color.ws_amber
            else -> R.color.ws_primary
        }
        return ContextCompat.getColor(context, id)
    }

    /** HRIS reports an unanswered leave as "Open"/empty; the UI always calls it "Pending". */
    fun displayLeave(status: String?): String =
        if (status.isNullOrBlank() || status.equals("Open", ignoreCase = true)) "Pending" else status

    /** "Draft" expenses/documents are shown as "Pending". */
    fun displayDraft(status: String?): String =
        if (status.isNullOrBlank() || status.equals("Draft", ignoreCase = true)) "Pending" else status

    fun tint(@ColorInt color: Int, alphaPercent: Int = 15): Int =
        ColorUtils.setAlphaComponent(color, (255 * alphaPercent / 100f).toInt())

    /**
     * Styles [view] as a status badge: [text], solid [color] text, [alphaPercent] % background.
     * The view should use `style="@style/Ws.Text.Badge"` (adds the rounded background).
     */
    fun badge(view: TextView, text: String?, @ColorInt color: Int = colorFor(view.context, text), alphaPercent: Int = 15) {
        view.text = text.orEmpty()
        view.setTextColor(color)
        view.background?.mutate()?.setTint(tint(color, alphaPercent))
    }

    /** Rounded-square icon badge: tinted background and tinted glyph. Use `@drawable/ws_icon_badge_bg`. */
    fun iconBadge(view: android.widget.ImageView, @ColorInt color: Int) {
        view.setColorFilter(color)
        view.background?.mutate()?.setTint(tint(color, 15))
    }

    @ColorInt
    fun transparent(): Int = Color.TRANSPARENT
}
