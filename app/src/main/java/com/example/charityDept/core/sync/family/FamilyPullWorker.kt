package com.example.charityDept.core.sync.family

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.Family
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

import com.example.charityDept.data.mappers.FamilyMappers
//import com.example.charityDept.data.mappers.FamilyMappers
@HiltWorker
class FamilyPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val familyDao: FamilyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    private val familyMappers = FamilyMappers()

    companion object {
        private const val FAMILIES_COLLECTION = "families"
        private const val PAGE_SIZE = 500L
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerDeps {
        fun familyDao(): FamilyDao
        fun firestore(): FirebaseFirestore
    }

    constructor(appContext: Context, params: WorkerParameters) : this(
        appContext,
        params,
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).familyDao(),
        EntryPointAccessors.fromApplication(appContext, WorkerDeps::class.java).firestore()
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val snap = firestore.collection(FAMILIES_COLLECTION)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .limit(PAGE_SIZE)
                .get(Source.SERVER)
                .await()

            val remoteFamilies = snap.documents.mapNotNull { doc ->
                val version = doc.getLong("version") ?: return@mapNotNull null
                val updatedAt = doc.getTimestamp("updatedAt") ?: return@mapNotNull null
                val obj = familyMappers.run { doc.toFamilyOrNull() } ?: return@mapNotNull null

                obj.copy(
                    familyId = obj.familyId.ifBlank { doc.id },
                    version = version,
                    updatedAt = updatedAt,
                    isDirty = false,
                    isDeleted = doc.getBoolean("isDeleted") ?: obj.isDeleted,
                    deletedAt = if (doc.getBoolean("isDeleted") == true) {
                        doc.getTimestamp("deletedAt") ?: obj.deletedAt
                    } else {
                        null
                    }
                )
            }
            if (remoteFamilies.isNotEmpty()) {
                familyDao.upsertAllFamilies(remoteFamilies)
            }

            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}