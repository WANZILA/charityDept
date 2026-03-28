package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
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
}