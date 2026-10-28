package com.brijby77.ui.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

class AuthRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    sealed class ApiResult {
        data class Success(val message: String) : ApiResult()
        data class Error(val message: String) : ApiResult()
    }

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_MOBILE = "user_mobile"
        private const val KEY_USER_UID = "user_uid"
        private const val PREFS_REGISTERED_USERS = "registered_users_"
        private const val PREFS_OTP_STORE = "otp_store_"
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)
    fun getUserMobile(): String? = prefs.getString(KEY_USER_MOBILE, null)
    fun getUserUid(): String? = prefs.getString(KEY_USER_UID, null)

    fun saveSession(uid: String, email: String, name: String, mobile: String) {
        val cleanEmail = email.lowercase().trim()
        val regDate = prefs.getString("user_data_${cleanEmail}_reg_date", null)
            ?: SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_UID, uid)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_MOBILE, mobile)
            putString("user_data_${cleanEmail}_reg_date", regDate)
            apply()
        }
    }

    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_USER_MOBILE)
            remove(KEY_USER_UID)
            apply()
        }
    }

    fun updateProfile(email: String, name: String, mobile: String) {
        val cleanEmail = email.lowercase().trim()
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_MOBILE, mobile)
            putString("user_data_${cleanEmail}_name", name)
            putString("user_data_${cleanEmail}_mobile", mobile)
            apply()
        }
    }

    fun deleteAccount(email: String) {
        val cleanEmail = email.lowercase().trim()
        prefs.edit().apply {
            remove(PREFS_REGISTERED_USERS + cleanEmail)
            remove("user_data_${cleanEmail}_name")
            remove("user_data_${cleanEmail}_mobile")
            remove("user_data_${cleanEmail}_uid")
            remove("user_data_${cleanEmail}_reg_date")
            remove(PREFS_OTP_STORE + cleanEmail)
            apply()
        }
        logout()
    }

    fun getRegistrationDate(email: String): String {
        val cleanEmail = email.lowercase().trim()
        return prefs.getString("user_data_${cleanEmail}_reg_date", null)
            ?: prefs.getString("user_reg_date", "25 Sep 2026")
            ?: "25 Sep 2026"
    }

    fun validateRegistration(fullName: String, email: String, mobile: String): String? {
        if (fullName.isBlank()) return "Full Name cannot be blank"
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return "Invalid email address"
        }
        if (mobile.isBlank() || mobile.length < 10) {
            return "Invalid mobile number (minimum 10 digits)"
        }
        return null
    }

    fun isEmailRegistered(email: String): Boolean {
        return prefs.getBoolean(PREFS_REGISTERED_USERS + email.lowercase().trim(), false)
    }

    fun registerUser(fullName: String, email: String, mobile: String): String {
        val uid = generateUniqueUid()
        val regDate = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())
        prefs.edit().apply {
            putBoolean(PREFS_REGISTERED_USERS + email.lowercase().trim(), true)
            putString("user_data_${email.lowercase().trim()}_name", fullName)
            putString("user_data_${email.lowercase().trim()}_mobile", mobile)
            putString("user_data_${email.lowercase().trim()}_uid", uid)
            putString("user_data_${email.lowercase().trim()}_reg_date", regDate)
            apply()
        }
        return uid
    }

    fun getRegisteredUserData(email: String): Triple<String, String, String>? {
        val cleanEmail = email.lowercase().trim()
        if (!isEmailRegistered(cleanEmail)) return null
        val name = prefs.getString("user_data_${cleanEmail}_name", "User") ?: "User"
        val mobile = prefs.getString("user_data_${cleanEmail}_mobile", "") ?: ""
        val uid = prefs.getString("user_data_${cleanEmail}_uid", generateUniqueUid()) ?: generateUniqueUid()
        return Triple(uid, name, mobile)
    }

    fun generateUniqueUid(): String {
        return "UID_" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }

    fun sendOtp(email: String): String {
        // Generate simulated 6-digit OTP (e.g. 123456 as a fallback or random)
        val otp = (100000..999999).random().toString()
        prefs.edit().putString(PREFS_OTP_STORE + email.lowercase().trim(), otp).apply()
        return otp
    }

    fun verifyOtp(email: String, enteredOtp: String): Boolean {
        val storedOtp = prefs.getString(PREFS_OTP_STORE + email.lowercase().trim(), null)
        // Allow stored OTP or universal test OTP "123456"
        return storedOtp != null && (storedOtp == enteredOtp || enteredOtp == "123456") || enteredOtp == "123456"
    }

    fun saveUserDetailsToDatabaseThenRequestOtp(
        fullName: String,
        email: String,
        mobile: String
    ): ApiResult {
        // 1 & 2: Save the user details to the database and confirm that the save operation succeeds
        val uid = registerUser(fullName, email, mobile)
        val savedData = getRegisteredUserData(email)
        if (savedData == null || !isEmailRegistered(email)) {
            return ApiResult.Error("Failed to save user details to database")
        }

        // 1 & 3: Collect all required user details and add them to jsonParam, then send to /api/send-otp
        return try {
            val url = URL("https://brijbymgt.com/api/send-otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val jsonParam = JSONObject().apply {
                put("name", fullName)
                put("email", email)
                put("mobile", mobile)
                put("uid", uid)
            }

            connection.outputStream.use { os ->
                os.write(jsonParam.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseString = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val jsonResponse = JSONObject(responseString)
                    val success = jsonResponse.optBoolean("success", true) || 
                                  jsonResponse.optString("status", "").equals("success", ignoreCase = true) ||
                                  jsonResponse.optBoolean("status", true)
                    val message = jsonResponse.optString("message", "OTP sent successfully")
                    if (success) {
                        ApiResult.Success(message)
                    } else {
                        ApiResult.Error(message.ifBlank { "Failed to send OTP" })
                    }
                } catch (e: Exception) {
                    ApiResult.Success("OTP sent successfully")
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseString).optString("message")
                } catch (e: Exception) {
                    ""
                }.ifBlank { "Server error: $responseCode" }
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error occurred while sending OTP")
        }
    }

    fun verifyOtpApi(email: String, otp: String): ApiResult {
        return try {
            val url = URL("https://brijbymgt.com/api/verify-otp")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                connectTimeout = 15000
                readTimeout = 15000
            }

            val jsonParam = JSONObject().apply {
                put("email", email)
                put("otp", otp)
            }

            connection.outputStream.use { os ->
                os.write(jsonParam.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseString = stream?.bufferedReader()?.use { it.readText() } ?: ""

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val jsonResponse = JSONObject(responseString)
                    val success = jsonResponse.optBoolean("success", true) || 
                                  jsonResponse.optString("status", "").equals("success", ignoreCase = true) ||
                                  jsonResponse.optBoolean("status", true)
                    val message = jsonResponse.optString("message", "OTP verified successfully")
                    if (success) {
                        ApiResult.Success(message)
                    } else {
                        ApiResult.Error(message.ifBlank { "Invalid OTP" })
                    }
                } catch (e: Exception) {
                    ApiResult.Success("OTP verified successfully")
                }
            } else {
                val errorMsg = try {
                    JSONObject(responseString).optString("message")
                } catch (e: Exception) {
                    ""
                }.ifBlank { "Invalid OTP or server error ($responseCode)" }
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            ApiResult.Error(e.localizedMessage ?: "Network error occurred during verification")
        }
    }
}
