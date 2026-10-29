package com.brijbymgt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuthRepositoryTest {

    @Test
    fun testUserDataFormattingForFirestore() {
        val email = "testuser@example.com"
        val name = "Test User"
        val mobile = "9876543210"
        val uid = "UID_TEST1234"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val userMap = hashMapOf<String, Any>(
            "uid" to uid,
            "email" to email,
            "name" to name,
            "mobile" to mobile,
            "updatedAt" to timestamp,
            "createdAt" to timestamp
        )

        assertEquals("UID_TEST1234", userMap["uid"])
        assertEquals("testuser@example.com", userMap["email"])
        assertEquals("Test User", userMap["name"])
        assertEquals("9876543210", userMap["mobile"])
        assertNotNull(userMap["updatedAt"])
    }

    @Test
    fun testEmailSanitization() {
        val rawEmail = "  User.Name@Example.Com  "
        val cleanEmail = rawEmail.lowercase().trim()
        val sanitizedKey = cleanEmail.replace(".", "_")

        assertEquals("user.name@example.com", cleanEmail)
        assertEquals("user_name@example_com", sanitizedKey)
    }
}
