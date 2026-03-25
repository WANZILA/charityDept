package com.example.charityDept.presentation.screens.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.di.AdminAuth
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.UserProfile
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.String

data class UserFormState(
    val email: String = "",
//    val password: String = "",
    val displayName: String = "",
    val selectedAssignedRole: AssignedRole = AssignedRole.VOLUNTEER,   // ← single role
    val disabled: Boolean = false,
    val sendResetLink: Boolean = false,

    val loading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false,
    val createdUid: String? = null,
    val editingUid: String? = null,         // null = create, non-null = edit mode
    val originalEmail: String? = null       // keep original email when editing
)

@HiltViewModel
class UserFormViewModel @Inject constructor(
    private val usersRepo: UsersRepository,
    @AdminAuth private val adminAuth: FirebaseAuth
) : ViewModel() {

    var ui by mutableStateOf(UserFormState())
        private set

    fun updateEmail(v: String) { ui = ui.copy(email = v, success = false, error = null) }
//    fun updatePassword(v: String) { ui = ui.copy(password = v, success = false, error = null) }
    fun updateDisplayName(v: String) { ui = ui.copy(displayName = v, success = false, error = null) }
    fun setRole(userRole: AssignedRole) { ui = ui.copy(selectedAssignedRole = userRole, success = false, error = null) } // ← single role
    fun setDisabled(v: Boolean) { ui = ui.copy(disabled = v) }

    fun setSendResetLink(v: Boolean) { ui = ui.copy(sendResetLink = v) }


    private fun isValidCreate(): Boolean {
        val e = ui.email.trim()
//        val p = ui.password
        val d = ui.displayName
        return e.isNotBlank() && '@' in e  && d.isNotBlank()
    }

    /** Load an existing user for editing. */
//    fun loadForEdit(uid: String) {
//        if (ui.editingUid == uid) return
//        ui = ui.copy(loading = true, error = null, success = false)
//        viewModelScope.launch {
//            try {
//                val p = usersRepo.fetchProfile(uid)
//                if (p == null) {
//                    ui = ui.copy(loading = false, error = "User not found.")
//                    return@launch
//                }
//                ui = ui.copy(
//                    loading = false,
//                    editingUid = uid,
//                    email = p.email ?: "",
//                    originalEmail = p.email,
//                    displayName = p.displayName ?: "",
//                    selectedAssignedRole = (p.userRole.name ?: AssignedRole.VOLUNTEER) as AssignedRole, // ← take the first role
//                    disabled = p.disabled,
//                    success = false,
//                    createdUid = null
//                )
//            } catch (t: Throwable) {
//                ui = ui.copy(loading = false, error = t.message ?: "Failed to load user.")
//            }
//        }
//    }

    fun loadForEdit(uid: String) = viewModelScope.launch {
        ui = ui.copy(loading = true, error = null, success = false)
        val existing = usersRepo.fetchProfile(uid)
        ui = if (existing != null) {
            ui.from(existing).copy(
                loading = false,
                editingUid = uid,                 // <-- critical: needed by saveEdits()
                originalEmail = existing.email,   // keep original
                createdUid = null                 // make sure we’re in edit mode
            )
        } else {
            ui.copy(loading = false, error = "User not found.")
        }
    }


    /** Create a NEW Firebase auth user + profile. */
    fun submit(isOnline: Boolean = true) {
        if (!isValidCreate()) {
            ui = ui.copy(error = "Please provide a valid email, a password (6+ chars), DisplayName.")
            return
        }
        ui = ui.copy(loading = true, error = null, success = false)

        viewModelScope.launch {
            try {
                val profile = usersRepo.registerUserAsAdmin(
                    adminAuth = adminAuth,
                    email = ui.email,
//                    password = ui.password,
                    displayName = ui.displayName.ifBlank { null },
                    userRole = ui.selectedAssignedRole,
                    disabled = ui.disabled
                )
                ui = ui.copy(loading = false, success = true, createdUid = profile.uid)
            } catch (t: Throwable) {
                ui = ui.copy(loading = false, error = t.message ?: "Failed to create user.")
            }
        }
    }


    /** Save changes to an EXISTING user (role/disabled/displayName). */
    fun saveEdits() {
        val uid = ui.editingUid ?: run {
            ui = ui.copy(error = "Nothing to save.")
            return
        }
        ui = ui.copy(loading = true, error = null, success = false)

        viewModelScope.launch {
            try {
                val updated = UserProfile(
                    uid = uid,
                    email = ui.originalEmail, // do not change auth email here
                    displayName = ui.displayName.ifBlank { null },
                    userRole = ui.selectedAssignedRole, // ← single role
                    disabled = ui.disabled
                )
                usersRepo.upsertProfileByUid(uid, updated)


                // If toggle is ON, send the password reset email
                if (ui.sendResetLink) {
                    val emailToUse = ui.originalEmail ?: ui.email
                    if (emailToUse.isNotBlank()) {
                        try {
                            usersRepo.sendPasswordReset(emailToUse)   // <— repo call
                        } catch (_: Throwable) {
                            // Optional: ui = ui.copy(error = "Could not send reset email") // non-blocking
                        }
                    }
                }
//                ui = ui.copy(loading = false, success = true)
                ui = ui.copy(loading = false, success = true, createdUid = updated.uid)
            } catch (t: Throwable) {
                ui = ui.copy(loading = false, error = t.message ?: "Failed to save changes.")
            }
        }
    }

    private fun UserFormState.from(user: UserProfile) = copy(
      email = user.email ?: "",
      displayName = user.displayName ?: "",
      selectedAssignedRole = user.userRole ,   // ← single role
      disabled = user.disabled,
    )

}

