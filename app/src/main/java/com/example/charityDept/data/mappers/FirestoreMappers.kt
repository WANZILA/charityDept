package com.example.charityDept.data.mappers

import com.example.charityDept.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.Timestamp
import kotlin.String

// ---------- Enum converters with safe defaults (no reified) ----------
//private fun String?.toReply(): Reply =
//    runCatching { if (this == null) Reply.NO else Reply.valueOf(this) }
//        .getOrDefault(Reply.NO)

//private fun String?.toIndividual(): Individual =
//    runCatching { if (this == null) Individual.UNCLE else Individual.valueOf(this) }
//        .getOrDefault(Individual.UNCLE)

//private fun String?.toEducationPref(): EducationPreference =
//    runCatching { if (this == null) EducationPreference.NONE else EducationPreference.valueOf(this) }
//        .getOrDefault(EducationPreference.NONE)

//private fun String?.toRelationship(): Relationship =
//    runCatching { if (this == null) Relationship.NONE else Relationship.valueOf(this) }
//        .getOrDefault(Relationship.NONE)

//private fun String?.toRegistrationStatus(): RegistrationStatus =
//    runCatching { if (this == null) RegistrationStatus.BASICINFOR else RegistrationStatus.valueOf(this) }
//        .getOrDefault(RegistrationStatus.BASICINFOR)

private fun String?.toEventStatus(): EventStatus =
    runCatching { if (this == null) EventStatus.SCHEDULED else EventStatus.valueOf(this) }
        .getOrDefault(EventStatus.SCHEDULED)

//private fun String?.toClassGroup(): ClassGroup =
//    runCatching { if (this == null) ClassGroup.SERGEANT else ClassGroup.valueOf(this) }
//        .getOrDefault(ClassGroup.SERGEANT)
private fun String?.toChildType(): ChildType =
    runCatching { if (this == null) ChildType.FAMILY else ChildType.valueOf(this) }
        .getOrDefault(ChildType.FAMILY)

private fun String?.toProgram(): Program =
    runCatching { if (this == null) Program.CHILDREN_OF_ZION else Program.valueOf(this) }
        .getOrDefault(Program.CHILDREN_OF_ZION)

private fun String?.toClassGroup(): ClassGroup =
    runCatching { if (this == null) ClassGroup.SERGEANT else ClassGroup.valueOf(this) }
        .getOrDefault(ClassGroup.SERGEANT)

private fun String?.toGender(): Gender =
    runCatching { if (this == null) Gender.MALE else Gender.valueOf(this) }
        .getOrDefault(Gender.MALE)


// ---------- Safe getters ----------
//private fun DocumentSnapshot.str(key: String): String? = this.getString(key)
//private fun DocumentSnapshot.lng(key: String): Long?   = this.getLong(key)
//private fun DocumentSnapshot.intPos(key: String): Int? = this.getLong(key)?.toInt()


/* ======================= SAFE GETTERS ======================= */
private fun DocumentSnapshot.str(key: String): String? = getString(key)
private fun DocumentSnapshot.intPos(key: String): Int? = getLong(key)?.toInt()
private fun DocumentSnapshot.ts(key: String): Timestamp? = getTimestamp(key)

/* ======================= ENUM PARSERS (safe defaults) ======================= */
private fun String?.toReply(): Reply =
    runCatching { if (this == null) Reply.NO else Reply.valueOf(this) }.getOrDefault(Reply.NO)

private fun String?.toIndividual(): Individual =
    runCatching { if (this == null) Individual.UNCLE else Individual.valueOf(this) }.getOrDefault(Individual.UNCLE)

private fun String?.toEducationPref(): EducationPreference =
    runCatching { if (this == null) EducationPreference.NONE else EducationPreference.valueOf(this) }
        .getOrDefault(EducationPreference.NONE)
private fun String?.toEducationLevel(): EducationLevel =
    runCatching { if (this == null) EducationLevel.NONE else EducationLevel.valueOf(this) }
        .getOrDefault(EducationLevel.NONE)

private fun String?.toRelationship(): Relationship =
    runCatching { if (this == null) Relationship.NONE else Relationship.valueOf(this) }
        .getOrDefault(Relationship.NONE)

