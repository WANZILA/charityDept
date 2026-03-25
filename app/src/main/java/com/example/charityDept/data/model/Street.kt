package com.example.charityDept.data.model

import com.google.firebase.Timestamp

data class Street(
    val streetId: String = "",
    val streetName: String = "",
    val manifestId: String = "",   // e.g., manifest record id
    val manifestName: String = "",     // e.g., route/manifest name or notes
    val isActive: Boolean = true,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)

