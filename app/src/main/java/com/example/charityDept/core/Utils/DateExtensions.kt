package com.example.charityDept.core.Utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toTimestamp(): Timestamp = Timestamp(Date(this))
fun Timestamp.toMillis(): Long = this.toDate().time

@RequiresApi(Build.VERSION_CODES.O)
fun formatDate(ts: Timestamp?): String {
    if (ts == null) return ""
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(ts.toDate())
}

fun Timestamp?.fmt(pattern: String = "yyyy-MM-dd HH:mm"): String =
    this?.toDate()?.let { SimpleDateFormat(pattern, Locale.getDefault()).format(it) } ?: "—"
