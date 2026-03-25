package com.example.charityDept.presentation.screens.children

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.ClassGroup
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.Reply
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
/// ADDED (imports at top with the others)
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.charityDept.core.sync.ChildrenSyncScheduler
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import com.example.charityDept.feature.children.childrenPager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest

//import com.example.charityDept.data.local.projection.ChildRow

// --- add to your data class ---
data class ChildrenListUiState(
    val loading: Boolean = true,
    val children: List<Child> = emptyList(),
    val activeFilter: EducationPreference? = null,
    val activeFilterStreet: String? = null,
    val activeFilterRegion: String? = null,
    val activeFilterSponsored: Boolean? = null,
    val activeFilterGraduated: Reply? = null,
    val activeFilterClassGroup: ClassGroup? = null,
    val activeFilterAcceptedJesus: Reply? = null,
    val activeFilterResettled: Boolean? = null,
    val activeFilterDobVerified: Boolean? = null,
    val total: Int = 0,
    val filtered: Int = 0,
    val isOffline: Boolean = true, // Room = single source of truth
    val isSyncing: Boolean = false, // true when there are dirty rows pending push
    val error: String? = null,
    val lastRefreshed: Timestamp? = null
)

@HiltViewModel
class ChildrenListViewModel @Inject constructor(
    private val childrenRepo: OfflineChildrenRepository, // kept for future writes / helpers
    savedState: SavedStateHandle,
    private val childDao: ChildDao,
    @ApplicationContext val appContext: Context
) : ViewModel() {
    // Paging from Room
    // /// CHANGED: paging now follows the query (needle)

//    val paging: Flow<PagingData<Child>> =
//        childrenPager(childDao).cachedIn(viewModelScope)

    private val _ui = MutableStateFlow(ChildrenListUiState())
    val ui: StateFlow<ChildrenListUiState> = _ui.asStateFlow()

//    private val _query = MutableStateFlow("")
private val _query = MutableStateFlow("")
    fun onSearchQueryChange(q: String) { _query.value = q }

    @OptIn(ExperimentalCoroutinesApi::class)
    val paging: Flow<PagingData<Child>> =
        _query
            .map { it.trim().lowercase() }
            .distinctUntilChanged()
            .flatMapLatest { needle -> childrenPager(childDao, needle) }
            .cachedIn(viewModelScope)
//    fun onSearchQueryChange(q: String) {
//        _query.value = q
//    }

    // -------- filters from nav args (optional) --------
    private val filterPref: EducationPreference? = runCatching {
        savedState.get<String>("eduPref").orEmpty().takeIf { it.isNotBlank() }
            ?.let { EducationPreference.valueOf(it) }
    }.getOrNull()

    private val filterStreet: String? =
        savedState.get<String>("street")?.takeIf { it.isNotBlank() }?.decodeAndNorm()

    private val filterRegion: String? =
        savedState.get<String>("region")?.takeIf { it.isNotBlank() }?.decodeAndNorm()

    private val filterSponsored: Boolean? =
        savedState.get<String>("sponsored")?.toBoolOrNull()

    private val filterGraduated: Reply? = runCatching {
        savedState.get<String>("graduated").orEmpty().takeIf { it.isNotBlank() }
            ?.let { Reply.valueOf(it) }
    }.getOrNull()

    private val filterClassGroup: ClassGroup? = runCatching {
        savedState.get<String>("classGroup").orEmpty().takeIf { it.isNotBlank() }
            ?.let { ClassGroup.valueOf(it) }
    }.getOrNull()

    private val filterAcceptedJesus: Reply? = runCatching {
        savedState.get<String>("accepted").orEmpty().takeIf { it.isNotBlank() }
            ?.let { Reply.valueOf(it) }
    }.getOrNull()

    private val filterResettled: Boolean? =
        savedState.get<String>("resettled")?.toBoolOrNull()

    private val filterDobVerified: Boolean? =
        savedState.get<String>("dobVerified")?.toBoolOrNull()

    init {
        // Base Room flow: either all active or pre-filter by educationPreference from nav args
        val baseFlow: Flow<List<Child>> =
            if (filterPref != null) childDao.observeByEducationPreference(filterPref)
            else childDao.observeAllActive()

        // Pending writes count drives the "syncing" badge
        val dirtyCount: Flow<Int> = childDao.observeDirtyCount()

        viewModelScope.launch {
            combine(
                baseFlow,
                _query.map { it.trim().lowercase() }.distinctUntilChanged(),
                dirtyCount
            ) { base, needle, dirty ->
                // 1) start with Room list
                var list = base

                // 2) apply optional local filters
                filterStreet?.let { target -> list = list.filter { (it.street).norm() == target } }
                filterRegion?.let { target -> list = list.filter { (it.region).norm() == target } }
                filterSponsored?.let { want ->
                    list = list.filter { it.partnershipForEducation == want }
                }
                filterGraduated?.let { want -> list = list.filter { it.graduated == want } }
                filterClassGroup?.let { want -> list = list.filter { it.classGroup == want } }
                filterAcceptedJesus?.let { want -> list = list.filter { it.acceptedJesus == want } }
                filterResettled?.let { want -> list = list.filter { it.resettled == want } }
                filterDobVerified?.let { want -> list = list.filter { it.dobVerified == want } }

                val totalCount = base.size

                // 3) client search by full name
                val searched = if (needle.isEmpty()) list else {
                    list.filter { c ->
                        val full = "${c.fName} ${c.lName}".trim().lowercase()
                        full.contains(needle)
                    }
                }

                // 4) sort by updatedAt desc, then createdAt desc
                val sorted = searched.sortedWith(
                    compareByDescending<Child> { it.updatedAt }
                        .thenByDescending { it.createdAt }
                )

                ChildrenListUiState(
                    loading = false,
                    children = sorted,
                    activeFilter = filterPref,
                    activeFilterStreet = filterStreet?.displayCase(),
                    activeFilterRegion = filterRegion?.displayCase(),
                    activeFilterSponsored = filterSponsored,
                    activeFilterGraduated = filterGraduated,
                    activeFilterClassGroup = filterClassGroup,
                    activeFilterAcceptedJesus = filterAcceptedJesus,
                    activeFilterResettled = filterResettled,
                    activeFilterDobVerified = filterDobVerified,
                    total = totalCount,
                    filtered = sorted.size,
                    isOffline = true,
                    isSyncing = dirty > 0,
                    error = null,
                    lastRefreshed = Timestamp.now()
                )
            }
                .onStart {
                    _ui.value = _ui.value.copy(
                        loading = true,
                        activeFilter = filterPref,
                        activeFilterStreet = filterStreet?.displayCase(),
                        activeFilterRegion = filterRegion?.displayCase(),
                        activeFilterSponsored = filterSponsored,
                        activeFilterGraduated = filterGraduated,
                        activeFilterClassGroup = filterClassGroup,
                        activeFilterAcceptedJesus = filterAcceptedJesus,
                        activeFilterResettled = filterResettled,
                        activeFilterDobVerified = filterDobVerified,
                        total = 0,
                        filtered = 0
                    )
                }
                .catch { e ->
                    _ui.value = _ui.value.copy(
                        loading = false,
                        error = e.message ?: "Failed to load children (Room)"
                    )
                }
                .collect { state -> _ui.value = state }
        }
    }

    /** Manual refresh: UX spinner + timestamp; data already live from Room flows. */
    fun refresh() {
        viewModelScope.launch {
            try {
                ChildrenSyncScheduler.enqueuePullNow(appContext)
                _ui.value = _ui.value.copy(loading = true)
                // Optional: quick health ping from DAO if you want to ensure DB is reachable
                // val _ = childDao.peekLocalCount()
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = e.message)
            } finally {
                _ui.value = _ui.value.copy(loading = false, lastRefreshed = Timestamp.now())
            }
        }
    }

    // ------------- helpers -------------
    private fun String?.norm(): String = (this ?: "")
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()

    private fun String.decodeAndNorm(): String = this
        .replace("%20", " ")
        .norm()

    private fun String?.displayCase(): String? =
        this?.split(' ')
            ?.filter { it.isNotBlank() }
            ?.joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }

    private fun String.toBoolOrNull(): Boolean? =
        when (this.lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
}

