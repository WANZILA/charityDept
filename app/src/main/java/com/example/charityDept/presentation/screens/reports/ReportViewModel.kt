package com.example.charityDept.presentation.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.MonthlyReport
import com.example.charityDept.data.model.QuarterlyReport
import com.example.charityDept.domain.repositories.online.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ReportUiState(
    val loading: Boolean = false,
    val monthly: MonthlyReport? = null,   // also used for custom range
    val quarterly: QuarterlyReport? = null,
    val error: String? = null
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val repo: ReportRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(ReportUiState())
    val ui: StateFlow<ReportUiState> = _ui

    fun load(
        monthYear: Pair<Int, Int> = currentYearMonth(),
        quarter: Pair<Int, Int> = currentYearQuarter()
    ) {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val (y, m) = monthYear
                val (qy, q) = quarter
                val monthly = repo.buildMonthlyReport(y, m)
                val quarterly = repo.buildQuarterlyReport(qy, q)
                _ui.value.copy(loading = false, monthly = monthly, quarterly = quarterly)
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message ?: "Failed to load reports") }
        }
    }

    fun loadCustom(startMillis: Long, endMillisExclusive: Long) {
        _ui.value = _ui.value.copy(loading = true, error = null)
        viewModelScope.launch {
            runCatching {
                val custom = repo.buildCustomReport(startMillis, endMillisExclusive)
                _ui.value.copy(loading = false, monthly = custom)
            }.onSuccess { _ui.value = it }
                .onFailure { _ui.value = _ui.value.copy(loading = false, error = it.message ?: "Failed to load custom report") }
        }
    }

    private fun currentYearMonth(): Pair<Int, Int> {
        val now = LocalDate.now()
        return now.year to now.monthValue
    }
    private fun currentYearQuarter(): Pair<Int, Int> {
        val now = LocalDate.now()
        val q = ((now.monthValue - 1) / 3) + 1
        return now.year to q
    }
}

