package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.axelliant.hris.R
import com.axelliant.hris.ui.designsystem.theme.AppFluentTheme
import com.axelliant.hris.ui.designsystem.tokens.DestructiveButtonTokens
import com.microsoft.fluentui.theme.token.StateBorderStroke
import com.microsoft.fluentui.theme.token.StateBrush
import com.microsoft.fluentui.theme.token.StateColor
import com.microsoft.fluentui.theme.token.controlTokens.ButtonInfo
import com.microsoft.fluentui.theme.token.controlTokens.ButtonSize
import com.microsoft.fluentui.theme.token.controlTokens.ButtonStyle
import com.microsoft.fluentui.theme.token.controlTokens.ButtonTokens
import com.microsoft.fluentui.tokenized.controls.Button
import kotlinx.parcelize.Parcelize

class AppButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {

    var insetTop: Int = 0
    var insetBottom: Int = 0
    var cornerRadius: Int = 0
        set(value) {
            field = value
            notifyStateChanged()
        }
    var strokeWidth: Int = 0
        set(value) {
            field = value
            notifyStateChanged()
        }
    var strokeColor: ColorStateList? = null
        set(value) {
            field = value
            notifyStateChanged()
        }

    private var isStateReady = false
    private var stateVersion by mutableIntStateOf(0)
    private var buttonText by mutableStateOf("")
    private var buttonEnabled = isEnabled
    private var contentDescriptionText: String? = null
    private var iconResId by mutableIntStateOf(0)
    private var iconSizePx by mutableIntStateOf(0)
    private var maxLinesState by mutableIntStateOf(1)
    private var textColorOverride by mutableStateOf<Long?>(null)
    private var iconTintOverride by mutableStateOf<Long?>(null)
    private var backgroundTintOverride: Long? = null
    private var usesExplicitAndroidBackground by mutableStateOf(false)
    private var textSizeSpOverride by mutableStateOf<Float?>(null)
    private var ellipsizeMode by mutableStateOf<TextUtils.TruncateAt?>(null)

    var text: CharSequence?
        get() = buttonText
        set(value) {
            buttonText = value?.toString().orEmpty()
        }

    var isAllCaps: Boolean = false
        set(value) {
            field = value
            notifyStateChanged()
        }

    var minWidth: Int
        get() = minimumWidth
        set(value) {
            minimumWidth = value
        }

    var maxLines: Int
        get() = maxLinesState
        set(value) {
            maxLinesState = value.coerceAtLeast(1)
        }

    var ellipsize: TextUtils.TruncateAt?
        get() = ellipsizeMode
        set(value) {
            ellipsizeMode = value
        }

    var iconTint: ColorStateList? = null
        set(value) {
            field = value
            iconTintOverride = value?.defaultColor?.toComposeColorLong()
        }

    var iconSize: Int
        get() = iconSizePx
        set(value) {
            iconSizePx = value.coerceAtLeast(0)
        }

    var iconPadding: Int = 0
        set(value) {
            field = value
            notifyStateChanged()
        }

    var iconGravity: Int = ICON_GRAVITY_TEXT_START
        set(value) {
            field = value
            notifyStateChanged()
        }

    var icon: Drawable? = null
        set(value) {
            field = value
            if (value == null) {
                iconResId = 0
            }
        }

    var noBackground: Boolean = false
        set(value) {
            field = value
            notifyStateChanged()
        }

    var fluentButtonStyle: Int = FLUENT_STYLE_PRIMARY
        set(value) {
            field = value
            notifyStateChanged()
        }

    init {
        layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
        minimumHeight = resources.getDimensionPixelSize(R.dimen.ds_button_height)
        isClickable = true
        isFocusable = true
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)

