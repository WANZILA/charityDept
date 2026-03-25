package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object AttendencesSeedScheduler {

    private const val UNIQUE_WORK_NAME = "attendances_seed_once"

    fun enqueue(context: Context) {
        val req = OneTimeWorkRequestBuilder<AttendencesSeedWorker>()
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP, // ✅ idempotent
            req
        )
    }
}

