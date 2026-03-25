package com.example.charityDept.data.model

import com.google.firebase.Timestamp

data class TechnicalSkill(
    val skillId: String = "",
    val name: String = "",          // e.g., "Kotlin", "Three.js"
    val category: String = "",      // e.g., "Mobile", "Web", "Backend", "3D"
    val level: Int = 1,             // 1–5 or any scale you prefer
    val isActive: Boolean = true,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

