package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "assessment_questions",
    indices = [
        Index("category"),
        Index("subCategory"),
        Index("isActive"),
        Index("updatedAt"),
        Index("isDirty"),
        Index("isDeleted")
    ]
)
data class AssessmentQuestion(
    @PrimaryKey val questionId: String = "",
    val category: String = "",      // Observation | Question | Spiritual
    val subCategory: String = "",
    val categoryKey: String = "",     // OBS | QA
    val subCategoryKey: String = "",  // OBS_GENERAL | QA_HEALTH ...

    val question: String = "",
    val isActive: Boolean = true,
    val indexNum: Int = 0,

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val version: Long = 0L
)

