// <app/src/main/java/com/example/zionkids/feature/children/EventsPager.kt>
// /// CHANGED: Keep simple pager; add safe fallback when needle is blank/short.
// /// CHANGED: Escape %/_ to avoid accidental wildcards; wrap with %needle%.
// /// CHANGED: File name cased to EventsPager.kt (optional), but not required by Kotlin.

package com.example.charityDept.feature.children

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.model.Event
import kotlinx.coroutines.flow.Flow

// page size tuned for smooth 10k+ rows; placeholders off to avoid extra binding work
private val defaultConfig = PagingConfig(
    pageSize = 50,
    prefetchDistance = 50,
    enablePlaceholders = false
)

// /// CHANGED: If needle is blank or too short, use pagingActive(); else use pagingSearch() with wildcard.
fun eventsPager(dao: EventDao, needle: String): Flow<PagingData<Event>> =
    Pager(config = defaultConfig) {
        val q = needle.trim()
        if (q.length < 2) {
            dao.pagingActive()
        } else {
            // escape %, _ to avoid pattern injection; use backslash which our DAO query supports via ESCAPE '\'
            val escaped = q
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            dao.pagingSearch("%$escaped%")
        }
    }.flow

//package com.example.charityDept.feature.children
//
////class eventsPager {
////}
//
////package com.example.charityDept.feature.children
//
//
//import androidx.paging.Pager
//import androidx.paging.PagingConfig
//import androidx.paging.PagingData
//import com.example.charityDept.data.local.dao.ChildDao
//import com.example.charityDept.data.local.dao.EventDao
//import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.Event
////import com.example.charityDept.data.model.ChildDao
//import kotlinx.coroutines.flow.Flow
//
//// page size tuned for smooth 10k+ rows; placeholders off to avoid extra binding work
//private val defaultConfig = PagingConfig(pageSize = 50, prefetchDistance = 50, enablePlaceholders = false)
//
//fun eventsPager(dao: EventDao, needle: String): Flow<PagingData<Event>> =
//    Pager(config = defaultConfig) { dao.pagingSearch(needle) }.flow
//


