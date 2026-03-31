package com.example.charityDept.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.model.AssessmentTaxonomy
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
class AssessmentTaxonomyPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val taxonomyDao: AssessmentTaxonomyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val COLLECTION = "assessment_taxonomy"
        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 50
        private const val TAG = "AssessmentTaxonomyPullWorker"
        private const val APP_TAG = "ZionKidsDebug"

        private const val PREFS = "sync_prefs"
        private const val KEY_LAST_PULL_SECONDS = "assessment_taxonomy_last_pull_seconds"
        private const val KEY_LAST_PULL_NANOS = "assessment_taxonomy_last_pull_nanos"
        private const val KEY_LAST_PULL_DOC_ID = "assessment_taxonomy_last_pull_doc_id"
        private const val KEY_LAST_SUCCESS_WALL_MS = "assessment_taxonomy_last_success_wall_ms"

        private const val NORMAL_OVERLAP_MINUTES = 10L
        private const val EXTENDED_OVERLAP_HOURS = 12L
        private const val OFFLINE_THRESHOLD_HOURS = 6L
        private const val CURSOR_FUTURE_GUARD_HOURS = 5L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun assessmentTaxonomyDao(): AssessmentTaxonomyDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentTaxonomyDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    private fun tsToMs(ts: Timestamp): Long =
        (ts.seconds * 1000L) + (ts.nanoseconds / 1_000_000L)

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

            var lastSec = prefs.getLong(KEY_LAST_PULL_SECONDS, 0L)
            var lastNanos = prefs.getInt(KEY_LAST_PULL_NANOS, 0)
            var lastDocId = prefs.getString(KEY_LAST_PULL_DOC_ID, "") ?: ""

            val nowMs = System.currentTimeMillis()
            val guardMs = TimeUnit.HOURS.toMillis(CURSOR_FUTURE_GUARD_HOURS)
            val maxAllowedCursorMs = nowMs + guardMs
            val storedCursorMs = (lastSec * 1000L) + (lastNanos / 1_000_000L)
            val hasStoredCursor = (lastSec != 0L) || lastDocId.isNotBlank()

            if (hasStoredCursor && storedCursorMs > maxAllowedCursorMs) {
                val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                lastSec = resetMs / 1000L
                lastNanos = ((resetMs % 1000L) * 1_000_000L).toInt()
                lastDocId = ""
            }

            val lastSuccessWallMs = prefs.getLong(KEY_LAST_SUCCESS_WALL_MS, 0L)
            val sinceSuccessMs = if (lastSuccessWallMs == 0L) Long.MAX_VALUE else (nowMs - lastSuccessWallMs)
            val useExtendedOverlap =
                (runAttemptCount > 0) ||
                        (lastSuccessWallMs == 0L) ||
                        (sinceSuccessMs > TimeUnit.HOURS.toMillis(OFFLINE_THRESHOLD_HOURS))

            val overlapMs =
                if (useExtendedOverlap) TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)
                else TimeUnit.MINUTES.toMillis(NORMAL_OVERLAP_MINUTES)

            val effectiveCursor = if (hasStoredCursor) {
                val cursorMs = (lastSec * 1000L) + (lastNanos / 1_000_000L)
                val effectiveMs = (cursorMs - overlapMs).coerceAtLeast(0L)
                Timestamp(effectiveMs / 1000L, ((effectiveMs % 1000L) * 1_000_000L).toInt())
            } else {
                Timestamp(lastSec, lastNanos)
            }

            val ref = firestore.collection(COLLECTION)

            var page = 0
            var lastSnapshot: DocumentSnapshot? = null
            var newestSeenTs: Timestamp? = null
            var newestSeenDocId: String? = null

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
                if (snap.isEmpty) break

                val remoteList = snap.documents.mapNotNull { doc ->
                    try {
                        val updatedAt = doc.getTimestamp("updatedAt") ?: return@mapNotNull null
                        val version = doc.getLong("version") ?: return@mapNotNull null
                        val id = (doc.getString("taxonomyId") ?: doc.id).ifBlank { doc.id }
                        val isDeleted = doc.getBoolean("isDeleted") ?: false
                        val deletedAt = if (isDeleted) doc.getTimestamp("deletedAt") else null
                        val createdAt = doc.getTimestamp("createdAt") ?: updatedAt

                        AssessmentTaxonomy(
                            taxonomyId = id,
                            assessmentKey = doc.getString("assessmentKey") ?: "",
                            assessmentLabel = doc.getString("assessmentLabel") ?: "",
                            categoryKey = doc.getString("categoryKey") ?: "",
                            categoryLabel = doc.getString("categoryLabel") ?: "",
                            subCategoryKey = doc.getString("subCategoryKey") ?: "",
                            subCategoryLabel = doc.getString("subCategoryLabel") ?: "",
                            indexNum = (doc.getLong("indexNum") ?: 0L).toInt(),
                            isActive = doc.getBoolean("isActive") ?: true,
                            createdAt = createdAt,
                            updatedAt = updatedAt,
                            isDirty = false,
                            isDeleted = isDeleted,
                            deletedAt = deletedAt,
                            version = version
                        )
                    } catch (ex: Exception) {
                        Log.w(TAG, "Failed to map taxonomy docId=${doc.id}: ${ex.message}", ex)
                        null
                    }
                }

                if (remoteList.isNotEmpty()) {
                    val ids = remoteList.map { it.taxonomyId }
                    val localsById = taxonomyDao.getByIds(ids).associateBy { it.taxonomyId }

                    val merged = remoteList.map { remote ->
                        val local = localsById[remote.taxonomyId]
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

                    taxonomyDao.upsertAll(merged)
                }

                val lastDoc = snap.documents.last()
                lastSnapshot = lastDoc
                val lastUpdatedAt = lastDoc.getTimestamp("updatedAt")
                val lastVersion = lastDoc.getLong("version")

                if (lastUpdatedAt != null && lastVersion != null) {
                    newestSeenTs = lastUpdatedAt
                    newestSeenDocId = lastDoc.id
                }

                page++
                if (snap.size() < PAGE_SIZE) break
            }

            if (newestSeenTs != null) {
                val newestMs = (newestSeenTs!!.seconds * 1000L) + (newestSeenTs!!.nanoseconds / 1_000_000L)
                val poisoned = newestMs > maxAllowedCursorMs

                val safeTs = if (poisoned) {
                    val resetMs = (nowMs - TimeUnit.HOURS.toMillis(EXTENDED_OVERLAP_HOURS)).coerceAtLeast(0L)
                    Timestamp(resetMs / 1000L, ((resetMs % 1000L) * 1_000_000L).toInt())
                } else {
                    newestSeenTs!!
                }

                prefs.edit()
                    .putLong(KEY_LAST_PULL_SECONDS, safeTs.seconds)
                    .putInt(KEY_LAST_PULL_NANOS, safeTs.nanoseconds)
                    .putString(KEY_LAST_PULL_DOC_ID, if (poisoned) "" else (newestSeenDocId ?: ""))
                    .putLong(KEY_LAST_SUCCESS_WALL_MS, nowMs)
                    .apply()
            } else {
                prefs.edit().putLong(KEY_LAST_SUCCESS_WALL_MS, nowMs).apply()
            }

            Result.success()
        } catch (t: Throwable) {
            if (t is FirebaseFirestoreException) {
                if (t.code == FirebaseFirestoreException.Code.UNAVAILABLE) return@withContext Result.retry()
                if (t.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION) return@withContext Result.retry()
                if (t.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return@withContext Result.failure()
            }
            Result.retry()
        }
    }
}