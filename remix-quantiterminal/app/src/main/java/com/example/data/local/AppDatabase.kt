package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        VirtualMachineEntity::class,
        InstalledPackageEntity::class,
        VirtualFileEntity::class,
        CommandHistoryEntity::class,
        TerminalSessionEntity::class,
        GeminiChatMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun virtualMachineDao(): VirtualMachineDao
    abstract fun installedPackageDao(): InstalledPackageDao
    abstract fun virtualFileDao(): VirtualFileDao
    abstract fun commandHistoryDao(): CommandHistoryDao
    abstract fun terminalSessionDao(): TerminalSessionDao
    abstract fun geminiChatDao(): GeminiChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cloudterm_vm_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
