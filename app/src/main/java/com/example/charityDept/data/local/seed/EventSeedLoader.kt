package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.room.withTransaction
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.EventStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object EventSeedLoader {

    private const val ASSET_FILE = "events_seed.json"

    suspend fun seedIfEventsEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        if (!isEventsTableEmpty(db)) return@withContext

        val json = readAsset(context, ASSET_FILE)
        val root = JSONObject(json)

        val eventsJson = root.optJSONArray("events") ?: JSONArray()
        if (eventsJson.length() == 0) return@withContext

        val events = ArrayList<Event>(eventsJson.length())
        for (i in 0 until eventsJson.length()) {
            val o = eventsJson.getJSONObject(i)
            events.add(o.toEventSeeded())
        }

        // Insert seeds as NOT dirty (offline-first SoT is Room)
        db.withTransaction { db.eventDao().upsertAll(events) }
    }

    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun isEventsTableEmpty(db: AppDatabase): Boolean {
        return runCatching {
            val c = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM events")
            c.use {
                if (!it.moveToFirst()) return true
                it.getLong(0) == 0L
            }
        }.getOrElse { true }
    }


    private fun JSONObject.toEventSeeded(): Event {
        val eventId = optString("eventId", "")
        val isChild = optInt("isChild", 0) == 1 || optBoolean("isChild", false)

        val eventDateMs = optLong("eventDate", 0L)
        val createdAtMs = optLong("createdAt", 0L)
        val updatedAtMs = optLong("updatedAt", 0L)
        val deletedAtMs: Long? = if (isNull("deletedAt")) null else optLong("deletedAt", 0L)

        // Your Event model uses non-null Timestamp, so provide safe defaults
        val eventDateTs = tsFromMillisOrNull(eventDateMs) ?: Timestamp.now()
        val createdAtTs = tsFromMillisOrNull(createdAtMs) ?: Timestamp.now()
        val updatedAtTs = tsFromMillisOrNull(updatedAtMs) ?: Timestamp.now()
        val deletedAtTs = tsFromMillisOrNull(deletedAtMs) // nullable

        val statusStr = optString("eventStatus", EventStatus.SCHEDULED.name)
        val status = runCatching { EventStatus.valueOf(statusStr) }.getOrElse { EventStatus.SCHEDULED }

        return Event(
            eventId = eventId,
            isChild = isChild,
            eventParentId = optString("eventParentId", ""),
            title = optString("title", ""),
            eventDate = eventDateTs,

            teamName = optString("teamName", ""),
            teamLeaderNames = optString("teamLeaderNames", ""),
            leaderTelephone1 = optString("leaderTelephone1", ""),
            leaderTelephone2 = optString("leaderTelephone2", ""),
            leaderEmail = optString("leaderEmail", ""),

            location = optString("location", ""),
            eventStatus = status,
            notes = optString("notes", ""),
            adminId = optString("adminId", ""),

            createdAt = createdAtTs,
            updatedAt = updatedAtTs,

            isDirty = false, // seeded rows are clean

            isDeleted = optInt("isDeleted", 0) == 1 || optBoolean("isDeleted", false),
            deletedAt = deletedAtTs,
            version = optLong("version", 0L)
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

