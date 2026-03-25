package com.example.charityDept.core.sync.attendance

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.AttendanceRef
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.Attendance
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
class AttendanceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @AttendanceRef private val attRef: CollectionReference,
    private val attDao: AttendanceDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AttendanceSyncWorker"
        private const val MAX_BATCH = 450
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerAttendanceDeps {
        @AttendanceRef fun attRef(): CollectionReference
        fun attDao(): AttendanceDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerAttendanceDeps::class.java).attRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerAttendanceDeps::class.java).attDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerAttendanceDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val totalMs = measureTimeMillis {
            Timber.i("%s: start attempt=%d", TAG, runAttemptCount)
        }

        try {
            // 1) Collect dirty attendances
            val dirty = attDao.loadDirtyBatch(limit = MAX_BATCH)
            Timber.i("%s: loaded dirty=%d", TAG, dirty.size)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()

            val toPush = mutableListOf<Attendance>()
            val conflictRemotes = mutableListOf<Attendance>()

            // 2) For each dirty local, check remote version+updatedAt from SNAPSHOT fields (avoid defaults)
            val prefetchMs = measureTimeMillis {
                for (local in dirty) {
                    if (local.attendanceId.isBlank()) {
                        Timber.w("%s skipping dirty row with blank attendanceId", TAG)
                        continue
                    }

                    val docRef = attRef.document(local.attendanceId)

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
                            "%s remote missing fields attendanceId=%s remoteVersion=%s remoteUpdatedAt=%s; pushing local",
                            TAG, local.attendanceId, remoteVersion, remoteUpdatedAt
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
                            "%s server-wins attendanceId=%s (remote.version=%d local.version=%d) (remote.updatedAt=%s local.updatedAt=%s)",
                            TAG, local.attendanceId, remoteVersion, localVersion, remoteUpdatedAt, localUpdatedAt
                        )

                        val remoteObj = snap.toObject(Attendance::class.java)
                        if (remoteObj != null) {
                            // IMPORTANT: enforce snapshot fields into the object (avoid defaults)
                            conflictRemotes += remoteObj.copy(
                                attendanceId = remoteObj.attendanceId.ifBlank { local.attendanceId },
                                version = remoteVersion,
                                updatedAt = remoteUpdatedAt,
                                isDirty = false
                            )
                        } else {
                            // If remote can't parse, do NOT overwrite local; keep it dirty and retry later
                            Timber.w(
                                "%s remote parse failed attendanceId=%s; leaving local dirty (will retry)",
                                TAG, local.attendanceId
                            )
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
                    attDao.upsertAll(conflictRemotes)
                }
                Timber.i(
                    "%s: applied conflicts into Room=%d (%dms)",
                    TAG, conflictRemotes.size, conflictUpsertMs
                )
            }

            // 4) Push safe locals to Firestore
            if (toPush.isNotEmpty()) {
                val pushMs = measureTimeMillis {
                    firestore.runBatch { b ->
                        toPush.forEach { a ->
                            val docRef = attRef.document(a.attendanceId)

                            // We bump version ON PUSH (remote authoritative), then reflect it back into Room after success
                            val nextVersion = a.version + 1

                            val toRemote = a.copy(
                                updatedAt = now,
                                version = nextVersion
                            )

                            val patch = toRemote.toFirestoreMapPatch().toMutableMap().apply {
                                // Enforce worker-consistent values (override mapper defaults)
                                this["attendanceId"] = toRemote.attendanceId
                                this["childId"] = toRemote.childId
                                this["eventId"] = toRemote.eventId
                                this["updatedAt"] = now
                                this["version"] = nextVersion

                                // Tombstones as docs (no delete)
                                this["isDeleted"] = toRemote.isDeleted
                                if (toRemote.isDeleted) {
                                    this["deletedAt"] = (toRemote.deletedAt ?: now)
                                }

                                // Keep createdAt stable (don’t invent it)
                                this["createdAt"] = toRemote.createdAt

                                // checkedAt may be null (do not invent it)
                                toRemote.checkedAt?.let { this["checkedAt"] = it }
                            }

                            b.set(docRef, patch, SetOptions.merge())
                        }
                    }.await()
                }

                Timber.i("%s: pushed=%d (%dms)", TAG, toPush.size, pushMs)

                // 5) Mark pushed rows clean in Room AND bump local version to match remote (+1)
                val pushedIds = toPush.map { it.attendanceId }
                if (pushedIds.isNotEmpty()) {
                    val markMs = measureTimeMillis {
                        attDao.markBatchPushed(
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

