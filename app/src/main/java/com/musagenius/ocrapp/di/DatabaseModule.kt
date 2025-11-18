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

        // PRODUCTION READINESS CHECKLIST (Complete before v1.0.0):
        // ✅ Migration safety: fallbackToDestructiveMigration() restricted to DEBUG builds only
        // ⚠️ BEFORE RELEASE: Verify all database migrations are tested (see AppDatabase.kt)
        // ⚠️ BEFORE RELEASE: Test migration paths from all schema versions
        // 💡 OPTIONAL: Add onDestructiveMigration callback to log unexpected data loss

        // Development aid: Allow destructive migration in debug builds only
        // WARNING: This will DESTROY USER DATA if migration fails. Safe because DEBUG only.
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