private fun String?.toConfessedBy(): ConfessedBy =
    runCatching { if (this == null) ConfessedBy.NONE else ConfessedBy.valueOf(this) }
        .getOrDefault(ConfessedBy.NONE)

private fun String?.toRegistrationStatus(): RegistrationStatus =
    runCatching { if (this == null) RegistrationStatus.BASICINFOR else RegistrationStatus.valueOf(this) }
        .getOrDefault(RegistrationStatus.BASICINFOR)

fun QuerySnapshot.toEvents(): List<Event> =
    this.documents.mapNotNull { doc ->
        val e = doc.toObject(Event::class.java)?.copy(eventId = doc.id) ?: return@mapNotNull null
        val status = (doc.getString("eventStatus")).toEventStatus()
        e.copy(eventStatus = status)
    }

private fun String?.toCountry(): Country =
    runCatching { if (this == null) Country.UGANDA else Country.valueOf(this) }
        .getOrDefault(Country.UGANDA)




/* ======================= DOC -> CHILD ======================= */
fun DocumentSnapshot.toChildOrNull(): Child? {

    if (data == null) return null
    val isDel = getBoolean("isDeleted") ?: false
    val delAt = if (isDel) (ts("deletedAt")) else null


    return Child(
        // ===== IDs =====
        childId = str("childId") ?: id,

        // ===== Basic Info =====
        // ===== Basic Info =====
        profileImg = str("profileImg") ?: "",
        profileImageStoragePath = str("profileImageStoragePath") ?: "",
        profileImageLocalPath = str("profileImageLocalPath") ?: "",
        profileImageUpdatedAt = ts("profileImageUpdatedAt"),


        fName = str("fName") ?: "",
        lName = str("lName") ?: "",
        oName = str("oName") ?: "",
        gender = (str("gender")).toGender(),

        age = intPos("age") ?: 0,
        ninNumber = str("ninNumber") ?: "",
        childType = (str("childType")).toChildType(),
        program = when {
            (intPos("age") ?: 0) >= 17 -> Program.BROTHERS_AND_SISTERS_OF_ZION
            else -> (str("program")).toProgram()
        },

        dob = ts("dob"),
        dobVerified = getBoolean("dobVerified") ?: false,

        street = str("street") ?: "",
        personalPhone1 = str("personalPhone1") ?: "",
        personalPhone2 = str("personalPhone2") ?: "",

        invitedBy = (str("invitedBy")).toIndividual(),
        invitedByIndividualId = str("invitedByIndividualId") ?: "",
        invitedByTypeOther = str("invitedByTypeOther") ?: "",

        educationPreference = (str("educationPreference")).toEducationPref(),
        technicalSkills = str("technicalSkills") ?: "",

        // ===== Background Info =====
        leftHomeDate = ts("leftHomeDate"),
        reasonLeftHome = str("reasonLeftHome") ?: "",
        leaveStreetDate = ts("leaveStreetDate") ?: ts("leftStreetDate"),

        // ===== Education Info =====
        educationLevel = (str("educationLevel")).toEducationLevel(),
        lastClass = str("lastClass") ?: "",
        previousSchool = str("previousSchool") ?: "",
        reasonLeftSchool = str("reasonLeftSchool") ?: "",
        formerSponsor  = (str("formerSponsor")).toRelationship(),
        formerSponsorOther = str("formerSponsorOther") ?: "",

        // ===== Family Resettlement =====
        resettlementPreference = run {
            val v = str("resettlementPreference")
            try { if (v != null) ResettlementPreference.valueOf(v) else ResettlementPreference.DIRECT_HOME }
            catch (_: IllegalArgumentException) { ResettlementPreference.DIRECT_HOME }
        },
        resettlementPreferenceOther = str("resettlementPreferenceOther") ?: "",
        resettled = getBoolean("resettled") ?: false,
        resettlementDate = ts("resettlementDate"),
        country = (str("country")).toCountry(),

        region = str("region") ?: "",
        district = str("district") ?: "",
        county = str("county") ?: "",
        subCounty = str("subCounty") ?: (str("sCounty") ?: ""),
        parish = str("parish") ?: "",
        village = str("village") ?: "",

        // ===== Family Members 1 =====
        memberFName1 = str("memberFName1") ?: "",
        memberLName1 = str("memberLName1") ?: "",
        relationship1 = (str("relationShip1") ?: str("relationship1")).toRelationship(),
        telephone1a = str("telephone1a") ?: "",
        telephone1b = str("telephone1b") ?: "",
        // ===== Family Member 1 — Ancestral Home =====
        member1AncestralCountry = (str("member1AncestralCountry")).toCountry(),
        member1AncestralRegion = str("member1AncestralRegion") ?: "",
        member1AncestralDistrict = str("member1AncestralDistrict") ?: "",
        member1AncestralCounty = str("member1AncestralCounty") ?: "",
        member1AncestralSubCounty = str("member1AncestralSubCounty") ?: "",
        member1AncestralParish = str("member1AncestralParish") ?: "",
        member1AncestralVillage = str("member1AncestralVillage") ?: "",

// ===== Family Member 1 — Rental Home =====
        member1RentalCountry = (str("member1RentalCountry")).toCountry(),
        member1RentalRegion = str("member1RentalRegion") ?: "",
        member1RentalDistrict = str("member1RentalDistrict") ?: "",
        member1RentalCounty = str("member1RentalCounty") ?: "",
        member1RentalSubCounty = str("member1RentalSubCounty") ?: "",
        member1RentalParish = str("member1RentalParish") ?: "",
        member1RentalVillage = str("member1RentalVillage") ?: "",


        // ===== Family Members 2 =====
        memberFName2 = str("memberFName2") ?: "",
        memberLName2 = str("memberLName2") ?: "",
        relationship2 = (str("relationShip2") ?: str("relationship2")).toRelationship(),
        telephone2a = str("telephone2a") ?: "",
        telephone2b = str("telephone2b") ?: "",

        // ===== Family Member 2 — Ancestral Home =====
        member2AncestralCountry = (str("member2AncestralCountry")).toCountry(),
        member2AncestralRegion = str("member2AncestralRegion") ?: "",
        member2AncestralDistrict = str("member2AncestralDistrict") ?: "",
        member2AncestralCounty = str("member2AncestralCounty") ?: "",
        member2AncestralSubCounty = str("member2AncestralSubCounty") ?: "",
        member2AncestralParish = str("member2AncestralParish") ?: "",
        member2AncestralVillage = str("member2AncestralVillage") ?: "",

// ===== Family Member 2 — Rental Home =====
        member2RentalCountry = (str("member2RentalCountry")).toCountry(),
        member2RentalRegion = str("member2RentalRegion") ?: "",
        member2RentalDistrict = str("member2RentalDistrict") ?: "",
        member2RentalCounty = str("member2RentalCounty") ?: "",
        member2RentalSubCounty = str("member2RentalSubCounty") ?: "",
        member2RentalParish = str("member2RentalParish") ?: "",
        member2RentalVillage = str("member2RentalVillage") ?: "",

        // ===== Family Members 3 =====
        memberFName3 = str("memberFName3") ?: "",
        memberLName3 = str("memberLName3") ?: "",
        relationship3 = (str("relationShip3") ?: str("relationship3")).toRelationship(),
        telephone3a = str("telephone3a") ?: "",
        telephone3b = str("telephone3b") ?: "",
        // ===== Family Member 3 — Ancestral Home =====
        member3AncestralCountry = (str("member3AncestralCountry")).toCountry(),
        member3AncestralRegion = str("member3AncestralRegion") ?: "",
        member3AncestralDistrict = str("member3AncestralDistrict") ?: "",
        member3AncestralCounty = str("member3AncestralCounty") ?: "",
        member3AncestralSubCounty = str("member3AncestralSubCounty") ?: "",
        member3AncestralParish = str("member3AncestralParish") ?: "",
        member3AncestralVillage = str("member3AncestralVillage") ?: "",

// ===== Family Member 3 — Rental Home =====
        member3RentalCountry = (str("member3RentalCountry")).toCountry(),
        member3RentalRegion = str("member3RentalRegion") ?: "",
        member3RentalDistrict = str("member3RentalDistrict") ?: "",
        member3RentalCounty = str("member3RentalCounty") ?: "",
        member3RentalSubCounty = str("member3RentalSubCounty") ?: "",
        member3RentalParish = str("member3RentalParish") ?: "",
        member3RentalVillage = str("member3RentalVillage") ?: "",


        // ===== Spiritual Info =====
        acceptedJesus = (str("acceptedJesus")).toReply(),
        confessedBy  = (str("confessedBy")).toConfessedBy(),
        ministryName = str("ministryName") ?: "",
        acceptedJesusDate = ts("acceptedJesusDate"),
        whoPrayed = (str("whoPrayed")).toIndividual(),
        whoPrayedOther = str("whoPrayedOther") ?: "",
        whoPrayedId = str("whoPrayedId") ?: "",
        classGroup = when (val age = intPos("age") ?: 0) {
            in 0..5 -> ClassGroup.SERGEANT
            in 6..9 -> ClassGroup.LIEUTENANT
            in 10..12 -> ClassGroup.CAPTAIN
            in 13..16 -> ClassGroup.GENERAL
            in 17..25 -> ClassGroup.BROTHERS_AND_SISTERS_OF_ZION
            else -> (str("classGroup")).toClassGroup()
        },
        outcome = str("outcome") ?: "",
        generalComments = str("generalComments") ?: "",

        // ===== Program statuses =====
        registrationStatus = (str("registrationStatus")).toRegistrationStatus(),
        graduated = (str("graduated")).toReply(),

        // ===== Sponsorship / Flags =====
        partnershipForEducation = getBoolean("sponsoredForEducation") ?: false,
        partnerId = str("sponsorId") ?: "",
        partnerFName = str("sponsorFName") ?: "",
        partnerLName = str("sponsorLName") ?: "",
        partnerTelephone1 = str("partnerTelephone1") ?: "",
        partnerTelephone2 = str("partnerTelephone2") ?: "",
        partnerEmail = str("sponsorEmail") ?: "",
        partnerNotes = str("sponsorNotes") ?: "",

        // ===== Audit =====
        createdAt = ts("createdAt") ?: Timestamp.now(),
        updatedAt = ts("updatedAt") ?: Timestamp.now(),
        // tombstone
        isDeleted = isDel,
        deletedAt = delAt,
      //  isDeleted = getBoolean("isDeleted") ?: false,
        //deletedAt = ts("deletedAt"),
        version   = getLong("version") ?: 0L
    )
}

