package com.example.charityDept.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.charityDept.data.local.dao.AssessmentAnswerDao
import com.example.charityDept.data.local.dao.AssessmentQuestionDao
import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.local.dao.AttendanceDao
import com.example.charityDept.data.local.dao.ChildDao
import com.example.charityDept.data.local.dao.EventDao
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.local.dao.KpiDao
import com.example.charityDept.data.local.dao.UgAdminDao
import com.example.charityDept.data.local.entities.KpiCounter
import com.example.charityDept.data.model.AssessmentAnswer
import com.example.charityDept.data.model.AssessmentQuestion
import com.example.charityDept.data.model.AssessmentTaxonomy
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.Event
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.data.model.UgCountyEntity
import com.example.charityDept.data.model.UgDistrictEntity
import com.example.charityDept.data.model.UgParishEntity
import com.example.charityDept.data.model.UgRegionEntity
import com.example.charityDept.data.model.UgSubCountyEntity
import com.example.charityDept.data.model.UgVillageEntity

@Database(
    entities = [
        Child::class,
        Event::class,
        Family::class,
        FamilyMember::class,
        KpiCounter::class,
        Attendance::class,

        // seed tables
        UgRegionEntity::class,
        UgDistrictEntity::class,
        UgCountyEntity::class,
        UgSubCountyEntity::class,
        UgParishEntity::class,
        UgVillageEntity::class,

        AssessmentQuestion::class,
        AssessmentAnswer::class,
        AssessmentTaxonomy::class,
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(TimestampConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun childDao(): ChildDao
    abstract fun eventDao(): EventDao
    abstract fun familyDao(): FamilyDao
    abstract fun kpiDao(): KpiDao
    abstract fun attendanceDao(): AttendanceDao

    abstract fun ugAdminDao(): UgAdminDao

    abstract fun assessmentQuestionDao(): AssessmentQuestionDao
    abstract fun assessmentAnswerDao(): AssessmentAnswerDao
    abstract fun assessmentTaxonomyDao(): AssessmentTaxonomyDao

    companion object {

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_regions (
                        regionCode TEXT NOT NULL,
                        regionName TEXT NOT NULL,
                        PRIMARY KEY(regionCode)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_districts (
                        districtCode TEXT NOT NULL,
                        districtName TEXT NOT NULL,
                        regionCode TEXT NOT NULL,
                        PRIMARY KEY(districtCode)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_districts_regionCode ON ug_districts(regionCode)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_counties (
                        countyCode TEXT NOT NULL,
                        countyName TEXT NOT NULL,
                        districtCode TEXT NOT NULL,
                        PRIMARY KEY(countyCode)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_counties_districtCode ON ug_counties(districtCode)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_subcounties (
                        subCountyCode TEXT NOT NULL,
                        subCountyName TEXT NOT NULL,
                        countyCode TEXT NOT NULL,
                        PRIMARY KEY(subCountyCode)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_subcounties_countyCode ON ug_subcounties(countyCode)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_parishes (
                        parishCode TEXT NOT NULL,
                        parishName TEXT NOT NULL,
                        subCountyCode TEXT NOT NULL,
                        PRIMARY KEY(parishCode)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_parishes_subCountyCode ON ug_parishes(subCountyCode)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS ug_villages (
                        villageCode TEXT NOT NULL,
                        villageName TEXT NOT NULL,
                        parishCode TEXT NOT NULL,
                        PRIMARY KEY(villageCode)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_ug_villages_parishCode ON ug_villages(parishCode)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assessment_questions (
                        questionId TEXT NOT NULL,
                        category TEXT NOT NULL,
                        subCategory TEXT NOT NULL,
                        question TEXT NOT NULL,
                        indexNum INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        version INTEGER NOT NULL,
                        PRIMARY KEY(questionId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_category ON assessment_questions(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_subCategory ON assessment_questions(subCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isActive ON assessment_questions(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_updatedAt ON assessment_questions(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isDirty ON assessment_questions(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_isDeleted ON assessment_questions(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_indexNum ON assessment_questions(indexNum)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assessment_answers (
                        answerId TEXT NOT NULL,
                        childId TEXT NOT NULL,
                        generalId TEXT NOT NULL,
                        questionId TEXT NOT NULL,
                        category TEXT NOT NULL,
                        subCategory TEXT NOT NULL,
                        questionSnapshot TEXT,
                        answer TEXT NOT NULL,
                        score INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        enteredByUid TEXT NOT NULL,
                        lastEditedByUid TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        deletedAt INTEGER,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        version INTEGER NOT NULL,
                        PRIMARY KEY(answerId)
                    )
                    """.trimIndent()
                )

                db.execSQL("DROP INDEX IF EXISTS uq_assessment_answers_child_session_question")

                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_assessment_answers_childId_generalId_questionId
                    ON assessment_answers(childId, generalId, questionId)
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_category ON assessment_answers(category)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_childId ON assessment_answers(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_enteredByUid ON assessment_answers(enteredByUid)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_generalId ON assessment_answers(generalId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_isDeleted ON assessment_answers(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_isDirty ON assessment_answers(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_questionId ON assessment_answers(questionId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_subCategory ON assessment_answers(subCategory)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_answers_updatedAt ON assessment_answers(updatedAt)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS assessment_taxonomy (
                        taxonomyId TEXT NOT NULL,
                        categoryKey TEXT NOT NULL,
                        categoryLabel TEXT NOT NULL,
                        subCategoryKey TEXT NOT NULL,
                        subCategoryLabel TEXT NOT NULL,
                        indexNum INTEGER NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        version INTEGER NOT NULL,
                        PRIMARY KEY(taxonomyId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_categoryKey ON assessment_taxonomy(categoryKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isActive ON assessment_taxonomy(isActive)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_updatedAt ON assessment_taxonomy(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isDirty ON assessment_taxonomy(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_taxonomy_isDeleted ON assessment_taxonomy(isDeleted)")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS index_assessment_taxonomy_categoryKey_subCategoryKey
                    ON assessment_taxonomy(categoryKey, subCategoryKey)
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE assessment_questions ADD COLUMN categoryKey TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assessment_questions ADD COLUMN subCategoryKey TEXT NOT NULL DEFAULT ''")

                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_categoryKey ON assessment_questions(categoryKey)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_assessment_questions_subCategoryKey ON assessment_questions(subCategoryKey)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS events (
                        eventId TEXT NOT NULL,
                        isChild INTEGER NOT NULL DEFAULT 0,

                        eventParentId TEXT NOT NULL,
                        title TEXT NOT NULL,
                        eventDate INTEGER NOT NULL,

                        teamName TEXT NOT NULL,
                        teamLeaderNames TEXT NOT NULL,
                        leaderTelephone1 TEXT NOT NULL,
                        leaderTelephone2 TEXT NOT NULL,
                        leaderEmail TEXT NOT NULL,

                        location TEXT NOT NULL,
                        eventStatus TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        adminId TEXT NOT NULL,

                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,

                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,

                        version INTEGER NOT NULL,

                        PRIMARY KEY(eventId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_title ON events(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_teamName ON events(teamName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_eventDate ON events(eventDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_updatedAt ON events(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_isDeleted ON events(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_isDirty ON events(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_isChild ON events(isChild)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_events_isDeleted_updatedAt ON events(isDeleted, updatedAt)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS attendances (
                        attendanceId TEXT NOT NULL,

                        childId TEXT NOT NULL,
                        eventId TEXT NOT NULL,
                        adminId TEXT NOT NULL,

                        status TEXT NOT NULL,
                        notes TEXT NOT NULL,

                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,

                        isDirty INTEGER NOT NULL,
                        version INTEGER NOT NULL,

                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,

                        checkedAt INTEGER,

                        PRIMARY KEY(attendanceId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendances_eventId ON attendances(eventId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendances_childId ON attendances(childId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendances_updatedAt ON attendances(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendances_eventId_status ON attendances(eventId, status)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attendances_isDirty_updatedAt ON attendances(isDirty, updatedAt)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS children (
                        childId TEXT NOT NULL,

                        profileImg TEXT NOT NULL,

                        fName TEXT NOT NULL,
                        lName TEXT NOT NULL,
                        oName TEXT NOT NULL,

                        age INTEGER NOT NULL,

                        dob INTEGER,
                        dobVerified INTEGER NOT NULL,
                        gender TEXT NOT NULL,

                        street TEXT NOT NULL,

                        invitedBy TEXT NOT NULL,
                        invitedByIndividualId TEXT NOT NULL,
                        invitedByTypeOther TEXT NOT NULL,

                        educationPreference TEXT NOT NULL,

                        leftHomeDate INTEGER,
                        reasonLeftHome TEXT NOT NULL,
                        leaveStreetDate INTEGER,

                        educationLevel TEXT NOT NULL,
                        lastClass TEXT NOT NULL,
                        previousSchool TEXT NOT NULL,
                        reasonLeftSchool TEXT NOT NULL,
                        formerSponsor TEXT NOT NULL,
                        formerSponsorOther TEXT NOT NULL,
                        technicalSkills TEXT NOT NULL,

                        resettlementPreference TEXT NOT NULL,
                        resettlementPreferenceOther TEXT NOT NULL,
                        resettled INTEGER NOT NULL,
                        resettlementDate INTEGER,
                        country TEXT NOT NULL,

                        region TEXT NOT NULL,
                        district TEXT NOT NULL,
                        county TEXT NOT NULL,
                        subCounty TEXT NOT NULL,
                        parish TEXT NOT NULL,
                        village TEXT NOT NULL,

                        memberFName1 TEXT NOT NULL,
                        memberLName1 TEXT NOT NULL,
                        relationship1 TEXT NOT NULL,
                        telephone1a TEXT NOT NULL,
                        telephone1b TEXT NOT NULL,

                        member1AncestralCountry TEXT NOT NULL,
                        member1AncestralRegion TEXT NOT NULL,
                        member1AncestralDistrict TEXT NOT NULL,
                        member1AncestralCounty TEXT NOT NULL,
                        member1AncestralSubCounty TEXT NOT NULL,
                        member1AncestralParish TEXT NOT NULL,
                        member1AncestralVillage TEXT NOT NULL,

                        member1RentalCountry TEXT NOT NULL,
                        member1RentalRegion TEXT NOT NULL,
                        member1RentalDistrict TEXT NOT NULL,
                        member1RentalCounty TEXT NOT NULL,
                        member1RentalSubCounty TEXT NOT NULL,
                        member1RentalParish TEXT NOT NULL,
                        member1RentalVillage TEXT NOT NULL,

                        memberFName2 TEXT NOT NULL,
                        memberLName2 TEXT NOT NULL,
                        relationship2 TEXT NOT NULL,
                        telephone2a TEXT NOT NULL,
                        telephone2b TEXT NOT NULL,

                        member2AncestralCountry TEXT NOT NULL,
                        member2AncestralRegion TEXT NOT NULL,
                        member2AncestralDistrict TEXT NOT NULL,
                        member2AncestralCounty TEXT NOT NULL,
                        member2AncestralSubCounty TEXT NOT NULL,
                        member2AncestralParish TEXT NOT NULL,
                        member2AncestralVillage TEXT NOT NULL,

                        member2RentalCountry TEXT NOT NULL,
                        member2RentalRegion TEXT NOT NULL,
                        member2RentalDistrict TEXT NOT NULL,
                        member2RentalCounty TEXT NOT NULL,
                        member2RentalSubCounty TEXT NOT NULL,
                        member2RentalParish TEXT NOT NULL,
                        member2RentalVillage TEXT NOT NULL,

                        memberFName3 TEXT NOT NULL,
                        memberLName3 TEXT NOT NULL,
                        relationship3 TEXT NOT NULL,
                        telephone3a TEXT NOT NULL,
                        telephone3b TEXT NOT NULL,

                        member3AncestralCountry TEXT NOT NULL,
                        member3AncestralRegion TEXT NOT NULL,
                        member3AncestralDistrict TEXT NOT NULL,
                        member3AncestralCounty TEXT NOT NULL,
                        member3AncestralSubCounty TEXT NOT NULL,
                        member3AncestralParish TEXT NOT NULL,
                        member3AncestralVillage TEXT NOT NULL,

                        member3RentalCountry TEXT NOT NULL,
                        member3RentalRegion TEXT NOT NULL,
                        member3RentalDistrict TEXT NOT NULL,
                        member3RentalCounty TEXT NOT NULL,
                        member3RentalSubCounty TEXT NOT NULL,
                        member3RentalParish TEXT NOT NULL,
                        member3RentalVillage TEXT NOT NULL,

                        acceptedJesus TEXT NOT NULL,
                        confessedBy TEXT NOT NULL,
                        ministryName TEXT NOT NULL,
                        acceptedJesusDate INTEGER,
                        whoPrayed TEXT NOT NULL,
                        whoPrayedOther TEXT NOT NULL,
                        whoPrayedId TEXT NOT NULL,
                        outcome TEXT NOT NULL,
                        generalComments TEXT NOT NULL,
                        classGroup TEXT NOT NULL,

                        registrationStatus TEXT NOT NULL,
                        graduated TEXT NOT NULL,

                        partnershipForEducation INTEGER NOT NULL,
                        partnerId TEXT NOT NULL,
                        partnerFName TEXT NOT NULL,
                        partnerLName TEXT NOT NULL,
                        partnerTelephone1 TEXT NOT NULL,
                        partnerTelephone2 TEXT NOT NULL,
                        partnerEmail TEXT NOT NULL,
                        partnerNotes TEXT NOT NULL,

                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,

                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,

                        version INTEGER NOT NULL,

                        PRIMARY KEY(childId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_isDeleted ON children(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_graduated ON children(graduated)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_partnershipForEducation ON children(partnershipForEducation)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_resettled ON children(resettled)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_registrationStatus ON children(registrationStatus)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_educationPreference ON children(educationPreference)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_region ON children(region)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_street ON children(street)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_createdAt ON children(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_updatedAt ON children(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_children_isDirty ON children(isDirty)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE children ADD COLUMN ninNumber TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE children ADD COLUMN childType TEXT NOT NULL DEFAULT 'FAMILY'")
                db.execSQL("ALTER TABLE children ADD COLUMN program TEXT NOT NULL DEFAULT 'CHILDREN_OF_ZION'")
                db.execSQL("ALTER TABLE children ADD COLUMN personalPhone1 TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE children ADD COLUMN personalPhone2 TEXT NOT NULL DEFAULT ''")

                db.execSQL(
                    """
            UPDATE children
            SET program = CASE
                WHEN age >= 17 THEN 'BROTHERS_AND_SISTERS_OF_ZION'
                ELSE 'CHILDREN_OF_ZION'
            END
            """.trimIndent()
                )

                db.execSQL(
                    """
            UPDATE children
            SET classGroup = CASE
                WHEN age BETWEEN 0 AND 5 THEN 'SERGEANT'
                WHEN age BETWEEN 6 AND 9 THEN 'LIEUTENANT'
                WHEN age BETWEEN 10 AND 12 THEN 'CAPTAIN'
                WHEN age BETWEEN 13 AND 16 THEN 'GENERAL'
                WHEN age >= 17 THEN 'BROTHERS_AND_SISTERS_OF_ZION'
                ELSE classGroup
            END
            """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS families (
                        familyId TEXT NOT NULL,
                       caseReferenceNumber TEXT NOT NULL,
                        dateOfAssessment INTEGER,
                        fName TEXT NOT NULL,
                        lName TEXT NOT NULL,
                        primaryContactHeadOfHousehold TEXT NOT NULL,
                        addressLocation TEXT NOT NULL,
                        isBornAgain INTEGER NOT NULL,
                        profileImg TEXT NOT NULL,
                        profileImageStoragePath TEXT NOT NULL,
                        profileImageLocalPath TEXT NOT NULL,
                        personalPhone1 TEXT NOT NULL,
                        personalPhone2 TEXT NOT NULL,
                        ninNumber TEXT NOT NULL,
                        memberAncestralCountry TEXT NOT NULL,
                        memberAncestralRegion TEXT NOT NULL,
                        memberAncestralDistrict TEXT NOT NULL,
                        memberAncestralCounty TEXT NOT NULL,
                        memberAncestralSubCounty TEXT NOT NULL,
                        memberAncestralParish TEXT NOT NULL,
                        memberAncestralVillage TEXT NOT NULL,
                        memberRentalCountry TEXT NOT NULL,
                        memberRentalRegion TEXT NOT NULL,
                        memberRentalDistrict TEXT NOT NULL,
                        memberRentalCounty TEXT NOT NULL,
                        memberRentalSubCounty TEXT NOT NULL,
                        memberRentalParish TEXT NOT NULL,
                        memberRentalVillage TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        version INTEGER NOT NULL,
                        enteredByUid TEXT NOT NULL,
                        lastEditedByUid TEXT NOT NULL,
                        purgeAt INTEGER,
                        PRIMARY KEY(familyId)
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_caseReferenceNumber ON families(caseReferenceNumber)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_primaryContactHeadOfHousehold ON families(primaryContactHeadOfHousehold)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_dateOfAssessment ON families(dateOfAssessment)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_updatedAt ON families(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_isDirty ON families(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_isDeleted ON families(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_isDeleted_updatedAt ON families(isDeleted, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_fName ON families(fName) " )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_families_lName ON families(lName)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS family_members (
                        familyMemberId TEXT NOT NULL,
                        familyId TEXT NOT NULL,
fName TEXT NOT NULL,
lName TEXT NOT NULL,
age INTEGER NOT NULL,
                        gender TEXT NOT NULL,
                        relationship TEXT NOT NULL,
                        occupationOrSchoolGrade TEXT NOT NULL,
                        healthOrDisabilityStatus TEXT NOT NULL,
                        profileImg TEXT NOT NULL,
                        profileImageStoragePath TEXT NOT NULL,
                        profileImageLocalPath TEXT NOT NULL,
                        personalPhone1 TEXT NOT NULL,
                        personalPhone2 TEXT NOT NULL,
                        ninNumber TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        isDirty INTEGER NOT NULL,
                        isDeleted INTEGER NOT NULL,
                        deletedAt INTEGER,
                        version INTEGER NOT NULL,
                        enteredByUid TEXT NOT NULL,
                        lastEditedByUid TEXT NOT NULL,
                        purgeAt INTEGER,
                        PRIMARY KEY(familyMemberId),
                        FOREIGN KEY(familyId) REFERENCES families(familyId) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_familyId ON family_members(familyId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_fName ON family_members(fName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_lName ON family_members(lName)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_relationship ON family_members(relationship)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_updatedAt ON family_members(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_isDirty ON family_members(isDirty)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_isDeleted ON family_members(isDeleted)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_family_members_familyId_isDeleted ON family_members(familyId, isDeleted)")
            }
        }


    }
}

