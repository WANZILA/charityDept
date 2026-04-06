package com.example.charityDept.core.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DatesUtils {

    fun Timestamp.asHuman(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
        return sdf.format(toDate())
    }

    fun Long.asHuman(): String {
        // this Long is nanosSinceEpoch (per TimestampConverters)
        val seconds = this / 1_000_000_000L
        val nanos = (this % 1_000_000_000L).toInt()
        return Timestamp(seconds, nanos).asHuman()
    }
}

