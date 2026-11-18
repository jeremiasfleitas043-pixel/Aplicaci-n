package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Database(
    entities = [CleaningHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cleaningHistoryDao(): CleaningHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clean_master_ai.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class CleaningHistoryRepository(private val dao: CleaningHistoryDao) {
    val allHistory: Flow<List<CleaningHistoryEntity>> = dao.getAllHistory()
    val recentHistory: Flow<List<CleaningHistoryEntity>> = dao.getRecentHistory(5)

    suspend fun recordEntry(
        appName: String,
        packageName: String,
        operationType: String,
        status: String,
        recoveredBytes: Long?,
        notes: String = ""
    ) {
        dao.insertHistory(
            CleaningHistoryEntity(
                timestampMillis = System.currentTimeMillis(),
                appName = appName,
                packageName = packageName,
                operationType = operationType,
                status = status,
                recoveredBytes = recoveredBytes,
                notes = notes
            )
        )
    }

    suspend fun clearAll() {
        dao.clearAllHistory()
    }
}
