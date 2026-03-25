package com.example.charityDept.core.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.charityDept.domain.sync.ChildrenPullWorker
import com.example.charityDept.domain.sync.ChildrenSyncWorker
import java.util.concurrent.TimeUnit

object ChildrenSyncScheduler {

    // IMPORTANT: do NOT share unique names between pull and push
    private const val UNIQUE_PERIODIC_PULL = "children_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "children_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "children_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "children_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        // TEMP: comment this out while debugging if your device is often in battery saver / low battery
        .setRequiresBatteryNotLow(true)
        .build()

    /** Periodic children pull sync (Firestore → Room) */
    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<ChildrenPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_PULL,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** One-off children pull sync (use this for “Refresh” + debugging) */
    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<ChildrenPullWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_OFF_PULL,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Periodic children push sync (Room → Firestore) */
    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<ChildrenSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_PUSH,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    /** One-off children push sync */
    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<ChildrenSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_OFF_PUSH,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }


}

