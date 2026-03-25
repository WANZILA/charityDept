//package com.example.charityDept.core.sync
//
//
//// /// CHANGED: new tiny prefs helper to track last successful pull timestamps/versions; minimal-diff (no DB schema)
//
//import android.content.Context
//import com.google.firebase.Timestamp
//import java.util.Date
//
//class SyncPrefs(context: Context) {
//    private val sp = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
//
//    fun lastVersion(): Long = sp.getLong("children_last_version", 0L)
//    fun lastUpdatedAt(): Timestamp =
//        Timestamp(Date(sp.getLong("children_last_updated_ms", 0L)))
//
//    fun setLast(version: Long, updatedAt: Timestamp) {
//        sp.edit()
//            .putLong("children_last_version", version)
//            .putLong("children_last_updated_ms", updatedAt.toDate().time)
//            .apply()
//    }
//}

