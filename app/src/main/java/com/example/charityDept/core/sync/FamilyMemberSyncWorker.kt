package com.example.charityDept.core.sync.familymember

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.FamilyMembersRef
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.FamilyMember
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
class FamilyMemberSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @FamilyMembersRef private val familyMembersRef: CollectionReference,
    private val familyDao: FamilyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "FamilyMemberSyncWorker"
        private const val MAX_BATCH = 500
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        @FamilyMembersRef fun familyMembersRef(): CollectionReference
        fun familyDao(): FamilyDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).familyMembersRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).familyDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dirty = familyDao.loadDirtyFamilyMemberBatch(limit = MAX_BATCH)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()
            val toPush = mutableListOf<FamilyMember>()
            val conflictRemotes = mutableListOf<FamilyMember>()

            for (local in dirty) {
                if (local.familyMemberId.isBlank()) continue

                val snap = familyMembersRef.document(local.familyMemberId).get(Source.SERVER).await()

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
                    val remote = snap.toObject(FamilyMember::class.java)
                    if (remote != null) {
                        conflictRemotes += remote.copy(
                            familyMemberId = remote.familyMemberId.ifBlank { local.familyMemberId },
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
                familyDao.upsertAllFamilyMembers(conflictRemotes)
            }

            if (toPush.isNotEmpty()) {
                firestore.runBatch { b ->
                    toPush.forEach { member ->
                        val nextVersion = member.version + 1
                        val toRemote = member.copy(
                            updatedAt = now,
                            version = nextVersion
                        )

                        b.set(
                            familyMembersRef.document(toRemote.familyMemberId),
                            toRemote,
                            SetOptions.merge()
                        )
                    }
                }.await()

                familyDao.markFamilyMemberBatchPushed(
                    ids = toPush.map { it.familyMemberId },
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