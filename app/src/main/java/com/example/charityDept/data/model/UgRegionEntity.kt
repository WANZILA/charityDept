package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ug_regions")
data class UgRegionEntity(
    @PrimaryKey val regionCode: String,   // adm1_pcode
    val regionName: String                // adm1_name
)

