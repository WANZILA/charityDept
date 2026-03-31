package com.example.charityDept.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.AssessmentAnswerDao // /// ADDED
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.dao.EventDao
import com.google.firebase.Timestamp
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class CleanerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val childDao: ChildDao,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val assessmentAnswerDao: AssessmentAnswerDao, // /// ADDED
    private val assessmentQuestionDao: AssessmentQuestionDao ,// /// ADDED,
//    private val assessmentQuestionDao: AssessmentQuestionDao,
    private val assessmentTaxonomyDao: AssessmentTaxonomyDao

) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_RETENTION_DAYS = "retentionDays"
        private const val DEFAULT_RETENTION_DAYS = 0L

        const val OUT_DELETED_CHILDREN = "deleted_children"
        const val OUT_DELETED_ATTENDANCES = "deleted_attendances"
        const val OUT_DELETED_EVENTS = "deleted_events"
        const val OUT_DELETED_ASSESSMENT_ANSWERS = "deleted_assessment_answers" // /// ADDED
        const val OUT_DELETED_ASSESSMENT_QUESTIONS = "deleted_assessment_questions" // /// ADDED

        const val OUT_DELETED_ASSESSMENT_TAXONOMY = "deleted_assessment_taxonomy"

        const val OUT_DELETED_TOTAL = "deleted_total"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun childDao(): ChildDao
        fun attendanceDao(): AttendanceDao
        fun eventDao(): EventDao
        fun assessmentAnswerDao(): AssessmentAnswerDao // /// ADDED
        fun assessmentQuestionDao(): AssessmentQuestionDao // /// ADDED
        fun assessmentTaxonomyDao(): AssessmentTaxonomyDao

    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).childDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).eventDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).attendanceDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentAnswerDao(), // /// ADDED
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentQuestionDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentTaxonomyDao()

    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val retentionDays =
                inputData.getLong(KEY_RETENTION_DAYS, DEFAULT_RETENTION_DAYS).coerceAtLeast(0L)

            val cutoffMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays)

            val cutoffSeconds = cutoffMs / 1000L
            val cutoffNanos = ((cutoffMs % 1000L) * 1_000_000L).toInt()
            val cutoff = Timestamp(cutoffSeconds, cutoffNanos)

            Timber.i("CleanerWorker: start retentionDays=%d cutoff=%s", retentionDays, cutoff)

            val deletedChildren = childDao.hardDeleteOldTombstones(cutoff)
            val deletedAttendances = attendanceDao.hardDeleteOldTombstones(cutoff)
            val deletedEvents = eventDao.hardDeleteOldTombstones(cutoff)
            val deletedAssessmentAnswers = assessmentAnswerDao.hardDeleteOldTombstones(cutoff) // /// ADDED
            val deletedAssessmentQuestions = assessmentQuestionDao.hardDeleteOldTombstones(cutoff) // /// ADDED
            val deletedAssessmentTaxonomy = assessmentTaxonomyDao.hardDeleteOldTombstones(cutoff)

            val deletedTotal =
                deletedChildren +
                        deletedAttendances +
                        deletedEvents +
                        deletedAssessmentAnswers +
                        deletedAssessmentQuestions +
                        deletedAssessmentTaxonomy
            Timber.i(
                "CleanerWorker: deleted children=%d attendances=%d events=%d assessment_answers=%d deletedAssessmentQuestions=%d total=%d",
                deletedChildren, deletedAttendances, deletedEvents, deletedAssessmentAnswers, deletedAssessmentQuestions, deletedTotal
            )

            val out = androidx.work.Data.Builder()
                .putInt(OUT_DELETED_CHILDREN, deletedChildren)
                .putInt(OUT_DELETED_ATTENDANCES, deletedAttendances)
                .putInt(OUT_DELETED_EVENTS, deletedEvents)
                .putInt(OUT_DELETED_ASSESSMENT_ANSWERS, deletedAssessmentAnswers) // /// ADDED
                .putInt(OUT_DELETED_ASSESSMENT_QUESTIONS, deletedAssessmentQuestions)
                .putInt(OUT_DELETED_TOTAL, deletedTotal)
                .putInt(OUT_DELETED_ASSESSMENT_TAXONOMY, deletedAssessmentTaxonomy)
                .build()

            Timber.i("CleanerWorker: done")
            Result.success(out)

        } catch (t: Throwable) {
            Timber.e(t, "CleanerWorker failed")
            Result.retry()
        }
    }
}

