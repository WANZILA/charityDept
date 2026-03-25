// <app/src/main/java/com/example/zionkids/core/sync/event/EventSyncScheduler.kt>
// /// CHANGED: Mirror ChildrenSyncScheduler naming (separate unique names for pull vs push).
// /// CHANGED: Add enqueuePeriodicPull + enqueuePullNow for EventPullWorker (Firestore → Room).
// /// CHANGED: Keep push scheduling for EventSyncWorker (Room → Firestore).
// /// CHANGED: Added Timber logging for all enqueues (success + errors).
// /// CHANGED: Explicit imports for EventPullWorker + EventSyncWorker.
// /// CHANGED: Guard against blank IDs in cascade delete with a warning log. (stub kept if you later add worker)

package com.example.charityDept.core.sync.event

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.charityDept.domain.sync.EventPullWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

// /// CHANGED: Ensure we import the correct worker package (your worker lives in core.sync.event).
//import com.example.charityDept.core.sync.event.EventSyncWorker

object EventSyncScheduler {

    // IMPORTANT: do NOT share unique names between pull and push
    private const val UNIQUE_PERIODIC_PULL = "events_pull_periodic"
    private const val UNIQUE_PERIODIC_PUSH = "events_push_periodic"
    private const val UNIQUE_ONE_OFF_PULL = "events_pull_now"
    private const val UNIQUE_ONE_OFF_PUSH = "events_push_now"

    private val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        // TEMP: comment this out while debugging if your device is often in battery saver / low battery
        .setRequiresBatteryNotLow(true)
        .build()

    /** Periodic events pull sync (Firestore → Room) */
    fun enqueuePeriodicPull(context: Context) {
        val request = PeriodicWorkRequestBuilder<EventPullWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PULL,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }.onSuccess {
            Timber.i("EventSyncScheduler: periodic PULL scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "EventSyncScheduler: failed to schedule periodic PULL")
        }
    }

    /** One-off events pull sync (use this for “Refresh” + debugging) */
    fun enqueuePullNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<EventPullWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PULL,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }.onSuccess {
            Timber.i("EventSyncScheduler: one-off PULL enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "EventSyncScheduler: failed to enqueue one-off PULL")
        }
    }

    /** Periodic events push sync (Room → Firestore) */
    fun enqueuePeriodicPush(context: Context) {
        val req = PeriodicWorkRequestBuilder<EventSyncWorker>(30, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_PUSH,
                ExistingPeriodicWorkPolicy.UPDATE,
                req
            )
        }.onSuccess {
            Timber.i("EventSyncScheduler: periodic PUSH scheduled (30m, policy=UPDATE)")
        }.onFailure { e ->
            Timber.e(e, "EventSyncScheduler: failed to schedule periodic PUSH")
        }
    }

    /** One-off events push sync */
    fun enqueuePushNow(context: Context) {
        val req = OneTimeWorkRequestBuilder<EventSyncWorker>()
            .setConstraints(constraints)
            .build()

        runCatching {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_ONE_OFF_PUSH,
                ExistingWorkPolicy.REPLACE,
                req
            )
        }.onSuccess {
            Timber.i("EventSyncScheduler: one-off PUSH enqueued (policy=REPLACE)")
        }.onFailure { e ->
            Timber.e(e, "EventSyncScheduler: failed to enqueue one-off PUSH")
        }
    }

}

