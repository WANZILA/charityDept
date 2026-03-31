package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.model.AssessmentTaxonomy
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentTaxonomyDao {

    @Query("SELECT * FROM assessment_taxonomy WHERE taxonomyId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<AssessmentTaxonomy>

    @Upsert
    suspend fun upsert(item: AssessmentTaxonomy)

    @Upsert
    suspend fun upsertAll(items: List<AssessmentTaxonomy>)

    @Query("SELECT COUNT(*) FROM assessment_taxonomy")
    suspend fun countAll(): Int

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDeleted = 0 AND isActive = 1
        ORDER BY assessmentLabel ASC, categoryLabel ASC, indexNum ASC, subCategoryLabel ASC
    """)
    fun observeActiveAll(): Flow<List<AssessmentTaxonomy>>

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDeleted = 0 AND isActive = 1
        ORDER BY assessmentLabel ASC, categoryLabel ASC, indexNum ASC, subCategoryLabel ASC
    """)
    suspend fun getActiveOnce(): List<AssessmentTaxonomy>

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDeleted = 0
        ORDER BY assessmentLabel ASC, categoryLabel ASC, indexNum ASC, subCategoryLabel ASC
    """)
    fun observeAllAdmin(): Flow<List<AssessmentTaxonomy>>

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE taxonomyId = :taxonomyId
        LIMIT 1
    """)
    suspend fun getOnce(taxonomyId: String): AssessmentTaxonomy?

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun loadDirtyBatch(limit: Int): List<AssessmentTaxonomy>
    @Query("""
        UPDATE assessment_taxonomy SET
            isDirty = 0,
            version = version + 1,
            updatedAt = :newUpdatedAt
        WHERE taxonomyId IN (:ids)
    """)
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)


    @Query("""
        DELETE FROM assessment_taxonomy
        WHERE taxonomyId = :taxonomyId AND isDeleted = 1
    """)
    suspend fun hardDeleteIfDeleted(taxonomyId: String)

    @Query("""
        DELETE FROM assessment_taxonomy
        WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :cutoff
    """)
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int
}