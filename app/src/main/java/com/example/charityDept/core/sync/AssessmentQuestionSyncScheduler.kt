// <app/src/main/java/com/example/zionkids/core/sync/assessment/AssessmentQuestionSyncScheduler.kt>
// Mirrors EventSyncScheduler:
// - separate unique names for pull vs push
// - enqueuePeriodicPull + enqueuePullNow for AssessmentQuestionPullWorker (Firestore → Room)
// - enqueuePeriodicPush + enqueuePushNow for AssessmentQuestionSyncWorker (Room → Firestore)

package com.example.charityDept.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.charityDept.domain.sync.AssessmentQuestionPullWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AssessmentQuestionSyncScheduler {

    // IMPORTANT: do NOT share unique names between pull and push
    private const val UNIQUE_PERIODIC_PULL = "assessment_questions_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "assessment_questions_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "assessment_questions_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "assessment_questions_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        // TEMP: comment this out while debugging if your device is often in battery saver / low battery
        .setRequiresBatteryNotLow(true)
        .build()

    /** Periodic AssessmentQuestions pull sync (Firestore → Room) */
    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<AssessmentQuestionPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentQuestionSyncScheduler: periodic PULL scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentQuestionSyncScheduler: failed to schedule periodic PULL")
        }
    }

    /** One-off AssessmentQuestions pull sync (use this for “Refresh” + debugging) */
    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AssessmentQuestionPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentQuestionSyncScheduler: one-off PULL enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentQuestionSyncScheduler: failed to enqueue one-off PULL")
        }
    }

    /** Periodic AssessmentQuestions push sync (Room → Firestore) */
    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<AssessmentQuestionSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("AssessmentQuestionSyncScheduler: periodic PUSH scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentQuestionSyncScheduler: failed to schedule periodic PUSH")
        }
    }

    /** One-off AssessmentQuestions push sync */
    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<AssessmentQuestionSyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("AssessmentQuestionSyncScheduler: one-off PUSH enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentQuestionSyncScheduler: failed to enqueue one-off PUSH")
        }
    }
}

