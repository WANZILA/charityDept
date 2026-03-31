package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.model.AssessmentTaxonomy
import com.google.firebase.Timestamp
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OfflineAssessmentTaxonomyRepositoryImpl @Inject constructor(
    private val dao: AssessmentTaxonomyDao
) : OfflineAssessmentTaxonomyRepository {

    override fun observeAdminAll() = dao.observeAllAdmin()

    override suspend fun getOnce(taxonomyId: String): AssessmentTaxonomy? =
        dao.getOnce(taxonomyId)

    override suspend fun upsertWithAudit(draft: AssessmentTaxonomy): String {
        val now = Timestamp.now()

        val id = if (draft.taxonomyId.isBlank()) UUID.randomUUID().toString() else draft.taxonomyId
        val current = dao.getOnce(id)

        val createdAt = current?.createdAt ?: draft.createdAt
        val nextVersion = (current?.version ?: 0L) + 1L

        val toSave = draft.copy(
            taxonomyId = id,
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

    override suspend fun softDelete(taxonomyId: String) {
        val current = dao.getOnce(taxonomyId) ?: return
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

    override suspend fun hardDeleteIfDeleted(taxonomyId: String) {
        dao.hardDeleteIfDeleted(taxonomyId)
    }

    override suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int {
        return dao.hardDeleteOldTombstones(cutoff)
    }
}