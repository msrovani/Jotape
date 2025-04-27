package com.jotape.data.local.converter

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Type converters for Room to handle non-primitive types like Instant.
 */
class TypeConverters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Instant? {
        return value?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Instant?): Long? {
        return date?.toEpochMilli()
    }
} 