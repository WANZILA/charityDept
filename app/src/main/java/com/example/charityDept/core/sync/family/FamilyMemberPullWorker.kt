package com.example.charityDept.core.sync.familymember

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.FamilyMember
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

@HiltWorker
class FamilyMemberPullWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val familyDao: FamilyDao,
    private val firestore: FirebaseFirestore
) : CoroutineWorker(appContext, params) {

    private val familyMappers = FamilyMappers()

    companion object {
        private const val COLLECTION = "family_members"
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
            val snap = firestore.collection(COLLECTION)
                .orderBy("updatedAt", Query.Direction.ASCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)
                .limit(PAGE_SIZE)
                .get(Source.SERVER)
                .await()

            val remoteMembers = snap.documents.mapNotNull { doc ->
                val version = doc.getLong("version") ?: return@mapNotNull null
                val updatedAt = doc.getTimestamp("updatedAt") ?: return@mapNotNull null
                val obj = doc.toObject(FamilyMember::class.java) ?: return@mapNotNull null

                obj.copy(
                    familyMemberId = obj.familyMemberId.ifBlank { doc.id },
                    version = version,
                    updatedAt = updatedAt,
                    isDirty = false
                )
            }
            if (remoteMembers.isNotEmpty()) {
                familyDao.upsertAllFamilyMembers(remoteMembers)
            }

            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}