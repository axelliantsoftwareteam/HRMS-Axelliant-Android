package com.axelliant.hris.ui.designsystem.components

import android.app.Dialog
import android.view.View
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner

fun Dialog.installAppViewTreeOwners(
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner?,
    savedStateRegistryOwner: SavedStateRegistryOwner?
) {
    listOfNotNull(
        window?.decorView,
        window?.decorView?.findViewById(android.R.id.content)
    ).forEach { view ->
        view.installAppViewTreeOwners(
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = viewModelStoreOwner,
            savedStateRegistryOwner = savedStateRegistryOwner
        )
    }
}

fun View.installAppViewTreeOwners(
    lifecycleOwner: LifecycleOwner,
    viewModelStoreOwner: ViewModelStoreOwner?,
    savedStateRegistryOwner: SavedStateRegistryOwner?
) {
    setViewTreeLifecycleOwner(lifecycleOwner)
    viewModelStoreOwner?.let(::setViewTreeViewModelStoreOwner)
    savedStateRegistryOwner?.let(::setViewTreeSavedStateRegistryOwner)
}