        readAndroidViewAttributes(attrs, defStyleAttr)
        context.obtainStyledAttributes(attrs, R.styleable.AppButtonView, defStyleAttr, 0).use {
            iconPadding = it.getDimensionPixelSize(R.styleable.AppButtonView_iconPadding, iconPadding)
            iconSizePx = it.getDimensionPixelSize(R.styleable.AppButtonView_iconSize, iconSizePx)
            iconTint = it.getColorStateList(R.styleable.AppButtonView_iconTint)
            iconGravity = it.getInt(R.styleable.AppButtonView_iconGravity, iconGravity)
            noBackground = it.getBoolean(R.styleable.AppButtonView_noBackground, noBackground)
            if (it.hasValue(R.styleable.AppButtonView_backgroundTint)) {
                backgroundTintList = it.getColorStateList(R.styleable.AppButtonView_backgroundTint)
            }
            cornerRadius = it.getDimensionPixelSize(
                R.styleable.AppButtonView_cornerRadius,
                cornerRadius
            )
            strokeColor = it.getColorStateList(R.styleable.AppButtonView_strokeColor)
            strokeWidth = it.getDimensionPixelSize(
                R.styleable.AppButtonView_strokeWidth,
                strokeWidth
            )
            fluentButtonStyle = it.getInt(
                R.styleable.AppButtonView_fluentButtonStyle,
                fluentButtonStyle
            )
            iconResId = it.getResourceId(R.styleable.AppButtonView_icon, iconResId)
            icon = it.getDrawable(R.styleable.AppButtonView_icon)
        }
        applyRawXmlAttributes(attrs)
        isStateReady = true
    }

    override fun onRtlPropertiesChanged(layoutDirection: Int) {
        super.onRtlPropertiesChanged(View.LAYOUT_DIRECTION_LTR)
        this.layoutDirection = View.LAYOUT_DIRECTION_LTR
        textDirection = View.TEXT_DIRECTION_LTR
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        buttonEnabled = enabled
        notifyStateChanged()
    }

    override fun setBackgroundTintList(tint: ColorStateList?) {
        super.setBackgroundTintList(tint)
        backgroundTintOverride = tint?.defaultColor?.toComposeColorLong()
        notifyStateChanged()
    }

    fun setText(text: String?) {
        this.text = text
    }

    fun setText(resId: Int) {
        text = context.getText(resId)
    }

    fun setTextColor(color: Int) {
        textColorOverride = color.toComposeColorLong()
    }

    fun setTextSize(size: Float) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size)
    }

    fun setTextSize(unit: Int, size: Float) {
        textSizeSpOverride = when (unit) {
            TypedValue.COMPLEX_UNIT_SP -> size
            TypedValue.COMPLEX_UNIT_PX -> {
                size / scaledFontDensity()
            }
            else -> size
        }
    }

    override fun setContentDescription(contentDescription: CharSequence?) {
        super.setContentDescription(contentDescription)
        contentDescriptionText = contentDescription?.toString()
        notifyStateChanged()
    }

    fun setIconResource(resId: Int) {
        iconResId = resId
        icon = AppCompatResources.getDrawable(context, resId)
    }

    fun clearIcon() {
        iconResId = 0
        icon = null
    }

    @Composable
    override fun Content() {
        AppFluentTheme {
            stateVersion
            val hasText = buttonText.isNotBlank()
            val hasIcon = iconResId != 0
            val isIconOnly = !hasText && hasIcon
            val hasCustomFluentSurface = backgroundTintOverride != null ||
                strokeColor != null ||
                strokeWidth > 0
            val effectiveNoBackground = noBackground ||
                usesExplicitAndroidBackground ||
                (isIconOnly && !hasCustomFluentSurface)
            val style = when {
                effectiveNoBackground -> ButtonStyle.TextButton
                fluentButtonStyle == FLUENT_STYLE_OUTLINE -> ButtonStyle.OutlinedButton
                fluentButtonStyle == FLUENT_STYLE_SUBTLE -> ButtonStyle.TextButton
                else -> ButtonStyle.Button
            }
            val leadingIcon = if (iconGravity == ICON_GRAVITY_TEXT_END) null else iconVectorOrNull()
            val trailingIcon = if (iconGravity == ICON_GRAVITY_TEXT_END) iconVectorOrNull() else null
            val text = buttonText
                .let { if (isAllCaps) it.uppercase() else it }
                .takeUnless { it.isBlank() }

            if (isIconOnly) {
                IconOnlyContent(
                    transparentBackground = effectiveNoBackground,
                    iconVector = iconVectorOrNull(),
                )
                return@AppFluentTheme
            }

            val tokens = when (fluentButtonStyle) {
                FLUENT_STYLE_DESTRUCTIVE -> DestructiveButtonTokens()
                else -> AppButtonViewTokens(
                    textColor = textColorOverride,
                    iconColor = iconTintOverride,
                    backgroundColor = backgroundTintOverride,
                    strokeColor = strokeColor?.defaultColor?.toComposeColorLong(),
                    strokeWidthPx = strokeWidth,
                    iconPaddingPx = if (isIconOnly) 0 else iconPadding,
                    iconSizePx = iconSizePx,
                    cornerRadiusPx = cornerRadius.takeIf { it > 0 },
                    textSizeSp = textSizeSpOverride,
                    transparentBackground = usesExplicitAndroidBackground,
                    preserveIconColors = iconTintOverride == null,
                    forceIconOnly = isIconOnly,
                )
            }

            Box(
                modifier = buttonContainerModifier(),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = { performClick() },
                    modifier = buttonModifier(),
                    style = style,
                    size = ButtonSize.Medium,
                    enabled = buttonEnabled,
                    icon = leadingIcon,
                    trailingIcon = trailingIcon,
                    text = text,
                    contentDescription = contentDescriptionText ?: buttonText,
                    buttonTokens = tokens,
                )
            }
        }
    }

    @Composable
    private fun iconVectorOrNull(): ImageVector? {
        return if (iconResId != 0) {
            ImageVector.vectorResource(iconResId)
        } else {
            null
        }
    }

    private fun readAndroidViewAttributes(attrs: AttributeSet?, defStyleAttr: Int) {
        context.obtainStyledAttributes(
            attrs,
            intArrayOf(
                android.R.attr.text,
                android.R.attr.contentDescription,
                android.R.attr.enabled,
                android.R.attr.textColor,
                android.R.attr.textSize,
                android.R.attr.textAllCaps,
                android.R.attr.minWidth,
                android.R.attr.minHeight,
            ),
            defStyleAttr,
            0
        ).use {
            buttonText = it.getText(0)?.toString().orEmpty()
            contentDescriptionText = it.getText(1)?.toString()
            isEnabled = it.getBoolean(2, isEnabled)
            textColorOverride = it.getColorStateList(3)?.defaultColor?.toComposeColorLong()
            if (it.hasValue(4)) {
                textSizeSpOverride = it.getDimensionPixelSize(4, 0)
                    .takeIf { size -> size > 0 }
                    ?.let { size -> size / scaledFontDensity() }
            }
            isAllCaps = it.getBoolean(5, isAllCaps)
            minimumWidth = it.getDimensionPixelSize(6, minimumWidth)
            minimumHeight = it.getDimensionPixelSize(7, minimumHeight)
        }
    }

    @Composable
    private fun IconOnlyContent(
        transparentBackground: Boolean,
        iconVector: ImageVector?,
    ) {
        val icon = iconVector ?: return
        val shape = RoundedCornerShape(
            cornerRadius.takeIf { it > 0 }?.let {
                with(LocalDensity.current) { it.toDp() }
            } ?: dimensionResource(R.dimen.ds_radius_sm)
        )
        val backgroundColor = backgroundTintOverride?.let { Color(it.toInt()) }
            ?: Color.Transparent
        val borderColor = strokeColor?.defaultColor?.let { Color(it) }
        val borderWidth = with(LocalDensity.current) { strokeWidth.toDp() }
        val tintColor = iconTintOverride?.let { Color(it.toInt()) }
        val standardIconSize = with(LocalDensity.current) {
            (iconSizePx.takeIf { it > 0 }
                ?: resources.getDimensionPixelSize(R.dimen.ds_icon_button_icon_size)).toDp()
        }
        val disabledAlpha = if (buttonEnabled) 1f else 0.45f
        val semanticsDescription = contentDescriptionText?.takeIf { it.isNotBlank() }
            ?: buttonText.takeIf { it.isNotBlank() }

        val containerModifier = buttonContainerModifier()
            .clip(shape)
            .then(
                if (!transparentBackground && backgroundColor != Color.Transparent) {
                    Modifier.background(backgroundColor, shape)
                } else {
                    Modifier
                }
            )
            .then(
                if (!transparentBackground && borderColor != null && strokeWidth > 0) {
                    Modifier.border(BorderStroke(borderWidth, borderColor), shape)
                } else {
                    Modifier
                }
            )
            .then(
                semanticsDescription?.let {
                    Modifier.semantics { contentDescription = it }
                } ?: Modifier
            )
            .clickable(enabled = buttonEnabled) { performClick() }

        Box(
            modifier = containerModifier,
            contentAlignment = Alignment.Center,
        ) {
            Image(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(standardIconSize),
                colorFilter = tintColor?.let { ColorFilter.tint(it.copy(alpha = disabledAlpha)) },
                alpha = if (tintColor == null) disabledAlpha else 1f,
            )
        }
    }

    private fun notifyStateChanged() {
        if (isStateReady) {
            stateVersion += 1
        }
    }

    private fun applyRawXmlAttributes(attrs: AttributeSet?) {
        if (attrs == null) return

        val rawBackgroundValue = attrs.getAttributeValue(ANDROID_NS, "background")
        if (rawBackgroundValue == "@null") {
            usesExplicitAndroidBackground = false
        } else if (
            rawBackgroundValue != null ||
            attrs.getAttributeResourceValue(ANDROID_NS, "background", 0) != 0
        ) {
            usesExplicitAndroidBackground = true
        }

        val rawIconResId = attrs.getAttributeResourceValue(AUTO_NS, "icon", 0)
        if (rawIconResId != 0) {
            setIconResource(rawIconResId)
        }

        val rawTintResId = attrs.getAttributeResourceValue(AUTO_NS, "iconTint", 0)
        if (rawTintResId != 0) {
            iconTint = ContextCompat.getColorStateList(context, rawTintResId)
        }

        val rawBackgroundTintResId = attrs.getAttributeResourceValue(ANDROID_NS, "backgroundTint", 0)
            .takeIf { it != 0 }
            ?: attrs.getAttributeResourceValue(AUTO_NS, "backgroundTint", 0)
        val rawBackgroundTintValue = attrs.getAttributeValue(AUTO_NS, "backgroundTint")
            ?: attrs.getAttributeValue(ANDROID_NS, "backgroundTint")
        if (rawBackgroundTintValue == "@null") {
            backgroundTintList = null
            noBackground = true
        } else if (rawBackgroundTintResId != 0) {
            backgroundTintList = ContextCompat.getColorStateList(context, rawBackgroundTintResId)
        }

        iconPadding = attrs.getAttributeResourceValue(AUTO_NS, "iconPadding", 0)
            .takeIf { it != 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: iconPadding

        iconSizePx = attrs.getAttributeResourceValue(AUTO_NS, "iconSize", 0)
            .takeIf { it != 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: iconSizePx

        cornerRadius = attrs.getAttributeResourceValue(AUTO_NS, "cornerRadius", 0)
            .takeIf { it != 0 }
            ?.let { resources.getDimensionPixelSize(it) }
            ?: cornerRadius

        noBackground = attrs.getAttributeBooleanValue(AUTO_NS, "noBackground", noBackground)

        when (attrs.getAttributeValue(AUTO_NS, "iconGravity")) {
            "textEnd", "end" -> iconGravity = ICON_GRAVITY_TEXT_END
            "textStart", "start" -> iconGravity = ICON_GRAVITY_TEXT_START
        }

        when (attrs.getAttributeValue(AUTO_NS, "fluentButtonStyle")) {
            "primary", "0" -> fluentButtonStyle = FLUENT_STYLE_PRIMARY
            "outline", "outlined", "1" -> fluentButtonStyle = FLUENT_STYLE_OUTLINE
            "subtle", "text", "2" -> fluentButtonStyle = FLUENT_STYLE_SUBTLE
            "destructive", "danger", "delete", "3" -> fluentButtonStyle = FLUENT_STYLE_DESTRUCTIVE
        }
    }

    private fun buttonModifier(): Modifier {
        val params = layoutParams
        val width = params?.width ?: ViewGroup.LayoutParams.WRAP_CONTENT
        val height = params?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        val fillWidth = width != ViewGroup.LayoutParams.WRAP_CONTENT
        val fillHeight = height != ViewGroup.LayoutParams.WRAP_CONTENT
        val fillModifier = when {
            fillWidth && fillHeight -> Modifier.fillMaxSize()
            fillWidth -> Modifier.fillMaxWidth()
            fillHeight -> Modifier.fillMaxHeight()
            else -> Modifier
        }
        return fillModifier
    }

    private fun buttonContainerModifier(): Modifier {
        val params = layoutParams
        val width = params?.width ?: ViewGroup.LayoutParams.WRAP_CONTENT
        val height = params?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        val fillWidth = width != ViewGroup.LayoutParams.WRAP_CONTENT
        val fillHeight = height != ViewGroup.LayoutParams.WRAP_CONTENT
        return when {
            fillWidth && fillHeight -> Modifier.fillMaxSize()
            fillWidth -> Modifier.fillMaxWidth()
            fillHeight -> Modifier.fillMaxHeight()
            else -> Modifier
        }
    }

    private fun Int.toComposeColorLong(): Long = toLong() and 0xFFFFFFFF

    private fun scaledFontDensity(): Float {
        val density = resources.displayMetrics.density.takeIf { it > 0f } ?: 1f
        val fontScale = resources.configuration.fontScale.takeIf { it > 0f } ?: 1f
        return density * fontScale
    }

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        const val AUTO_NS = "http://schemas.android.com/apk/res-auto"
        const val FLUENT_STYLE_PRIMARY = 0
        const val FLUENT_STYLE_OUTLINE = 1
        const val FLUENT_STYLE_SUBTLE = 2
        const val FLUENT_STYLE_DESTRUCTIVE = 3
        const val ICON_GRAVITY_TEXT_START = 1
        const val ICON_GRAVITY_TEXT_END = 2
    }
}

