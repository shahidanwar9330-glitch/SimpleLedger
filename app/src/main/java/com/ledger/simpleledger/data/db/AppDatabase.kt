package com.ledger.simpleledger.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ledger.simpleledger.data.db.dao.CategoryDao
import com.ledger.simpleledger.data.db.dao.PersonDao
import com.ledger.simpleledger.data.db.dao.TransactionDao
import com.ledger.simpleledger.data.db.entities.CategoryEntity
import com.ledger.simpleledger.data.db.entities.PersonEntity
import com.ledger.simpleledger.data.db.entities.TransactionEntity

@Database(
    entities = [PersonEntity::class, CategoryEntity::class, TransactionEntity::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        private const val DB_NAME = "simple_ledger.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN voiceNotePath TEXT")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    // Future schema changes must add proper Migration objects here
                    // instead of destructive fallback, to protect user data.
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build().also { INSTANCE = it }
            }
        }

        fun dbFile(context: Context) = context.getDatabasePath(DB_NAME)
    }
}