/* ======================= SNAPSHOT -> LIST ======================= */
fun QuerySnapshot.toChildren(): List<Child> = documents.mapNotNull { it.toChildOrNull() }

/* ======================= CHILD -> FIRESTORE (FULL) ======================= */
/** Full map for create/replace. Writes all fields; nullable fields are skipped. */

/** Build a PATCH map (only non-null / non-blank fields) for merge updates. */
fun Event.toFirestoreMapPatch(): Map<String, Any> = buildMap {
    fun putIfNotBlank(key: String, v: String?) {
        if (!v.isNullOrBlank()) put(key, v)
    }

    // ===== Basic Info =====
    putIfNotBlank("eventId", eventId)
    putIfNotBlank("eventParentId", eventParentId)
    putIfNotBlank("title", title)
    put("eventDate", eventDate)
    put("isChild", isChild)
    // Team / contact
    putIfNotBlank("teamName", teamName)
    putIfNotBlank("teamLeaderNames", teamLeaderNames)
    putIfNotBlank("leaderTelephone1", leaderTelephone1)
    putIfNotBlank("leaderTelephone2", leaderTelephone2)
    putIfNotBlank("leaderEmail", leaderEmail)

    // Logistics
    putIfNotBlank("location", location)
    put("eventStatus", eventStatus.name)
    putIfNotBlank("notes", notes)

    // Admin / sync flags
    putIfNotBlank("adminId", adminId)
    put("isDeleted", isDeleted)
    put("version", version)

    // Timestamps
    // createdAt should exist (best set once; but we at least include it)
    put("createdAt", createdAt)
    put("updatedAt", Timestamp.now())

    // Tombstone timestamp only when deleted
    if (isDeleted) {
        put("deletedAt", deletedAt ?: Timestamp.now())
    } else {
        // optional: explicitly clear deletedAt on undelete if you support undelete
        // put("deletedAt", null) // NOTE: null handling depends on your SetOptions/merge behavior
    }
}

