package com.example.charityDept.presentation.screens.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.AssessmentAnswerDao
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.dao.EventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AdminDashboardUi(
    val isLoading: Boolean = false,
    val error: String? = null,

    val isEmpty: Boolean = false,

    val childrenTotal: Int = 0,
    val eventsTotal: Int = 0,
    val attendanceTotal: Int = 0,

    val questionsTotal: Int = 0,
    val questionsActive: Int = 0,

    val assessmentAnswersTotal: Int = 0,
    val assessmentSessionsTotal: Int = 0, // distinct generalId (non-deleted)

    val dirtyTotal: Int = 0,
    val deletedPendingTotal: Int = 0
)

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val childDao: ChildDao,
    private val eventDao: EventDao,
    private val attendanceDao: AttendanceDao,
    private val questionDao: AssessmentQuestionDao,
    private val answerDao: AssessmentAnswerDao
) : ViewModel() {

    private val _ui = MutableStateFlow(AdminDashboardUi(isLoading = true))
    val ui: StateFlow<AdminDashboardUi> = _ui.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _ui.value = _ui.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                combine(
                    childDao.streamAllAdmin(),               // Flow<List<Child>>
                    eventDao.streamAllAdmin(),               // Flow<List<Event>>
                    attendanceDao.streamAllAdmin(),          // Flow<List<Attendance>>
                    questionDao.observeAllAdmin(),           // Flow<List<AssessmentQuestion>>
                    answerDao.streamAllAdmin()               // Flow<List<AssessmentAnswer>>
                ) { children, events, attendance, questions, answers ->

                    val childrenTotal = children.count { !it.isDeleted }
                    val eventsTotal = events.count { !it.isDeleted }
                    val attendanceTotal = attendance.count { !it.isDeleted }

                    val qTotal = questions.count { !it.isDeleted }
                    val qActive = questions.count { !it.isDeleted && it.isActive }

                    val aTotal = answers.count { !it.isDeleted }
                    val sessionCount = answers
                        .asSequence()
                        .filter { !it.isDeleted }
                        .map { it.generalId }
                        .distinct()
                        .count()

                    val dirty = children.count { it.isDirty } +
                            events.count { it.isDirty } +
                            attendance.count { it.isDirty } +
                            questions.count { it.isDirty } +
                            answers.count { it.isDirty }

                    val deletedPending = children.count { it.isDeleted && it.isDirty } +
                            events.count { it.isDeleted && it.isDirty } +
                            attendance.count { it.isDeleted && it.isDirty } +
                            questions.count { it.isDeleted && it.isDirty } +
                            answers.count { it.isDeleted && it.isDirty }

                    AdminDashboardUi(
                        isLoading = false,
                        error = null,
                        isEmpty = childrenTotal == 0 && eventsTotal == 0 && qTotal == 0,

                        childrenTotal = childrenTotal,
                        eventsTotal = eventsTotal,
                        attendanceTotal = attendanceTotal,

                        questionsTotal = qTotal,
                        questionsActive = qActive,

                        assessmentAnswersTotal = aTotal,
                        assessmentSessionsTotal = sessionCount,

                        dirtyTotal = dirty,
                        deletedPendingTotal = deletedPending
                    )
                }.collect { computed ->
                    _ui.value = computed
                }
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    isLoading = false,
                    error = e.message ?: "Something went wrong"
                )
            }
        }
    }
}

