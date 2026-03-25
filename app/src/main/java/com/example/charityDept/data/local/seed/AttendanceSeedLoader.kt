package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.room.withTransaction
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object AttendanceSeedLoader {

    private const val ASSET_FILE = "attendances_seed.json"

    suspend fun seedIfAttendancesEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        if (!isAttendancesTableEmpty(db)) return@withContext

        val json = readAsset(context, ASSET_FILE)
        val root = JSONObject(json)

        val attendancesJson = root.optJSONArray("attendances") ?: JSONArray()
        if (attendancesJson.length() == 0) return@withContext

        val attendances = ArrayList<Attendance>(attendancesJson.length())
        for (i in 0 until attendancesJson.length()) {
            val o = attendancesJson.getJSONObject(i)
            attendances.add(o.toAttendanceSeeded())
        }

        // Insert seeds as NOT dirty (offline-first SoT is Room)
//        db.attendanceDao().upsertAll(attendances)
        db.withTransaction { db.attendanceDao().upsertAll(attendances) }

    }

    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun isAttendancesTableEmpty(db: AppDatabase): Boolean {
        return runCatching {
            val c = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM attendances")
            c.use {
                if (!it.moveToFirst()) return true
                it.getLong(0) == 0L
            }
        }.getOrElse { true }
    }

    private fun JSONObject.toAttendanceSeeded(): Attendance {
        val attendanceId = optString("attendanceId", "")

        val createdAtMs = optLong("createdAt", 0L)
        val updatedAtMs = optLong("updatedAt", 0L)
        val checkedAtMs: Long? = if (isNull("checkedAt")) null else optLong("checkedAt", 0L)
        val deletedAtMs: Long? = if (isNull("deletedAt")) null else optLong("deletedAt", 0L)

        val createdAtTs = tsFromMillisOrNull(createdAtMs) ?: Timestamp.now()
        val updatedAtTs = tsFromMillisOrNull(updatedAtMs) ?: Timestamp.now()
        val checkedAtTs = tsFromMillisOrNull(checkedAtMs) // nullable
        val deletedAtTs = tsFromMillisOrNull(deletedAtMs) // nullable

        val statusStr = optString("status", AttendanceStatus.ABSENT.name)
        val status = runCatching { AttendanceStatus.valueOf(statusStr) }.getOrElse { AttendanceStatus.ABSENT }

        return Attendance(
            attendanceId = attendanceId,
            childId = optString("childId", ""),
            eventId = optString("eventId", ""),
            adminId = optString("adminId", ""),

            status = status,
            notes = optString("notes", ""),

            isDeleted = optInt("isDeleted", 0) == 1 || optBoolean("isDeleted", false),
            deletedAt = deletedAtTs,

            isDirty = false, // seeded rows are clean
            version = optLong("version", 0L),

            createdAt = createdAtTs,
            updatedAt = updatedAtTs,
            checkedAt = checkedAtTs
        )
    }

    private fun tsFromMillisOrNull(ms: Long?): Timestamp? {
        val v = ms ?: return null
        if (v <= 0L) return null
        val seconds = v / 1000L
        val nanos = ((v % 1000L).toInt()) * 1_000_000
        return Timestamp(seconds, nanos)
    }
}

