package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.sync.event.EventSyncScheduler
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.local.seed.EventSeedLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class EventsSeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // 1) Seed only if events table is empty
            EventSeedLoader.seedIfEventsEmpty(applicationContext, db)

            // 2) Immediately enqueue your normal Firestore pull/push pipeline
            // (this will apply any changes since the seed)
            EventSyncScheduler.enqueuePullNow(applicationContext)
            EventSyncScheduler.enqueuePushNow(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

