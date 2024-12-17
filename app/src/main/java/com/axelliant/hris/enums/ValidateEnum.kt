package com.axelliant.hris.enums

enum class ValidateEnum(val value: String) {
    Password("Password cannot be empty"),
    CurrentPassword("Password cannot be empty"),
    ConfrimPassword("Confirmed password cannot be empty"),
    ValidatePassword("Password length must me at least 8 char which contains one upper, one lower and one special character"),
    CurrentValidatePassword("Password length must me at least 8 char which contains one upper, one lower and one special character"),
    ValidateConfrimPassword("Confirm Password length must me at least 8 char which contains one upper, one lower and one special character"),
    EqualityConfrimPassword("Password and confirmed password do not match"),
    ValidPassword("Password is valid"),
    InvalidDay("Invalid Day"),
    InvalidMonth("Invalid Month"),
    InvalidYear("Invalid Year"),
    Valid("valid"),
    ChooseInterest("Please choose interests"),
    EnterBio("Please enter bio"),
    QuizEnterText("Please enter text"),
    QuizChooseOption("Please choose an option")
}