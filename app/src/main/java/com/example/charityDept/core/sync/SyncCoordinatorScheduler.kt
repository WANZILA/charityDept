// app/src/main/java/com/example/zionkids/core/sync/SyncCoordinatorScheduler.kt
package com.example.charityDept.core.sync

import android.content.Context
import com.example.charityDept.core.sync.attendance.AttendanceSyncScheduler
import com.example.charityDept.core.sync.event.EventSyncScheduler

object SyncCoordinatorScheduler {

    /**
     * Call this once (e.g., app start after login, or in Application.onCreate if safe).
     * This sets up all periodic jobs in one place.
     */
    fun enqueuePushAllPeriodic(
        ctx: Context,
        cleanerRetentionDays: Long = 30L
    ) {

        // Children: Room -> Firestore
        ChildrenSyncScheduler.enqueuePeriodicPush(ctx)

        // Attendance: Room -> Firestore
        AttendanceSyncScheduler.enqueuePeriodicPull(ctx)

        //events
        EventSyncScheduler.enqueuePeriodicPush(ctx)

        AssessmentQuestionSyncScheduler.enqueuePeriodicPull(ctx)

        AssessmentAnswerSyncScheduler.enqueuePeriodicPull(ctx)

        // Cleaner: local tombstone cleanup
        CleanerScheduler.enqueuePeriodic(ctx, retentionDays = cleanerRetentionDays)
    }

    fun enqueuePullAllPeriodic(
        ctx: Context,
        cleanerRetentionDays: Long = 30L
    ) {
        // Children: Firestore -> Room
        ChildrenSyncScheduler.enqueuePeriodicPull(ctx)

        AttendanceSyncScheduler.enqueuePeriodicPush(ctx)
        EventSyncScheduler.enqueuePeriodicPull(ctx)
        AssessmentQuestionSyncScheduler.enqueuePeriodicPush(ctx)
        AssessmentAnswerSyncScheduler.enqueuePeriodicPush(ctx)
        CleanerScheduler.enqueuePeriodic(ctx, cleanerRetentionDays)


    }

    /**
     * Optional: one-shot "sync now" for debugging / manual refresh.
     * (Pull + push + attendance push + cleaner now)
     */
    fun enqueuePushAllNow(
        ctx: Context,
        cleanerRetentionDays: Long = 30L
    ) {
        ChildrenSyncScheduler.enqueuePushNow(ctx)
        AttendanceSyncScheduler.enqueuePushNow(ctx)
        EventSyncScheduler.enqueuePushNow(ctx)
        AssessmentQuestionSyncScheduler.enqueuePushNow(ctx)
        AssessmentAnswerSyncScheduler.enqueuePullNow(ctx)
        CleanerScheduler.enqueueNow(ctx, retentionDays = cleanerRetentionDays)

    }

    fun enqueuePullAllNow(
        ctx: Context,
        cleanerRetentionDays: Long = 30L
    ) {

        ChildrenSyncScheduler.enqueuePullNow(ctx)

        AttendanceSyncScheduler.enqueuePullNow(ctx)

        EventSyncScheduler.enqueuePullNow(ctx)
        AssessmentQuestionSyncScheduler.enqueuePullNow(ctx)
        AssessmentAnswerSyncScheduler.enqueuePullNow(ctx)
        CleanerScheduler.enqueueNow(ctx, retentionDays = cleanerRetentionDays)

    }
}

