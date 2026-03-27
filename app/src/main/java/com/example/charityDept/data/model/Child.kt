package com.example.charityDept.data.model

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Keep
@Entity(
    tableName = "children",
    indices = [
        Index("isDeleted"),
        Index("graduated"),
        Index("partnershipForEducation"),
        Index("resettled"),
        Index("registrationStatus"),
        Index("educationPreference"),
        Index("region"),
        Index("street"),
        Index("createdAt"),
        Index("updatedAt"),
        Index("isDirty"),
//        Index(value = ["graduated"]),
//        Index(value = ["registrationStatus"]),
//        Index(value = ["updatedAt"]),
//        // /// CHANGED: support delta queries & sync scans
//        Index(value = ["isDirty"]),
//        Index(value = ["isDeleted"]),
//        Index(value = ["version"]),
//        // /// CHANGED: common compound filter for remote delta + tombstones
//        Index(value = ["isDeleted", "updatedAt"])
        // Add these back only if you include the columns:
        // Index(value = ["backgroundUpdatedAt"]),
        // Index(value = ["educationUpdatedAt"]),
        // Index(value = ["familyUpdatedAt"]),
        // Index(value = ["spiritualUpdatedAt"])
    ]
)
data class Child(
    @PrimaryKey val childId: String = "",

    // ===== Basic Info =====
    val profileImg: String = "",

    val fName: String = "",
    val lName: String = "",
    val oName: String = "",

    val age: Int = 0,
    val ninNumber: String = "",
    val childType: ChildType = ChildType.FAMILY,
    val program: Program = Program.CHILDREN_OF_ZION,

    val dob: Timestamp? = null,
    val dobVerified: Boolean = false,
    val gender: Gender = Gender.MALE,

    val street: String = "",
    val personalPhone1: String = "",
    val personalPhone2: String = "",



    val invitedBy: Individual = Individual.UNCLE,
    val invitedByIndividualId: String = "",
    val invitedByTypeOther: String = "",

    val educationPreference: EducationPreference = EducationPreference.NONE,

    // ===== Background Info =====
    val leftHomeDate: Timestamp? = null,
    val reasonLeftHome: String = "",
    val leaveStreetDate: Timestamp? = null,

    // ===== Education Info =====
    val educationLevel: EducationLevel = EducationLevel.NONE,
    val lastClass: String = "",
    val previousSchool: String = "",
    val reasonLeftSchool: String = "",
    val formerSponsor: Relationship = Relationship.NONE,
    val formerSponsorOther: String ="",
    val technicalSkills: String = "",

    // ===== Family Resettlement =====
    val resettlementPreference: ResettlementPreference = ResettlementPreference.DIRECT_HOME,
    val resettlementPreferenceOther: String = "",
    val resettled: Boolean = false,
    val resettlementDate: Timestamp? = null,
    val country: Country = Country.UGANDA,

    val region: String = "",
    val district: String = "",
    val county: String = "",
    val subCounty: String = "",
    val parish: String = "",
    val village: String = "",

    // ===== Family Members 1 =====
    val memberFName1: String = "",
    val memberLName1: String = "",
    val relationship1: Relationship = Relationship.NONE,
    val telephone1a: String = "",
    val telephone1b: String = "",
    // ===== Family Member 1 — Ancestral Home =====
    val member1AncestralCountry: Country = Country.UGANDA,

    val member1AncestralRegion: String = "",
    val member1AncestralDistrict: String = "",
    val member1AncestralCounty: String = "",
    val member1AncestralSubCounty: String = "",
    val member1AncestralParish: String = "",
    val member1AncestralVillage: String = "",

// ===== Family Member 1 — Rental Home =====
    val member1RentalCountry: Country = Country.UGANDA,
    val member1RentalRegion: String = "",
    val member1RentalDistrict: String = "",
    val member1RentalCounty: String = "",
    val member1RentalSubCounty: String = "",
    val member1RentalParish: String = "",
    val member1RentalVillage: String = "",


    // ===== Family Members 2 =====
    val memberFName2: String = "",
    val memberLName2: String = "",
    val relationship2: Relationship = Relationship.NONE,
    val telephone2a: String = "",
    val telephone2b: String = "",
    // ===== Family Member 2 — Ancestral Home =====
    val member2AncestralCountry: Country = Country.UGANDA,

    val member2AncestralRegion: String = "",
    val member2AncestralDistrict: String = "",
    val member2AncestralCounty: String = "",
    val member2AncestralSubCounty: String = "",
    val member2AncestralParish: String = "",
    val member2AncestralVillage: String = "",

// ===== Family Member 2 — Rental Home =====
    val member2RentalCountry: Country = Country.UGANDA,

    val member2RentalRegion: String = "",
    val member2RentalDistrict: String = "",
    val member2RentalCounty: String = "",
    val member2RentalSubCounty: String = "",
    val member2RentalParish: String = "",
    val member2RentalVillage: String = "",



    // ===== Family Members 3 =====
    val memberFName3: String = "",
    val memberLName3: String = "",
    val relationship3: Relationship = Relationship.NONE,
    val telephone3a: String = "",
    val telephone3b: String = "",
    // ===== Family Member 3 — Ancestral Home =====
    val member3AncestralCountry: Country = Country.UGANDA,

    val member3AncestralRegion: String = "",
    val member3AncestralDistrict: String = "",
    val member3AncestralCounty: String = "",
    val member3AncestralSubCounty: String = "",
    val member3AncestralParish: String = "",
    val member3AncestralVillage: String = "",

// ===== Family Member 3 — Rental Home =====
    val member3RentalCountry: Country = Country.UGANDA,

    val member3RentalRegion: String = "",
    val member3RentalDistrict: String = "",
    val member3RentalCounty: String = "",
    val member3RentalSubCounty: String = "",
    val member3RentalParish: String = "",
    val member3RentalVillage: String = "",


    // ===== Spiritual Info =====
    val acceptedJesus: Reply = Reply.NO,
    val confessedBy: ConfessedBy = ConfessedBy.NONE,
    val ministryName: String = "",
    val acceptedJesusDate: Timestamp? = null,
    val whoPrayed: Individual = Individual.UNCLE,
    val whoPrayedOther: String = "",
    val whoPrayedId: String = "",
    val outcome: String = "",
    val generalComments: String = "",
    val classGroup: ClassGroup = ClassGroup.SERGEANT,

    // ===== Program statuses =====
    val registrationStatus: RegistrationStatus = RegistrationStatus.BASICINFOR,
//    when a child has completed a skilling or school and they are working
    val graduated: Reply = Reply.NO,

    // ===== Sponsorship/Family flags =====
    val partnershipForEducation: Boolean = false,
    val partnerId: String = "",
    val partnerFName: String = "",
    val partnerLName: String = "",
    val partnerTelephone1: String = "",
    val partnerTelephone2: String = "",
    val partnerEmail: String = "",
    val partnerNotes: String = "",

    // ===== Audit =====
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),

    // Mark local edits for push; prefer batch pushes of dirty rows.
    val isDirty: Boolean = false,
    // Soft-deletes (tombstones) so Room remains source of truth and we can sync deletions.
    val isDeleted: Boolean = false,
    // Tombstone timestamp: when we deleted this record (used for cleanup after retention)
    val deletedAt: Timestamp? = null,

    // Cheap conflict resolution: prefer higher version (server increments), else newer updatedAt.
    val version: Long = 0L
) {
    fun fullName(): String =
        listOf(fName, oName, lName).map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

    fun hasPhone(): Boolean =
        listOf(personalPhone1, personalPhone2, telephone1a, telephone1b, telephone2a, telephone2b, telephone3a, telephone3b)
            .any { it.trim().isNotEmpty() }

    fun addressLine(): String =
        listOf(village, parish, subCounty, county, district, region)
            .map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")

    fun registrationProgress(): Int = when (registrationStatus) {
        RegistrationStatus.BASICINFOR -> 1
        RegistrationStatus.BACKGROUND -> 2
        RegistrationStatus.EDUCATION -> 3
        RegistrationStatus.FAMILY -> 4
        RegistrationStatus.SPONSORSHIP -> 5
        RegistrationStatus.SPIRITUAL -> 6
        RegistrationStatus.COMPLETE -> 7
    }
}