//data class ChildrenListUiState(
//    val loading: Boolean = true,
//    val children: List<Child> = emptyList(),
//    val activeFilter: EducationPreference? = null,
//    val activeFilterStreet: String? = null,
//    val activeFilterRegion: String? = null,
//    val activeFilterSponsored: Boolean? = null,
//    val activeFilterGraduated: Reply? = null,
//    val activeFilterClassGroup: ClassGroup? = null,
//    val activeFilterAcceptedJesus: Reply? = null,
//    val activeFilterResettled: Boolean? = null,
//    val activeFilterDobVerified: Boolean? = null,
//
//    // 👇 NEW
//    val total: Int = 0,      // total before search/filter
//    val filtered: Int = 0,   // count after applying filters + search
//
//    val isOffline: Boolean = false,
//    val isSyncing: Boolean = false,
//    val error: String? = null,
//    val lastRefreshed: Timestamp? = null
//)

//@HiltViewModel
//class ChildrenListViewModel @Inject constructor(
////    private val childrenRepo: ChildrenRepository,
//    private val childrenRepo: OfflineChildrenRepository,
//    savedState: SavedStateHandle,
//    private val childDao: ChildDao
//) : ViewModel()
//{
//    val paging: Flow<PagingData<Child>> =
//        childrenPager(childDao).cachedIn(viewModelScope)
//    /// ADDED — Room Paging flow (read-only, cached in VM scope)
//
//    /// ADDED — ensure paging from Room is available (keep yours if already present)
////    val paging: Flow<PagingData<ChildRow>> = childrenRepo
////        .pagingChildren()
////        .cachedIn(viewModelScope)
//
////    val paging: Flow<PagingData<ChildRow>> = childrenRepo
////        .pagingChildren()
////        .cachedIn(viewModelScope)
//
//
//    private val _ui = MutableStateFlow(ChildrenListUiState())
//    val ui: StateFlow<ChildrenListUiState> = _ui.asStateFlow()
//
//    private val _query = MutableStateFlow("")
//    fun onSearchQueryChange(q: String) { _query.value = q }
//
//    // latest snapshot for flags and fast local filtering
//    private var latestSnap: ChildrenSnapshot =
//        ChildrenSnapshot(emptyList(), fromCache = true, hasPendingWrites = false)
//
//    // -------- read optional filters from route query args --------
//    // children_list?eduPref=SCHOOL|SKILLING|NONE&street=...&region=...
////    private val filterPref: EducationPreference? = runCatching {
////        savedState.get<String>("eduPref").orEmpty().takeIf { it.isNotBlank() }
////            ?.let { EducationPreference.valueOf(it) }
////    }.getOrNull()
//
//
//    private val filterPref: EducationPreference? = runCatching {
//        savedState.get<String>("eduPref").orEmpty().takeIf { it.isNotBlank() }
//            ?.let { EducationPreference.valueOf(it) }
//    }.getOrNull()
//
//    private val filterStreet: String? =
//        savedState.get<String>("street")?.takeIf { it.isNotBlank() }?.decodeAndNorm()
//
//    private val filterRegion: String? =
//        savedState.get<String>("region")?.takeIf { it.isNotBlank() }?.decodeAndNorm()
//
//    // children_list?...&sponsored=true|false&graduated=YES|NO&classGroup=SERGEANT|...&
//    //                accepted=YES|NO&resettled=true|false&dobVerified=true|false
//    private val filterSponsored: Boolean? =
//        savedState.get<String>("sponsored")?.toBoolOrNull()
//
//    private val filterGraduated: Reply? = runCatching {
//        savedState.get<String>("graduated").orEmpty().takeIf { it.isNotBlank() }
//            ?.let { Reply.valueOf(it) }
//    }.getOrNull()
//
//    private val filterClassGroup: ClassGroup? = runCatching {
//        savedState.get<String>("classGroup").orEmpty().takeIf { it.isNotBlank() }
//            ?.let { ClassGroup.valueOf(it) }
//    }.getOrNull()
//
//    private val filterAcceptedJesus: Reply? = runCatching {
//        savedState.get<String>("accepted").orEmpty().takeIf { it.isNotBlank() }
//            ?.let { Reply.valueOf(it) }
//    }.getOrNull()
//
//    private val filterResettled: Boolean? =
//        savedState.get<String>("resettled")?.toBoolOrNull()
//
//    private val filterDobVerified: Boolean? =
//        savedState.get<String>("dobVerified")?.toBoolOrNull()
//
//    private val hasAnyFilter: Boolean =
//        listOf<Any?>(
//            filterPref, filterStreet, filterRegion, filterSponsored,
//            filterGraduated, filterClassGroup, filterAcceptedJesus,
//            filterResettled, filterDobVerified
//        ).any { it != null }
//
////    init {
////        childrenRepo.triggerSimpleDeltaIn()
////
////        viewModelScope.launch {
////            childrenRepo.triggerDeltaInNow()
////            kotlinx.coroutines.delay(2000)
////            val cnt = childrenRepo.peekLocalCount()
////            timber.log.Timber.i("ChildrenListVM: Room children count after DeltaIn = %d", cnt)
////        }
////    }
////    init {
////        viewModelScope.launch {
////            kotlinx.coroutines.delay(1500)
////            val cnt = try { childrenRepo.peekLocalCount() } catch (_: Throwable) { -1 }
////            timber.log.Timber.d("Children(Room) count after DeltaIn = %d", cnt)
////        }
////    }
////    init {
//////        viewModelScope.launch {
//////            // server page is generous; UI still pages from Room at 50
//////            childrenRepo.triggerDeltaInNow(pageSize = 200, maxPages = 50)
//////            // Give the worker a moment on cold start; totally optional
//////            childrenRepo.verifyParityNow()
//////        }
////        /// ADDED — inside ChildrenListViewModel.init, after triggerDeltaInNow()
////        viewModelScope.launch {
////            kotlinx.coroutines.delay(1500) // small delay for first page
////            val localCount = childrenRepo.peekLocalCount() // see below
////            timber.log.Timber.d("Local children count after pull = $localCount")
////        }
////
////        // server stream (eduPref if present) + local filters + search + sort
////        viewModelScope.launch {
//////            val sourceFlow =
//////                if (filterPref == null) childrenRepo.streamAllNotGraduated()
//////                else childrenRepo.streamByEducationPreferenceResilient(filterPref)
////            val sourceFlow =
////                when {
////                    !hasAnyFilter -> childrenRepo.streamAllNotGraduated() // ← show ALL children when no filters passed
////                    filterPref != null -> childrenRepo.streamByEducationPreferenceResilient(filterPref)
////                    else -> childrenRepo.streamAllNotGraduated()
////                }
////            combine(
////                sourceFlow,
////                _query.map { it.trim().lowercase() }.distinctUntilChanged()
////            ) { snap, needle ->
////                latestSnap = snap
////
////                // 1) start with snapshot children
////                var base = snap.children
////
////
////                // 2) apply local filters (normalized where needed)
////                filterStreet?.let { target ->
////                    base = base.filter { (it.street).norm() == target }
////                }
////                filterRegion?.let { target ->
////                    base = base.filter { (it.region).norm() == target }
////                }
////                filterSponsored?.let { want ->
////                    base = base.filter { it.sponsoredForEducation == want }
////                }
////                filterGraduated?.let { want ->
////                    base = base.filter { it.graduated == want }
////                }
////                filterClassGroup?.let { want ->
////                    base = base.filter { it.classGroup == want }
////                }
////                filterAcceptedJesus?.let { want ->
////                    base = base.filter { it.acceptedJesus == want }
////                }
////                filterResettled?.let { want ->
////                    base = base.filter { it.resettled == want }
////                }
////                filterDobVerified?.let { want ->
////                    base = base.filter { it.dobVerified == want }
////                }
////
////                // 3) UI search (full name)
////                val searched = if (needle.isEmpty()) base else {
////                    base.filter { c ->
////                        val full = "${c.fName} ${c.lName}".trim().lowercase()
////                        full.contains(needle)
////                    }
////                }
////
////                // 4) sort: keep your existing (updatedAt desc, then createdAt desc)
////                val sorted = searched.sortedWith(
////                    compareByDescending<Child> { it.updatedAt }
////                        .thenByDescending { it.createdAt }
////                )
////
////
////                ChildrenListUiState(
////                    loading = false,
////                    children = sorted,
////                    activeFilter = filterPref,
////                    activeFilterStreet = filterStreet?.displayCase(),
////                    activeFilterRegion = filterRegion?.displayCase(),
////                    activeFilterSponsored = filterSponsored,
////                    activeFilterGraduated = filterGraduated,
////                    activeFilterClassGroup = filterClassGroup,
////                    activeFilterAcceptedJesus = filterAcceptedJesus,
////                    activeFilterResettled = filterResettled,
////                    activeFilterDobVerified = filterDobVerified,
////                    total = snap.children.size,
////                    filtered = sorted.size,
////                    isOffline = snap.fromCache,
////                    isSyncing = snap.hasPendingWrites,
////                    error = null,
////                    lastRefreshed = Timestamp.now()
////                )
////            }
////                .onStart {
////                    _ui.value = _ui.value.copy(
////                        loading = true,
////                        activeFilter = filterPref,
////                        activeFilterStreet = filterStreet?.displayCase(),
////                        activeFilterRegion = filterRegion?.displayCase(),
////                        activeFilterSponsored = filterSponsored,
////                        activeFilterGraduated = filterGraduated,
////                        activeFilterClassGroup = filterClassGroup,
////                        activeFilterAcceptedJesus = filterAcceptedJesus,
////                        activeFilterResettled = filterResettled,
////                        activeFilterDobVerified = filterDobVerified,
////                        total = 0,
////                        filtered = 0
////                    )
////                }
////                .catch { e ->
////                    _ui.value = _ui.value.copy(
////                        loading = false,
////                        error = e.message ?: "Failed to load children"
////                    )
////                }
////                .collect { state -> _ui.value = state }
////        }
////    }
//
//    /** Optional: prime cache via a one-shot fetch, then the stream will update. */
//    fun refresh() {
//        viewModelScope.launch {
//            try {
//                _ui.value = _ui.value.copy(loading = true)
////                if (filterPref == null) {
////                    childrenRepo.getAllNotGraduated()
////                } else {
////                    childrenRepo.getByEducationPreference(filterPref)
////                }
////                if (!hasAnyFilter) {
////                    childrenRepo.getAll()
////                } else if (filterPref != null) {
////                    childrenRepo.getByEducationPreference(filterPref)
////                } else {
////                    childrenRepo.streamAllNotGraduated()
////                }
//                if (!hasAnyFilter) {
//                                       childrenRepo.getAll()
//                                   } else if (filterPref != null) {
//                                       childrenRepo.getByEducationPreference(filterPref)
//                                   } else {
//                                       childrenRepo.getAllNotGraduated()   // /// CHANGED: call suspend function, not Flow
//                                   }
//            } catch (e: Exception) {
//                _ui.value = _ui.value.copy(error = e.message)
//            } finally {
//                _ui.value = _ui.value.copy(loading = false, lastRefreshed = Timestamp.now())
//            }
//        }
//    }
//
//    // ------------- helpers -------------
//    private fun String?.norm(): String = (this ?: "")
//        .trim()
//        .replace(Regex("\\s+"), " ")
//        .lowercase()
//
//    private fun String.decodeAndNorm(): String = this
//        .replace("%20", " ")
//        .norm()
//
//    private fun String?.displayCase(): String? =
//        this?.split(' ')
//            ?.filter { it.isNotBlank() }
//            ?.joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
//
//    private fun String.toBoolOrNull(): Boolean? =
//        when (this.lowercase()) {
//            "true", "1", "yes" -> true
//            "false", "0", "no" -> false
//            else -> null
//        }
//}

