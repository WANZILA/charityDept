// <app/src/main/java/com/example/zionkids/data/mappers/AssessmentAnswerMappers.kt>
package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.AssessmentAnswer
import com.google.firebase.Timestamp

fun AssessmentAnswer.toFirestoreMapPatch(): Map<String, Any?> {
    return mapOf(
        // ids
        "answerId" to answerId,
        "generalId" to generalId,
        "childId" to childId,
        "questionId" to questionId,

        // snapshots
        "category" to category,
        "subCategory" to subCategory,
        "questionSnapshot" to questionSnapshot,

        // inputs
        "answer" to answer,
        "score" to score,
        "notes" to notes,

        // audit
        "enteredByUid" to enteredByUid,
        "lastEditedByUid" to lastEditedByUid,

        // sync/audit fields
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDirty" to isDirty,     // optional but fine to store
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "version" to version
    )
}

