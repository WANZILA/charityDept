package com.example.charityDept.data.local.db

import androidx.room.TypeConverter
import com.example.charityDept.data.model.Country
import com.google.firebase.Timestamp

object TimestampConverters {

    private const val NANOS_PER_SECOND = 1_000_000_000L

    @TypeConverter
    @JvmStatic
    fun fromTimestamp(ts: Timestamp?): Long? {
        if (ts == null) return null
        // nanosSinceEpoch = seconds * 1e9 + nanos
        return ts.seconds * NANOS_PER_SECOND + ts.nanoseconds.toLong()
    }

    @TypeConverter
    @JvmStatic
    fun toTimestamp(value: Long?): Timestamp? {
        if (value == null) return null
        val seconds = value / NANOS_PER_SECOND
        val nanos = (value % NANOS_PER_SECOND).toInt()
        return Timestamp(seconds, nanos)
    }

    @TypeConverter
    fun fromCountry(value: Country?): String {
        return (value ?: Country.UGANDA).name
    }

    @TypeConverter
    fun toCountry(value: String?): Country {
        return try {
            Country.valueOf(value ?: Country.UGANDA.name)
        } catch (e: Exception) {
            Country.UGANDA
        }
    }
}

