package com.axelliant.hris.core.network

object ApiErrorMessages {
    const val NO_INTERNET = "No internet connection. Please try again."
    const val TIMEOUT = "The request timed out. Please try again."
    const val UNAUTHORIZED = "Your session has expired. Please log in again."
    const val UNKNOWN = "Something went wrong. Please try again."
    const val EMPTY_BODY = "Response body is empty."
}
