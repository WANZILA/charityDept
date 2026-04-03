package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.data.model.Gender
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot


private fun String?.toGender(): Gender =
    runCatching { if (this == null) Gender.MALE else Gender.valueOf(this) }
        .getOrDefault(Gender.MALE)

/* ======================= SAFE GETTERS ======================= */
private fun DocumentSnapshot.str(key: String): String? = getString(key)
private fun DocumentSnapshot.intPos(key: String): Int? = getLong(key)?.toInt()
private fun DocumentSnapshot.ts(key: String): Timestamp? = getTimestamp(key)

class FamilyMappers {

    fun Family.toFirestoreMapPatch(): Map<String, Any> = buildMap {
        fun putIfNotBlank(key: String, v: String?) {
            if (!v.isNullOrBlank()) put(key, v)
        }
        fun putIfNotNull(key: String, v: Any?) {
            if (v != null) put(key, v)
        }

        put("familyId", familyId)

        putIfNotBlank("caseReferenceNumber", caseReferenceNumber)
        putIfNotNull("dateOfAssessment", dateOfAssessment)

        putIfNotBlank("fName", fName)
        putIfNotBlank("lName", lName)
        putIfNotBlank("gender", gender.name)


        putIfNotBlank("occupationOrSchoolGrade", occupationOrSchoolGrade)

        putIfNotBlank("primaryContactHeadOfHousehold", primaryContactHeadOfHousehold)
        putIfNotBlank("addressLocation", addressLocation)

        put("isBornAgain", isBornAgain)

        putIfNotBlank("profileImg", profileImg)
        putIfNotBlank("profileImageStoragePath", profileImageStoragePath)
        putIfNotBlank("profileImageLocalPath", profileImageLocalPath)

        putIfNotBlank("personalPhone1", personalPhone1)
        putIfNotBlank("personalPhone2", personalPhone2)
        putIfNotBlank("ninNumber", ninNumber)

        putIfNotBlank("memberAncestralCountry", memberAncestralCountry)
        putIfNotBlank("memberAncestralRegion", memberAncestralRegion)
        putIfNotBlank("memberAncestralDistrict", memberAncestralDistrict)
        putIfNotBlank("memberAncestralCounty", memberAncestralCounty)
        putIfNotBlank("memberAncestralSubCounty", memberAncestralSubCounty)
        putIfNotBlank("memberAncestralParish", memberAncestralParish)
        putIfNotBlank("memberAncestralVillage", memberAncestralVillage)

        putIfNotBlank("memberRentalCountry", memberRentalCountry)
        putIfNotBlank("memberRentalRegion", memberRentalRegion)
        putIfNotBlank("memberRentalDistrict", memberRentalDistrict)
        putIfNotBlank("memberRentalCounty", memberRentalCounty)
        putIfNotBlank("memberRentalSubCounty", memberRentalSubCounty)
        putIfNotBlank("memberRentalParish", memberRentalParish)
        putIfNotBlank("memberRentalVillage", memberRentalVillage)

        if (isDeleted) {
            put("isDeleted", true)
            put("deletedAt", deletedAt ?: Timestamp.now())
        }

        put("createdAt", createdAt)
    }

