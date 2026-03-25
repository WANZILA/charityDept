package com.example.charityDept.domain.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.ChildrenRef
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.Child
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
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
import timber.log.Timber
import kotlin.system.measureTimeMillis

@HiltWorker
class ChildrenSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @ChildrenRef private val childrenRef: CollectionReference,
    private val childDao: ChildDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "ChildrenSyncWorker"
        private const val MAX_BATCH = 500
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        @ChildrenRef fun childrenRef(): CollectionReference
        fun childDao(): ChildDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childrenRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val totalMs = measureTimeMillis {
            Timber.i("%s: start attempt=%d", TAG, runAttemptCount)
        }

        try {
            // 1) Collect dirty children
            val dirty = childDao.loadDirtyBatch(limit = MAX_BATCH)
            Timber.i("%s: loaded dirty=%d", TAG, dirty.size)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()

            val toPush = mutableListOf<Child>()
            val conflictRemotes = mutableListOf<Child>()

            // 2) For each dirty local, check remote version+updatedAt from SNAPSHOT fields (avoid defaults)
            val prefetchMs = measureTimeMillis {
                for (local in dirty) {
                    if (local.childId.isBlank()) {
                        Timber.w("%s skipping dirty row with blank childId", TAG)
                        continue
                    }

                    val docRef = childrenRef.document(local.childId)

                    // Force server read so we don't compare against stale cache
                    val snap = docRef.get(Source.SERVER).await()

                    if (!snap.exists()) {
                        toPush += local
                        continue
                    }

                    val remoteVersion = snap.getLong("version")
                    val remoteUpdatedAt = snap.getTimestamp("updatedAt")

                    // If remote is missing required fields, treat LOCAL as winner (but log loudly)
                    if (remoteVersion == null || remoteUpdatedAt == null) {
                        Timber.w(
                            "%s remote missing fields childId=%s remoteVersion=%s remoteUpdatedAt=%s; pushing local",
                            TAG, local.childId, remoteVersion, remoteUpdatedAt
                        )
                        toPush += local
                        continue
                    }

                    val localVersion = local.version
                    val localUpdatedAt = local.updatedAt

                    // Server-wins rule:
                    // 1) higher version wins
                    // 2) if version ties, newer updatedAt wins
                    val serverWins =
                        (remoteVersion > localVersion) ||
                                (remoteVersion == localVersion &&
                                        remoteUpdatedAt.toDate().time > localUpdatedAt.toDate().time)

                    if (serverWins) {
                        Timber.d(
                            "%s server-wins childId=%s (remote.version=%d local.version=%d) (remote.updatedAt=%s local.updatedAt=%s)",
                            TAG, local.childId, remoteVersion, localVersion, remoteUpdatedAt, localUpdatedAt
                        )

                        val remoteObj = snap.toObject(Child::class.java)
                        if (remoteObj != null) {
                            // IMPORTANT: enforce snapshot fields into the object (avoid defaults)
                            conflictRemotes += remoteObj.copy(
                                childId = remoteObj.childId.ifBlank { local.childId },
                                version = remoteVersion,
                                updatedAt = remoteUpdatedAt,
                                isDirty = false
                            )
                        } else {
                            // If remote can't parse, do NOT overwrite local; keep it dirty and retry later
                            Timber.w("%s remote parse failed childId=%s; leaving local dirty (will retry)", TAG, local.childId)
                        }
                    } else {
                        toPush += local
                    }
                }
            }

            Timber.i(
                "%s: prefetch done toPush=%d conflicts=%d (%dms)",
                TAG, toPush.size, conflictRemotes.size, prefetchMs
            )

            // 3) Apply server-wins conflicts into Room (clean)
            if (conflictRemotes.isNotEmpty()) {
                val conflictUpsertMs = measureTimeMillis {
                    childDao.upsertAll(conflictRemotes)
                }
                Timber.i("%s: applied conflicts into Room=%d (%dms)", TAG, conflictRemotes.size, conflictUpsertMs)
            }

            // 4) Push safe locals to Firestore (tombstones are written as docs; no delete)
            if (toPush.isNotEmpty()) {
                val pushMs = measureTimeMillis {
                    firestore.runBatch { b ->
                        toPush.forEach { child ->
                            val docRef = childrenRef.document(child.childId)

                            // bump version ON PUSH, then reflect back into Room after success
                            val nextVersion = child.version + 1

                            val toRemote = child.copy(
                                updatedAt = now,
                                version = nextVersion
                            )

                            val patch = toRemote.toFirestoreMapPatch().toMutableMap().apply {
                                // Enforce worker-consistent values (override mapper defaults)
                                this["childId"] = toRemote.childId
                                this["updatedAt"] = now
                                this["version"] = nextVersion

                                // Tombstones as docs (no delete)
                                this["isDeleted"] = toRemote.isDeleted
                                if (toRemote.isDeleted) {
                                    this["deletedAt"] = (toRemote.deletedAt ?: now)
                                }
                            }

                            b.set(docRef, patch, SetOptions.merge())
                        }
                    }.await()
                }

                Timber.i("%s: pushed=%d (%dms)", TAG, toPush.size, pushMs)

                // 5) Mark pushed rows clean in Room AND bump local version to match remote (+1)
                val pushedIds = toPush.map { it.childId }
                if (pushedIds.isNotEmpty()) {
                    val markMs = measureTimeMillis {
                        // Your DAO increments version = version + 1 internally (correct)
                        childDao.markBatchPushed(
                            ids = pushedIds,
                            newUpdatedAt = now
                        )
                    }
                    Timber.i("%s: marked clean ids=%d (+1 version) (%dms)", TAG, pushedIds.size, markMs)
                }

                Timber.d("%s: pushed=%d (dirty=%d) conflicts=%d", TAG, toPush.size, dirty.size, conflictRemotes.size)
            } else {
                Timber.d(
                    "%s: nothing to push after conflict checks (dirty=%d) conflicts=%d",
                    TAG, dirty.size, conflictRemotes.size
                )
            }

            Timber.i("%s: success totalMs~%d", TAG, totalMs)
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "%s failed; will retry.", TAG)
            Result.retry()
        }
    }
}

