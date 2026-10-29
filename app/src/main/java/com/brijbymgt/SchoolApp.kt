package com.brijbymgt

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.brijbymgt.data.database.AppDatabase

class SchoolApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    companion object {
        lateinit var instance: SchoolApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            FirebaseApp.initializeApp(this)
            Log.i("SchoolApp", "FirebaseApp initialized successfully")
        } catch (e: Exception) {
            Log.e("SchoolApp", "FirebaseApp initialization failed: ${e.message}", e)
        }
    }
}