fun DocumentSnapshot.toEventOrNull(): Event? {
    if (data == null) return null
    fun ts(key: String): Timestamp? = getTimestamp(key)

    val statusRaw = getString("eventStatus") ?: getString("status")
    val status = runCatching {
        if (statusRaw.isNullOrBlank()) EventStatus.SCHEDULED else EventStatus.valueOf(statusRaw)
    }.getOrDefault(EventStatus.SCHEDULED)

    val isDel = getBoolean("isDeleted") ?: false
    val delAt = if (isDel) ts("deletedAt") else null

    // ✅ If updatedAt is missing, this doc is unsafe for delta sync ordering.
    // Return null so it doesn't poison Room/cursor (and you’ll see it in failedIds logs).
    val updatedAt = ts("updatedAt") ?: return null

    return Event(
        eventId = getString("eventId") ?: id,
        eventParentId = getString("eventParentId") ?: "",
        title = getString("title") ?: "",

        // keep as-is OR make strict too
        eventDate = ts("eventDate") ?: Timestamp.now(),

        teamName = getString("teamName") ?: "",
        teamLeaderNames = getString("teamLeaderNames") ?: "",
        leaderTelephone1 = getString("leaderTelephone1") ?: "",
        leaderTelephone2 = getString("leaderTelephone2") ?: "",
        leaderEmail = getString("leaderEmail") ?: "",
        location = getString("location") ?: "",
//        isChild = getBoolean("isChild") ?: true,
        isChild = getBoolean("isChild") ?: false,
        eventStatus = status,
        notes = getString("notes") ?: "",
        adminId = getString("adminId") ?: "",

        // ✅ createdAt: if missing, keep something stable (could be updatedAt)
        createdAt = ts("createdAt") ?: updatedAt,
        updatedAt = updatedAt,

        isDeleted = isDel,
        deletedAt = delAt,

        isDirty = false,
        version = getLong("version") ?: 0L
    )
}

