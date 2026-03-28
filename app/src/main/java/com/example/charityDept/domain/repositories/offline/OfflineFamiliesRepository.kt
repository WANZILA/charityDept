package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.core.Utils.GenerateId
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class FamilySnapshot(
    val families: List<Family>,
    val totalFamilies: Int,
    val totalMembers: Int
)

data class FamilyDetailsSnapshot(
    val family: Family?,
    val members: List<FamilyMember> = emptyList()
)
interface OfflineFamiliesRepository {
    fun streamFamilies(): Flow<FamilySnapshot>
    fun observeFamily(familyId: String): Flow<Family?>
    fun observeFamilyDetails(familyId: String): Flow<FamilyDetailsSnapshot>
    fun observeMembersByFamily(familyId: String): Flow<List<FamilyMember>>
    suspend fun upsertFamily(family: Family, isNew: Boolean): String
    suspend fun softDeleteFamily(familyId: String)

    fun observeFamilyMember(familyMemberId: String): Flow<FamilyMember?>

    suspend fun upsertFamilyMember(
        member: FamilyMember,
        isNew: Boolean
    ): String

    suspend fun softDeleteFamilyMember(familyMemberId: String)
}

@Singleton
class OfflineFamiliesRepositoryImpl @Inject constructor(
    private val familyDao: FamilyDao
) : OfflineFamiliesRepository {

    override fun streamFamilies(): Flow<FamilySnapshot> =
        combine(
            familyDao.observeAllActiveFamilies(),
            familyDao.observeActiveFamilyCount(),
            familyDao.observeActiveFamilyMemberCount()
        ) { families, totalFamilies, totalMembers ->
            FamilySnapshot(
                families = families,
                totalFamilies = totalFamilies,
                totalMembers = totalMembers
            )
        }

    override fun observeFamily(familyId: String): Flow<Family?> =
        familyDao.observeFamilyById(familyId)

    override fun observeFamilyDetails(familyId: String): Flow<FamilyDetailsSnapshot> =
        combine(
            familyDao.observeFamilyById(familyId),
            familyDao.observeActiveMembersByFamilyId(familyId)
        ) { family, members ->
            FamilyDetailsSnapshot(
                family = family,
                members = members
            )
        }

    override fun observeFamilyMember(familyMemberId: String): Flow<FamilyMember?> =
        familyDao.observeFamilyMemberById(familyMemberId)

    override suspend fun upsertFamilyMember(
        member: FamilyMember,
        isNew: Boolean
    ): String {
        val now = Timestamp.now()
        val id = member.familyMemberId.ifBlank { "family_member_${now.seconds}_${now.nanoseconds}" }

        val toSave = member.copy(
            familyMemberId = id,
            createdAt = if (isNew) now else member.createdAt,
            updatedAt = now,
            isDirty = true,
            isDeleted = false,
            version = if (isNew) 1L else member.version
        )

        familyDao.upsertFamilyMember(toSave)
        return id
    }

    override suspend fun softDeleteFamilyMember(familyMemberId: String) {
        familyDao.softDeleteFamilyMember(familyMemberId, Timestamp.now())
    }

    override fun observeMembersByFamily(familyId: String): Flow<List<FamilyMember>> =
        familyDao.observeActiveMembersByFamilyId(familyId)

    override suspend fun upsertFamily(family: Family, isNew: Boolean): String {
        val now = Timestamp.now()
        val id = GenerateId.generateId("family")

        val toSave = family.copy(
            familyId = id,
            createdAt = if (isNew) now else family.createdAt,
            updatedAt = now,
            isDirty = true,
            isDeleted = false,
            version = (family.version + 1).coerceAtLeast(1)
        )

        familyDao.upsertFamily(toSave)
        return id
    }

    override suspend fun softDeleteFamily(familyId: String) {
        familyDao.softDeleteFamilyCascade(familyId, Timestamp.now())
    }
}