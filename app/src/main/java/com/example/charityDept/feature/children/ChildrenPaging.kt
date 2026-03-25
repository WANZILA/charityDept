package com.example.charityDept.feature.children


import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.model.Child
//import com.example.charityDept.data.model.ChildDao
import kotlinx.coroutines.flow.Flow

// page size tuned for smooth 10k+ rows; placeholders off to avoid extra binding work
private val defaultConfig = PagingConfig(pageSize = 50, prefetchDistance = 50, enablePlaceholders = false)

fun childrenPager(dao: ChildDao, needle: String): Flow<PagingData<Child>> =
    Pager(config = defaultConfig) { dao.pagingSearch(needle) }.flow



