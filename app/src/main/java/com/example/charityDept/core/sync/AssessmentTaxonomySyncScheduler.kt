package com.example.charityDept.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.charityDept.domain.sync.AssessmentTaxonomyPullWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

object AssessmentTaxonomySyncScheduler {

    private const val UNIQUE_PERIODIC_PULL = "assessment_taxonomy_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "assessment_taxonomy_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "assessment_taxonomy_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "assessment_taxonomy_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<AssessmentTaxonomyPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentTaxonomySyncScheduler: periodic PULL scheduled")
        }.onFailure { e ->
            Timber.e(e, "AssessmentTaxonomySyncScheduler: failed to schedule periodic PULL")
        }
    }

    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AssessmentTaxonomyPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentTaxonomySyncScheduler: one-off PULL enqueued")
        }.onFailure { e ->
            Timber.e(e, "AssessmentTaxonomySyncScheduler: failed to enqueue one-off PULL")
        }
    }

    fun enqueuePeriodicPush(context: Context) {
        val request = PeriodicWorkRequestBuilder<AssessmentTaxonomySyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentTaxonomySyncScheduler: periodic PUSH scheduled")
        }.onFailure { e ->
            Timber.e(e, "AssessmentTaxonomySyncScheduler: failed to schedule periodic PUSH")
        }
    }

    fun enqueuePushNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AssessmentTaxonomySyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("AssessmentTaxonomySyncScheduler: one-off PUSH enqueued")
        }.onFailure { e ->
            Timber.e(e, "AssessmentTaxonomySyncScheduler: failed to enqueue one-off PUSH")
        }
    }
}