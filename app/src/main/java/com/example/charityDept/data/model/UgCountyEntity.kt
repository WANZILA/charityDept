package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ug_counties",
    indices = [Index("districtCode")]
)
data class UgCountyEntity(
    @PrimaryKey val countyCode: String,   // adm3_pcode
    val countyName: String,               // adm3_name
    val districtCode: String,
//    val districtName: String,// adm2_pcode
//    val constituencyCodeRaw: String,
)

