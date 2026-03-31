package com.example.charityDept.core.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.core.di.AssessmentTaxonomyRef
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.mappers.toFirestoreMapPatch
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber

@HiltWorker
class AssessmentTaxonomySyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    @AssessmentTaxonomyRef private val taxonomyRef: CollectionReference,
    private val taxonomyDao: AssessmentTaxonomyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "AssessmentTaxonomySyncWorker"
        private const val MAX_BATCH = 500
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun firestore(): FirebaseFirestore
        fun assessmentTaxonomyDao(): AssessmentTaxonomyDao

        @AssessmentTaxonomyRef
        fun assessmentTaxonomyRef(): CollectionReference
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentTaxonomyRef(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).assessmentTaxonomyDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val dirty = taxonomyDao.loadDirtyBatch(limit = MAX_BATCH)
            if (dirty.isEmpty()) {
                Timber.i("%s: nothing dirty", TAG)
                return@withContext Result.success()
            }

            firestore.runBatch { batch ->
                dirty.forEach { item ->
                    val doc = taxonomyRef.document(item.taxonomyId)
                    batch.set(doc, item.toFirestoreMapPatch(), SetOptions.merge())
                }
            }.await()

            taxonomyDao.markBatchPushed(
                ids = dirty.map { it.taxonomyId },
                newUpdatedAt = Timestamp.now()
            )

            Timber.i("%s: pushed=%d", TAG, dirty.size)
            Result.success()
        } catch (t: Throwable) {
            Timber.e(t, "%s failed", TAG)
            Result.retry()
        }
    }
}