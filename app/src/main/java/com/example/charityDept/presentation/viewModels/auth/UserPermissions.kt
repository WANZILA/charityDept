// UserPermissions.kt
package com.example.charityDept.presentation.viewModels.auth

import com.example.charityDept.data.model.AssignedRole

data class UserPermissions(
    val canCreateEvent: Boolean = false,
    val canEditEvent: Boolean = false,
    val canDeleteEvent: Boolean = false,

    val canCreateChild: Boolean = false,
    val canEditChild: Boolean = false,
    val canDeleteChild: Boolean = false,

    val canMakeAllPresent: Boolean = false,
    val canMakeAllAbsent: Boolean = false,
    val canViewAttendance: Boolean = false,

    val canListUsers: Boolean = false,
    val canCreateUser: Boolean = false,
    val canEditUser: Boolean = false,
    val canDeleteUser: Boolean = false,
)

// Accept Role enums
fun permissionsForRoles(assignedRoles: List<AssignedRole>): UserPermissions =
    permissionsForNames(assignedRoles.map { it.name })

// Accept role names (stringy sources / Firestore), normalize & dedup
fun permissionsForNames(roleNames: List<String>): UserPermissions {
    val names: Set<String> = roleNames
        .map { it.trim().uppercase() }
        .toSet()

    val isAdmin = "ADMIN" in names
    val isLead = "LEADER" in names
    val isVolunteer = "VOLUNTEER" in names

    val adminOrLead = isAdmin || isLead
    val anyoneWhoCanMarkAttendance = isAdmin || isLead || isVolunteer

    return UserPermissions(
        // Events
        canCreateEvent   = adminOrLead,
        canEditEvent     = adminOrLead,
        canDeleteEvent   = isAdmin,

        // Children
        canCreateChild   = adminOrLead || isVolunteer,
        canEditChild     = isAdmin,
        canDeleteChild   = isAdmin,

        // Attendance
        canMakeAllPresent = anyoneWhoCanMarkAttendance,
        canMakeAllAbsent  = anyoneWhoCanMarkAttendance,
        canViewAttendance = anyoneWhoCanMarkAttendance,

        // Users / Admin
        canListUsers     = isAdmin,
        canCreateUser    = isAdmin,
        canEditUser      = isAdmin,
        canDeleteUser    = isAdmin,
    )
}

