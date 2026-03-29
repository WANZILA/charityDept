package com.example.charityDept.core.sync.family

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.FamilyRef
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.Family
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

@HiltWorker
class FamilySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @FamilyRef private val familyRef: CollectionReference,
    private val familyDao: FamilyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "FamilySyncWorker"
        private const val MAX_BATCH = 500
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerFamilies {
        @FamilyRef fun familyRef(): CollectionReference
        fun familyDao(): FamilyDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerFamilies::class.java).familyRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerFamilies::class.java).familyDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerFamilies::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dirty = familyDao.loadDirtyFamilyBatch(limit = MAX_BATCH)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()
            val toPush = mutableListOf<Family>()
            val conflictRemotes = mutableListOf<Family>()

            for (local in dirty) {
                if (local.familyId.isBlank()) continue

                val snap = familyRef.document(local.familyId).get(Source.SERVER).await()
                if (!snap.exists()) {
                    toPush += local
                    continue
                }

                val remoteVersion = snap.getLong("version")
                val remoteUpdatedAt = snap.getTimestamp("updatedAt")

                if (remoteVersion == null || remoteUpdatedAt == null) {
                    toPush += local
                    continue
                }

                val serverWins =
                    (remoteVersion > local.version) ||
                            (remoteVersion == local.version &&
                                    remoteUpdatedAt.toDate().time > local.updatedAt.toDate().time)

                if (serverWins) {
                    val remote = snap.toObject(Family::class.java)
                    if (remote != null) {
                        conflictRemotes += remote.copy(
                            familyId = remote.familyId.ifBlank { local.familyId },
                            version = remoteVersion,
                            updatedAt = remoteUpdatedAt,
                            isDirty = false
                        )
                    }
                } else {
                    toPush += local
                }
            }

            if (conflictRemotes.isNotEmpty()) {
                familyDao.upsertAllFamilies(conflictRemotes)
            }

            if (toPush.isNotEmpty()) {
                firestore.runBatch { b ->
                    toPush.forEach { family ->
                        val nextVersion = family.version + 1
                        val toRemote = family.copy(
                            updatedAt = now,
                            version = nextVersion
                        )

                        b.set(
                            familyRef.document(toRemote.familyId),
                            toRemote,
                            SetOptions.merge()
                        )
                    }
                }.await()

                familyDao.markFamilyBatchPushed(
                    ids = toPush.map { it.familyId },
                    newUpdatedAt = now
                )
            }

            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "%s failed; will retry.", TAG)
            Result.retry()
        }
    }
}