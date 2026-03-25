package com.example.charityDept.presentation.screens.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.ClassGroup
import com.example.charityDept.data.model.EducationPreference
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

data class SyncHealth(
    val localCount: Int = 0,
    val dirtyCount: Int = 0
)

data class ChildrenSummaryUi(
    val loading: Boolean = false,
    val error: String? = null,
    val total: Int = 0,
    val newThisMonth: Int = 0,
    val graduated: Int = 0,
    val sponsored: Int = 0,
    val reunited: Int = 0,
    val inProgram: Int = 0,
    val avgAge: Double = 0.0,

    // computed only when open
    val eduDist: Map<EducationPreference, Int> = emptyMap(),
    val classDist: Map<ClassGroup, Int> = emptyMap(),

    val regionTop: List<Pair<String, Int>> = emptyList(),
    val streetTop: List<Pair<String, Int>> = emptyList(),
    val staleUpdates: Int = 0
)

@HiltViewModel
class ChildrenDashboardViewModel @Inject constructor(
    private val childDao: ChildDao
) : ViewModel() {

    private val eduExpanded = MutableStateFlow(false)
    private val classExpanded = MutableStateFlow(false)

    fun setEduExpanded(v: Boolean) { eduExpanded.value = v }
    fun setClassExpanded(v: Boolean) { classExpanded.value = v }

    // Health banner: only totals + dirty count
    val health: StateFlow<SyncHealth> = combine(
        childDao.observeAllActive().map { it.size },
        childDao.observeDirtyCount()
    ) { total, dirty ->
        SyncHealth(localCount = total, dirtyCount = dirty)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SyncHealth())

    // Main UI: compute maps ONLY when their accordion is open
    val ui: StateFlow<ChildrenSummaryUi> = combine(
        childDao.observeAllActive(),
        eduExpanded,
        classExpanded
    ) { list, eduOpen, classOpen ->
        compute(list, eduOpen, classOpen)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ChildrenSummaryUi(loading = false))

    // -------- helpers --------

    private fun compute(
        all: List<Child>,
        eduOpen: Boolean,
        classOpen: Boolean
    ): ChildrenSummaryUi {
//        val nowMs = System.currentTimeMillis()
//        val (monthStart, monthEnd) = monthBounds(nowMs)
//        val staleCutoff = nowMs - THIRTY_DAYS

        val total = 0
        val newThisMonth = 0
//        val total = all.size
//
//        val newThisMonth = all.count { c ->
//            val created = c.createdAt.toMillis()
//            created in monthStart..monthEnd
//        }
        val graduated = 0
        val sponsored = 0
        val reunited = 0
        val staleUpdates = 0
        val ages = 1
        val avgAge = 1.0
        val inProgram = 1
//        val graduated = all.count { it.graduated == Reply.YES }
//        val sponsored = all.count { it.partnershipForEducation }
//        val reunited  = all.count { it.resettled }
//        val inProgram = all.count { it.registrationStatus != RegistrationStatus.COMPLETE }
//
//        val staleUpdates = all.count { c -> c.updatedAt.toMillis() < staleCutoff }
//
//        val ages = all.mapNotNull { a -> a.age.takeIf { it > 0 } }
//        val avgAge = if (ages.isNotEmpty()) ages.average() else 0.0

        // ✅ Only compute these when expanded (saves work when collapsed)
        val eduDist: Map<EducationPreference, Int> =
            if (eduOpen) all.groupingBy { it.educationPreference }.eachCount() else emptyMap()

        val classDist: Map<ClassGroup, Int> =
            if (classOpen) all.groupingBy { it.classGroup }.eachCount() else emptyMap()

        val regionTop = topN(all.mapNotNull { it.region.normalizeOrNull() }, n = 3)
        val streetTop = topN(all.mapNotNull { it.street.normalizeOrNull() }, n = 3)

        return ChildrenSummaryUi(
            loading = false,
            error = null,
            total = total,
            newThisMonth = newThisMonth,
            graduated = graduated,
            sponsored = sponsored,
            reunited = reunited,
            inProgram = inProgram,
            avgAge = "%.1f".format(Locale.getDefault(), avgAge).toDoubleOrNull() ?: 0.0,
            eduDist = eduDist,
            classDist = classDist,
            regionTop = regionTop,
            streetTop = streetTop,
            staleUpdates = staleUpdates
        )
    }

    private fun monthBounds(nowMs: Long): Pair<Long, Long> {
        val c = Calendar.getInstance().apply { timeInMillis = nowMs }
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        val start = c.timeInMillis
        c.add(Calendar.MONTH, 1)
        c.add(Calendar.MILLISECOND, -1)
        return start to c.timeInMillis
    }

    private fun Timestamp?.toMillis(): Long = this?.toDate()?.time ?: 0L

    private fun String?.normalizeOrNull(): String? {
        val t = this?.trim()?.replace(Regex("\\s+"), " ") ?: return null
        if (t.isEmpty()) return null
        return t.lowercase(Locale.getDefault())
            .split(' ')
            .joinToString(" ") { w -> w.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }
    }

    private fun topN(names: List<String>, n: Int): List<Pair<String, Int>> =
        names.groupingBy { it }
            .eachCount()
            .toList()
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
            .take(n)

    companion object {
        private const val THIRTY_DAYS: Long = 30L * 24 * 60 * 60 * 1000L
    }
}

