package com.example.charityDept.data.local.seed

import android.content.Context
import androidx.room.withTransaction
import com.example.charityDept.data.local.db.AppDatabase
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.ChildType
import com.example.charityDept.data.model.ClassGroup
import com.example.charityDept.data.model.ConfessedBy
import com.example.charityDept.data.model.Country
import com.example.charityDept.data.model.EducationLevel
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.Gender
import com.example.charityDept.data.model.Individual
import com.example.charityDept.data.model.Program
import com.example.charityDept.data.model.RegistrationStatus
import com.example.charityDept.data.model.Relationship
import com.example.charityDept.data.model.Reply
import com.example.charityDept.data.model.ResettlementPreference
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
//import com.google.firebase.Timestamp

object ChildrenSeedLoader {

    private const val ASSET_FILE = "children_seed.json"

    suspend fun seedIfChildrenEmpty(context: Context, db: AppDatabase) = withContext(Dispatchers.IO) {
        if (!isChildrenTableEmpty(db)) return@withContext

        val json = readAsset(context, ASSET_FILE)
        val root = JSONObject(json)

        val childrenJson = root.optJSONArray("children") ?: JSONArray()
        if (childrenJson.length() == 0) return@withContext

        val children = ArrayList<Child>(childrenJson.length())
        for (i in 0 until childrenJson.length()) {
            val o = childrenJson.getJSONObject(i)
            children.add(o.toChildSeeded())
        }

        // Insert seeds as NOT dirty (offline-first SoT is Room)
//        db.childDao().upsertAll(children)
        db.withTransaction {
            db.childDao().upsertAll(children)
        }


    }

    private fun readAsset(context: Context, fileName: String): String {
        return context.assets.open(fileName).bufferedReader().use { it.readText() }
    }

    private fun isChildrenTableEmpty(db: AppDatabase): Boolean {
        return runCatching {
            val c = db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM children")
            c.use {
                if (!it.moveToFirst()) return true
                it.getLong(0) == 0L
            }
        }.getOrElse { true } // if table missing / migration not applied yet, treat as empty
    }


