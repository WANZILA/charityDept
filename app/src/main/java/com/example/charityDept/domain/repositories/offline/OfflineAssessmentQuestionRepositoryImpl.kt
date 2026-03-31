package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.model.AssessmentQuestion
import com.google.firebase.Timestamp
import javax.inject.Inject
import javax.inject.Singleton
import java.util.UUID


private fun normalizeAssessmentKey(raw: String): String {
    return raw
        .trim()
        .uppercase()
        .replace("&", "_&_")
        .replace(Regex("[^A-Z0-9&_]+"), "_")
        .replace(Regex("_+"), "_")
        .trim('_')
}

private fun assessmentPrefixFor(assessmentKey: String): String {
    val parts = assessmentKey
        .trim()
        .uppercase()
        .split("_")
        .filter { it.isNotBlank() }

    return when {
        parts.size >= 2 -> parts.joinToString("") { it.first().toString() }
        parts.size == 1 -> parts.first().take(2)
        else -> ""
    }
}

@Singleton
class OfflineAssessmentQuestionRepositoryImpl @Inject constructor(
    private val dao: AssessmentQuestionDao
) : OfflineAssessmentQuestionRepository {

    override fun observeAdminAll() = dao.observeAllAdmin()

    override suspend fun getOnce(questionId: String): AssessmentQuestion? =
        dao.getOnce(questionId)

    override suspend fun renameAssessmentLabel(assessmentKey: String, newAssessmentLabel: String) {
        val rows = dao.getActiveByAssessmentKey(assessmentKey)
        if (rows.isEmpty()) return

        val now = Timestamp.now()

        dao.upsertAll(
            rows.map { row ->
                row.copy(
                    assessmentLabel = newAssessmentLabel.trim().uppercase(),
                    updatedAt = now,
                    isDirty = true,
                    version = row.version + 1L
                )
            }
        )
    }

    override suspend fun softDeleteAssessment(assessmentKey: String) {
        val rows = dao.getActiveByAssessmentKey(assessmentKey)
        if (rows.isEmpty()) return

        val now = Timestamp.now()

        dao.upsertAll(
            rows.map { row ->
                row.copy(
                    updatedAt = now,
                    isDirty = true,
                    isDeleted = true,
                    deletedAt = now,
                    version = row.version + 1L
                )
            }
        )
    }

    override suspend fun upsertWithAudit(draft: AssessmentQuestion): String {
        val now = Timestamp.now()

        val id = if (draft.questionId.isBlank()) UUID.randomUUID().toString() else draft.questionId
        val current = dao.getOnce(id)

        val createdAt = current?.createdAt ?: draft.createdAt
        val nextVersion = (current?.version ?: 0L) + 1L

        val toSave = draft.copy(
            questionId = id,
            createdAt = createdAt,
            updatedAt = now,
            isDirty = true,
            isDeleted = false,
            deletedAt = null,
            version = nextVersion
        )

        dao.upsert(toSave)
        return id
    }

    override suspend fun softDelete(questionId: String) {
        val current = dao.getOnce(questionId) ?: return
        val now = Timestamp.now()

        dao.upsert(
            current.copy(
                updatedAt = now,
                isDirty = true,
                isDeleted = true,
                deletedAt = now,
                version = current.version + 1L
            )
        )
    }

    // cleanup-only
    override suspend fun hardDeleteIfDeleted(questionId: String) {
        dao.hardDeleteIfDeleted(questionId)
    }

    override suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int {
        return dao.hardDeleteOldTombstones(cutoff)
    }

}

