package com.example.charityDept.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "ug_villages",
    indices = [Index("parishCode")] // CHANGED
)

data class UgVillageEntity(
    @PrimaryKey val villageCode: String,   // adm4_pcode
    val villageName: String,               // adm4_name
    val parishCode: String,          // adm3_pcode
)
