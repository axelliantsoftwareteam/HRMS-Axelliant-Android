package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.res.ColorStateList
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.axelliant.hris.R

/**
 * Reusable XML toolbar bridge for app screens.
 * The actions use AppButtonView, so button rendering stays on the Fluent2 path.
 */
class AppTopBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var centerTitle = true
    private var inverseColors = false

    val backButton: AppButtonView = AppButtonView(context).apply {
        id = View.generateViewId()
        setIconResource(R.drawable.ic_arrow_back)
        noBackground = true
        minimumWidth = 0
        minWidth = 0
    }

    val titleView: AppTextView = AppTextView(context).apply {
        id = View.generateViewId()
        setTextAppearance(R.style.Text_Fluent2_Title_Primary_Strong)
        maxLines = 1
        ellipsize = TextUtils.TruncateAt.END
    }

    val searchButton: AppButtonView = AppButtonView(context).apply {
        id = View.generateViewId()
        setIconResource(R.drawable.ia_ic_search)
        noBackground = true
        text = ""
        minimumWidth = 0
        minWidth = 0
    }

    val actionButton: AppButtonView = AppButtonView(context).apply {
        id = View.generateViewId()
        setIconResource(R.drawable.ic_more_vertical)
        noBackground = true
        text = ""
        minimumWidth = 0
        minWidth = 0
        visibility = GONE
    }

    init {
        minHeight = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._40sdp)
        addView(backButton)
        addView(titleView)
        addView(searchButton)
        addView(actionButton)
        applyAttributes(attrs)
        applyToolbarColors()
        bindLayout()
    }

    fun setTitle(text: CharSequence?) {
        titleView.text = text
    }

    fun setTitle(resId: Int) {
        titleView.setText(resId)
    }

    fun setBackVisible(visible: Boolean) {
        backButton.visibility = if (visible) VISIBLE else GONE
    }

    fun setSearchVisible(visible: Boolean) {
        searchButton.visibility = if (visible) VISIBLE else GONE
        bindLayout()
    }

    fun setSearchIconResource(resId: Int) {
        searchButton.setIconResource(resId)
    }

    fun setActionVisible(visible: Boolean) {
        actionButton.visibility = if (visible) VISIBLE else GONE
        bindLayout()
    }

    fun setActionIconResource(resId: Int) {
        actionButton.setIconResource(resId)
    }

    fun setActionText(text: CharSequence?) {
        actionButton.text = text
        if (!text.isNullOrBlank()) {
            actionButton.clearIcon()
        }
        bindLayout()
    }

    fun setCenterTitle(center: Boolean) {
        centerTitle = center
        bindLayout()
    }

    fun setInverseColors(inverse: Boolean) {
        inverseColors = inverse
        applyToolbarColors()
    }

    fun setOnBackClickListener(listener: OnClickListener?) {
        backButton.setOnClickListener(listener)
    }

    fun setOnSearchClickListener(listener: OnClickListener?) {
        searchButton.setOnClickListener(listener)
    }

    fun setOnActionClickListener(listener: OnClickListener?) {
        actionButton.setOnClickListener(listener)
    }

    private fun bindLayout() {
        val buttonSize = resources.getDimensionPixelSize(com.intuit.sdp.R.dimen._40sdp)
        val titleMargin = resources.getDimensionPixelSize(R.dimen.ds_space_8)

        backButton.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
            startToStart = LayoutParams.PARENT_ID
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
        }

        val actionHasText = !actionButton.text.isNullOrBlank()
        actionButton.layoutParams = LayoutParams(
            if (actionHasText) LayoutParams.WRAP_CONTENT else buttonSize,
            buttonSize
        ).apply {
            endToEnd = LayoutParams.PARENT_ID
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
        }

        searchButton.layoutParams = LayoutParams(buttonSize, buttonSize).apply {
            if (actionButton.visibility == VISIBLE) {
                endToStart = actionButton.id
            } else {
                endToEnd = LayoutParams.PARENT_ID
            }
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
        }

        val titleEndTarget = when {
            searchButton.visibility == VISIBLE -> searchButton.id
            actionButton.visibility == VISIBLE -> actionButton.id
            else -> LayoutParams.PARENT_ID
        }
        titleView.gravity = if (centerTitle) {
            Gravity.CENTER
        } else {
            Gravity.CENTER_VERTICAL
        }
        titleView.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT).apply {
            topToTop = LayoutParams.PARENT_ID
            bottomToBottom = LayoutParams.PARENT_ID
            if (centerTitle) {
                marginStart = buttonSize + titleMargin
                marginEnd = buttonSize + titleMargin
                startToStart = LayoutParams.PARENT_ID
                endToEnd = LayoutParams.PARENT_ID
            } else {
                marginStart = titleMargin
                marginEnd = titleMargin
                startToEnd = backButton.id
                if (titleEndTarget == LayoutParams.PARENT_ID) {
                    endToEnd = LayoutParams.PARENT_ID
                } else {
                    endToStart = titleEndTarget
                }
            }
        }
    }

    private fun applyAttributes(attrs: AttributeSet?) {
        if (attrs == null) return
        context.obtainStyledAttributes(attrs, R.styleable.AppTopBarView).use { typedArray ->
            titleView.text = typedArray.getText(R.styleable.AppTopBarView_titleText)
            centerTitle = typedArray.getBoolean(R.styleable.AppTopBarView_centerTitle, centerTitle)
            inverseColors = typedArray.getBoolean(
                R.styleable.AppTopBarView_inverseColors,
                inverseColors
            )
            setBackVisible(typedArray.getBoolean(R.styleable.AppTopBarView_showBackButton, true))
            setSearchVisible(typedArray.getBoolean(R.styleable.AppTopBarView_showSearchButton, false))
            val searchIcon = typedArray.getResourceId(
                R.styleable.AppTopBarView_searchIcon,
                R.drawable.ia_ic_search
            )
            setSearchIconResource(searchIcon)
            setActionVisible(typedArray.getBoolean(R.styleable.AppTopBarView_showActionButton, false))
            val actionIcon = typedArray.getResourceId(
                R.styleable.AppTopBarView_actionIcon,
                R.drawable.ic_more_vertical
            )
            setActionIconResource(actionIcon)
            setActionText(typedArray.getText(R.styleable.AppTopBarView_actionText))
        }
    }

    private fun applyToolbarColors() {
        val color = ContextCompat.getColor(
            context,
            if (inverseColors) R.color.ds_neutral_white else R.color.ds_text_primary
        )
        val tint = ColorStateList.valueOf(color)
        titleView.setTextColor(color)
        backButton.iconTint = tint
        searchButton.iconTint = tint
        actionButton.iconTint = tint
    }
}
