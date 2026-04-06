package com.example.charityDept.core.utils.picker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PickerFeature(
    scope: CoroutineScope,
    // a flow that emits ALL options (e.g., from a repo stream)
    optionsFlow: Flow<List<PickerOption>>,
) {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _options = MutableStateFlow<List<PickerOption>>(emptyList())
    val options: StateFlow<List<PickerOption>> = _options.asStateFlow()

    // Public filtered list based on query (all filtering here)
    val filtered: StateFlow<List<PickerOption>> =
        combine(_options, _query) { options, q ->
            val tokens = q.trim().lowercase().split(" ").filter { it.isNotBlank() }
            if (tokens.isEmpty()) options
            else options.filter { opt ->
                val hay = (opt.name + " " + opt.id).lowercase()
                tokens.all { hay.contains(it) }
            }
        }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        scope.launch {
            optionsFlow.collect { list ->
                _options.value = list
            }
        }
    }

    fun updateQuery(q: String) { _query.value = q }
    fun clearQuery() { _query.value = "" }
}

