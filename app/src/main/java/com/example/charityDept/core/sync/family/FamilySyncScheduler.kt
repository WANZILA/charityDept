package com.example.charityDept.core.sync.family

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import timber.log.Timber

object FamilySyncScheduler {

    private const val UNIQUE_PERIODIC_PUSH = "family_periodic_push"
    private const val UNIQUE_PERIODIC_PULL = "family_periodic_pull"
    private const val UNIQUE_ONE_OFF_PUSH = "family_one_off_push"
    private const val UNIQUE_ONE_OFF_PULL = "family_one_off_pull"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<FamilySyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("FamilySyncScheduler: periodic PUSH scheduled")
        }.onFailure { e ->
            Timber.e(e, "FamilySyncScheduler: failed to schedule periodic PUSH")
        }
    }

    fun enqueuePeriodicPull(context: Context) {
        val req = PeriodicWorkRequestBuilder<FamilyPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("FamilySyncScheduler: periodic PULL scheduled")
        }.onFailure { e ->
            Timber.e(e, "FamilySyncScheduler: failed to schedule periodic PULL")
        }
    }

    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<FamilySyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("FamilySyncScheduler: one-off PUSH enqueued")
        }.onFailure { e ->
            Timber.e(e, "FamilySyncScheduler: failed to enqueue PUSH")
        }
    }

    fun enqueuePullNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<FamilyPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("FamilySyncScheduler: one-off PULL enqueued")
        }.onFailure { e ->
            Timber.e(e, "FamilySyncScheduler: failed to enqueue PULL")
        }
    }
}