enum class Individual { UNCLE, AUNTY, CHILD, OTHER }
enum class EducationPreference { SCHOOL, SKILLING, NONE }
enum class ResettlementPreference { DIRECT_HOME, TEMPORARY_HOME, OTHER }
enum class Reply { YES, NO }
enum class Relationship { NONE, PARENT, UNCLE, AUNTY, OTHER }


enum class ChildType {
    STREET,
    INMATES,
    FAMILY,
    HOSPITAL
}

enum class Program {
    CHILDREN_OF_ZION,
    BROTHERS_AND_SISTERS_OF_ZION
}

enum class ClassGroup {
    SERGEANT,
    LIEUTENANT,
    CAPTAIN,
    GENERAL,
    BROTHERS_AND_SISTERS_OF_ZION,
}

enum class Gender { MALE, FEMALE }
enum class RegistrationStatus { BASICINFOR, BACKGROUND, EDUCATION, FAMILY,SPONSORSHIP, SPIRITUAL, COMPLETE }

enum class  ConfessedBy{ NONE,PHANEROO, OTHER}

enum class EducationLevel { NONE, NURSERY,PRIMARY, SECONDARY}

enum class Country {
    UGANDA,
    KENYA,
    TANZANIA,
    RWANDA,
    SUDAN,
    BURUNDI
}

