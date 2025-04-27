package com.jotape.data.di

import android.content.Context
import androidx.room.Room
import com.jotape.data.local.AppDatabase
import com.jotape.data.local.dao.InteractionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of the AppDatabase.
     */
    @Provides
    @Singleton // Ensures only one instance of the database is created
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
        // .fallbackToDestructiveMigration() // Use migrations properly in production
        .build()
    }

    /**
     * Provides an instance of the InteractionDao.
     * Hilt will automatically get the AppDatabase instance from the provideAppDatabase function.
     */
    @Provides
    fun provideInteractionDao(appDatabase: AppDatabase): InteractionDao {
        return appDatabase.interactionDao()
    }

} 