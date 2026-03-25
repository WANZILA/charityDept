package com.example.charityDept.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.charityDept.domain.sync.AssessmentAnswerPullWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AssessmentAnswerSyncScheduler {

    private const val UNIQUE_PERIODIC_PULL = "assessment_answers_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "assessment_answers_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "assessment_answers_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "assessment_answers_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Periodic AssessmentAnswers pull (Firestore → Room) */
    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<AssessmentAnswerPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentAnswerSyncScheduler: periodic PULL scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentAnswerSyncScheduler: failed to schedule periodic PULL")
        }
    }

    /** One-off AssessmentAnswers pull */
    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AssessmentAnswerPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentAnswerSyncScheduler: one-off PULL enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentAnswerSyncScheduler: failed to enqueue one-off PULL")
        }
    }

    /** Periodic AssessmentAnswers push (Room → Firestore) */
    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<AssessmentAnswerSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("AssessmentAnswerSyncScheduler: periodic PUSH scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentAnswerSyncScheduler: failed to schedule periodic PUSH")
        }
    }

    /** One-off AssessmentAnswers push */
    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<AssessmentAnswerSyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("AssessmentAnswerSyncScheduler: one-off PUSH enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "AssessmentAnswerSyncScheduler: failed to enqueue one-off PUSH")
        }
    }
}

