// <app/src/main/java/com/example/zionkids/core/sync/AssessmentAnswerSyncWorker.kt>
// Mirrors AssessmentQuestionSyncWorker, but for assessment_answers (Room → Firestore)

package com.example.charityDept.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.AssessmentAnswersRef
import com.example.charityDept.data.local.dao.AssessmentAnswerDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.example.charityDept.data.model.AssessmentAnswer
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
class AssessmentAnswerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @AssessmentAnswersRef private val answerRef: CollectionReference,
    private val answerDao: AssessmentAnswerDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AssessmentAnswerSyncWorker"
        private const val MAX_BATCH = 500
    }

    // ---- Fallback path when HiltWorkerFactory isn’t used ----
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun firestore(): FirebaseFirestore
        fun assessmentAnswerDao(): AssessmentAnswerDao

        // NOTE: create a qualifier like @AssessmentAnswersRef (same pattern as @AssessmentQuestionsRef)
        @AssessmentAnswersRef
        fun assessmentAnswerRef(): CollectionReference
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentAnswerRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentAnswerDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val totalMs = measureTimeMillis {
            Timber.i("%s: start attempt=%d", TAG, runAttemptCount)
        }

        try {
            // 1) Collect dirty answers
            val dirty = answerDao.loadDirtyBatch(limit = MAX_BATCH)
            Timber.i("%s: loaded dirty=%d", TAG, dirty.size)
            if (dirty.isEmpty()) return@withContext Result.success()

            val now = Timestamp.now()

            val toPush = mutableListOf<AssessmentAnswer>()
            val conflictRemotes = mutableListOf<AssessmentAnswer>()

            // 2) For each dirty local, check remote version+updatedAt from snapshot
            val prefetchMs = measureTimeMillis {
                for (local in dirty) {
                    if (local.answerId.isBlank()) {
                        Timber.w("%s skipping dirty row with blank answerId", TAG)
                        continue
                    }

                    val docRef = answerRef.document(local.answerId)
                    val snap = docRef.get(Source.SERVER).await()

                    if (!snap.exists()) {
                        // no remote yet
                        toPush += local
                        continue
                    }

                    val remoteVersion = snap.getLong("version")
                    val remoteUpdatedAt = snap.getTimestamp("updatedAt")

                    // if remote missing required fields, push local (but log loudly)
                    if (remoteVersion == null || remoteUpdatedAt == null) {
                        Timber.w(
                            "%s remote missing fields answerId=%s remoteVersion=%s remoteUpdatedAt=%s; pushing local",
                            TAG, local.answerId, remoteVersion, remoteUpdatedAt
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
                            "%s server-wins answerId=%s (remote.version=%d local.version=%d) (remote.updatedAt=%s local.updatedAt=%s)",
                            TAG, local.answerId, remoteVersion, localVersion, remoteUpdatedAt, localUpdatedAt
                        )

                        val remoteObj = snap.toObject(AssessmentAnswer::class.java)
                        if (remoteObj != null) {
                            // enforce snapshot fields into object (avoid defaults)
                            conflictRemotes += remoteObj.copy(
                                answerId = remoteObj.answerId.ifBlank { local.answerId },
                                version = remoteVersion,
                                updatedAt = remoteUpdatedAt,
                                isDirty = false
                            )
                        } else {
                            // if remote can't parse, keep local dirty and retry later
                            Timber.w("%s remote parse failed answerId=%s; leaving local dirty (retry later)", TAG, local.answerId)
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
                val ms = measureTimeMillis {
                    answerDao.upsertAll(conflictRemotes)
                }
                Timber.i("%s: applied conflicts into Room=%d (%dms)", TAG, conflictRemotes.size, ms)
            }

            // 4) Push safe locals to Firestore
            if (toPush.isNotEmpty()) {
                val pushMs = measureTimeMillis {
                    firestore.runBatch { b ->
                        toPush.forEach { a ->
                            val docRef = answerRef.document(a.answerId)

                            val nextVersion = a.version + 1

                            val patch = a.toFirestoreMapPatch().toMutableMap().apply {
                                this["answerId"] = a.answerId
                                this["updatedAt"] = now
                                this["version"] = nextVersion

                                // Tombstones as docs (no delete)
                                this["isDeleted"] = a.isDeleted
                                if (a.isDeleted) {
                                    this["deletedAt"] = (a.deletedAt ?: now)
                                } else {
                                    // optional: omit to avoid churn
                                    // this.remove("deletedAt")
                                }
                            }

                            b.set(docRef, patch, SetOptions.merge())
                        }
                    }.await()
                }

                Timber.i("%s: pushed=%d (%dms)", TAG, toPush.size, pushMs)

                // 5) Mark pushed rows clean in Room AND bump local version to match remote (+1)
                val pushedIds = toPush.map { it.answerId }
                if (pushedIds.isNotEmpty()) {
                    val markMs = measureTimeMillis {
                        answerDao.markBatchPushed(
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

