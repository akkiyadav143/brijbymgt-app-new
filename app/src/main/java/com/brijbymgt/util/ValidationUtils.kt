package com.brijbymgt.util

object ValidationUtils {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null
    )

    fun validateName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult(false, "Student Name is required")
        }
        if (name.trim().length < 2) {
            return ValidationResult(false, "Name must be at least 2 characters")
        }
        return ValidationResult(true)
    }

    fun validateClass(studentClass: String): ValidationResult {
        if (studentClass.isBlank()) {
            return ValidationResult(false, "Class is required")
        }
        return ValidationResult(true)
    }

    fun validateGender(gender: String): ValidationResult {
        if (gender.isBlank() || (!gender.equals("Male", ignoreCase = true) && !gender.equals("Female", ignoreCase = true))) {
            return ValidationResult(false, "Gender selection is required")
        }
        return ValidationResult(true)
    }

    fun validateDbt(dbt: String): ValidationResult {
        if (dbt.isBlank()) {
            return ValidationResult(false, "DBT selection is required")
        }
        return ValidationResult(true)
    }

    fun validateSrNumber(srNumber: String): ValidationResult {
        if (srNumber.isNotBlank() && srNumber.trim().length < 1) {
            return ValidationResult(false, "Invalid SR Number format")
        }
        return ValidationResult(true)
    }

    fun validateAdhar(adhar: String, fieldName: String = "Adhar"): ValidationResult {
        if (adhar.isNotBlank()) {
            val cleaned = adhar.replace(" ", "").replace("-", "")
            if (cleaned.length != 12 || !cleaned.all { it.isDigit() }) {
                return ValidationResult(false, "$fieldName must be 12 digits")
            }
        }
        return ValidationResult(true)
    }

    fun validateMobile(mobile: String): ValidationResult {
        if (mobile.isNotBlank()) {
            val cleaned = mobile.trim()
            if (cleaned.length != 10 || !cleaned.all { it.isDigit() }) {
                return ValidationResult(false, "Mobile Number must be 10 digits")
            }
        }
        return ValidationResult(true)
    }

    fun validateIfsc(ifsc: String): ValidationResult {
        if (ifsc.isNotBlank()) {
            val cleaned = ifsc.trim().uppercase()
            if (cleaned.length != 11) {
                return ValidationResult(false, "IFSC Code must be 11 characters")
            }
        }
        return ValidationResult(true)
    }

    fun validateAccountNo(accountNo: String): ValidationResult {
        if (accountNo.isNotBlank()) {
            val cleaned = accountNo.trim()
            if (cleaned.length < 6 || !cleaned.all { it.isDigit() }) {
                return ValidationResult(false, "Account Number must be valid digits")
            }
        }
        return ValidationResult(true)
    }
}

