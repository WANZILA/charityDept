package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.core.utils.GenerateId
import com.example.charityDept.data.local.dao.AssessmentAnswerDao
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.local.dao.AssessmentSessionRow
import com.example.charityDept.data.local.dao.AssessmentToolOption
import com.example.charityDept.data.model.AssessmentAnswer
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChanged
interface OfflineAssessmentRepository {

    fun observeAvailableAssessmentTools(mode: String): Flow<List<AssessmentToolOption>>
    fun observeSessionRows(childId: String, mode: String): Flow<List<AssessmentSessionRow>>
    fun observeSession(
        childId: String,
        generalId: String,
        mode: String,
        assessmentKey: String
    ): Flow<List<AssessmentAnswer>>



    suspend fun upsertAnswerWithAudit(draft: AssessmentAnswer, editorUid: String)
    suspend fun startNewAssessment(childId: String, editorUid: String): String
    suspend fun startNewSession(childId: String, mode: String, editorUid: String): String
    suspend fun softDeleteAnswer(answerId: String, editorUid: String)
    suspend fun softDeleteSession(childId: String, generalId: String, editorUid: String)
    suspend fun hardDeleteAnswerIfDeleted(answerId: String)
    suspend fun hardDeleteSessionIfDeleted(childId: String, generalId: String)
    suspend fun hardCleanupSoftDeletedSynced(): Int
}

@Singleton
class OfflineAssessmentRepositoryImpl @Inject constructor(
    private val answerDao: AssessmentAnswerDao,
    private val questionDao: AssessmentQuestionDao, // /// ADDED
) : OfflineAssessmentRepository {

    override fun observeAvailableAssessmentTools(mode: String): Flow<List<AssessmentToolOption>> =
        questionDao.observeAvailableAssessmentTools(mode)
            .distinctUntilChanged()

//    override fun observeSessionRows(childId: String): Flow<List<AssessmentSessionRow>> =
//        answerDao.observeSessionRows(childId)
override fun observeSessionRows(childId: String, mode: String): Flow<List<AssessmentSessionRow>> =
    answerDao.observeSessionRows(childId, mode)

    override fun observeSession(
        childId: String,
        generalId: String,
        mode: String,
        assessmentKey: String
    ): Flow<List<AssessmentAnswer>> =
        answerDao.observeSession(childId, generalId, mode, assessmentKey)
            .distinctUntilChanged()


    override suspend fun upsertAnswerWithAudit(draft: AssessmentAnswer, editorUid: String) {
        val now = Timestamp.now()

        val current = answerDao.getOnce(draft.answerId)

        val isNew = current == null
        val nextVersion = (current?.version ?: 0L) + 1L

        val createdAt = if (isNew) now else (current?.createdAt ?: draft.createdAt)
        val enteredBy = if (isNew) editorUid
        else (current?.enteredByUid?.ifBlank { editorUid } ?: draft.enteredByUid.ifBlank { editorUid })

        val toSave = draft.copy(
            createdAt = createdAt,
            updatedAt = now,
            enteredByUid = enteredBy,
            lastEditedByUid = editorUid,
            version = nextVersion,
            isDirty = true,
            isDeleted = false,
            deletedAt = null
        )

        answerDao.upsert(toSave)
    }

//    override suspend fun upsertAnswerWithAudit(draft: AssessmentAnswer, editorUid: String) {
//        val now = Timestamp.now()
//
//        val current = answerDao.getOnce(draft.answerId)
//        val nextVersion = (current?.version ?: 0L) + 1L
//        val createdAt = current?.createdAt ?: draft.createdAt
//        val enteredBy = current?.enteredByUid?.ifBlank { editorUid } ?: draft.enteredByUid.ifBlank { editorUid }
//
//        val toSave = draft.copy(
//            createdAt = createdAt,
//            updatedAt = now,
//            enteredByUid = enteredBy,
//            lastEditedByUid = editorUid,
//            version = nextVersion,
//            isDirty = true
//        )
//
//        answerDao.upsert(toSave)
//    }

    // /// ADDED: Phase 3
    override suspend fun startNewAssessment(childId: String, editorUid: String): String {
        // just create session id; answers are created on Save
        return GenerateId.generateId("gen")
    }
    override suspend fun startNewSession(childId: String, mode: String, editorUid: String): String {
        // no seeding anymore
        return  GenerateId.generateId("gen")
    }

    val now = Timestamp.now()

    override suspend fun softDeleteAnswer(answerId: String, editorUid: String) {

        answerDao.softDeleteAnswer(
            answerId = answerId,
            editorUid = editorUid,
            now = now
        )
    }

    override suspend fun softDeleteSession(childId: String, generalId: String, editorUid: String) {
//        val now = nowNanos()
        answerDao.softDeleteSession(
            childId = childId,
            generalId = generalId,
            editorUid = editorUid,
            now = now
        )
    }

    // ✅ cleanup-only hard delete (guarded)
    override suspend fun hardDeleteAnswerIfDeleted(answerId: String) {
        answerDao.hardDeleteAnswerIfDeleted(answerId)
    }

    override suspend fun hardDeleteSessionIfDeleted(childId: String, generalId: String) {
        answerDao.hardDeleteSessionIfDeleted(childId, generalId)
    }

    override suspend fun hardCleanupSoftDeletedSynced(): Int {
        return answerDao.hardCleanupSoftDeletedSynced()
    }

}

