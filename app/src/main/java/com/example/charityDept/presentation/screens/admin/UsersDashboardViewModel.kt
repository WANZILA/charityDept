package com.example.charityDept.presentation.viewModels.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.domain.repositories.online.LockedAccountsRepository
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.example.charityDept.domain.repositories.online.UsersSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UsersDashboardUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val total: Int = 0,
    val active: Int = 0,
    val disabled: Int = 0,
    val assignedRoleDist: Map<AssignedRole, Int> = emptyMap()
)

data class LockedAccountUi(
    val emailLower: String,
    val count: Int,
    val updatedAtMillis: Long?,      // for “locked X mins ago”
    val userRole: AssignedRole?,     // from /users
    val disabled: Boolean?           // from /users
)

data class LockedListUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val totalLocked: Int = 0,
    val locked: List<LockedAccountUi> = emptyList()
)

private const val LOCK_THRESHOLD = 3
@HiltViewModel
class UsersDashboardViewModel @Inject constructor(
    private val usersRepo: UsersRepository,
    private val lockedRepo: LockedAccountsRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(UsersDashboardUiState())
    val ui: StateFlow<UsersDashboardUiState> = _ui.asStateFlow()

    // add state holder for locked list
    private val _locked = MutableStateFlow(LockedListUiState()
    )
    val locked: StateFlow<LockedListUiState> = _locked.asStateFlow()




    init {
        viewModelScope.launch {
            usersRepo.streamAllUserSnapshot()
                .onStart { _ui.update { it.copy(loading = true, error = null) } }
                .catch { e -> _ui.update { it.copy(loading = false, error = e.message ?: "Failed to load users") } }
                .collect { list -> _ui.update { compute(list) } }
        }

        // new locked accounts stream
        viewModelScope.launch {
            lockedRepo.streamLockedAccounts()
                .catch { e -> _locked.update { it.copy(loading = false, error = e.message ?: "Failed to load locked accounts") } }
                .collect { rows ->
                    _locked.update {
                        it.copy(
                            loading = false,
                            error = null,
                            totalLocked = rows.size,
                            locked = rows.map { r ->
                                LockedAccountUi(
                                    emailLower = r.emailLower,
                                    count = r.count,
                                    updatedAtMillis = r.updatedAtMillis,
                                    userRole = r.userRole,
                                    disabled = r.disabled
                                )
                            }
                        )
                    }
                }
        }



        //for counting only locked Accounts
//        viewModelScope.launch {
//            lockedRepo.streamLockedAccounts()
//                .map { rows -> rows.count { it.count >= LOCK_THRESHOLD } }
//                .distinctUntilChanged()
//                .catch { e ->
//                    _locked.update { it.copy(loading = false, error = e.message ?: "Failed to load locked accounts") }
//                }
//                .collect { total ->
//                    _locked.update { it.copy(loading = false, error = null, totalLocked = total) }
//                }
//        }
    }
    // optional helper that just forwards to repo
    fun adminUnlock(emailLower: String) = viewModelScope.launch {
        runCatching { lockedRepo.adminUnlock(emailLower) }
            .onFailure { e ->
                _locked.update { it.copy(error = e.message ?: "Unlock failed") }
            }
    }

    private fun compute(all: UsersSnapshot): UsersDashboardUiState {
        val total = all.users.size
        val disabled = all.users.count { it.disabled }
        val active = total - disabled

        // Count how many users hold each Role
//        val assignedRoleCounts: Map<AssignedRole, Int> = AssignedRole.values().associateWith { role ->
//            all.users.count { profile -> true }
//        }.filterValues { it > 0 } // keep it compact (optional)
        val assignedRoleCounts: Map<AssignedRole, Int> =
            AssignedRole.entries.associateWith { role ->
                all.users.count { profile -> profile.userRole == role }
            }.filterValues { it > 0 }

        return UsersDashboardUiState(
            loading = false,
            error = null,
            total = total,
            active = active,
            disabled = disabled,
            assignedRoleDist = assignedRoleCounts

        )
    }
}

