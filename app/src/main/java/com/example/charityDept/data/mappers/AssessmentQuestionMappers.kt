package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.AssessmentQuestion

fun AssessmentQuestion.toFirestoreMapPatch(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "assessmentKey" to assessmentKey,
        "assessmentLabel" to assessmentLabel,
        "category" to category,
        "subCategory" to subCategory,
        "categoryKey" to categoryKey,
        "subCategoryKey" to subCategoryKey,
        "question" to question,
        "isActive" to isActive,
        "indexNum" to indexNum,

        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "version" to version
    )
}