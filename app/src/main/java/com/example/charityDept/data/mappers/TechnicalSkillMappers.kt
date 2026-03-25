package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.TechnicalSkill
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

fun TechnicalSkill.toFirestoreMap(): Map<String, Any?> = mapOf(
    "skillId" to skillId,
    "name" to name,
    "category" to category,
    "level" to level,
    "isActive" to isActive,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

fun TechnicalSkill.toFirestoreMapPatch(): Map<String, Any?> = mapOf(
    "name" to name,
    "category" to category,
    "level" to level,
    "isActive" to isActive,
    "updatedAt" to Timestamp.now()
)

fun DocumentSnapshot.toTechnicalSkill(): TechnicalSkill? {
    if (!exists()) return null
    return TechnicalSkill(
        skillId = getString("skillId") ?: id,
        name = getString("name") ?: "",
        category = getString("category") ?: "",
        level = (getLong("level") ?: 0L).toInt(),
        isActive = getBoolean("isActive") ?: true,
        createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
        updatedAt = getTimestamp("updatedAt") ?: Timestamp.now()
    )
}

fun QuerySnapshot.toTechnicalSkills(): List<TechnicalSkill> =
    documents.mapNotNull { it.toTechnicalSkill() }

