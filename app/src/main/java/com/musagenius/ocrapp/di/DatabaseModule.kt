package com.musagenius.ocrapp.di

import android.content.Context
import androidx.room.Room
import com.musagenius.ocrapp.BuildConfig
import com.musagenius.ocrapp.data.local.dao.ScanDao
import com.musagenius.ocrapp.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Database module
 * Provides Room database and DAO instances
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(*AppDatabase.getAllMigrations())

        // TODO: BEFORE PRODUCTION RELEASE
        // 1. Remove fallbackToDestructiveMigration() completely for production
        // 2. Verify all migrations in AppDatabase.getAllMigrations() are complete and tested
        // 3. Test migration paths from every schema version to the latest
        // 4. Consider adding onDestructiveMigration callback to log data loss events
        // Issue: Create GitHub issue to track migration verification before v1.0.0

        // WARNING: fallbackToDestructiveMigration() DESTROYS USER DATA on migration failure
        // Only enabled in debug builds to aid development. NEVER ship to production.
        if (BuildConfig.DEBUG) {
            builder.fallbackToDestructiveMigration()
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideScanDao(database: AppDatabase): ScanDao {
        return database.scanDao()
    }
}
