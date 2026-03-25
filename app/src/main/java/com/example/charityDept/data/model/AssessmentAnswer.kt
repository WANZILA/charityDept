package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "assessment_answers",
    indices = [
        Index("childId"),
        Index("generalId"),
        Index("questionId"),
        Index("category"),
        Index("subCategory"),
        Index("enteredByUid"),
        Index("updatedAt"),
        Index("isDirty"),
        Index("isDeleted"),
        Index(value = ["childId", "generalId", "questionId"], unique = true)
    ]
)
data class AssessmentAnswer(
    @PrimaryKey val answerId: String = "",

    val generalId: String = "",  // session UUID
    val childId: String = "",
    val questionId: String = "",

    // snapshots (you asked for these)
    val category: String = "",
    val subCategory: String = "",
    val questionSnapshot: String? = null,

    // minister inputs
    val answer: String = "",
    val score: Int = 0,  // 1..10
    val notes: String = "",

    // audit
    val enteredByUid: String = "",
    val lastEditedByUid: String = "",

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val version: Long = 0L
)

