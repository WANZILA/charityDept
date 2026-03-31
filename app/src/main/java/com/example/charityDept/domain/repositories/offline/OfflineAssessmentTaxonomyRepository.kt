package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.data.model.AssessmentTaxonomy
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface OfflineAssessmentTaxonomyRepository {
    fun observeAdminAll(): Flow<List<AssessmentTaxonomy>>
    suspend fun getOnce(taxonomyId: String): AssessmentTaxonomy?
    suspend fun upsertWithAudit(draft: AssessmentTaxonomy): String
    suspend fun softDelete(taxonomyId: String)
    suspend fun hardDeleteIfDeleted(taxonomyId: String)
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int
}