/** Build a PATCH map (only non-null / non-blank fields) for merge updates. */
fun Child.toFirestoreMapPatch(): Map<String, Any> = buildMap {
    fun putIfNotBlank(key: String, v: String?) { if (!v.isNullOrBlank()) put(key, v) }
    fun putIfNotNull(key: String, v: Any?) { if (v != null) put(key, v) }

    // ===== Basic Info =====
    // ===== Basic Info =====
    putIfNotBlank("profileImg", profileImg)
    putIfNotBlank("profileImageStoragePath", profileImageStoragePath)
    putIfNotBlank("profileImageLocalPath", profileImageLocalPath)
    putIfNotNull("profileImageUpdatedAt", profileImageUpdatedAt)
    putIfNotBlank("fName", fName)
    putIfNotBlank("lName", lName)
    putIfNotBlank("oName", oName)
    putIfNotBlank("gender", gender.name)

    if (age > 0) put("age", age)

    putIfNotBlank("ninNumber", ninNumber)
    put("childType", childType.name)
    put("program", program.name)

    putIfNotNull("dob", dob)
    put("dobVerified", dobVerified)

    putIfNotBlank("street", street)
    putIfNotBlank("personalPhone1", personalPhone1)
    putIfNotBlank("personalPhone2", personalPhone2)

    put("invitedBy", invitedBy.name)
    putIfNotBlank("invitedByIndividualId", invitedByIndividualId)
    putIfNotBlank("invitedByTypeOther", invitedByTypeOther)

    put("educationPreference", educationPreference.name)
    putIfNotBlank("technicalSkills", technicalSkills)


    // ===== Background =====

    putIfNotNull("leftHomeDate", leftHomeDate)
    putIfNotBlank("reasonLeftHome", reasonLeftHome)
    putIfNotNull("leaveStreetDate", leaveStreetDate)

    // ===== Education =====
    put("educationLevel",educationLevel.name)
    putIfNotBlank("lastClass", lastClass)
    putIfNotBlank("previousSchool", previousSchool)
    putIfNotBlank("reasonLeftSchool", reasonLeftSchool)
    put("formerSponsor",formerSponsor.name)
    putIfNotBlank("formerSponsorOther", formerSponsorOther)

    // ===== Family Resettlement =====
    put("resettlementPreference", resettlementPreference.name)
    putIfNotBlank("resettlementPreferenceOther", resettlementPreferenceOther)
    put("resettled", resettled)
    putIfNotNull("resettlementDate", resettlementDate)

    put("country", country.name)

    putIfNotBlank("region", region)
    putIfNotBlank("district", district)
    putIfNotBlank("county", county)
    putIfNotBlank("subCounty", subCounty)
    putIfNotBlank("parish", parish)
    putIfNotBlank("village", village)

    // ===== Family Members 1 =====
    putIfNotBlank("memberFName1", memberFName1)
    putIfNotBlank("memberLName1", memberLName1)
    put("relationship1", relationship1.name)
    putIfNotBlank("telephone1a", telephone1a)
    putIfNotBlank("telephone1b", telephone1b)

    put("member1AncestralCountry", member1AncestralCountry.name)
    putIfNotBlank("member1AncestralRegion", member1AncestralRegion)
    putIfNotBlank("member1AncestralDistrict", member1AncestralDistrict)
    putIfNotBlank("member1AncestralCounty", member1AncestralCounty)
    putIfNotBlank("member1AncestralSubCounty", member1AncestralSubCounty)
    putIfNotBlank("member1AncestralParish", member1AncestralParish)
    putIfNotBlank("member1AncestralVillage", member1AncestralVillage)

    put("member1RentalCountry", member1RentalCountry.name)
    putIfNotBlank("member1RentalRegion", member1RentalRegion)
    putIfNotBlank("member1RentalDistrict", member1RentalDistrict)
    putIfNotBlank("member1RentalCounty", member1RentalCounty)
    putIfNotBlank("member1RentalSubCounty", member1RentalSubCounty)
    putIfNotBlank("member1RentalParish", member1RentalParish)
    putIfNotBlank("member1RentalVillage", member1RentalVillage)

    // ===== Family Members 2 =====
    putIfNotBlank("memberFName2", memberFName2)
    putIfNotBlank("memberLName2", memberLName2)
    put("relationship2", relationship2.name)
    putIfNotBlank("telephone2a", telephone2a)
    putIfNotBlank("telephone2b", telephone2b)

    put("member3AncestralCountry", member3AncestralCountry.name)
    putIfNotBlank("member3AncestralRegion", member3AncestralRegion)
    putIfNotBlank("member3AncestralDistrict", member3AncestralDistrict)
    putIfNotBlank("member3AncestralCounty", member3AncestralCounty)
    putIfNotBlank("member3AncestralSubCounty", member3AncestralSubCounty)
    putIfNotBlank("member3AncestralParish", member3AncestralParish)
    putIfNotBlank("member3AncestralVillage", member3AncestralVillage)

    put("member3RentalCountry", member3RentalCountry.name)
    putIfNotBlank("member3RentalRegion", member3RentalRegion)
    putIfNotBlank("member3RentalDistrict", member3RentalDistrict)
    putIfNotBlank("member3RentalCounty", member3RentalCounty)
    putIfNotBlank("member3RentalSubCounty", member3RentalSubCounty)
    putIfNotBlank("member3RentalParish", member3RentalParish)
    putIfNotBlank("member3RentalVillage", member3RentalVillage)

    // ===== Family Members 3 =====
    putIfNotBlank("memberFName3", memberFName3)
    putIfNotBlank("memberLName3", memberLName3)
    put("relationship3", relationship3.name)
    putIfNotBlank("telephone3a", telephone3a)
    putIfNotBlank("telephone3b", telephone3b)

    put("member3AncestralCountry", member3AncestralCountry.name)
    putIfNotBlank("member3AncestralRegion", member3AncestralRegion)
    putIfNotBlank("member3AncestralDistrict", member3AncestralDistrict)
    putIfNotBlank("member3AncestralCounty", member3AncestralCounty)
    putIfNotBlank("member3AncestralSubCounty", member3AncestralSubCounty)
    putIfNotBlank("member3AncestralParish", member3AncestralParish)
    putIfNotBlank("member3AncestralVillage", member3AncestralVillage)

    put("member3RentalCountry", member3RentalCountry.name)
    putIfNotBlank("member3RentalRegion", member3RentalRegion)
    putIfNotBlank("member3RentalDistrict", member3RentalDistrict)
    putIfNotBlank("member3RentalCounty", member3RentalCounty)
    putIfNotBlank("member3RentalSubCounty", member3RentalSubCounty)
    putIfNotBlank("member3RentalParish", member3RentalParish)
    putIfNotBlank("member3RentalVillage", member3RentalVillage)

    // ===== Spiritual =====
    put("acceptedJesus", acceptedJesus.name)
    putIfNotNull("acceptedJesusDate", acceptedJesusDate)
    put("confessedBy", confessedBy.name)
    putIfNotBlank("ministryName", ministryName)
    put("whoPrayed", whoPrayed.name)
    putIfNotBlank("whoPrayedOther", whoPrayedOther)
    putIfNotBlank("whoPrayedId", whoPrayedId)
    put("classGroup", classGroup.name)

    putIfNotBlank("outcome", outcome)
    putIfNotBlank("generalComments", generalComments)

    // ===== Status =====
    put("registrationStatus", registrationStatus.name)
    put("graduated", graduated.name)

    // ===== Sponsorship =====
    put("sponsoredForEducation", partnershipForEducation)
    putIfNotBlank("sponsorId", partnerId)
    putIfNotBlank("sponsorFName", partnerFName)
    putIfNotBlank("sponsorLName", partnerLName)
    putIfNotBlank("partnerTelephone1", partnerTelephone1)
    putIfNotBlank("partnerTelephone2", partnerTelephone2)
    putIfNotBlank("sponsorEmail", partnerEmail)
    putIfNotBlank("sponsorNotes", partnerNotes)

    // Tombstone strategy: ONLY write delete fields when deleted.
    if (isDeleted) {
        put("isDeleted", true)
        put("deletedAt", deletedAt ?: Timestamp.now())
    }
    // ===== Audit =====
    put("createdAt", createdAt)
    put("updatedAt", Timestamp.now())
}



