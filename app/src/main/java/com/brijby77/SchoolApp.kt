package com.brijby77

import android.app.Application
import com.brijby77.data.database.AppDatabase

class SchoolApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }
}

