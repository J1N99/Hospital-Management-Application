package com.example.hospitalmanagementapplication.utils

class PasswordValidation {
    companion object {
        fun isPasswordValid(password: String): Boolean {
            // Implement your custom password validation logic here
            // For example, you can check length, character types, etc.
            // Return true if the password is valid, otherwise return false
            val minLength = 8 // Minimum password length
            val hasUppercase = password.any { it.isUpperCase() }
            val hasLowercase = password.any { it.isLowerCase() }
            val hasDigit = password.any { it.isDigit() }
            val hasSpecialChar = password.any { it.isLetterOrDigit().not() }

            return password.length >= minLength &&
                    hasUppercase &&
                    hasLowercase &&
                    hasDigit &&
                    hasSpecialChar
        }
    }
}