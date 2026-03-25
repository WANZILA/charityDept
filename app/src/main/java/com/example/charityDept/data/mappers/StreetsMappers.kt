package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.Street
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

fun Street.toFirestoreMap(): Map<String, Any?> = mapOf(
    "streetId" to streetId,
    "name" to streetName,
    "manifestId" to manifestId,
    "manifest" to manifestName,
    "isActive" to isActive,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt
)

/** Do not overwrite createdAt in patches. */
fun Street.toFirestoreMapPatch(): Map<String, Any?> = mapOf(
    "name" to streetName,
    "manifestId" to manifestId,
    "manifest" to manifestName,
    "isActive" to isActive,
    "updatedAt" to Timestamp.now()
)

fun DocumentSnapshot.toStreet(): Street? {
    if (!exists()) return null
    return Street(
        streetId = getString("streetId") ?: id,
        streetName = getString("name") ?: "",
        manifestId = getString("manifestId") ?: "",
        manifestName = getString("manifest") ?: "",
        isActive = getBoolean("isActive") ?: true,
        createdAt = getTimestamp("createdAt") ?: Timestamp.now(),
        updatedAt = getTimestamp("updatedAt") ?: Timestamp.now()
    )
}

fun QuerySnapshot.toStreets(): List<Street> =
    documents.mapNotNull { it.toStreet() }

