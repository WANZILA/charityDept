package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.AssessmentQuestion

// /// ADDED: minimal patch map (mirrors Event.toFirestoreMapPatch style)
fun AssessmentQuestion.toFirestoreMapPatch(): Map<String, Any?> {
    return mapOf(
        "questionId" to questionId,
        "category" to category,
        "subCategory" to subCategory,
        "categoryKey" to categoryKey,
        "subCategoryKey" to subCategoryKey,
        "question" to question,
        "isActive" to isActive,
        "indexNum" to indexNum,

        // audit
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "version" to version
    )
}
