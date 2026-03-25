package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "ug_parishes",
    indices = [Index("subCountyCode")] // CHANGED
)

data class UgParishEntity(
    @PrimaryKey val parishCode: String,   // adm4_pcode
    val parishName: String,               // adm4_name
    val subCountyCode: String,          // adm3_pcode
)
