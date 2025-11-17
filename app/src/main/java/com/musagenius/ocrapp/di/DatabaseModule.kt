package com.musagenius.ocrapp.di

import android.content.Context
import androidx.room.Room
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
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(*AppDatabase.getAllMigrations())
            .fallbackToDestructiveMigration() // Remove in production after first release
            .build()
    }

    @Provides
    @Singleton
    fun provideScanDao(database: AppDatabase): ScanDao {
        return database.scanDao()
    }
}
