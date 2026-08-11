package com.axelliant.hris.ui.designsystem.components

import android.content.Context
import com.axelliant.hris.R
import com.google.android.material.bottomsheet.BottomSheetDialog

fun Context.createAppBottomSheetDialog(): BottomSheetDialog =
    BottomSheetDialog(this, R.style.ThemeOverlay_Fluent2_BottomSheetDialog)
