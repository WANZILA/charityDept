package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.data.model.AssessmentQuestion
import kotlinx.coroutines.flow.Flow

interface OfflineAssessmentQuestionRepository {
    fun observeAdminAll(): Flow<List<AssessmentQuestion>>
    suspend fun getOnce(questionId: String): AssessmentQuestion?

    suspend fun renameAssessmentLabel(assessmentKey: String, newAssessmentLabel: String)
    suspend fun softDeleteAssessment(assessmentKey: String)
    suspend fun upsertWithAudit(draft: AssessmentQuestion): String

    // ✅ soft delete
    suspend fun softDelete(questionId: String)

    // ✅ cleanup-only hard delete
    suspend fun hardDeleteIfDeleted(questionId: String)
    suspend fun hardDeleteOldTombstones(cutoff: com.google.firebase.Timestamp): Int
}

