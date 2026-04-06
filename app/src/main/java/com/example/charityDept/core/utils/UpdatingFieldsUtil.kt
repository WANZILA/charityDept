package com.example.charityDept.core.utils

import com.example.charityDept.data.model.AssignedRole
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun adminSetRoleByEmail(email: String, role: AssignedRole) {
    val db = FirebaseFirestore.getInstance()
    val doc = db.collection("users")
        .whereEqualTo("email", email.trim().lowercase())
        .limit(1).get().await()
        .documents.firstOrNull() ?: return
    doc.reference.update("userRole", role.name).await()
}

