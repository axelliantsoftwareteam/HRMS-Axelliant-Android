package com.axelliant.hrms.network

enum class ErrorMessages(val errorString: String) {
    NoInternetError("No internet error"),
    SocketException("Unknown host name error"),
    SocketTimeout("Socket time out exception"),
    UnknownError("Unknown error occurs"),
    NotFound404("Api not found"),
    SessionExpired401("Session expired, please login again not found"),
    BadRequest400("Bad request"),
    InternalServerError500("Internal server error"),
    UNABLE_TO_EDIT_LEAVE("You cannot edit leave type, but you can still delete this pending leave")
}