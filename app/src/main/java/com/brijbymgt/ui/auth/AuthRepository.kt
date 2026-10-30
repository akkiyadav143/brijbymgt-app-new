package com.brijbymgt.ui.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.util.Patterns
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
        private const val TAG = "AuthRepository"
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

    private fun ensureFirebaseAuth(onReady: () -> Unit) {
        try {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser != null) {
                onReady()
            } else {
                auth.signInAnonymously()
                    .addOnSuccessListener {
                        Log.d(TAG, "Firebase Anonymous Auth SUCCESS: uid=${it.user?.uid}")
                        onReady()
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Firebase Anonymous Auth Warning: ${e.message}")
                        onReady()
                    }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase Auth Exception: ${e.message}")
            onReady()
        }
    }

    fun syncUserToFirebase(email: String, name: String, mobile: String, uid: String) {
        val cleanEmail = email.lowercase().trim()
        if (cleanEmail.isBlank()) return
        val sanitizedKey = cleanEmail.replace(".", "_")
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val userUid = if (uid.isNotBlank()) uid else generateUniqueUid()

        val userMap = hashMapOf<String, Any>(
            "uid" to userUid,
            "email" to cleanEmail,
            "name" to name,
            "mobile" to mobile,
            "updatedAt" to timestamp,
            "createdAt" to timestamp
        )

        Log.i(TAG, "Syncing user to Firebase... Email: $cleanEmail, Name: $name, Mobile: $mobile, UID: $userUid")

        val doSync = {
            // 1. Cloud Firestore write (Single document per user: users/{cleanEmail})
            try {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(cleanEmail)
                    .set(userMap, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.i(TAG, "Firestore write SUCCESS for $cleanEmail")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Firestore write FAILED for $cleanEmail: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Firestore Exception for $cleanEmail: ${e.message}", e)
            }

            // 2. Realtime Database write (Single node per user: users/{sanitizedKey})
            try {
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(sanitizedKey)
                    .setValue(userMap)
                    .addOnSuccessListener {
                        Log.i(TAG, "Realtime Database write SUCCESS for $cleanEmail")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Realtime Database write FAILED for $cleanEmail: ${e.message}", e)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Realtime Database Exception for $cleanEmail: ${e.message}", e)
            }
        }

        try {
            ensureFirebaseAuth {
                doSync()
            }
        } catch (e: Exception) {
            Log.w(TAG, "ensureFirebaseAuth Exception (${e.message}), performing direct sync")
            doSync()
        }
    }

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
        val uid = getUserUid() ?: generateUniqueUid()
        prefs.edit().apply {
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_MOBILE, mobile)
            putString("user_data_${cleanEmail}_name", name)
            putString("user_data_${cleanEmail}_mobile", mobile)
            apply()
        }
        syncUserToFirebase(cleanEmail, name, mobile, uid)
    }

    fun deleteAccount(email: String) {
        val cleanEmail = email.lowercase().trim()
        val sanitizedKey = cleanEmail.replace(".", "_")
        prefs.edit().apply {
            remove(PREFS_REGISTERED_USERS + cleanEmail)
            remove("user_data_${cleanEmail}_name")
            remove("user_data_${cleanEmail}_mobile")
            remove("user_data_${cleanEmail}_uid")
            remove("user_data_${cleanEmail}_reg_date")
            remove(PREFS_OTP_STORE + cleanEmail)
            apply()
        }
        try {
            FirebaseFirestore.getInstance().collection("users").document(cleanEmail).delete()
            FirebaseDatabase.getInstance().getReference("users").child(sanitizedKey).removeValue()
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user from Firebase: ${e.message}", e)
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
        val cleanEmail = email.lowercase().trim()
        prefs.edit().apply {
            putBoolean(PREFS_REGISTERED_USERS + cleanEmail, true)
            putString("user_data_${cleanEmail}_name", fullName)
            putString("user_data_${cleanEmail}_mobile", mobile)
            putString("user_data_${cleanEmail}_uid", uid)
            putString("user_data_${cleanEmail}_reg_date", regDate)
            apply()
        }
        syncUserToFirebase(cleanEmail, fullName, mobile, uid)
        return uid
    }

    fun getRegisteredUserData(email: String): Triple<String, String, String>? {
        val cleanEmail = email.lowercase().trim()
        if (!isEmailRegistered(cleanEmail)) return null
        val name = prefs.getString("user_data_${cleanEmail}_name", "") ?: ""
        val mobile = prefs.getString("user_data_${cleanEmail}_mobile", "") ?: ""
        val uid = prefs.getString("user_data_${cleanEmail}_uid", generateUniqueUid()) ?: generateUniqueUid()
        return Triple(uid, name, mobile)
    }

    fun generateUniqueUid(): String {
        return "UID_" + UUID.randomUUID().toString().substring(0, 8).uppercase()
    }

    fun sendOtpApi(
        email: String,
        fullName: String = "",
        mobile: String = "",
        uid: String = "",
        isRegister: Boolean = false
    ): ApiResult {
        val cleanEmail = email.lowercase().trim()
        val savedData = getRegisteredUserData(cleanEmail)
        val userUid = if (uid.isNotBlank()) uid else (savedData?.first ?: generateUniqueUid())
        val userName = if (fullName.isNotBlank()) fullName else (savedData?.second ?: "")
        val userMobile = if (mobile.isNotBlank()) mobile else (savedData?.third ?: "")

        Log.i(TAG, "sendOtpApi initiated: email=$cleanEmail, name=$userName, mobile=$userMobile, uid=$userUid, isRegister=$isRegister")

        val endpoints = if (isRegister) {
            listOf(
                "https://brijbymgt.com/api/register",
                "https://brijbymgt.com/api/signup",
                "https://brijbymgt.com/api/create-account",
                "https://brijbymgt.com/api/send-otp"
            )
        } else {
            listOf(
                "https://brijbymgt.com/api/send-otp"
            )
        }

        var lastErrorMsg = "Failed to send OTP"

        for (endpoint in endpoints) {
            try {
                val url = URL(endpoint)
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("Accept", "application/json")
                    doOutput = true
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                val jsonParam = JSONObject().apply {
                    put("email", cleanEmail)
                    if (userName.isNotBlank()) put("name", userName)
                    if (userMobile.isNotBlank()) put("mobile", userMobile)
                    if (userUid.isNotBlank()) put("uid", userUid)
                    if (isRegister) {
                        put("type", "register")
                        put("action", "register")
                        put("is_register", true)
                        put("signup", true)
                    }
                }

                Log.i(TAG, "POST Payload to $endpoint: $jsonParam")

                connection.outputStream.use { os ->
                    os.write(jsonParam.toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                val responseString = stream?.bufferedReader()?.use { it.readText() } ?: ""

                Log.i(TAG, "HTTP $responseCode from $endpoint: $responseString")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try {
                        val jsonResponse = JSONObject(responseString)
                        val isStatusError = jsonResponse.optString("status", "").equals("error", ignoreCase = true) ||
                                            jsonResponse.optString("status", "").equals("false", ignoreCase = true) ||
                                            !jsonResponse.optBoolean("status", true)
                        val isSuccessFalse = jsonResponse.has("success") && !jsonResponse.optBoolean("success", true)
                        val isExplicitError = isStatusError || isSuccessFalse

                        val message = jsonResponse.optString("message", "OTP sent successfully")

                        if (!isExplicitError && (
                            jsonResponse.optBoolean("success", false) ||
                            jsonResponse.optString("status", "").equals("success", ignoreCase = true) ||
                            jsonResponse.optString("status", "").equals("true", ignoreCase = true) ||
                            message.contains("sent", ignoreCase = true) ||
                            message.contains("success", ignoreCase = true)
                        )) {
                            Log.i(TAG, "sendOtpApi SUCCESS on $endpoint: $message")
                            return ApiResult.Success(message)
                        } else {
                            lastErrorMsg = message.ifBlank { "Failed to send OTP" }
                        }
                    } catch (e: Exception) {
                        Log.i(TAG, "sendOtpApi OK non-json response on $endpoint: $responseString")
                        return ApiResult.Success("OTP sent successfully")
                    }
                } else {
                    val errorMsg = try {
                        JSONObject(responseString).optString("message")
                    } catch (e: Exception) {
                        ""
                    }.ifBlank { "Server error: $responseCode" }
                    lastErrorMsg = errorMsg
                }
            } catch (e: Exception) {
                Log.e(TAG, "Endpoint $endpoint Exception: ${e.message}", e)
                lastErrorMsg = e.localizedMessage ?: "Network error occurred"
            }
        }

        return ApiResult.Error(lastErrorMsg)
    }

    fun saveUserDetailsToDatabaseThenRequestOtp(
        fullName: String,
        email: String,
        mobile: String
    ): ApiResult {
        val cleanEmail = email.lowercase().trim()
        val uid = registerUser(fullName, cleanEmail, mobile)

        val result = sendOtpApi(
            email = cleanEmail,
            fullName = fullName,
            mobile = mobile,
            uid = uid,
            isRegister = true
        )

        if (result is ApiResult.Error) {
            // Clean up unverified registration from local storage if API call fails
            prefs.edit().apply {
                remove(PREFS_REGISTERED_USERS + cleanEmail)
                remove("user_data_${cleanEmail}_name")
                remove("user_data_${cleanEmail}_mobile")
                remove("user_data_${cleanEmail}_uid")
                remove("user_data_${cleanEmail}_reg_date")
                apply()
            }
        }

        return result
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
                put("email", email.lowercase().trim())
                put("otp", otp.trim())
            }

            Log.i(TAG, "POST Payload to /api/verify-otp: $jsonParam")

            connection.outputStream.use { os ->
                os.write(jsonParam.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val responseString = stream?.bufferedReader()?.use { it.readText() } ?: ""

            Log.i(TAG, "verifyOtpApi Response Code: $responseCode, Response Body: $responseString")

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try {
                    val jsonResponse = JSONObject(responseString)
                    val isStatusError = jsonResponse.optString("status", "").equals("error", ignoreCase = true) ||
                                        jsonResponse.optString("status", "").equals("false", ignoreCase = true) ||
                                        !jsonResponse.optBoolean("status", true)
                    val isSuccessFalse = jsonResponse.has("success") && !jsonResponse.optBoolean("success", true)
                    val isExplicitError = isStatusError || isSuccessFalse

                    val message = jsonResponse.optString("message", "OTP verified successfully")

                    if (!isExplicitError && (
                        jsonResponse.optBoolean("success", false) ||
                        jsonResponse.optString("status", "").equals("success", ignoreCase = true) ||
                        jsonResponse.optString("status", "").equals("true", ignoreCase = true) ||
                        message.contains("verified", ignoreCase = true) ||
                        message.contains("success", ignoreCase = true)
                    )) {
                        Log.i(TAG, "verifyOtpApi SUCCESS: $message")
                        ApiResult.Success(message)
                    } else {
                        Log.w(TAG, "verifyOtpApi FAILED from JSON message: $message")
                        ApiResult.Error(message.ifBlank { "Invalid OTP" })
                    }
                } catch (e: Exception) {
                    Log.i(TAG, "verifyOtpApi OK non-json response: $responseString")
                    ApiResult.Success("OTP verified successfully")
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
            Log.e(TAG, "verifyOtpApi Exception: ${e.message}", e)
            ApiResult.Error(e.localizedMessage ?: "Network error occurred during verification")
        }
    }
}
