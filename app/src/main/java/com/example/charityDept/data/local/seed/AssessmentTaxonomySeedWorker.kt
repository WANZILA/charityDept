package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.sync.AssessmentTaxonomySyncScheduler
import com.example.charityDept.data.local.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AssessmentTaxonomySeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // 1) Bootstrap defaults only if taxonomy table is empty
            AssessmentTaxonomySeeder(db.assessmentTaxonomyDao()).seedIfEmpty()

            // 2) Immediately hand off to normal taxonomy sync
            AssessmentTaxonomySyncScheduler.enqueuePullNow(applicationContext)
            AssessmentTaxonomySyncScheduler.enqueuePushNow(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}