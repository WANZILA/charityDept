package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "assessment_taxonomy",
    indices = [
        Index("categoryKey"),
        Index("isActive"),
        Index("updatedAt"),
        Index("isDirty"),
        Index("isDeleted"),
        Index(value = ["categoryKey", "subCategoryKey"], unique = true)
    ]
)
data class AssessmentTaxonomy(
    @PrimaryKey val taxonomyId: String = "",

    // stable keys used by code (filters never break)
    val categoryKey: String = "",       // OBS | QA
    val categoryLabel: String = "",     // Observation | Questions

    val subCategoryKey: String = "",    // OBS_GENERAL | QA_HEALTH ...
    val subCategoryLabel: String = "",  // General | Health ...

    val indexNum: Int = 0,
    val isActive: Boolean = true,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val version: Long = 0L
)