    private fun JSONObject.toChildSeeded(): Child {
        val childId = optString("childId", "")
        val childType = optEnum("childType", ChildType.FAMILY) { ChildType.valueOf(it) }

        val age = optInt("age", 0)
        val program = when {
            age >= 17 -> Program.BROTHERS_AND_SISTERS_OF_ZION
            else -> optEnum("program", Program.CHILDREN_OF_ZION) { Program.valueOf(it) }
        }
        val classGroup = when (age) {
            in 0..5 -> ClassGroup.SERGEANT
            in 6..9 -> ClassGroup.LIEUTENANT
            in 10..12 -> ClassGroup.CAPTAIN
            in 13..16 -> ClassGroup.GENERAL
            in 17..25 -> ClassGroup.BROTHERS_AND_SISTERS_OF_ZION
            else -> optEnum("classGroup", ClassGroup.SERGEANT) { ClassGroup.valueOf(it) }
        }

        // timestamps (nullable ones handled as null)
        val dobMs: Long? = if (isNull("dob")) null else optLong("dob", 0L)
        val leftHomeDateMs: Long? = if (isNull("leftHomeDate")) null else optLong("leftHomeDate", 0L)
        val leaveStreetDateMs: Long? = if (isNull("leaveStreetDate")) null else optLong("leaveStreetDate", 0L)
        val resettlementDateMs: Long? = if (isNull("resettlementDate")) null else optLong("resettlementDate", 0L)
        val acceptedJesusDateMs: Long? = if (isNull("acceptedJesusDate")) null else optLong("acceptedJesusDate", 0L)
        val deletedAtMs: Long? = if (isNull("deletedAt")) null else optLong("deletedAt", 0L)

        val createdAtMs = optLong("createdAt", 0L)
        val updatedAtMs = optLong("updatedAt", 0L)

        val dobTs = tsFromMillisOrNull(dobMs)
        val leftHomeDateTs = tsFromMillisOrNull(leftHomeDateMs)
        val leaveStreetDateTs = tsFromMillisOrNull(leaveStreetDateMs)
        val resettlementDateTs = tsFromMillisOrNull(resettlementDateMs)
        val acceptedJesusDateTs = tsFromMillisOrNull(acceptedJesusDateMs)
        val deletedAtTs = tsFromMillisOrNull(deletedAtMs)

        val createdAtTs = tsFromMillisOrNull(createdAtMs) ?: Timestamp.now()
        val updatedAtTs = tsFromMillisOrNull(updatedAtMs) ?: Timestamp.now()

        // enums (stored as strings in seed json)
        val gender = optEnum("gender", Gender.MALE) { Gender.valueOf(it) }
        val invitedBy = optEnum("invitedBy", Individual.UNCLE) { Individual.valueOf(it) }
        val educationPreference = optEnum("educationPreference", EducationPreference.NONE) { EducationPreference.valueOf(it) }

        val educationLevel = optEnum("educationLevel", EducationLevel.NONE) { EducationLevel.valueOf(it) }
        val formerSponsor = optEnum("formerSponsor", Relationship.NONE) { Relationship.valueOf(it) }

        val resettlementPreference = optEnum("resettlementPreference", ResettlementPreference.DIRECT_HOME) {
            ResettlementPreference.valueOf(it)
        }
        val country = optEnum("country", Country.UGANDA) { Country.valueOf(it) }

        val relationship1 = optEnum("relationship1", Relationship.NONE) { Relationship.valueOf(it) }
        val member1AncestralCountry = optEnum("member1AncestralCountry", Country.UGANDA) { Country.valueOf(it) }
        val member1RentalCountry = optEnum("member1RentalCountry", Country.UGANDA) { Country.valueOf(it) }

        val relationship2 = optEnum("relationship2", Relationship.NONE) { Relationship.valueOf(it) }
        val member2AncestralCountry = optEnum("member2AncestralCountry", Country.UGANDA) { Country.valueOf(it) }
        val member2RentalCountry = optEnum("member2RentalCountry", Country.UGANDA) { Country.valueOf(it) }

        val relationship3 = optEnum("relationship3", Relationship.NONE) { Relationship.valueOf(it) }
        val member3AncestralCountry = optEnum("member3AncestralCountry", Country.UGANDA) { Country.valueOf(it) }
        val member3RentalCountry = optEnum("member3RentalCountry", Country.UGANDA) { Country.valueOf(it) }

        val acceptedJesus = optEnum("acceptedJesus", Reply.NO) { Reply.valueOf(it) }
        val confessedBy = optEnum("confessedBy", ConfessedBy.NONE) { ConfessedBy.valueOf(it) }
        val whoPrayed = optEnum("whoPrayed", Individual.UNCLE) { Individual.valueOf(it) }

        val registrationStatus = optEnum("registrationStatus", RegistrationStatus.BASICINFOR) {
            RegistrationStatus.valueOf(it)
        }
        val graduated = optEnum("graduated", Reply.NO) { Reply.valueOf(it) }

        return Child(
            childId = childId,

            // ===== Basic Info =====
            profileImg = optString("profileImg", ""),
            profileImageStoragePath = optString("profileImageStoragePath", ""),
            profileImageLocalPath = optString("profileImageLocalPath", ""),
            profileImageUpdatedAt = optTimestamp("profileImageUpdatedAt"),

            fName = optString("fName", ""),
            lName = optString("lName", ""),
            oName = optString("oName", ""),

            age = age,
            ninNumber = optString("ninNumber", ""),
            childType = childType,
            program = program,

            dob = dobTs,
            dobVerified = optInt("dobVerified", 0) == 1 || optBoolean("dobVerified", false),
            gender = gender,

            street = optString("street", ""),
            personalPhone1 = optString("personalPhone1", ""),
            personalPhone2 = optString("personalPhone2", ""),

            invitedBy = invitedBy,
            invitedByIndividualId = optString("invitedByIndividualId", ""),
            invitedByTypeOther = optString("invitedByTypeOther", ""),

            educationPreference = educationPreference,

            // ===== Background Info =====
            leftHomeDate = leftHomeDateTs,
            reasonLeftHome = optString("reasonLeftHome", ""),
            leaveStreetDate = leaveStreetDateTs,

            // ===== Education Info =====
            educationLevel = educationLevel,
            lastClass = optString("lastClass", ""),
            previousSchool = optString("previousSchool", ""),
            reasonLeftSchool = optString("reasonLeftSchool", ""),
            formerSponsor = formerSponsor,
            formerSponsorOther = optString("formerSponsorOther", ""),
            technicalSkills = optString("technicalSkills", ""),

            // ===== Family Resettlement =====
            resettlementPreference = resettlementPreference,
            resettlementPreferenceOther = optString("resettlementPreferenceOther", ""),
            resettled = optInt("resettled", 0) == 1 || optBoolean("resettled", false),
            resettlementDate = resettlementDateTs,
            country = country,

            region = optString("region", ""),
            district = optString("district", ""),
            county = optString("county", ""),
            subCounty = optString("subCounty", ""),
            parish = optString("parish", ""),
            village = optString("village", ""),

            // ===== Family Members 1 =====
            memberFName1 = optString("memberFName1", ""),
            memberLName1 = optString("memberLName1", ""),
            relationship1 = relationship1,
            telephone1a = optString("telephone1a", ""),
            telephone1b = optString("telephone1b", ""),

            member1AncestralCountry = member1AncestralCountry,
            member1AncestralRegion = optString("member1AncestralRegion", ""),
            member1AncestralDistrict = optString("member1AncestralDistrict", ""),
            member1AncestralCounty = optString("member1AncestralCounty", ""),
            member1AncestralSubCounty = optString("member1AncestralSubCounty", ""),
            member1AncestralParish = optString("member1AncestralParish", ""),
            member1AncestralVillage = optString("member1AncestralVillage", ""),

            member1RentalCountry = member1RentalCountry,
            member1RentalRegion = optString("member1RentalRegion", ""),
            member1RentalDistrict = optString("member1RentalDistrict", ""),
            member1RentalCounty = optString("member1RentalCounty", ""),
            member1RentalSubCounty = optString("member1RentalSubCounty", ""),
            member1RentalParish = optString("member1RentalParish", ""),
            member1RentalVillage = optString("member1RentalVillage", ""),

            // ===== Family Members 2 =====
            memberFName2 = optString("memberFName2", ""),
            memberLName2 = optString("memberLName2", ""),
            relationship2 = relationship2,
            telephone2a = optString("telephone2a", ""),
            telephone2b = optString("telephone2b", ""),

            member2AncestralCountry = member2AncestralCountry,
            member2AncestralRegion = optString("member2AncestralRegion", ""),
            member2AncestralDistrict = optString("member2AncestralDistrict", ""),
            member2AncestralCounty = optString("member2AncestralCounty", ""),
            member2AncestralSubCounty = optString("member2AncestralSubCounty", ""),
            member2AncestralParish = optString("member2AncestralParish", ""),
            member2AncestralVillage = optString("member2AncestralVillage", ""),

            member2RentalCountry = member2RentalCountry,
            member2RentalRegion = optString("member2RentalRegion", ""),
            member2RentalDistrict = optString("member2RentalDistrict", ""),
            member2RentalCounty = optString("member2RentalCounty", ""),
            member2RentalSubCounty = optString("member2RentalSubCounty", ""),
            member2RentalParish = optString("member2RentalParish", ""),
            member2RentalVillage = optString("member2RentalVillage", ""),

            // ===== Family Members 3 =====
            memberFName3 = optString("memberFName3", ""),
            memberLName3 = optString("memberLName3", ""),
            relationship3 = relationship3,
            telephone3a = optString("telephone3a", ""),
            telephone3b = optString("telephone3b", ""),

            member3AncestralCountry = member3AncestralCountry,
            member3AncestralRegion = optString("member3AncestralRegion", ""),
            member3AncestralDistrict = optString("member3AncestralDistrict", ""),
            member3AncestralCounty = optString("member3AncestralCounty", ""),
            member3AncestralSubCounty = optString("member3AncestralSubCounty", ""),
            member3AncestralParish = optString("member3AncestralParish", ""),
            member3AncestralVillage = optString("member3AncestralVillage", ""),

            member3RentalCountry = member3RentalCountry,
            member3RentalRegion = optString("member3RentalRegion", ""),
            member3RentalDistrict = optString("member3RentalDistrict", ""),
            member3RentalCounty = optString("member3RentalCounty", ""),
            member3RentalSubCounty = optString("member3RentalSubCounty", ""),
            member3RentalParish = optString("member3RentalParish", ""),
            member3RentalVillage = optString("member3RentalVillage", ""),

            // ===== Spiritual Info =====
            acceptedJesus = acceptedJesus,
            confessedBy = confessedBy,
            ministryName = optString("ministryName", ""),
            acceptedJesusDate = acceptedJesusDateTs,
            whoPrayed = whoPrayed,
            whoPrayedOther = optString("whoPrayedOther", ""),
            whoPrayedId = optString("whoPrayedId", ""),
            outcome = optString("outcome", ""),
            generalComments = optString("generalComments", ""),
            classGroup = classGroup,

            // ===== Program statuses =====
            registrationStatus = registrationStatus,
            graduated = graduated,

            // ===== Sponsorship/Family flags =====
            partnershipForEducation = optInt("partnershipForEducation", 0) == 1 || optBoolean("partnershipForEducation", false),
            partnerId = optString("partnerId", ""),
            partnerFName = optString("partnerFName", ""),
            partnerLName = optString("partnerLName", ""),
            partnerTelephone1 = optString("partnerTelephone1", ""),
            partnerTelephone2 = optString("partnerTelephone2", ""),
            partnerEmail = optString("partnerEmail", ""),
            partnerNotes = optString("partnerNotes", ""),

            // ===== Audit =====
            createdAt = createdAtTs,
            updatedAt = updatedAtTs,

            isDirty = false, // seeded rows are clean

            isDeleted = optInt("isDeleted", 0) == 1 || optBoolean("isDeleted", false),
            deletedAt = deletedAtTs,

            version = optLong("version", 0L)
        )
    }

