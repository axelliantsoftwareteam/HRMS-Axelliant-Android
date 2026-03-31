package com.axelliant.hris.utils

// Validator.kt
import android.util.Patterns
import com.axelliant.hris.enums.ValidateEnum
import java.util.Calendar

class Validator() {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val isEmailError: Boolean = true
    )


    fun validateEmptyField(value: String?): ValidationResult {
        if (value.isNullOrBlank()) {
            return ValidationResult(false, "This field cannot be empty", true)
        }
        return ValidationResult(true)
    }


    fun validateNameField(name: String?): ValidationResult {

        if (name.isNullOrBlank()) {
            return ValidationResult(false, "Name field cannot be empty", true)
        }

        // Use a regex to check if the name contains only alphabetic characters

        if (name.length < 3) {
            return ValidationResult(false, "Name Length must be at-least 3 characters", true)
        }

         val regex = Regex("^[a-z ,.'-]+$")
        if (!regex.matches(name)) {
            return ValidationResult(false, "Name must contain only alphabetic characters", true)
        }

        return ValidationResult(true)
    }

    fun validateEmailAndPhoneField(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult(false, "This field can not empty", true)
        }

//        // Check if the entered email has a valid format
//        if (!isEmailValid(email)) {
//            return ValidationResult(false, "Please enter a valid email address", true)
//        }

        return ValidationResult(true)
    }

    fun validateEmailField(email: String?): ValidationResult {
        if (email.isNullOrBlank()) {
            return ValidationResult(false, "Please enter email", true)
        }

        // Check if the entered email has a valid format
        if (!isEmailValid(email)) {
            return ValidationResult(false, "Please enter a valid email address", true)
        }


        return ValidationResult(true)
    }


    fun validatePhoneNoField(phoneNo: String?): ValidationResult {
        if (phoneNo.isNullOrBlank()) {
            return ValidationResult(false, "Please enter phone number", true)
        }
        // Check if the phone number contains only numeric characters
        val regex = Regex("^[0-9]+$")
        if (!regex.matches(phoneNo)) {
            return ValidationResult(
                false,
                "Phone number must contain only numeric characters",
                true
            )
        }
        // Check if the length of the phone number is exactly 10 digits
        if (phoneNo.length != 10) {
            return ValidationResult(false, "Phone number must be 10 digits", true)
        }

        return ValidationResult(true)
    }


    fun validatePasswordField(password: String?): ValidationResult {
        // Check for non-empty fields

        if (password.isNullOrBlank()) {
            return ValidationResult(false, "Password cannot be empty", false)
        }

        // Check password criteria
        if (!isPasswordValid(password)) {
            return ValidationResult(
                false,
                "Password length must me at least 8 char which contains one upper, one lower and one special character",
                false
            )

        }

        // All validations passed
        return ValidationResult(true)
    }

     fun isEmailValid(email: String?): Boolean {
        return !email.isNullOrBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isPasswordValid(password: String?): Boolean {
        password?.let {
            // Check password length
            if (it.length < 8) return false

            // Check for at least one digit
            if (!it.any { char -> char.isDigit() }) return false

            // Check for at least one upper and one lower case letter
            if (!(it.any { char -> char.isLowerCase() })) return false

            // Check for at least one special character
            val specialChars = "!@#$%^&*()_-+=<>?/{}|\\[]~"
            return it.any { char -> specialChars.contains(char) }
        }
        return false
    }

    fun validateDateField(day: String?, month: String?, year: String?): ValidationResult {

        if (!validateDay(day)) {
            return ValidationResult(false, ValidateEnum.InvalidDay.value, false)
        }
        if (!validateMonth(month)) {
            return ValidationResult(false, ValidateEnum.InvalidMonth.value, false)

        }
        if (!validateYear(year)) {
            return ValidationResult(false, ValidateEnum.InvalidYear.value, false)
        }

        return ValidationResult(true, ValidateEnum.Valid.value, false)
    }

    private fun validateDay(day: String?): Boolean {

        if (day.isNullOrBlank()) {
            return false
        }
        val dayValue = day.toIntOrNull()
        return dayValue != null && dayValue in 1..31
    }

    private fun validateMonth(month: String?): Boolean {
        if (month.isNullOrBlank()) {
            return false
        }

        val monthValue = month.toIntOrNull()
        return monthValue != null && monthValue in 1..12
    }

    private fun validateYear(year: String?): Boolean {
        if (year.isNullOrBlank()) {
            return false
        }
        val yearValue = year.toIntOrNull()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        return yearValue != null && yearValue in 1900..currentYear
    }

    fun validateConfirmPasswordField(
        password: String?,
        confirmedPassword: String?
    ): ValidationResult {

        if (password.isNullOrBlank()) {
            return ValidationResult(false, ValidateEnum.Password.value, true)
        }
        if (!isPasswordValid(password)) {
            return ValidationResult(
                false,
                ValidateEnum.ValidatePassword.value,
                false
            )
        }
        if (confirmedPassword.isNullOrBlank()) {

            return ValidationResult(false, ValidateEnum.ConfrimPassword.value, true)
        }

        if (!isPasswordValid(confirmedPassword)) {
            return ValidationResult(
                false,
                ValidateEnum.ValidateConfrimPassword.value,
                false
            )
        }
        if (password != confirmedPassword) {
            return ValidationResult(false, ValidateEnum.EqualityConfrimPassword.value, false)
        }
        // Add any other password validation rules as needed
        return ValidationResult(true, ValidateEnum.ValidPassword.value)

    }

    fun validatePasswordSettingField(
//        currentPassword: String?,
        newPassword: String?,
        confirmedPassword: String?
    ): ValidationResult {
//        if (currentPassword.isNullOrBlank()) {
//            return ValidationResult(false, ValidateEnum.CurrentPassword.value, true)
//        }
//         if (!isPasswordValid(currentPassword)) {
//            return ValidationResult(false, ValidateEnum.CurrentValidatePassword.value, false)
//        }
        if (newPassword.isNullOrBlank()) {
            return ValidationResult(false, ValidateEnum.Password.value, true)
        }
         if (!isPasswordValid(newPassword)) {
            return ValidationResult(
                false,
                ValidateEnum.ValidatePassword.value,
                false
            )
        }

         if (confirmedPassword.isNullOrBlank()) {

            return ValidationResult(false, ValidateEnum.ConfrimPassword.value, true)
        }
         if (!isPasswordValid(confirmedPassword)) {
            return ValidationResult(
                false,
                ValidateEnum.ValidateConfrimPassword.value,
                false
            )
        }

         if (newPassword != confirmedPassword) {
            return ValidationResult(false, ValidateEnum.EqualityConfrimPassword.value, false)
        }
        // Add any other password validation rules as needed
        return ValidationResult(true, ValidateEnum.ValidPassword.value)

    }

}
