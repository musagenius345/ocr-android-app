package com.musagenius.ocrapp.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.musagenius.ocrapp.data.local.Converters
import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.local.entity.ScanEntity

/**
 * Main Room database for the OCR application
 * Contains all database tables and provides DAOs
 */
@Database(
    entities = [ScanEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * Get the ScanDao for performing database operations
     */
    abstract fun scanDao(): ScanDao

    companion object {
        const val DATABASE_NAME = "ocr_app_database"

        /**
         * Migration from version 1 to version 2
         * Example: Add this when schema changes are needed
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Example migration - uncomment and modify when needed:
                // database.execSQL("ALTER TABLE scans ADD COLUMN new_column TEXT DEFAULT ''")
            }
        }

        /**
         * Get all available migrations
         */
        fun getAllMigrations(): Array<Migration> {
            return arrayOf(
                // MIGRATION_1_2, // Add migrations here as the schema evolves
            )
        }
    }
}
