package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.model.AssessmentAnswer
import com.example.charityDept.data.model.AssessmentQuestion
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
//import com.google.firebase.Timestamp

data class AssessmentSessionRow(
    val generalId: String,
    val lastUpdated: Long,
    val questionSnapshot: String,
    val itemCount: Int,
    val enteredByUid: String?
)

@Dao
interface AssessmentAnswerDao {

    @Upsert
    suspend fun upsert(item: AssessmentAnswer)

    @Upsert
    suspend fun upsertAll(items: List<AssessmentAnswer>)

    @Query("""
SELECT
  COALESCE(a.answerId, :generalId || '_' || q.questionId) AS answerId,
  :generalId AS generalId,
  :childId AS childId,
  q.questionId AS questionId,

  COALESCE(a.assessmentKey, q.assessmentKey, t.assessmentKey, '') AS assessmentKey,
  COALESCE(a.assessmentLabel, q.assessmentLabel, t.assessmentLabel, '') AS assessmentLabel,

  q.category AS category,
  q.subCategory AS subCategory,
  q.question AS questionSnapshot,

  COALESCE(a.answer, '') AS answer,
  COALESCE(a.recommendation, '') AS recommendation,
  COALESCE(a.score, 0) AS score,
  COALESCE(a.notes, '') AS notes,
  COALESCE(a.enteredByUid, '') AS enteredByUid,
  COALESCE(a.lastEditedByUid, '') AS lastEditedByUid,
  COALESCE(a.createdAt, 0) AS createdAt,
  COALESCE(a.updatedAt, 0) AS updatedAt,
  COALESCE(a.isDirty, 0) AS isDirty,
  0 AS isDeleted,
  NULL AS deletedAt,
  COALESCE(a.version, 0) AS version
FROM assessment_questions q
LEFT JOIN assessment_answers a
  ON a.questionId = q.questionId
 AND a.childId = :childId
 AND a.generalId = :generalId
 AND a.isDeleted = 0
LEFT JOIN assessment_taxonomy t
  ON t.assessmentKey = q.assessmentKey
 AND t.categoryKey = q.categoryKey
 AND t.subCategoryKey = q.subCategoryKey
 AND t.isDeleted = 0
 AND t.isActive = 1
WHERE q.isDeleted = 0
  AND q.isActive = 1
ORDER BY
  COALESCE(t.indexNum, 999999),
  COALESCE(q.indexNum, 999999),
  COALESCE(q.subCategoryKey, ''),
  q.questionId
""")
    fun observeSession(childId: String, generalId: String): Flow<List<AssessmentAnswer>>

    @Query("""
        SELECT * FROM assessment_answers
        WHERE answerId = :answerId
        LIMIT 1
    """)
    suspend fun getOnce(answerId: String): AssessmentAnswer?

    @Query("""
        SELECT
          generalId AS generalId,
          MAX(updatedAt) AS lastUpdated,
          COALESCE(MIN(questionSnapshot), '') AS questionSnapshot,
          COUNT(*) AS itemCount,
          MIN(enteredByUid) AS enteredByUid
        FROM assessment_answers
        WHERE isDeleted = 0
          AND childId = :childId
        GROUP BY generalId
        HAVING (
            (:mode = 'OBS' AND SUM(CASE WHEN LOWER(category) = 'observation' THEN 1 ELSE 0 END) > 0)
         OR (:mode = 'QA'  AND SUM(CASE WHEN LOWER(category) <> 'observation' THEN 1 ELSE 0 END) > 0)
         OR (:mode NOT IN ('OBS','QA'))
        )
        ORDER BY MAX(updatedAt) DESC
    """)
    fun observeSessionRows(childId: String, mode: String): Flow<List<AssessmentSessionRow>>

//    @Query("""
//        SELECT
//          generalId AS generalId,
//          MAX(updatedAt) AS lastUpdated,
//          COALESCE(MIN(questionSnapshot), '') AS questionSnapshot,
//          COUNT(*) AS itemCount,
//          MIN(enteredByUid) AS enteredByUid
//        FROM assessment_answers
//        WHERE isDeleted = 0
//          AND childId = :childId
//        GROUP BY generalId
//        HAVING (
//            (:mode = 'OBS' AND SUM(CASE WHEN LOWER(category) = 'observation' THEN 1 ELSE 0 END) > 0)
//         OR (:mode = 'QA'  AND SUM(CASE WHEN LOWER(category) <> 'observation' THEN 1 ELSE 0 END) > 0)
//         OR (:mode NOT IN ('OBS','QA'))
//        )
//        ORDER BY MAX(updatedAt) DESC
//    """)
//    fun observeSessionRows(childId: String, mode: String): Flow<List<AssessmentSessionRow>>

