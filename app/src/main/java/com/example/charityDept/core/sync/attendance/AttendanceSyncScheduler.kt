package com.example.charityDept.core.sync.attendance

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object AttendanceSyncScheduler {

    // IMPORTANT: do NOT share unique names between pull and push
    private const val UNIQUE_PERIODIC_PULL = "attendance_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "attendance_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "attendance_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "attendance_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /** Periodic attendance pull sync (Firestore → Room) */
    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<AttendancePullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_PULL,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** One-off attendance pull sync (use this for “Refresh” + debugging) */
    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<AttendancePullWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_ONE_OFF_PULL,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Periodic attendance push sync (Room → Firestore) */
    fun enqueuePeriodicPush(ctx: Context) {
        val req = PeriodicWorkRequestBuilder<AttendanceSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC_PUSH,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    /** One-off attendance push sync */
    fun enqueuePushNow(ctx: Context) {
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            UNIQUE_ONE_OFF_PUSH,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AttendanceSyncWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag("attendance_sync_now")
                .build()
        )
    }
}

