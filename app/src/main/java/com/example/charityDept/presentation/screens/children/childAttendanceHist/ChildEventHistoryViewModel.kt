package com.example.charityDept.presentation.screens.children.childAttendanceHist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.projection.EventAttendanceRow
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*

enum class ChildEventMode { ALL, ATTENDED, MISSED }

data class ChildEventHistoryUi(
    val loading: Boolean = true,
    val error: String? = null,
    val childId: String = "",
    val mode: ChildEventMode = ChildEventMode.ALL,
    val items: List<EventAttendanceRow> = emptyList()
)

@HiltViewModel
class ChildEventHistoryViewModel @Inject constructor(
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
    private val mode = MutableStateFlow(ChildEventMode.ALL)

    fun setChildId(id: String) { childId.value = id }
    fun setMode(m: ChildEventMode) { mode.value = m }

    val ui: StateFlow<ChildEventHistoryUi> =
        combine(childId, mode) { cid, m -> cid to m }
            .distinctUntilChanged()
            .flatMapLatest { (cid, m) ->
                if (cid.isBlank()) {
                    flowOf(ChildEventHistoryUi(loading = false, error = "Missing child id"))
                } else {
                    val source: Flow<List<EventAttendanceRow>> = when (m) {
                        // ✅ includes notes, and only shows ABSENT if notes is not blank (per your rule)
                        ChildEventMode.ALL -> attendanceDao.observeAllRecordedEventsForChild(cid)

                        // ✅ attended list (PRESENT)
                        ChildEventMode.ATTENDED -> attendanceDao.observeAttendedEventsForChild(cid)

                        // ✅ missed-with-reason list (ABSENT + notes not blank)
                        ChildEventMode.MISSED -> attendanceDao.observeMissedEventsForChildWithReason(cid)
                    }

                    source
                        .map { list ->
                            ChildEventHistoryUi(
                                loading = false,
                                error = null,
                                childId = cid,
                                mode = m,
                                items = list
                            )
                        }
                        .onStart {
                            emit(
                                ChildEventHistoryUi(
                                    loading = true,
                                    childId = cid,
                                    mode = m
                                )
                            )
                        }
                        .catch { e ->
                            emit(
                                ChildEventHistoryUi(
                                    loading = false,
                                    error = e.message ?: "Failed to load events",
                                    childId = cid,
                                    mode = m
                                )
                            )
                        }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ChildEventHistoryUi(loading = true, childId = parseChildId())
            )
}

