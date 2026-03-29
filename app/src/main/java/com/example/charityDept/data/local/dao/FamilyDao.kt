package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface FamilyDao {

    @Query("""
        SELECT * FROM families
        WHERE isDeleted = 0
        ORDER BY updatedAt DESC, createdAt DESC
    """)
    fun observeAllActiveFamilies(): Flow<List<Family>>

    @Query("""
        SELECT * FROM families
        WHERE familyId = :familyId
        LIMIT 1
    """)
    fun observeFamilyById(familyId: String): Flow<Family?>

    @Query("""
        SELECT * FROM family_members
        WHERE familyId = :familyId AND isDeleted = 0
        ORDER BY updatedAt DESC, createdAt DESC
    """)
    fun observeActiveMembersByFamilyId(familyId: String): Flow<List<FamilyMember>>

    @Query("""
        SELECT COUNT(*) FROM families
        WHERE isDeleted = 0
    """)
    fun observeActiveFamilyCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM family_members
        WHERE isDeleted = 0
    """)
    fun observeActiveFamilyMemberCount(): Flow<Int>

    @Upsert
    suspend fun upsertFamily(item: Family)

    @Upsert
    suspend fun upsertFamilies(items: List<Family>)

    @Query("""
    SELECT * FROM family_members
    WHERE familyMemberId = :familyMemberId
    LIMIT 1
""")
    fun observeFamilyMemberById(familyMemberId: String): Flow<FamilyMember?>

    @Query("""
    SELECT * FROM family_members
    WHERE familyMemberId = :familyMemberId
    LIMIT 1
""")
    suspend fun getFamilyMemberById(familyMemberId: String): FamilyMember?

    @Upsert
    suspend fun upsertFamilyMember(item: FamilyMember)

    @Upsert
    suspend fun upsertFamilyMembers(items: List<FamilyMember>)

    @Query("""
        SELECT * FROM families
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun loadDirtyFamilyBatch(limit: Int): List<Family>

    @Query("""
        SELECT * FROM families
        WHERE familyId IN (:ids)
    """)
    suspend fun getFamiliesByIds(ids: List<String>): List<Family>

    @Upsert
    suspend fun upsertAllFamilies(items: List<Family>)

    @Query("""
        UPDATE families
        SET isDirty = 0,
            updatedAt = :newUpdatedAt,
            version = version + 1
        WHERE familyId IN (:ids)
    """)
    suspend fun markFamilyBatchPushed(
        ids: List<String>,
        newUpdatedAt: Timestamp
    )
    @Query("""
        UPDATE families
        SET isDeleted = 1,
            isDirty = 1,
            deletedAt = :now,
            updatedAt = :now,
            version = version + 1
        WHERE familyId = :familyId AND isDeleted = 0
    """)
    suspend fun softDeleteFamily(familyId: String, now: com.google.firebase.Timestamp)

    @Query("""
        UPDATE family_members
        SET isDeleted = 1,
            isDirty = 1,
            deletedAt = :now,
            updatedAt = :now,
            version = version + 1
        WHERE familyMemberId = :familyMemberId AND isDeleted = 0
    """)
    suspend fun softDeleteFamilyMember(familyMemberId: String, now: com.google.firebase.Timestamp)

    @Query("""
        UPDATE family_members
        SET isDeleted = 1,
            isDirty = 1,
            deletedAt = :now,
            updatedAt = :now,
            version = version + 1
        WHERE familyId = :familyId AND isDeleted = 0
    """)
    suspend fun softDeleteMembersByFamilyId(familyId: String, now: com.google.firebase.Timestamp)

    @Transaction
    suspend fun softDeleteFamilyCascade(familyId: String, now: com.google.firebase.Timestamp) {
        softDeleteFamily(familyId, now)
        softDeleteMembersByFamilyId(familyId, now)
    }

    @Query("""
        SELECT * FROM family_members
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun loadDirtyFamilyMemberBatch(limit: Int): List<FamilyMember>

    @Query("""
        SELECT * FROM family_members
        WHERE familyMemberId IN (:ids)
    """)
    suspend fun getFamilyMembersByIds(ids: List<String>): List<FamilyMember>

    @Upsert
    suspend fun upsertAllFamilyMembers(items: List<FamilyMember>)

    @Query("""
        UPDATE family_members
        SET isDirty = 0,
            updatedAt = :newUpdatedAt,
            version = version + 1
        WHERE familyMemberId IN (:ids)
    """)
    suspend fun markFamilyMemberBatchPushed(
        ids: List<String>,
        newUpdatedAt: Timestamp
    )


    @Query("""
        SELECT COUNT(*) FROM families
        WHERE isDeleted = 0
          AND TRIM(primaryContactHeadOfHousehold) = ''
    """)
    fun observeFamiliesMissingHeadCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM families
        WHERE isDeleted = 0
          AND familyId NOT IN (
              SELECT DISTINCT familyId
              FROM family_members
              WHERE isDeleted = 0
          )
    """)
    fun observeFamiliesWithNoMembersCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM family_members
        WHERE isDeleted = 0
          AND TRIM(ninNumber) = ''
    """)
    fun observeFamilyMembersMissingNinCount(): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM families
        WHERE isDeleted = 0
          AND createdAt >= :since
    """)
    fun observeRecentFamiliesCount(since: Timestamp): Flow<Int>
}