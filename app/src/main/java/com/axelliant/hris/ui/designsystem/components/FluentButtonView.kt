package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.util.AttributeSet
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.runtime.Composable
import com.axelliant.hris.ui.designsystem.theme.AppFluentTheme

/**
 * XML bridge that renders the real Fluent 2 Compose button.
 *
 * XML:
 * <com.axelliant.hris.ui.designsystem.components.FluentButtonView
 *     android:id="@+id/saveButton"
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content" />
 *
 * Kotlin:
 * binding.saveButton.text = getString(R.string.save)
 * binding.saveButton.setOnFluentClickListener { save() }
 */
class FluentButtonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : AbstractComposeView(context, attrs) {

    var text: String = ""
        set(value) {
            field = value
            invalidate()
        }

    var buttonStyle: AppButtonStyle = AppButtonStyle.Primary
        set(value) {
            field = value
            invalidate()
        }

    private var action: () -> Unit = {}

    fun setOnFluentClickListener(listener: () -> Unit) {
        action = listener
    }

    @Composable
    override fun Content() {
        AppFluentTheme {
            AppButton(
                text = text,
                onClick = action,
                style = buttonStyle,
                enabled = isEnabled,
            )
        }
    }
}
