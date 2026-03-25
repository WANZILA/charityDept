// app/src/main/java/com/example/zionkids/core/sync/CleanerScheduler.kt
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
import java.util.concurrent.TimeUnit

object CleanerScheduler {

    private const val UNIQUE_PERIODIC = "cleaner_periodic"
    private const val UNIQUE_ONE_OFF  = "cleaner_now"

    private val constraints = Constraints.Builder()
        // Cleaning is local DB work; network not required.
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .setRequiresBatteryNotLow(true)
        .build()

    /** Periodic cleanup (Room tombstones → hard delete after retention) */
    fun enqueuePeriodic(ctx: Context, retentionDays: Long = 30L) {
        val input = Data.Builder()
            .putLong(CleanerWorker.KEY_RETENTION_DAYS, retentionDays)
            .build()

        val req = PeriodicWorkRequestBuilder<CleanerWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            req
        )
    }

    /** One-off cleanup (useful for debug / after big deletes) */
    fun enqueueNow(ctx: Context, retentionDays: Long = 30L) {
        val input = Data.Builder()
            .putLong(CleanerWorker.KEY_RETENTION_DAYS, retentionDays)
            .build()

        val req = OneTimeWorkRequestBuilder<CleanerWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .build()

        WorkManager.getInstance(ctx).enqueueUniqueWork(
            UNIQUE_ONE_OFF,
            ExistingWorkPolicy.REPLACE,
            req
        )
    }
}

