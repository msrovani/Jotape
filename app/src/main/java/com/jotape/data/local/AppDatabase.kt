package com.jotape.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.jotape.data.local.converter.TypeConverters as AppTypeConverters // Update import
import com.jotape.data.local.dao.InteractionDao // Update import
import com.jotape.data.local.model.InteractionEntity // Update import

/**
 * The Room database for the application.
 */
@Database(
    entities = [InteractionEntity::class],
    version = 1, // Start with version 1. Increment if schema changes.
    exportSchema = false // Schema export is recommended for complex projects, but false is simpler for now.
)
@TypeConverters(AppTypeConverters::class) // Register the type converters
abstract class AppDatabase : RoomDatabase() {

    abstract fun interactionDao(): InteractionDao

    companion object {
        // We'll use Hilt for providing the instance, so no need for Singleton pattern here.
        const val DATABASE_NAME = "jotape_database"
    }
} 