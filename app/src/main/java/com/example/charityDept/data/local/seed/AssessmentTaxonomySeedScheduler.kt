package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

object AssessmentTaxonomySeedScheduler {

    private const val UNIQUE_WORK_NAME = "assessment_taxonomy_seed_once"

    fun enqueue(context: Context) {
        val req = OneTimeWorkRequestBuilder<AssessmentTaxonomySeedWorker>()
            .addTag(UNIQUE_WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            req
        )
    }
}