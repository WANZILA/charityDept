package com.example.charityDept.core.sync.familymember

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit

object FamilyMemberSyncScheduler {

    private const val UNIQUE_PERIODIC_PULL = "family_members_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "family_members_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "family_members_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "family_members_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(true)
        .build()

    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<FamilyMemberPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("FamilyMemberSyncScheduler: periodic PULL scheduled")
        }.onFailure { e ->
            Timber.e(e, "FamilyMemberSyncScheduler: failed to schedule periodic PULL")
        }
    }

    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<FamilyMemberPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("FamilyMemberSyncScheduler: one-off PULL enqueued")
        }.onFailure { e ->
            Timber.e(e, "FamilyMemberSyncScheduler: failed to enqueue one-off PULL")
        }
    }

    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<FamilyMemberSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("FamilyMemberSyncScheduler: periodic PUSH scheduled")
        }.onFailure { e ->
            Timber.e(e, "FamilyMemberSyncScheduler: failed to schedule periodic PUSH")
        }
    }

    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<FamilyMemberSyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("FamilyMemberSyncScheduler: one-off PUSH enqueued")
        }.onFailure { e ->
            Timber.e(e, "FamilyMemberSyncScheduler: failed to enqueue one-off PUSH")
        }
    }
}