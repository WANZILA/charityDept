package com.example.charityDept.domain.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.utils.ChildImageFileHelper
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.mappers.toChildOrNull
import com.example.charityDept.data.model.Child
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

@HiltWorker
class ChildrenPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val childDao: ChildDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val CHILDREN_COLLECTION = "children"
        private const val PAGE_SIZE = 500
        private const val MAX_PAGES = 50
        private const val TAG = "ChildrenPullWorker"
        private const val APP_TAG = "ZionKidsDebug"

        private const val PREFS = "sync_prefs"
        private const val KEY_LAST_PULL_SECONDS = "children_last_pull_seconds"
        private const val KEY_LAST_PULL_NANOS = "children_last_pull_nanos"
        private const val KEY_LAST_PULL_DOC_ID = "children_last_pull_doc_id"

        // Track last SUCCESS wall time for overlap choice
        private const val KEY_LAST_SUCCESS_WALL_MS = "children_last_success_wall_ms"

        // Overlap policy
        private const val NORMAL_OVERLAP_MINUTES = 10L
        private const val EXTENDED_OVERLAP_HOURS = 12L
        private const val OFFLINE_THRESHOLD_HOURS = 6L

        // Cursor poisoning guard: don’t allow cursor beyond now + 5 hours
        private const val CURSOR_FUTURE_GUARD_HOURS = 5L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun childDao(): ChildDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    private fun tsToMs(ts: Timestamp): Long {
        return (ts.seconds * 1000L) + (ts.nanoseconds / 1_000_000L)
    }

    private fun hasUsableLocalProfileImage(localPath: String): Boolean {
        return localPath.isNotBlank() && File(localPath).exists()
    }

    private suspend fun localizeChildProfileImage(
        remote: Child,
        local: Child?
    ): String {
        val remoteStoragePath = remote.profileImageStoragePath
        if (remoteStoragePath.isBlank()) {
            return local?.profileImageLocalPath.orEmpty()
        }

        val existingLocalPath = local?.profileImageLocalPath.orEmpty()
        val existingLocalOk = hasUsableLocalProfileImage(existingLocalPath)

        val sameStoragePath = local?.profileImageStoragePath == remote.profileImageStoragePath
        val sameUpdatedAt =
            local?.profileImageUpdatedAt?.seconds == remote.profileImageUpdatedAt?.seconds &&
                    local?.profileImageUpdatedAt?.nanoseconds == remote.profileImageUpdatedAt?.nanoseconds

        if (existingLocalOk && sameStoragePath && sameUpdatedAt) {
            return existingLocalPath
        }

        return try {
            val destFile = ChildImageFileHelper.getChildProfileFile(
                applicationContext,
                remote.childId
            )

            FirebaseStorage.getInstance()
                .reference
                .child(remoteStoragePath)
                .getFile(destFile)
                .await()

            Log.d(
                APP_TAG,
                "$TAG downloaded profile image for childId=${remote.childId} to ${destFile.absolutePath}"
            )
            destFile.absolutePath
        } catch (e: Exception) {
            Log.w(
                APP_TAG,
                "$TAG failed to download profile image for childId=${remote.childId} path=$remoteStoragePath",
                e
            )
            if (existingLocalOk) existingLocalPath else ""
        }
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

            val childrenRef = firestore.collection(CHILDREN_COLLECTION)

            var page = 0
            var lastSnapshot: DocumentSnapshot? = null

            var newestSeenTs: Timestamp? = null
            var newestSeenDocId: String? = null

            var totalUpserts = 0

            while (page < MAX_PAGES) {

                // ✅ Query: pull only docs with updatedAt >= effectiveCursor (delta window)
                // Stable ordering + pagination via DocumentSnapshot.
                var query: Query = childrenRef
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

                val remoteList: List<Child> =
                    snap.documents.mapNotNull { doc ->
                        try {
                            // ✅ ENFORCE updatedAt exists (mapper should already enforce, but we hard-check too)
                            val updatedAt = doc.getTimestamp("updatedAt") ?: run {
                                failedDocIds += doc.id
                                Log.w(APP_TAG, "$TAG docId=${doc.id} missing updatedAt (skip). Fix writer.")
                                return@mapNotNull null
                            }

                            // ✅ ENFORCE version exists (do NOT silently default to 0)
                            val version = doc.getLong("version") ?: run {
                                failedDocIds += doc.id
                                Log.w(APP_TAG, "$TAG docId=${doc.id} missing version (skip). Fix writer.")
                                return@mapNotNull null
                            }

                            val child = doc.toChildOrNull() ?: run {
                                failedDocIds += doc.id
                                return@mapNotNull null
                            }

                            val fixedIsDeleted = doc.getBoolean("isDeleted") ?: child.isDeleted
                            val fixedDeletedAt =
                                if (fixedIsDeleted) (doc.getTimestamp("deletedAt") ?: child.deletedAt) else null

                            child.copy(
                                childId = child.childId.ifBlank { doc.id },
                                isDirty = false,
                                updatedAt = updatedAt,
                                version = version,
                                isDeleted = fixedIsDeleted,
                                deletedAt = fixedDeletedAt
                            )
                        } catch (e: Exception) {
                            failedDocIds += doc.id
                            Log.w(TAG, "Failed to map docId=${doc.id}: ${e.message}", e)
                            null
                        }
                    }

                if (remoteList.isNotEmpty()) {
                    val ids = remoteList.map { it.childId }
                        .filter { it.isNotBlank() }
                        .distinct()

                    val localList =
                        if (ids.isNotEmpty()) childDao.getByIds(ids) else emptyList()

                    val localsById = localList.associateBy { it.childId }

                    val localizedRemoteList = remoteList.map { remote ->
                        val local = localsById[remote.childId]
                        val localizedPath = if (!remote.isDeleted) {
                            localizeChildProfileImage(remote, local)
                        } else {
                            ""
                        }

                        remote.copy(
                            profileImageLocalPath = localizedPath
                        )
                    }

                    // ✅ Merge rule:
                    // - never overwrite local dirty
                    // - else: higher version wins
                    // - if version ties: newer updatedAt wins
                    // - if still ties: keep local, and prevent tombstone resurrection on ties
                    // - if remote now has a localized path and local does not, prefer remote
                    val merged = localizedRemoteList.map { remote ->
                        val local = localsById[remote.childId]
                        when {
                            local == null -> remote

                            local.isDirty -> local

                            remote.isDeleted && !local.isDeleted -> remote

                            remote.version > local.version -> remote
                            remote.version < local.version -> local

                            tsToMs(remote.updatedAt) > tsToMs(local.updatedAt) -> remote
                            tsToMs(remote.updatedAt) < tsToMs(local.updatedAt) -> local

                            local.isDeleted && !remote.isDeleted -> local

                            remote.profileImageLocalPath.isNotBlank() &&
                                    local.profileImageLocalPath.isBlank() -> remote

                            else -> local
                        }
                    }

                    childDao.upsertAll(merged)
                    totalUpserts += merged.size
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

