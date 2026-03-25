// <app/src/main/java/com/example/zionkids/data/local/dao/KpiDao.kt>
// /// CHANGED: Remove unused KpiCounter import.
// /// CHANGED: Implement inc(...) using ensureKey(...) + add(...); remove undefined insertIfAbsent/addDelta calls.

package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
// import com.example.charityDept.data.local.entities.KpiCounter   // /// CHANGED: unused, remove
import kotlinx.coroutines.flow.Flow

@Dao
interface KpiDao {
    @Query("INSERT OR IGNORE INTO kpi_counters(`key`, count) VALUES(:key, 0)")
    suspend fun ensureKey(key: String)

    @Query("UPDATE kpi_counters SET count = count + :delta WHERE `key` = :key")
    suspend fun add(key: String, delta: Long)

    // Observe a KPI live (for dashboards)
    @Query("SELECT count FROM kpi_counters WHERE `key` = :key")
    fun observe(key: String): Flow<Long>

    // Synchronous read (for boot/hydrate)
    @Query("SELECT count FROM kpi_counters WHERE `key` = :key")
    suspend fun get(key: String): Long?

    @Transaction
    suspend fun bump(key: String, delta: Long) {
        ensureKey(key)
        add(key, delta)
    }

    // 🔑 The method your repo calls
    @Transaction
    suspend fun inc(key: String, delta: Long) {           // /// CHANGED: use same param name for consistency
        ensureKey(key)                                    // /// CHANGED
        add(key, delta)                                   // /// CHANGED
    }
}

//package com.example.charityDept.data.local.dao
//
//
//import androidx.room.Dao
//import androidx.room.Query
//import androidx.room.Transaction
//import com.example.charityDept.data.local.entities.KpiCounter
//import kotlinx.coroutines.flow.Flow
//
//@Dao
//interface KpiDao {
//    @Query("INSERT OR IGNORE INTO kpi_counters(`key`, count) VALUES(:key, 0)")
//    suspend fun ensureKey(key: String)
//
//    @Query("UPDATE kpi_counters SET count = count + :delta WHERE `key` = :key")
//    suspend fun add(key: String, delta: Long)
//
//    // Observe a KPI live (for dashboards)
//    @Query("SELECT count FROM kpi_counters WHERE `key` = :key")
//    fun observe(key: String): Flow<Long>
//
//    // Synchronous read (for boot/hydrate)
//    @Query("SELECT count FROM kpi_counters WHERE `key` = :key")
//    suspend fun get(key: String): Long?
//
//    @Transaction
//    suspend fun bump(key: String, delta: Long) {
//        ensureKey(key)
//        add(key, delta)
//    }
//
//    // 🔑 The method your repo calls
//    @Transaction
//    suspend fun inc(name: String, delta: Long) {
//        // ensure the row exists
//        insertIfAbsent(KpiCounter(name = name, value = 0))
//        // apply delta
//        addDelta(name, delta)
//    }
//}

