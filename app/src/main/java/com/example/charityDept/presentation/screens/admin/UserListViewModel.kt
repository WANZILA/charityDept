package com.example.charityDept.presentation.screens.admin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.UserProfile
import com.example.charityDept.domain.repositories.online.UsersRepository
import com.example.charityDept.domain.repositories.online.UsersSnapshot
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Boolean

// --- UI STATE (users-specific) — parity with ChildrenListUiState ---
data class UserListUiState(
    val loading: Boolean = true,
    val users: List<UserProfile> = emptyList(),

    // active filters reflected back to UI
    val disabledFilter: Boolean? = null,

    val userRoleFilter: AssignedRole? = null,

    val total: Int = 0,      // total before search/filter
    val filtered: Int = 0,   // count after applying filters + search

    val isOffline: Boolean = false,
    val isSyncing: Boolean = false,
    val error: String? = null,
    val lastRefreshed: Timestamp? = null,

    // optional CRUD helpers (kept for future use)
    val success: Boolean = false,
    val createdUid: String? = null,
//    val editingUid: String? = null,
    val originalEmail: String? = null
)

@HiltViewModel
class UserListViewModel @Inject constructor(
    private val usersRepo: UsersRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _ui = MutableStateFlow(UserListUiState())
    val ui: StateFlow<UserListUiState> = _ui.asStateFlow()

    private val _query = MutableStateFlow("")
    fun onSearchQueryChange(q: String) { _query.value = q }

    private var latestSnap: UsersSnapshot =
        UsersSnapshot(emptyList(), fromCache = true, hasPendingWrites = false)

    private val filterDisabled: Boolean? =
        savedState.get<String>("disabled")?.toBoolOrNull()

//    private val filterVolunteer: String? =
//        savedState.get<String>("volunteer")?.takeIf { it.isNotBlank() }?.decodeAndNorm()

    private val filterUserRole: AssignedRole? = runCatching {
        savedState.get<String>("userRole").orEmpty().takeIf { it.isNotBlank() }
            ?.let { AssignedRole.valueOf(it) }
    }.getOrNull()


    private val hasAnyFilter: Boolean = listOf(
        filterDisabled, filterUserRole
    ).any { it != null }


    init {
        // server stream (eduPref if present) + local filters + search + sort
        viewModelScope.launch {
//            val sourceFlow =
//                if (filterPref == null) childrenRepo.streamAllNotGraduated()
//                else childrenRepo.streamByEducationPreferenceResilient(filterPref)
            val sourceFlow =
                when {
                    !hasAnyFilter -> usersRepo.streamAllUserSnapshot() // ← show ALL children when no filters passed
                    filterUserRole != null -> usersRepo.streamByViewerPreferenceResilient(filterUserRole)
//                    else -> childrenRepo.streamAllNotGraduated()
                    else -> usersRepo.streamAllUserSnapshot()
                }

            combine(
                sourceFlow,
                _query.map { it.trim().lowercase() }.distinctUntilChanged()
            ) { snap, needle ->
                // 1) start with users
                latestSnap = snap

                // 1) start with snapshot children
                var base = snap.users

                // 2) apply local filters (roles)
                filterDisabled?.let    { want ->
                    base = base.filter { it.disabled == want  }
                }



                // 3) UI search (full name)
                val searched = if (needle.isEmpty()) base else {
                    base.filter { c ->
                        val full = "${c.displayName}".trim().lowercase()
                        full.contains(needle)
                    }
                }

                // 4) sort: keep your existing (updatedAt desc, then createdAt desc)
                val sorted = searched.sortedWith(
                    compareByDescending<UserProfile> { it.updatedAt }
                        .thenByDescending { it.createdAt }
                )


                UserListUiState(
                    loading = false,
                    users = sorted,
                    userRoleFilter = filterUserRole,
                    disabledFilter = filterDisabled,
                    total = snap.users.size,
                    filtered = sorted.size,
                    isOffline = snap.fromCache,
                    isSyncing = snap.hasPendingWrites,
                    error = null,
                    lastRefreshed = Timestamp.now()
                )
            }
                .onStart {
                    _ui.value = _ui.value.copy(
                        loading = true,
                        userRoleFilter = filterUserRole,
                        disabledFilter = filterDisabled,
                        total = 0,
                        filtered = 0
                    )
                }
                .catch { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = e.message ?: "Failed to load children"
                    )
                }
                .collect { state -> _ui.value = state }
        }
    }


    /** Optional: prime cache via a one-shot fetch, then the stream will update. */
    fun refresh() {
        viewModelScope.launch {
            try {
                _ui.value = _ui.value.copy(loading = true)
                usersRepo.streamAllUserSnapshot()// prime cache once; stream will reflect updates if your repo caches
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message)
            } finally {
                _ui.value = _ui.value.copy(loading = false, lastRefreshed = Timestamp.now())
            }
        }
    }

    // ------------- helpers -------------
    private fun String.toBoolOrNull(): Boolean? = when (this.lowercase()) {
        "true", "1", "yes" -> true
        "false", "0", "no" -> false
        else -> null
    }



}