///* ======================= DOC -> user ======================= */
//fun DocumentSnapshot.toUserOrNull(): UserProfile? {
//    if (data == null) return null
//
//    return UserProfile(
//         // ===== IDs =====
//        uid = str("uid") ?: id,
//
//     email = str("email") ?: "",
//     displayName= str("displayName") ?: "",
//     roles = (str("roles")).toRole(),
//     disabled = getBoolean("disabled") ?: false,
//
//
//    )
//}
//
///* ======================= SNAPSHOT -> LISt ======================= */
//
//fun QuerySnapshot.toUsers(): List<UserProfile> = documents.mapNotNull { it.toUserOrNull() }
//
//private fun String?.toRole(): List<Role> =
//    runCatching {
//        if (this == null){ Role.ADMIN  }
//        else if (this == null){ Role.LEAD  }
//        else if (this == null){ Role.VOLUNTEER  }
//        else if (this == null){ Role.SPONSOR  }
//        else Role.valueOf(this)
//    }
//        .getOrDefault(Role.VIEWER)


fun DocumentSnapshot.toUserOrNull(): UserProfile? {
    if (data == null) return null

    val rolesRaw = get("roles") // could be String, List<String>, List<Role>, or null
    return UserProfile(
        // ===== IDs =====
        uid = str("uid") ?: id,

        // ===== BASIC =====
        email = str("email") ?: "",
        displayName = str("displayName") ?: "",

        // ===== ROLES =====
//        userRoles = rolesRaw,
//         userRole = (str("assignedRoles")).toAssignedRole(),
        userRole = (str("userRole") ?: str("assignedRole") ?: str("assignedRoles") ?: str("role")).toAssignedRole(),

        // ===== FLAGS =====
        disabled = getBoolean("disabled") ?: false
    )
}

