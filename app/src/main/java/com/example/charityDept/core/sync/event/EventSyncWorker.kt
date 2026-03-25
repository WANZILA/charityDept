package com.example.charityDept.core.sync.event

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.EventsRef
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.Event
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
class EventSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @EventsRef private val eventRef: CollectionReference,
    private val eventDao: EventDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "EventSyncWorker"
        private const val MAX_BATCH = 500
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEvents {
        @EventsRef fun eventRef(): CollectionReference
        fun eventDao(): EventDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerEvents::class.java).eventRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerEvents::class.java).eventDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerEvents::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val totalMs = measureTimeMillis {
            Timber.i("%s: start attempt=%d", TAG, runAttemptCount)
        }

        try {
            // 1) Collect dirty events
            val dirty = eventDao.loadDirtyBatch(limit = MAX_BATCH)
            Timber.i("%s: loaded dirty=%d", TAG, dirty.size)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()

            val toPush = mutableListOf<Event>()
            val conflictRemotes = mutableListOf<Event>()

            // 2) For each dirty local, check remote version+updatedAt from SNAPSHOT fields (avoid defaults)
            val prefetchMs = measureTimeMillis {
                for (local in dirty) {
                    if (local.eventId.isBlank()) {
                        Timber.w("%s skipping dirty row with blank eventId", TAG)
                        continue
                    }

                    val docRef = eventRef.document(local.eventId)

                    // Force server read so we don't compare against stale cache
                    val snap = docRef.get(Source.SERVER).await()

                    if (!snap.exists()) {
                        // No remote doc yet -> safe to push local
                        toPush += local
                        continue
                    }

                    val remoteVersion = snap.getLong("version")
                    val remoteUpdatedAt = snap.getTimestamp("updatedAt")

                    // If remote is missing required fields, treat LOCAL as winner (but log loudly)
                    if (remoteVersion == null || remoteUpdatedAt == null) {
                        Timber.w(
                            "%s remote missing fields eventId=%s remoteVersion=%s remoteUpdatedAt=%s; pushing local",
                            TAG, local.eventId, remoteVersion, remoteUpdatedAt
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
                            "%s server-wins eventId=%s (remote.version=%d local.version=%d) (remote.updatedAt=%s local.updatedAt=%s)",
                            TAG, local.eventId, remoteVersion, localVersion, remoteUpdatedAt, localUpdatedAt
                        )

                        val remoteObj = snap.toObject(Event::class.java)
                        if (remoteObj != null) {
                            // IMPORTANT: enforce snapshot fields into the object (avoid defaults)
                            conflictRemotes += remoteObj.copy(
                                eventId = remoteObj.eventId.ifBlank { local.eventId },
                                version = remoteVersion,
                                updatedAt = remoteUpdatedAt,
                                isDirty = false
                            )
                        } else {
                            // If remote can't parse, do NOT overwrite local; keep it dirty and retry later
                            Timber.w("%s remote parse failed eventId=%s; leaving local dirty (will retry)", TAG, local.eventId)
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
                    eventDao.upsertAll(conflictRemotes)
                }
                Timber.i("%s: applied conflicts into Room=%d (%dms)", TAG, conflictRemotes.size, conflictUpsertMs)
            }

            // 4) Push safe locals to Firestore
            if (toPush.isNotEmpty()) {
                val pushMs = measureTimeMillis {
                    firestore.runBatch { b ->
                        toPush.forEach { ev ->
                            val docRef = eventRef.document(ev.eventId)

                            // We bump version ON PUSH (remote authoritative), then reflect it back into Room after success
                            val nextVersion = ev.version + 1

                            val toRemote = ev.copy(
                                updatedAt = now,
                                version = nextVersion
                            )

                            val patch = toRemote.toFirestoreMapPatch().toMutableMap().apply {
                                // Enforce worker-consistent values (override mapper defaults)
                                this["eventId"] = toRemote.eventId
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
                val pushedIds = toPush.map { it.eventId }
                if (pushedIds.isNotEmpty()) {
                    val markMs = measureTimeMillis {
                        eventDao.markBatchPushed(
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