    private fun tsFromMillisOrNull(ms: Long?): Timestamp? {
        val v = ms ?: return null
        if (v <= 0L) return null
        val seconds = v / 1000L
        val nanos = ((v % 1000L).toInt()) * 1_000_000
        return Timestamp(seconds, nanos)
    }

    private fun JSONObject.optTimestamp(key: String): Timestamp? {
        if (!has(key) || isNull(key)) return null

        return when (val value = opt(key)) {
            is Number -> Timestamp(value.toLong() / 1000, 0)
            is String -> value.toLongOrNull()?.let { Timestamp(it / 1000, 0) }
            is JSONObject -> {
                val seconds = when (val s = value.opt("seconds")) {
                    is Number -> s.toLong()
                    is String -> s.toLongOrNull()
                    else -> null
                } ?: return null

                val nanos = when (val n = value.opt("nanoseconds")) {
                    is Number -> n.toInt()
                    is String -> n.toIntOrNull()
                    else -> 0
                } ?: 0

                Timestamp(seconds, nanos)
            }
            else -> null
        }
    }

    private inline fun <T> JSONObject.optEnum(
        key: String,
        fallback: T,
        parse: (String) -> T
    ): T {
        val raw = optString(key, "")
        if (raw.isBlank()) return fallback
        return runCatching { parse(raw) }.getOrElse { fallback }
    }
}