@Parcelize
private class AppButtonViewTokens(
    private val textColor: Long? = null,
    private val iconColor: Long? = null,
    private val backgroundColor: Long? = null,
    private val strokeColor: Long? = null,
    private val strokeWidthPx: Int = 0,
    private val iconPaddingPx: Int = 0,
    private val iconSizePx: Int = 0,
    private val cornerRadiusPx: Int? = null,
    private val textSizeSp: Float? = null,
    private val transparentBackground: Boolean = false,
    private val preserveIconColors: Boolean = true,
    private val forceIconOnly: Boolean = false,
) : ButtonTokens() {

    @Composable
    override fun textColor(buttonInfo: ButtonInfo): StateColor {
        return textColor?.let { stateColor(it) } ?: super.textColor(buttonInfo)
    }

    @Composable
    override fun iconColor(buttonInfo: ButtonInfo): StateColor {
        if (preserveIconColors) return stateColor(Color.Unspecified)
        return iconColor?.let { stateColor(it) } ?: super.iconColor(buttonInfo)
    }

    @Composable
    override fun trailingIconColor(buttonInfo: ButtonInfo): StateColor {
        if (preserveIconColors) return stateColor(Color.Unspecified)
        return iconColor?.let { stateColor(it) } ?: super.trailingIconColor(buttonInfo)
    }

    @Composable
    override fun backgroundBrush(buttonInfo: ButtonInfo): StateBrush {
        if (transparentBackground) return transparentStateBrush()
        return backgroundColor?.let { stateBrush(it) } ?: super.backgroundBrush(buttonInfo)
    }

    @Composable
    override fun borderStroke(buttonInfo: ButtonInfo): StateBorderStroke {
        if (transparentBackground) return transparentBorderStroke()
        val color = strokeColor ?: return super.borderStroke(buttonInfo)
        if (strokeWidthPx <= 0) return super.borderStroke(buttonInfo)
        val stroke = with(LocalDensity.current) {
            BorderStroke(strokeWidthPx.toDp(), SolidColor(Color(color)))
        }
        val disabledStroke = with(LocalDensity.current) {
            BorderStroke(strokeWidthPx.toDp(), SolidColor(Color(color).copy(alpha = 0.45f)))
        }
        return StateBorderStroke(
            rest = listOf(stroke),
            pressed = listOf(stroke),
            selected = listOf(stroke),
            focused = listOf(stroke),
            disabled = listOf(disabledStroke),
        )
    }

    @Composable
    override fun typography(buttonInfo: ButtonInfo): TextStyle {
        return textSizeSp?.let { super.typography(buttonInfo).copy(fontSize = it.sp) }
            ?: super.typography(buttonInfo)
    }

    @Composable
    override fun spacing(buttonInfo: ButtonInfo): Dp {
        if (forceIconOnly) return 0.dp
        return if (iconPaddingPx > 0) {
            with(LocalDensity.current) { iconPaddingPx.toDp() }
        } else {
            super.spacing(buttonInfo)
        }
    }

    @Composable
    override fun iconSize(buttonInfo: ButtonInfo): Dp {
        return if (iconSizePx > 0) {
            with(LocalDensity.current) { iconSizePx.toDp() }
        } else {
            super.iconSize(buttonInfo)
        }
    }

    @Composable
    override fun trailingIconSize(buttonInfo: ButtonInfo): Dp {
        return if (iconSizePx > 0) {
            with(LocalDensity.current) { iconSizePx.toDp() }
        } else {
            super.trailingIconSize(buttonInfo)
        }
    }

    @Composable
    override fun cornerRadius(buttonInfo: ButtonInfo): Dp {
        return cornerRadiusPx?.let { with(LocalDensity.current) { it.toDp() } }
            ?: dimensionResource(R.dimen.ds_radius_sm)
    }

    private fun stateColor(color: Long): StateColor {
        return stateColor(Color(color.toInt()))
    }

    private fun stateColor(composeColor: Color): StateColor {
        return StateColor(
            rest = composeColor,
            pressed = composeColor,
            selected = composeColor,
            focused = composeColor,
            disabled = if (composeColor == Color.Unspecified) {
                Color.Unspecified
            } else {
                composeColor.copy(alpha = 0.45f)
            },
        )
    }

    private fun stateBrush(color: Long): StateBrush {
        val composeColor = Color(color.toInt())
        val brush = SolidColor(composeColor)
        val disabledBrush = SolidColor(composeColor.copy(alpha = 0.45f))
        return StateBrush(
            rest = brush,
            pressed = brush,
            selected = brush,
            focused = brush,
            disabled = disabledBrush,
        )
    }

    private fun transparentStateBrush(): StateBrush {
        val brush = SolidColor(Color.Transparent)
        return StateBrush(
            rest = brush,
            pressed = brush,
            selected = brush,
            focused = brush,
            disabled = brush,
        )
    }

    private fun transparentBorderStroke(): StateBorderStroke {
        val stroke = BorderStroke(0.dp, SolidColor(Color.Transparent))
        return StateBorderStroke(
            rest = listOf(stroke),
            pressed = listOf(stroke),
            selected = listOf(stroke),
            focused = listOf(stroke),
            disabled = listOf(stroke),
        )
    }
}
