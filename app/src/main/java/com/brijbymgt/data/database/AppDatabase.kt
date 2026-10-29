package com.brijbymgt.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.brijbymgt.data.dao.BankDetailsDao
import com.brijbymgt.data.dao.DocumentDao
import com.brijbymgt.data.dao.ExamDao
import com.brijbymgt.data.dao.ExamStudentDao
import com.brijbymgt.data.dao.HealthDetailsDao
import com.brijbymgt.data.dao.HeightWeightHistoryDao
import com.brijbymgt.data.dao.MarksDao
import com.brijbymgt.data.dao.MdmReportDao
import com.brijbymgt.data.dao.NoteDao
import com.brijbymgt.data.dao.StudentDao
import com.brijbymgt.data.dao.SubjectDao
import com.brijbymgt.data.entity.BankDetailsEntity
import com.brijbymgt.data.entity.DocumentEntity
import com.brijbymgt.data.entity.ExamEntity
import com.brijbymgt.data.entity.ExamStudentEntity
import com.brijbymgt.data.entity.HealthDetailsEntity
import com.brijbymgt.data.entity.HeightWeightHistoryEntity
import com.brijbymgt.data.entity.MarksEntity
import com.brijbymgt.data.entity.MdmReportEntity
import com.brijbymgt.data.entity.NoteEntity
import com.brijbymgt.data.entity.StudentEntity
import com.brijbymgt.data.entity.SubjectEntity

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

