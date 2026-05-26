package com.nary.gym.ui.utils

object FieldValidators {
    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email is required"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
            else -> null
        }
    }

    fun validatePhoneNumber(phone: String): String? {
        val digits = phone.filter { it.isDigit() }
        return when {
            phone.isBlank() -> "Phone number is required"
            digits.length !in 9..13 -> "Phone number must be between 9 and 13 digits"
            else -> null
        }
    }

    fun validateName(name: String): String? {
        return when{
            name.isBlank() -> "Name cannot be empty"
            name.length !in 3..25 -> "Name must be between 3 and 25 characters"
            else -> null
        }
    }

    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 8 -> "Password length should be at least 8 characters"
            !password.any { it.isUpperCase() } -> "Password should have at least one uppercase letter"
            !password.any { it.isLowerCase() } -> "Password should have at least one lowercase letter"
            !password.any { it.isDigit() } -> "Password should have at least one number"
            !password.any { !it.isLetterOrDigit() } -> "Password should contain at least 1 special character"
            else -> null

        }
    }
}