package com.example.charityDept.data.model


import com.example.charityDept.data.model.AssignedRole

data class LockedAccount(
    val emailLower: String,
    val count: Int,
    val updatedAtMillis: Long?,
    val userRole: AssignedRole?,   // from /users
    val disabled: Boolean?         // from /users
)

