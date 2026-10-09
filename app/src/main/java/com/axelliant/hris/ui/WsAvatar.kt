package com.axelliant.hris.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.axelliant.hris.R
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

/** Round placeholder with the person's initials (primary text on a 15 % primary circle). */
class InitialsDrawable(
    private val initials: String,
    private val textColor: Int,
    private val backgroundColor: Int,
    private val typeface: Typeface? = null
) : Drawable() {

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = backgroundColor }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textAlign = Paint.Align.CENTER
        typeface = this@InitialsDrawable.typeface ?: Typeface.DEFAULT_BOLD
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val r = minOf(b.width(), b.height()) / 2f
        canvas.drawCircle(b.exactCenterX(), b.exactCenterY(), r, bgPaint)
        textPaint.textSize = r * 2 * 0.36f
        val y = b.exactCenterY() - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(initials, b.exactCenterX(), y, textPaint)
    }

    // Views such as CircleImageView copy the drawable into a bitmap and need a real size.
    override fun getIntrinsicWidth(): Int = 192
    override fun getIntrinsicHeight(): Int = 192

    override fun setAlpha(alpha: Int) {
        bgPaint.alpha = alpha
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        bgPaint.colorFilter = colorFilter
        textPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}

/**
 * Loads a circular photo, falling back to the person's initials while loading, on error or when
 * there is no photo. [path] may be relative to the HRIS host.
 */
fun ImageView.setAvatar(path: String?, name: String?) {
    val ctx = context
    val primary = ContextCompat.getColor(ctx, R.color.ws_primary)
    val placeholder = InitialsDrawable(
        WsFormat.initials(name),
        primary,
        WsStatus.tint(primary, 15),
        ResourcesCompat.getFont(ctx, R.font.poppins_semibold)
    )
    val url = WsFormat.mediaUrl(path)
    if (url.isEmpty()) {
        Glide.with(this).clear(this)
        setImageDrawable(placeholder)
        return
    }
    Glide.with(this)
        .load(url)
        .apply(RequestOptions.circleCropTransform())
        .placeholder(placeholder)
        .error(placeholder)
        .into(this)
}
