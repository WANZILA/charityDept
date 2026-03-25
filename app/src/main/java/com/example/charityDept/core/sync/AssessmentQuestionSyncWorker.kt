package com.example.charityDept.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.AssessmentQuestionsRef
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.AssessmentQuestion
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
class AssessmentQuestionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @AssessmentQuestionsRef private val questionRef: CollectionReference,
    private val questionDao: AssessmentQuestionDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AssessmentQuestionSyncWorker"
        private const val MAX_BATCH = 500
    }

    // ---- Fallback path when HiltWorkerFactory isn’t used ----
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun firestore(): FirebaseFirestore
        fun assessmentQuestionDao(): AssessmentQuestionDao

        // /// CHANGED: if you already have a @Qualifier like @EventsRef, make one like @AssessmentQuestionsRef
        @AssessmentQuestionsRef
        fun assessmentQuestionRef(): CollectionReference
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentQuestionRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentQuestionDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val totalMs = measureTimeMillis {
            Timber.i("%s: start attempt=%d", TAG, runAttemptCount)
        }

        try {
            // 1) Collect dirty questions
            val dirty = questionDao.loadDirtyBatch(limit = MAX_BATCH)
            Timber.i("%s: loaded dirty=%d", TAG, dirty.size)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()

            val toPush = mutableListOf<AssessmentQuestion>()
            val conflictRemotes = mutableListOf<AssessmentQuestion>()

            // 2) For each dirty local, check remote version+updatedAt
            val prefetchMs = measureTimeMillis {
                for (local in dirty) {
                    if (local.questionId.isBlank()) {
                        Timber.w("%s skipping dirty row with blank questionId", TAG)
                        continue
                    }

                    val docRef = questionRef.document(local.questionId)
                    val snap = docRef.get(Source.SERVER).await()

                    if (!snap.exists()) {
                        toPush += local
                        continue
                    }

                    val remoteVersion = snap.getLong("version")
                    val remoteUpdatedAt = snap.getTimestamp("updatedAt")

                    if (remoteVersion == null || remoteUpdatedAt == null) {
                        Timber.w(
                            "%s remote missing fields questionId=%s remoteVersion=%s remoteUpdatedAt=%s; pushing local",
                            TAG, local.questionId, remoteVersion, remoteUpdatedAt
                        )
                        toPush += local
                        continue
                    }

                    val localVersion = local.version
                    val localUpdatedAt = local.updatedAt

                    val serverWins =
                        (remoteVersion > localVersion) ||
                                (remoteVersion == localVersion &&
                                        remoteUpdatedAt.toDate().time > localUpdatedAt.toDate().time)

                    if (serverWins) {
                        Timber.d(
                            "%s server-wins questionId=%s (remote.version=%d local.version=%d) (remote.updatedAt=%s local.updatedAt=%s)",
                            TAG, local.questionId, remoteVersion, localVersion, remoteUpdatedAt, localUpdatedAt
                        )

                        val remoteObj = snap.toObject(AssessmentQuestion::class.java)
                        if (remoteObj != null) {
                            conflictRemotes += remoteObj.copy(
                                questionId = remoteObj.questionId.ifBlank { local.questionId },
                                version = remoteVersion,
                                updatedAt = remoteUpdatedAt,
                                isDirty = false
                            )
                        } else {
                            Timber.w("%s remote parse failed questionId=%s; leaving local dirty (retry later)", TAG, local.questionId)
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
                    questionDao.upsertAll(conflictRemotes)
                }
                Timber.i("%s: applied conflicts into Room=%d (%dms)", TAG, conflictRemotes.size, conflictUpsertMs)
            }

            // 4) Push safe locals to Firestore
            if (toPush.isNotEmpty()) {
                val pushMs = measureTimeMillis {
                    firestore.runBatch { b ->
                        toPush.forEach { q ->
                            val docRef = questionRef.document(q.questionId)

                            val nextVersion = q.version + 1

                            val patch = q.toFirestoreMapPatch().toMutableMap().apply {
                                this["questionId"] = q.questionId
                                this["updatedAt"] = now
                                this["version"] = nextVersion

                                this["isDeleted"] = q.isDeleted
                                if (q.isDeleted) {
                                    this["deletedAt"] = (q.deletedAt ?: now)
                                } else {
                                    // optional: you can omit this; leaving as-is avoids churn
                                    // this.remove("deletedAt")
                                }
                            }

                            b.set(docRef, patch, SetOptions.merge())
                        }
                    }.await()
                }

                Timber.i("%s: pushed=%d (%dms)", TAG, toPush.size, pushMs)

                // 5) Mark pushed rows clean in Room AND bump local version to match remote (+1)
                val pushedIds = toPush.map { it.questionId }
                if (pushedIds.isNotEmpty()) {
                    val markMs = measureTimeMillis {
                        questionDao.markBatchPushed(
                            ids = pushedIds,
                            newUpdatedAt = now
                        )
                    }
                    Timber.i("%s: marked clean ids=%d (+1 version) (%dms)", TAG, pushedIds.size, markMs)
                }
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
