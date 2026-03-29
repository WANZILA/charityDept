package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.AssessmentAnswer

fun AssessmentAnswer.toFirestoreMapPatch(): Map<String, Any?> {
    return mapOf(
        "answerId" to answerId,
        "childId" to childId,
        "generalId" to generalId,
        "questionId" to questionId,

        "assessmentKey" to assessmentKey,
        "assessmentLabel" to assessmentLabel,

        "category" to category,
        "subCategory" to subCategory,
        "questionSnapshot" to questionSnapshot,

        "answer" to answer,
        "recommendation" to recommendation,
        "score" to score,
        "notes" to notes,

        "enteredByUid" to enteredByUid,
        "lastEditedByUid" to lastEditedByUid,

        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "version" to version
    )
}