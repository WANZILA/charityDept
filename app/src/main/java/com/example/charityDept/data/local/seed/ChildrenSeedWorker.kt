package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.sync.ChildrenSyncScheduler
//import com.example.charityDept.core.sync.child.ChildSyncScheduler
import com.example.charityDept.data.local.db.AppDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ChildrenSeedWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val db: AppDatabase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            // 1) Seed only if children table is empty
            ChildrenSeedLoader.seedIfChildrenEmpty(applicationContext, db)

            // 2) Immediately enqueue your normal Firestore pull/push pipeline
            // (this will apply any changes since the seed)
            ChildrenSyncScheduler.enqueuePullNow(applicationContext)
            ChildrenSyncScheduler.enqueuePushNow(applicationContext)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() }
        )
    }
}

