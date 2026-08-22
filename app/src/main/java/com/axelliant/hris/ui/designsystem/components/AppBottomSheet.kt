package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.axelliant.hris.R
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.createAppBottomSheetDialog(): BottomSheetDialog =
    AppBottomSheetDialog(this)

private class AppBottomSheetDialog(
    context: Context
) : BottomSheetDialog(context, R.style.ThemeOverlay_Fluent2_BottomSheetDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installViewTreeOwners()
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        installViewTreeOwners()
    }

    override fun setContentView(view: View) {
        super.setContentView(view)
        installViewTreeOwners(view)
    }

    override fun setContentView(view: View, params: ViewGroup.LayoutParams?) {
        super.setContentView(view, params)
        installViewTreeOwners(view)
    }

    override fun show() {
        super.show()
        installViewTreeOwners()
    }

    private fun installViewTreeOwners(contentView: View? = null) {
        val lifecycleOwner = context.findOwner<LifecycleOwner>() ?: return
        val viewModelStoreOwner = context.findOwner<ViewModelStoreOwner>()
        val savedStateRegistryOwner = context.findOwner<SavedStateRegistryOwner>()

        listOfNotNull(
            contentView,
            window?.decorView,
            window?.decorView?.findViewById(android.R.id.content),
            findViewById(com.google.android.material.R.id.container),
        ).forEach { view ->
            view.setViewTreeLifecycleOwner(lifecycleOwner)
            viewModelStoreOwner?.let(view::setViewTreeViewModelStoreOwner)
            savedStateRegistryOwner?.let(view::setViewTreeSavedStateRegistryOwner)
        }
    }
}

private inline fun <reified T> Context.findOwner(): T? {
    var current: Context? = this
    while (current != null) {
        if (current is T) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