/* ======================= SNAPSHOT -> LIST ======================= */
fun QuerySnapshot.toUsers(): List<UserProfile> =
    documents.mapNotNull { it.toUserOrNull() }



/* ======================Attendance ================= */

fun Attendance.toFirestoreMapPatch(): Map<String, Any> = buildMap {
    fun putIfNotBlank(key: String, v: String?) {
        if (!v.isNullOrBlank()) put(key, v)
    }
    fun putIfNotNull(key: String, v: Any?) {
        if (v != null) put(key, v)
    }

    put("attendanceId", attendanceId)
    put("childId", childId)
    put("eventId", eventId)
    putIfNotBlank("adminId", adminId)

    put("status", status.name)

    // Keep notes stable (you can switch back to omit-if-blank later if you want)
    put("notes", notes.orEmpty())

    // Tombstone strategy: ONLY write delete fields when deleted.
    if (isDeleted) {
        put("isDeleted", true)
        put("deletedAt", deletedAt ?: Timestamp.now())
    }

    put("version", version)

    // Let the WORKER set updatedAt = now (single source of "now").
    // put("updatedAt", ...)  <-- intentionally omitted here

    putIfNotNull("checkedAt", checkedAt)
}

// com.example.charityDept.data.mappers (same file where toChildOrNull lives)


