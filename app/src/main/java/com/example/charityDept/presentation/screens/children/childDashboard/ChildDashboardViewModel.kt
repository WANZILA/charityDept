package com.example.charityDept.presentation.screens.children.childDashboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.model.AttendanceStatus
import com.example.charityDept.data.model.Child
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class ChildDashboardState(
    val loading: Boolean = true,
    val error: String? = null,
    val childId: String = "",
    val child: Child? = null,
    val eventsCount: Int = 0,
    val attendanceTotal: Int = 0,
    val attendancePresent: Int = 0,
    val lastUpdated: Timestamp? = null
)

@HiltViewModel
class ChildDashboardViewModel @Inject constructor(
    private val childDao: ChildDao,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private fun parseChildId(): String {
        return (savedStateHandle["childIdArg"] as? String)
            ?: (savedStateHandle["childId"] as? String)
            ?: (savedStateHandle["id"] as? String)
            ?: ""
    }

    private val childId = MutableStateFlow(parseChildId())

    fun setChildId(id: String) {
        childId.value = id
    }

    val uiState: StateFlow<ChildDashboardState> =
        childId
            // ✅ tiny win
            .flatMapLatest { id ->
                if (id.isBlank()) {
                    flowOf(ChildDashboardState(loading = false, error = "Missing child id"))
                } else {
                    combine(
                        childDao.observeById(id),
                        eventDao.observeActiveCount(),
                        attendanceDao.observeCountForChild(id),
                        attendanceDao.observeCountForChildByStatus(id, AttendanceStatus.PRESENT)
                    ) { child, eventsCount, attTotal, attPresent ->
                        ChildDashboardState(
                            loading = false,
                            error = null,
                            childId = id,
                            child = child,
                            eventsCount = eventsCount,
                            attendanceTotal = attTotal,
                            attendancePresent = attPresent,
                            lastUpdated = child?.updatedAt
                        )
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChildDashboardState(loading = true, childId = parseChildId())
            )
}

