// app/src/main/java/com/example/zionkids/domain/sync/AssessmentQuestionPullWorker.kt
// Mirrors EventPullWorker: delta pull by updatedAt, overlap window, cursor poisoning guard,
// version+updatedAt merge, preserves local isDirty, supports tombstones (isDeleted/deletedAt).

package com.example.charityDept.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.model.AssessmentQuestion
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
class AssessmentQuestionPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val questionDao: AssessmentQuestionDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        // /// CHANGED: set this to your real Firestore collection name
        private const val QUESTIONS_COLLECTION = "assessment_questions"

        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 50
        private const val TAG = "AssessmentQuestionPullWorker"
        private const val APP_TAG = "ZionKidsDebug"

        private const val PREFS = "sync_prefs"
        private const val KEY_LAST_PULL_SECONDS = "assessment_questions_last_pull_seconds"
        private const val KEY_LAST_PULL_NANOS = "assessment_questions_last_pull_nanos"
        private const val KEY_LAST_PULL_DOC_ID = "assessment_questions_last_pull_doc_id"
        private const val KEY_LAST_SUCCESS_WALL_MS = "assessment_questions_last_success_wall_ms"

        private const val NORMAL_OVERLAP_MINUTES = 10L
        private const val EXTENDED_OVERLAP_HOURS = 12L
        private const val OFFLINE_THRESHOLD_HOURS = 6L

        private const val CURSOR_FUTURE_GUARD_HOURS = 5L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun assessmentQuestionDao(): AssessmentQuestionDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentQuestionDao(),
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

            // ✅ Guard poisoned STORED cursor
            run {
                val guardMs = TimeUnit.HOURS.toMillis(CURSOR_FUTURE_GUARD_HOURS)
                val maxAllowedCursorMs = nowMs + guardMs

                val storedCursorMs = (lastSec * 1000L) + (lastNanos / 1_000_000L)
                val hasStoredCursor = (lastSec != 0L) || lastDocId.isNotBlank()

                if (hasStoredCursor && storedCursorMs > maxAllowedCursorMs) {
                    val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                    val resetSec = resetMs / 1000L
                    val resetN = ((resetMs % 1000L) * 1_000_000L).toInt()

                    Log.w(
                        APP_TAG,
                        "$TAG STORED CURSOR POISONED storedMs=$storedCursorMs > maxAllowed=$maxAllowedCursorMs; resetting to now-12h"
                    )

                    lastSec = resetSec
                    lastNanos = resetN
                    lastDocId = ""

                    prefs.edit()
                        .putLong(KEY_LAST_PULL_SECONDS, lastSec)
                        .putInt(KEY_LAST_PULL_NANOS, lastNanos)
                        .putString(KEY_LAST_PULL_DOC_ID, "")
                        .putLong(KEY_LAST_SUCCESS_WALL_MS, 0L)
                        .commit()
                }
            }

            val hasCursor = (lastSec != 0L) || lastDocId.isNotBlank()
            val lastPulledAt = Timestamp(lastSec, lastNanos)

            // Overlap decision
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

            val ref = firestore.collection(QUESTIONS_COLLECTION)

            var page = 0
            var lastSnapshot: DocumentSnapshot? = null

            var newestSeenTs: Timestamp? = null
            var newestSeenDocId: String? = null

            var totalUpserts = 0

            while (page < MAX_PAGES) {
                var query: Query = ref
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

                val remoteList: List<AssessmentQuestion> =
                    snap.documents.mapNotNull { doc ->
                        try {
                            val updatedAt = doc.getTimestamp("updatedAt") ?: run {
                                failedDocIds += doc.id
                                Log.w(APP_TAG, "$TAG docId=${doc.id} missing updatedAt (skip). Fix writer.")
                                return@mapNotNull null
                            }

                            val version = doc.getLong("version") ?: run {
                                failedDocIds += doc.id
                                Log.w(APP_TAG, "$TAG docId=${doc.id} missing version (skip). Fix writer.")
                                return@mapNotNull null
                            }

                            val id = (doc.getString("questionId") ?: doc.id).ifBlank { doc.id }

                            val isDeleted = doc.getBoolean("isDeleted") ?: false
                            val deletedAt =
                                if (isDeleted) doc.getTimestamp("deletedAt") else null

                            val createdAt = doc.getTimestamp("createdAt") ?: updatedAt

                            AssessmentQuestion(
                                questionId = id,
                                category = doc.getString("category") ?: "",
                                subCategory = doc.getString("subCategory") ?: "",
                                categoryKey = doc.getString("categoryKey") ?: "",
                                subCategoryKey = doc.getString("subCategoryKey") ?: "",
                                question = doc.getString("question") ?: "",
                                isActive = doc.getBoolean("isActive") ?: true,
                                indexNum = (doc.getLong("indexNum") ?: 0L).toInt(),

                                createdAt = createdAt,
                                updatedAt = updatedAt,
                                isDirty = false,
                                isDeleted = isDeleted,
                                deletedAt = deletedAt,
                                version = version
                            )
                        } catch (ex: Exception) {
                            failedDocIds += doc.id
                            Log.w(TAG, "Failed to map docId=${doc.id}: ${ex.message}", ex)
                            null
                        }
                    }

                if (remoteList.isNotEmpty()) {
                    val ids = remoteList.map { it.questionId }
                    val localsById = questionDao.getByIds(ids).associateBy { it.questionId }

                    val merged = remoteList.map { remote ->
                        val local = localsById[remote.questionId]
                        when {
                            local == null -> remote
                            local.isDirty -> local

                            remote.version > local.version -> remote
                            remote.version < local.version -> local

                            tsToMs(remote.updatedAt) > tsToMs(local.updatedAt) -> remote
                            tsToMs(remote.updatedAt) < tsToMs(local.updatedAt) -> local

                            else -> local
                        }
                    }

                    questionDao.upsertAll(merged)
                    totalUpserts += merged.size

                    Log.e(APP_TAG, "$TAG upserted=${merged.size} (version+updatedAt enforced; dirty locals preserved)")
                }

                Log.d(
                    TAG,
                    "page=$page remoteDocs=${remoteList.size} failedToMap=${failedDocIds.size} failedIds=${failedDocIds.joinToString()}"
                )

                val lastDoc = snap.documents.last()
                lastSnapshot = lastDoc

                val lastUpdatedAt = lastDoc.getTimestamp("updatedAt")
                val lastVersion = lastDoc.getLong("version")

                if (lastUpdatedAt == null || lastVersion == null) {
                    Log.w(APP_TAG, "$TAG docId=${lastDoc.id} missing updatedAt/version; cursor won't advance using this doc.")
                } else {
                    newestSeenTs = lastUpdatedAt
                    newestSeenDocId = lastDoc.id
                }

                page++
                if (snap.size() < PAGE_SIZE) break
            }

            // ✅ Save cursor with future-guard
            if (newestSeenTs != null) {
                val guardMs = TimeUnit.HOURS.toMillis(CURSOR_FUTURE_GUARD_HOURS)
                val maxAllowedCursorMs = nowMs + guardMs

                val newestMs = (newestSeenTs!!.seconds * 1000L) + (newestSeenTs!!.nanoseconds / 1_000_000L)
                val poisoned = newestMs > maxAllowedCursorMs

                val (safeTs, safeDocId) = if (poisoned) {
                    val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                    val resetSec = resetMs / 1000L
                    val resetN = ((resetMs % 1000L) * 1_000_000L).toInt()
                    val resetTs = Timestamp(resetSec, resetN)

                    Log.w(
                        APP_TAG,
                        "$TAG CURSOR GUARD: newestSeenTs=$newestSeenTs poisoned; resetting cursor to $resetTs"
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

                Log.e(APP_TAG, "$TAG CURSOR SAVE ok=$ok savedTs=$safeTs savedDocId=$safeDocId totalUpserts=$totalUpserts")
            } else {
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