    fun DocumentSnapshot.toFamilyOrNull(): Family? {
        if (data == null) return null

        val isDel = getBoolean("isDeleted") ?: false
        val delAt = if (isDel) ts("deletedAt") else null

        return Family(
            familyId = str("familyId") ?: id,
            caseReferenceNumber = str("caseReferenceNumber") ?: "",
            dateOfAssessment = ts("dateOfAssessment"),
            fName = str("fName") ?: "",
            lName = str("lName") ?: "",
            gender = (str("gender")).toGender(),
            occupationOrSchoolGrade = str("occupationOrSchoolGrade") ?: "",
            primaryContactHeadOfHousehold = str("primaryContactHeadOfHousehold") ?: "",
            addressLocation = str("addressLocation") ?: "",
            isBornAgain = getBoolean("isBornAgain") ?: false,

            profileImg = str("profileImg") ?: "",
            profileImageStoragePath = str("profileImageStoragePath") ?: "",
            profileImageLocalPath = str("profileImageLocalPath") ?: "",

            personalPhone1 = str("personalPhone1") ?: "",
            personalPhone2 = str("personalPhone2") ?: "",
            ninNumber = str("ninNumber") ?: "",

            memberAncestralCountry = str("memberAncestralCountry") ?: "",
            memberAncestralRegion = str("memberAncestralRegion") ?: "",
            memberAncestralDistrict = str("memberAncestralDistrict") ?: "",
            memberAncestralCounty = str("memberAncestralCounty") ?: "",
            memberAncestralSubCounty = str("memberAncestralSubCounty") ?: "",
            memberAncestralParish = str("memberAncestralParish") ?: "",
            memberAncestralVillage = str("memberAncestralVillage") ?: "",

            memberRentalCountry = str("memberRentalCountry") ?: "",
            memberRentalRegion = str("memberRentalRegion") ?: "",
            memberRentalDistrict = str("memberRentalDistrict") ?: "",
            memberRentalCounty = str("memberRentalCounty") ?: "",
            memberRentalSubCounty = str("memberRentalSubCounty") ?: "",
            memberRentalParish = str("memberRentalParish") ?: "",
            memberRentalVillage = str("memberRentalVillage") ?: "",

            createdAt = ts("createdAt") ?: Timestamp.now(),
            updatedAt = ts("updatedAt") ?: Timestamp.now(),
            isDirty = false,
            isDeleted = isDel,
            deletedAt = delAt,
            version = getLong("version") ?: 0L,
            enteredByUid = str("enteredByUid") ?: "",
            lastEditedByUid = str("lastEditedByUid") ?: "",
            purgeAt = ts("purgeAt")
        )
    }

    fun FamilyMember.toFirestoreMapPatch(): Map<String, Any> = buildMap {
        fun putIfNotBlank(key: String, v: String?) {
            if (!v.isNullOrBlank()) put(key, v)
        }

        put("familyMemberId", familyMemberId)
        putIfNotBlank("familyId", familyId)

        putIfNotBlank("fName", fName)
        putIfNotBlank("lName", lName)
        if (age > 0) put("age", age)

        putIfNotBlank("gender", gender)
        putIfNotBlank("relationship", relationship)
        putIfNotBlank("occupationOrSchoolGrade", occupationOrSchoolGrade)
        putIfNotBlank("healthOrDisabilityStatus", healthOrDisabilityStatus)

        putIfNotBlank("profileImg", profileImg)
        putIfNotBlank("profileImageStoragePath", profileImageStoragePath)
        putIfNotBlank("profileImageLocalPath", profileImageLocalPath)

        putIfNotBlank("personalPhone1", personalPhone1)
        putIfNotBlank("personalPhone2", personalPhone2)
        putIfNotBlank("ninNumber", ninNumber)

        if (isDeleted) {
            put("isDeleted", true)
            put("deletedAt", deletedAt ?: Timestamp.now())
        }

        put("createdAt", createdAt)
    }

    fun DocumentSnapshot.toFamilyMemberOrNull(): FamilyMember? {
        if (data == null) return null

        val isDel = getBoolean("isDeleted") ?: false
        val delAt = if (isDel) ts("deletedAt") else null

        return FamilyMember(
            familyMemberId = str("familyMemberId") ?: id,
            familyId = str("familyId") ?: "",

            fName = str("fName") ?: "",
            lName = str("lName") ?: "",
            age = intPos("age") ?: 0,

            gender = str("gender") ?: "",
            relationship = str("relationship") ?: "",
            occupationOrSchoolGrade = str("occupationOrSchoolGrade") ?: "",
            healthOrDisabilityStatus = str("healthOrDisabilityStatus") ?: "",

            profileImg = str("profileImg") ?: "",
            profileImageStoragePath = str("profileImageStoragePath") ?: "",
            profileImageLocalPath = str("profileImageLocalPath") ?: "",

            personalPhone1 = str("personalPhone1") ?: "",
            personalPhone2 = str("personalPhone2") ?: "",
            ninNumber = str("ninNumber") ?: "",

            createdAt = ts("createdAt") ?: Timestamp.now(),
            updatedAt = ts("updatedAt") ?: Timestamp.now(),
            isDirty = false,
            isDeleted = isDel,
            deletedAt = delAt,
            version = getLong("version") ?: 0L,
            enteredByUid = str("enteredByUid") ?: "",
            lastEditedByUid = str("lastEditedByUid") ?: "",
            purgeAt = ts("purgeAt")
        )
    }
}