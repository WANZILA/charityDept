package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "families",
    indices = [
        Index(value = ["caseReferenceNumber"]),
        Index(value = ["fName"]),
        Index(value = ["lName"]),
        Index(value = ["gender"]),
        Index(value = ["primaryContactHeadOfHousehold"]),
        Index(value = ["dateOfAssessment"]),
        Index(value = ["updatedAt"]),
        Index(value = ["isDirty"]),
        Index(value = ["isDeleted"]),
        Index(value = ["isDeleted", "updatedAt"])
    ]
)
data class Family(
    @PrimaryKey val familyId: String = "",

    val caseReferenceNumber: String = "",
    val dateOfAssessment: Timestamp? = null,
    val fName: String = "",
    val lName: String = "",
    val primaryContactHeadOfHousehold: String = "",
    val addressLocation: String = "",

    val gender: Gender = Gender.MALE,
    val occupationOrSchoolGrade: String = "",
    val isBornAgain: Boolean = false,

    val profileImg: String = "",
    val profileImageStoragePath: String = "",
    val profileImageLocalPath: String = "",

    val personalPhone1: String = "",
    val personalPhone2: String = "",
    val ninNumber: String = "",

    val memberAncestralCountry: String = "",
    val memberAncestralRegion: String = "",
    val memberAncestralDistrict: String = "",
    val memberAncestralCounty: String = "",
    val memberAncestralSubCounty: String = "",
    val memberAncestralParish: String = "",
    val memberAncestralVillage: String = "",

    val memberRentalCountry: String = "",
    val memberRentalRegion: String = "",
    val memberRentalDistrict: String = "",
    val memberRentalCounty: String = "",
    val memberRentalSubCounty: String = "",
    val memberRentalParish: String = "",
    val memberRentalVillage: String = "",

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