fun DocumentSnapshot.toAttendanceOrNull(): Attendance? {
    if (data == null) return null

    fun ts(key: String): Timestamp? = getTimestamp(key)

    val statusRaw = getString("status")
    val status = runCatching {
        if (statusRaw.isNullOrBlank()) AttendanceStatus.ABSENT else AttendanceStatus.valueOf(statusRaw)
    }.getOrDefault(AttendanceStatus.ABSENT)

    val isDel = getBoolean("isDeleted") ?: false
    val delAt = if (isDel) (ts("deletedAt")) else null

    return Attendance(
        attendanceId = getString("attendanceId") ?: id,
        childId = getString("childId") ?: "",
        eventId = getString("eventId") ?: "",
        adminId = getString("adminId") ?: "",
        status = status,
        notes = getString("notes") ?: "",

        // tombstone
        isDeleted = isDel,
        deletedAt = delAt,

        // from server => always clean
        isDirty = false,
        version = getLong("version") ?: 0L,

        createdAt = ts("createdAt") ?: Timestamp.now(),
        updatedAt = ts("updatedAt") ?: Timestamp.now(),

        // IMPORTANT: don’t default to now on pull; keep null if missing
        checkedAt = ts("checkedAt")
    )
}

/* ======================= ROLE PARSING ======================= */

/* ======================= ROLE PARSING ======================= */


/* ======================= ROLE PARSING ======================= */

private fun String.toRoleOrNull(): AssignedRole? = runCatching {
    // Normalize common variants; keep your current enum spelling "LEAD"
    when (val norm = trim().uppercase()) {
        "VOLUNTEER" -> AssignedRole.VOLUNTEER   // alias if some data writes "LEADER"
        else -> AssignedRole.valueOf(norm)
    }
}.getOrNull()
private fun String?.toAssignedRole(): AssignedRole =
    runCatching { if (this == null) AssignedRole.VOLUNTEER else AssignedRole.valueOf(this) }
        .getOrDefault(AssignedRole.VOLUNTEER)

//private fun String?.toAssignedRole(): AssignedRole =
//    when (this?.trim()?.uppercase()) {
//        null, "", "VOLUNTEER" -> AssignedRole.VOLUNTEER
//        "LEADER"      -> AssignedRole.LEADER   // alias supported
//        "ADMIN"               -> AssignedRole.ADMIN
//        "VIEWER"              -> AssignedRole.VIEWER
//        "SPONSOR"             -> AssignedRole.SPONSOR
//        else                  -> AssignedRole.VOLUNTEER // unknowns default
//    }


