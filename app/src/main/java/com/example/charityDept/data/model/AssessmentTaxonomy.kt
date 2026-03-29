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
        Index("assessmentKey"),
        Index("categoryKey"),
        Index("subCategoryKey"),
        Index("isActive"),
        Index("updatedAt"),
        Index("isDirty"),
        Index("isDeleted"),
        Index(value = ["assessmentKey", "categoryKey", "subCategoryKey"], unique = true)
    ]
)
data class AssessmentTaxonomy(
    @PrimaryKey val taxonomyId: String = "",

    // main assessment family
    val assessmentKey: String = "",      // FAMILY_NEEDS | SKILLING | SOCIAL_WORK
    val assessmentLabel: String = "",    // Family Needs Assessment | Skilling Assessment | Social Work Assessment

    // keeps the existing app split
    val categoryKey: String = "",        // OBS | QA
    val categoryLabel: String = "",      // Observation | Questions

    // section inside that split
    val subCategoryKey: String = "",     // e.g. SW_HEALTH | FN_HOUSING | SK_APTITUDE
    val subCategoryLabel: String = "",   // e.g. Health | Housing & Environment | Interest & Aptitude Profile

    val indexNum: Int = 0,
    val isActive: Boolean = true,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val version: Long = 0L
)
//package com.example.charityDept.data.model
//
//import androidx.annotation.Keep
//import androidx.room.Entity
//import androidx.room.Index
//import androidx.room.PrimaryKey
//import com.google.firebase.Timestamp
//
//@Keep
//@Entity(
//    tableName = "assessment_taxonomy",
//    indices = [
//        Index("categoryKey"),
//        Index("isActive"),
//        Index("updatedAt"),
//        Index("isDirty"),
//        Index("isDeleted"),
//        Index(value = ["categoryKey", "subCategoryKey"], unique = true)
//    ]
//)
//data class AssessmentTaxonomy(
//    @PrimaryKey val taxonomyId: String = "",
//
//    // stable keys used by code (filters never break)
//    val categoryKey: String = "",       // OBS | QA
//    val categoryLabel: String = "",     // Observation | Questions
//
//    val subCategoryKey: String = "",    // OBS_GENERAL | QA_HEALTH ...
//    val subCategoryLabel: String = "",  // General | Health ...
//
//    val indexNum: Int = 0,
//    val isActive: Boolean = true,
//
//    val createdAt: Timestamp = Timestamp.now(),
//    val updatedAt: Timestamp = Timestamp.now(),
//    val isDirty: Boolean = false,
//    val isDeleted: Boolean = false,
//    val deletedAt: Timestamp? = null,
//    val version: Long = 0L
//)
//
