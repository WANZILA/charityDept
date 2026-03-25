package com.example.charityDept.data.local.entities

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

//@Keep
//@Entity(
//    tableName = "kpi_counters",
//    indices = [Index(value = ["name"], unique = true)]
//)
//data class KpiCounter(
//    @PrimaryKey val name: String,
//    val value: Long = 0
//)
@Keep
@Entity(
    tableName = "kpi_counters",
    indices = [Index(value = ["key"], unique = true)]
)
data class KpiCounter(
    @PrimaryKey val key: String,
    val count: Long = 0L
)
