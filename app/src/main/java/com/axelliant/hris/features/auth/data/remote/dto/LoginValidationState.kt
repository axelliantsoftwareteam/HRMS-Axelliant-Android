package com.axelliant.hris.features.auth.data.remote.dto

import androidx.annotation.StringRes

data class LoginValidationState(
    @StringRes val emailErrorRes: Int? = null,
    @StringRes val passwordErrorRes: Int? = null
)
