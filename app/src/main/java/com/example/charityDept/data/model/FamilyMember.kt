package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "family_members",
    foreignKeys = [
        ForeignKey(
            entity = Family::class,
            parentColumns = ["familyId"],
            childColumns = ["familyId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [
        Index(value = ["familyId"]),
        Index(value = ["fName"]),
        Index(value = ["lName"]),
        Index(value = ["relationship"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isDirty"]),
        Index(value = ["isDeleted"]),
        Index(value = ["familyId", "isDeleted"])
    ]
)
data class FamilyMember(
    @PrimaryKey val familyMemberId: String = "",

    val familyId: String = "",

    val fName: String = "",
    val lName: String = "",
    val age: Int = 0,

    val gender: String = "",
    val relationship: String = "",
    val occupationOrSchoolGrade: String = "",
    val healthOrDisabilityStatus: String = "",

    val profileImg: String = "",
    val profileImageStoragePath: String = "",
    val profileImageLocalPath: String = "",

    val personalPhone1: String = "",
    val personalPhone2: String = "",
    val ninNumber: String = "",

    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isDirty: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Timestamp? = null,
    val version: Long = 0L,
    val enteredByUid: String = "",
    val lastEditedByUid: String = "",
    val purgeAt: Timestamp? = null
)