package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.model.AssessmentQuestion
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentQuestionDao {

    @Query("SELECT * FROM assessment_questions WHERE questionId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<AssessmentQuestion>

    @Upsert
    suspend fun upsert(item: AssessmentQuestion)

    @Upsert
    suspend fun upsertAll(items: List<AssessmentQuestion>)

    @Query("""
    SELECT q.* FROM assessment_questions q
    LEFT JOIN assessment_taxonomy t
      ON t.categoryKey = q.categoryKey
     AND t.subCategoryKey = q.subCategoryKey
     AND t.isDeleted = 0
     AND t.isActive = 1
    WHERE q.isDeleted = 0
      AND q.isActive = 1
    ORDER BY 
      COALESCE(t.indexNum, 999999),
      COALESCE(q.indexNum, 999999),
      q.questionId
""")
    fun observeActive(): Flow<List<AssessmentQuestion>>

    @Query("""
    SELECT q.* FROM assessment_questions q
    LEFT JOIN assessment_taxonomy t
      ON t.categoryKey = q.categoryKey
     AND t.subCategoryKey = q.subCategoryKey
     AND t.isDeleted = 0
     AND t.isActive = 1
    WHERE q.isDeleted = 0
      AND q.isActive = 1
    ORDER BY 
      COALESCE(t.indexNum, 999999),
      COALESCE(q.indexNum, 999999),
      q.questionId
""")
    suspend fun getActiveOnce(): List<AssessmentQuestion>


    // /// ADDED: Admin list (includes inactive, excludes deleted)
    @Query("""
    SELECT * FROM assessment_questions
    WHERE isDeleted = 0
    ORDER BY categoryKey, subCategoryKey, indexNum, questionId
""")
    fun observeAllAdmin(): Flow<List<AssessmentQuestion>>

    // /// ADDED: Load one
    @Query("""
        SELECT * FROM assessment_questions
        WHERE questionId = :questionId
        LIMIT 1
    """)
    suspend fun getOnce(questionId: String): AssessmentQuestion?

    @Query("""
        UPDATE assessment_questions
        SET isDeleted = 1,
            deletedAt = :nowNanos,
            updatedAt = :nowNanos,
            isDirty = 1,
            version = version + 1
        WHERE questionId = :questionId
          AND isDeleted = 0
    """)
    suspend fun softDeleteQuestion(
        questionId: String,
        nowNanos: Long
    ): Int

    // ✅ HARD DELETE tombstones during cleanup ONLY
    @Query("""
        DELETE FROM assessment_questions
        WHERE isDeleted = 1
          AND deletedAt IS NOT NULL
          AND deletedAt < :cutoff
    """)
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int

    // ✅ HARD DELETE guarded (single row) — only if already deleted
    @Query("""
        DELETE FROM assessment_questions
        WHERE questionId = :questionId
          AND isDeleted = 1
    """)
    suspend fun hardDeleteIfDeleted(questionId: String): Int


    @Query("SELECT * FROM assessment_questions WHERE isDirty = 1 ORDER BY updatedAt ASC LIMIT :limit")
    suspend fun loadDirtyBatch(limit: Int): List<AssessmentQuestion>

    @Query("""
    UPDATE assessment_questions SET
        isDirty = 0,
        version = version + 1,
        updatedAt = :newUpdatedAt
    WHERE questionId IN (:ids)
""")
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)


}

