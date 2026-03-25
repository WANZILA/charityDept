package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ug_districts",
    indices = [Index("regionCode")]
)
data class UgDistrictEntity(
    @PrimaryKey val districtCode: String,   // adm2_pcode
    val districtName: String,               // adm2_name
    val regionCode: String,
//    val regionName: String,// adm1_pcode
)

