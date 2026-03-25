package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ug_subcounties",
    indices = [Index("countyCode")]
)
data class UgSubCountyEntity(
    @PrimaryKey val subCountyCode: String,   // adm4_pcode
    val subCountyName: String,               // adm4_name
    val countyCode: String,          // adm3_pcode
)

