package com.example.charityDept.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.mappers.toEventOrNull
import com.example.charityDept.data.model.Event
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

@HiltWorker
class EventPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val eventDao: EventDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val EVENTS_COLLECTION = "events"
        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 50
        private const val TAG = "EventPullWorker"
        private const val APP_TAG = "ZionKidsDebug"

        private const val PREFS = "sync_prefs"
        private const val KEY_LAST_PULL_SECONDS = "events_last_pull_seconds"
        private const val KEY_LAST_PULL_NANOS = "events_last_pull_nanos"
        private const val KEY_LAST_PULL_DOC_ID = "events_last_pull_doc_id"

        // Track last SUCCESS wall time for overlap choice
        private const val KEY_LAST_SUCCESS_WALL_MS = "events_last_success_wall_ms"

        // Overlap policy (as requested)
        private const val NORMAL_OVERLAP_MINUTES = 10L
        private const val EXTENDED_OVERLAP_HOURS = 12L
        private const val OFFLINE_THRESHOLD_HOURS = 6L

        // Cursor poisoning guard: don’t allow cursor beyond now + 5 hours
        private const val CURSOR_FUTURE_GUARD_HOURS = 5L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun eventDao(): EventDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).eventDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    private fun tsToMs(ts: Timestamp): Long {
        return (ts.seconds * 1000L) + (ts.nanoseconds / 1_000_000L)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            var lastSec = prefs.getLong(KEY_LAST_PULL_SECONDS, 0L)
            var lastNanos = prefs.getInt(KEY_LAST_PULL_NANOS, 0)
            var lastDocId = prefs.getString(KEY_LAST_PULL_DOC_ID, "") ?: ""

            val nowMs = System.currentTimeMillis()

            // ✅ Early guard: if STORED cursor is already poisoned, reset it BEFORE querying.
            run {
                val guardMs = TimeUnit.HOURS.toMillis(CURSOR_FUTURE_GUARD_HOURS)
                val maxAllowedCursorMs = nowMs + guardMs

                val storedCursorMs = (lastSec * 1000L) + (lastNanos / 1_000_000L)
                val hasStoredCursor = (lastSec != 0L) || lastDocId.isNotBlank()

                if (hasStoredCursor && storedCursorMs > maxAllowedCursorMs) {
                    val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                    val resetSec = resetMs / 1000L
                    val resetNanos = ((resetMs % 1000L) * 1_000_000L).toInt()

                    Log.w(
                        APP_TAG,
                        "$TAG STORED CURSOR POISONED storedMs=$storedCursorMs > maxAllowed=$maxAllowedCursorMs; resetting cursor to now-12h"
                    )

                    lastSec = resetSec
                    lastNanos = resetNanos
                    lastDocId = ""

                    prefs.edit()
                        .putLong(KEY_LAST_PULL_SECONDS, lastSec)
                        .putInt(KEY_LAST_PULL_NANOS, lastNanos)
                        .putString(KEY_LAST_PULL_DOC_ID, "")
                        .putLong(KEY_LAST_SUCCESS_WALL_MS, 0L) // force extended overlap behavior
                        .commit()
                }
            }

            val hasCursor = (lastSec != 0L) || lastDocId.isNotBlank()
            val lastPulledAt = Timestamp(lastSec, lastNanos)

            // Overlap window decision
            val lastSuccessWallMs = prefs.getLong(KEY_LAST_SUCCESS_WALL_MS, 0L)
            val sinceSuccessMs =
                if (lastSuccessWallMs == 0L) Long.MAX_VALUE else (nowMs - lastSuccessWallMs)

            val useExtendedOverlap =
                (runAttemptCount > 0) ||
                        (lastSuccessWallMs == 0L) ||
                        (sinceSuccessMs > TimeUnit.HOURS.toMillis(OFFLINE_THRESHOLD_HOURS))

            val overlapMs =
                if (useExtendedOverlap) TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)
                else TimeUnit.MINUTES.toMillis(NORMAL_OVERLAP_MINUTES)

            // Move cursor backward by overlap window (if we have a cursor)
            val effectiveCursor: Timestamp =
                if (hasCursor) {
                    val cursorMs = (lastSec * 1000L) + (lastNanos / 1_000_000L)
                    val effectiveMs = (cursorMs - overlapMs).coerceAtLeast(0L)
                    val effSec = effectiveMs / 1000L
                    val effNanos = ((effectiveMs % 1000L) * 1_000_000L).toInt()
                    Timestamp(effSec, effNanos)
                } else {
                    lastPulledAt
                }

            Log.e(
                APP_TAG,
                "$TAG START pull hasCursor=$hasCursor lastPulledAt=$lastPulledAt lastDocId=$lastDocId " +
                        "effectiveCursor=$effectiveCursor overlapMs=$overlapMs extended=$useExtendedOverlap attempt=$runAttemptCount"
            )

            val eventsRef = firestore.collection(EVENTS_COLLECTION)

            var page = 0
            var lastSnapshot: DocumentSnapshot? = null

            var newestSeenTs: Timestamp? = null
            var newestSeenDocId: String? = null

            var totalUpserts = 0

            while (page < MAX_PAGES) {
                // ✅ Query: pull only docs with updatedAt >= effectiveCursor (delta window)
                // Stable ordering + pagination via DocumentSnapshot.
                var query: Query = eventsRef
                    .whereGreaterThanOrEqualTo("updatedAt", effectiveCursor)
                    .orderBy("updatedAt", Query.Direction.ASCENDING)
                    .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                    .limit(PAGE_SIZE.toLong())

                if (page > 0 && lastSnapshot != null) {
                    query = query.startAfter(lastSnapshot!!)
                }

                val snap = query.get(Source.SERVER).await()
                Log.d(TAG, "page=$page server snapSize=${snap.size()}")

                if (snap.isEmpty) {
                    Log.d(TAG, "DONE no changes page=$page")
                    break
                }

                val failedDocIds = mutableListOf<String>()

                val remoteList: List<Event> =
                    snap.documents.mapNotNull { doc ->
                        try {
                            // ✅ ENFORCE updatedAt exists: your toEventOrNull() already returns null if missing updatedAt
                            val e = doc.toEventOrNull() ?: run {
                                failedDocIds += doc.id
                                return@mapNotNull null
                            }

                            // ✅ ENFORCE version exists on the Firestore doc (do NOT silently default to 0)
                            val version = doc.getLong("version") ?: run {
                                failedDocIds += doc.id
                                Log.w(APP_TAG, "$TAG docId=${doc.id} missing version (skip). Fix writer.")
                                return@mapNotNull null
                            }

                            val fixedId = e.eventId.ifBlank { doc.id }

                            val fixedIsDeleted = doc.getBoolean("isDeleted") ?: e.isDeleted
                            val fixedDeletedAt =
                                if (fixedIsDeleted) (doc.getTimestamp("deletedAt") ?: e.deletedAt) else null

                            e.copy(
                                eventId = fixedId,
                                isDirty = false,
                                isDeleted = fixedIsDeleted,
                                deletedAt = fixedDeletedAt,
                                version = version
                            )
                        } catch (ex: Exception) {
                            failedDocIds += doc.id
                            Log.w(TAG, "Failed to map docId=${doc.id}: ${ex.message}", ex)
                            null
                        }
                    }

                if (remoteList.isNotEmpty()) {
                    val ids = remoteList.map { it.eventId }
                    val localsById = eventDao.getByIds(ids).associateBy { it.eventId }

                    // ✅ Merge rule:
                    // - never overwrite local dirty
                    // - else: higher version wins
                    // - if version ties: newer updatedAt wins
                    // - if still ties: keep local (stable)
                    val merged = remoteList.map { remote ->
                        val local = localsById[remote.eventId]
                        when {
                            local == null -> remote
                            local.isDirty -> local

//                            remote.version > local.version -> remote
//                            remote.version < local.version -> local
                            remote.version > local.version -> remote
                            remote.version < local.version -> local

                            // version tie => compare updatedAt
                            tsToMs(remote.updatedAt) > tsToMs(local.updatedAt) -> remote
                            tsToMs(remote.updatedAt) < tsToMs(local.updatedAt) -> local

                            else -> local
                        }
                    }

                    eventDao.upsertAll(merged)
                    totalUpserts += merged.size

                    Log.e(APP_TAG, "$TAG upserted=${merged.size} (version+updatedAt enforced; dirty locals preserved)")
                }

                Log.d(
                    TAG,
                    "page=$page remoteDocs=${remoteList.size} failedToMap=${failedDocIds.size} failedIds=${failedDocIds.joinToString()}"
                )

                val lastDoc = snap.documents.last()
                lastSnapshot = lastDoc

                // ✅ ENFORCE cursor fields used for ordering
                val lastUpdatedAt = lastDoc.getTimestamp("updatedAt")
                val lastVersion = lastDoc.getLong("version")

                if (lastUpdatedAt == null || lastVersion == null) {
                    // If missing, we do NOT move the cursor forward based on this doc.
                    Log.w(
                        APP_TAG,
                        "$TAG docId=${lastDoc.id} missing updatedAt/version; cursor will not advance using this doc. Fix writer."
                    )
                } else {
                    newestSeenTs = lastUpdatedAt
                    newestSeenDocId = lastDoc.id
                }

                page++
                if (snap.size() < PAGE_SIZE) break
            }

            // ✅ Save cursor (with poisoning reset-to-past)
            if (newestSeenTs != null) {
                val guardMs = TimeUnit.HOURS.toMillis(CURSOR_FUTURE_GUARD_HOURS)
                val maxAllowedCursorMs = nowMs + guardMs

                val newestMs = (newestSeenTs!!.seconds * 1000L) + (newestSeenTs!!.nanoseconds / 1_000_000L)
                val poisoned = newestMs > maxAllowedCursorMs

                val (safeTs, safeDocId) = if (poisoned) {
                    val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                    val resetSec = resetMs / 1000L
                    val resetNanos = ((resetMs % 1000L) * 1_000_000L).toInt()
                    val resetTs = Timestamp(resetSec, resetNanos)

                    Log.w(
                        APP_TAG,
                        "$TAG CURSOR GUARD: newestSeenTs=$newestSeenTs is poisoned (newestMs=$newestMs > maxAllowed=$maxAllowedCursorMs); " +
                                "resetting cursor to resetTs=$resetTs (docId cleared)"
                    )
                    resetTs to ""
                } else {
                    newestSeenTs!! to (newestSeenDocId ?: "")
                }

                val ok = prefs.edit()
                    .putLong(KEY_LAST_PULL_SECONDS, safeTs.seconds)
                    .putInt(KEY_LAST_PULL_NANOS, safeTs.nanoseconds)
                    .putString(KEY_LAST_PULL_DOC_ID, safeDocId)
                    .putLong(KEY_LAST_SUCCESS_WALL_MS, nowMs)
                    .commit()

                Log.e(
                    APP_TAG,
                    "$TAG CURSOR SAVE ok=$ok savedTs=$safeTs savedDocId=$safeDocId totalUpserts=$totalUpserts"
                )
            } else {
                // Still mark success so we don’t stay in extended overlap forever
                prefs.edit().putLong(KEY_LAST_SUCCESS_WALL_MS, nowMs).apply()
                Log.e(APP_TAG, "$TAG CURSOR UNCHANGED (no safe updates) totalUpserts=$totalUpserts")
            }

            Result.success()
        } catch (t: Throwable) {
            if (t is FirebaseFirestoreException) {
                if (t.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    Log.e(APP_TAG, "$TAG Firestore UNAVAILABLE (network/TLS). retry. ${t.message}", t)
                    return@withContext Result.retry()
                }
                if (t.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                    Log.e(APP_TAG, "$TAG Firestore index error: ${t.message}", t)
                    return@withContext Result.retry()
                }
                if (t.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    Log.e(APP_TAG, "$TAG Firestore permission denied: ${t.message}", t)
                    return@withContext Result.failure()
                }
            }

            Log.e(APP_TAG, "$TAG failed; will retry. ${t.message}", t)
            Result.retry()
        }
    }
}