    @Query("SELECT * FROM assessment_answers")
    fun streamAllAdmin(): Flow<List<AssessmentAnswer>>

    // ✅ SOFT DELETE (single answer) — keep row, mark deleted, bump version, mark dirty
    // /// CHANGED: now is Timestamp (matches entity + converters)
    @Query("""
        UPDATE assessment_answers
        SET isDeleted = 1,
            deletedAt = :now,
            updatedAt = :now,
            lastEditedByUid = :editorUid,
            isDirty = 1,
            version = version + 1
        WHERE answerId = :answerId
          AND isDeleted = 0
    """)
    suspend fun softDeleteAnswer(
        answerId: String,
        editorUid: String,
        now: Timestamp
    ): Int

    // ✅ SOFT DELETE (whole session)
    // /// CHANGED: now is Timestamp
    @Query("""
        UPDATE assessment_answers
        SET isDeleted = 1,
            deletedAt = :now,
            updatedAt = :now,
            lastEditedByUid = :editorUid,
            isDirty = 1,
            version = version + 1
        WHERE childId = :childId
          AND generalId = :generalId
          AND isDeleted = 0
    """)
    suspend fun softDeleteSession(
        childId: String,
        generalId: String,
        editorUid: String,
        now: Timestamp
    ): Int

    // ✅ OPTIONAL: UNDO soft delete (single answer)
    @Query("""
        UPDATE assessment_answers
        SET isDeleted = 0,
            deletedAt = NULL,
            updatedAt = :now,
            lastEditedByUid = :editorUid,
            isDirty = 1,
            version = version + 1
        WHERE answerId = :answerId
          AND isDeleted = 1
    """)
    suspend fun restoreAnswer(
        answerId: String,
        editorUid: String,
        now: Timestamp
    ): Int

    // ✅ OPTIONAL: UNDO soft delete (whole session)
    @Query("""
        UPDATE assessment_answers
        SET isDeleted = 0,
            deletedAt = NULL,
            updatedAt = :now,
            lastEditedByUid = :editorUid,
            isDirty = 1,
            version = version + 1
        WHERE childId = :childId
          AND generalId = :generalId
          AND isDeleted = 1
    """)
    suspend fun restoreSession(
        childId: String,
        generalId: String,
        editorUid: String,
        now: Timestamp
    ): Int

    // ✅ HARD DELETE during cleanup ONLY (guarded)
    @Query("""
        DELETE FROM assessment_answers
        WHERE answerId = :answerId
          AND isDeleted = 1
    """)
    suspend fun hardDeleteAnswerIfDeleted(answerId: String): Int

    @Query("""
        DELETE FROM assessment_answers
        WHERE childId = :childId
          AND generalId = :generalId
          AND isDeleted = 1
    """)
    suspend fun hardDeleteSessionIfDeleted(childId: String, generalId: String): Int

    // ✅ OPTIONAL: cleanup everything already soft-deleted AND already synced (isDirty=0)
    @Query("""
        DELETE FROM assessment_answers
        WHERE isDeleted = 1
          AND isDirty = 0
    """)
    suspend fun hardCleanupSoftDeletedSynced(): Int

    // ✅ HARD DELETE old tombstones
    @Query("""
        DELETE FROM assessment_answers
        WHERE isDeleted = 1
          AND deletedAt IS NOT NULL
          AND deletedAt < :cutoff
    """)
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int

    // <app/src/main/java/com/example/zionkids/data/local/dao/AssessmentAnswerDao.kt>
// /// ADDED: loadDirtyBatch(limit) for SyncWorker
// /// ADDED: markBatchPushed(ids, newUpdatedAt) to clear dirty + bump version
// /// ADDED: getByIds(ids) used by PullWorker merge


    @Query("""SELECT * FROM assessment_answers WHERE isDirty = 1 ORDER BY updatedAt ASC LIMIT :limit""")
    suspend fun loadDirtyBatch(limit: Int): List<AssessmentAnswer>

    @Query("""
    UPDATE assessment_answers SET
        isDirty = 0,
        version = version + 1,
        updatedAt = :newUpdatedAt
    WHERE answerId IN (:ids)
""")
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)

    @Query("SELECT * FROM assessment_answers WHERE answerId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<AssessmentAnswer>



}

