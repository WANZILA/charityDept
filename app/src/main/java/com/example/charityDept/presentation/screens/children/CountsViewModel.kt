package com.example.charityDept.presentation.screens.children

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.data.local.dao.KeyCount
//import com.example.charityDept.data.model.KeyCount
import com.example.charityDept.domain.repositories.offline.OfflineChildrenRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CountMode { STREETS, DISTRICTS }

data class CountsUi(
    val loading: Boolean = true,
    val items: List<Pair<String, Int>> = emptyList(), // [name, count]
    val totalChildren: Int = 0,
    val uniqueKeys: Int = 0,
    val error: String? = null
)

@HiltViewModel
class CountsViewModel @Inject constructor(
    private val repo: OfflineChildrenRepository,
    savedState: SavedStateHandle
) : ViewModel() {

    val title: String get() = when (mode) {
        CountMode.STREETS -> "Streets"
        CountMode.DISTRICTS -> "Districts"
    }



    private val mode = runCatching {
        CountMode.valueOf(savedState.get<String>("mode") ?: "STREETS")
    }.getOrDefault(CountMode.STREETS)

    private val _ui = MutableStateFlow(CountsUi())
    val ui: StateFlow<CountsUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val countsFlow: Flow<List<KeyCount>> = when (mode) {
                CountMode.STREETS -> repo.watchStreetCounts()
                CountMode.DISTRICTS -> repo.watchMember1AncestralDistrictCounts()
            }

            combine(
                countsFlow,
                repo.watchTotalChildren()
            ) { counts, total ->
                val items = counts.map { it.name to it.count }
                CountsUi(
                    loading = false,
                    items = items,
                    totalChildren = total,
                    uniqueKeys = items.size,
                    error = null
                )
            }
                .onStart { _ui.value = CountsUi(loading = true) }
                .catch { e -> _ui.value = _ui.value.copy(loading = false, error = e.message) }
                .collect { next -> _ui.value = next }
        }
    }
}

