package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.AssessmentTaxonomy

fun AssessmentTaxonomy.toFirestoreMapPatch(): Map<String, Any?> {
    return mapOf(
        "taxonomyId" to taxonomyId,
        "assessmentKey" to assessmentKey,
        "assessmentLabel" to assessmentLabel,
        "categoryKey" to categoryKey,
        "categoryLabel" to categoryLabel,
        "subCategoryKey" to subCategoryKey,
        "subCategoryLabel" to subCategoryLabel,
        "indexNum" to indexNum,
        "isActive" to isActive,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
        "deletedAt" to deletedAt,
        "version" to version
    )
}