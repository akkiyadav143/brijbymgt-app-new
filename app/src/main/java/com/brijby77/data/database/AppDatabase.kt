package com.brijby77.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.brijby77.data.dao.BankDetailsDao
import com.brijby77.data.dao.DocumentDao
import com.brijby77.data.dao.ExamDao
import com.brijby77.data.dao.ExamStudentDao
import com.brijby77.data.dao.HealthDetailsDao
import com.brijby77.data.dao.HeightWeightHistoryDao
import com.brijby77.data.dao.MarksDao
import com.brijby77.data.dao.MdmReportDao
import com.brijby77.data.dao.NoteDao
import com.brijby77.data.dao.StudentDao
import com.brijby77.data.dao.SubjectDao
import com.brijby77.data.entity.BankDetailsEntity
import com.brijby77.data.entity.DocumentEntity
import com.brijby77.data.entity.ExamEntity
import com.brijby77.data.entity.ExamStudentEntity
import com.brijby77.data.entity.HealthDetailsEntity
import com.brijby77.data.entity.HeightWeightHistoryEntity
import com.brijby77.data.entity.MarksEntity
import com.brijby77.data.entity.MdmReportEntity
import com.brijby77.data.entity.NoteEntity
import com.brijby77.data.entity.StudentEntity
import com.brijby77.data.entity.SubjectEntity

@Database(
    entities = [
        StudentEntity::class,
        HealthDetailsEntity::class,
        HeightWeightHistoryEntity::class,
        BankDetailsEntity::class,
        MdmReportEntity::class,
        ExamEntity::class,
        SubjectEntity::class,
        ExamStudentEntity::class,
        MarksEntity::class,
        DocumentEntity::class,
        NoteEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
    abstract fun healthDetailsDao(): HealthDetailsDao
    abstract fun heightWeightHistoryDao(): HeightWeightHistoryDao
    abstract fun bankDetailsDao(): BankDetailsDao
    abstract fun mdmReportDao(): MdmReportDao
    abstract fun examDao(): ExamDao
    abstract fun subjectDao(): SubjectDao
    abstract fun examStudentDao(): ExamStudentDao
    abstract fun marksDao(): MarksDao
    abstract fun documentDao(): DocumentDao
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "school_student_record_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

