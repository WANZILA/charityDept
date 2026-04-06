package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.core.utils.GenerateId
import com.example.charityDept.data.local.dao.FamilyDao
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.charityDept.core.sync.family.FamilySyncScheduler
import com.example.charityDept.core.sync.familymember.FamilyMemberSyncScheduler

data class FamilySnapshot(
    val families: List<Family>,
    val totalFamilies: Int,
    val totalMembers: Int
)

data class FamilyDetailsSnapshot(
    val family: Family?,
    val members: List<FamilyMember> = emptyList()
)

data class FamilyDashboardSnapshot(
    val totalFamilies: Int = 0,
    val totalMembers: Int = 0,
    val familiesWithNoMembers: Int = 0,
    val familiesMissingHead: Int = 0,
    val membersMissingNin: Int = 0,
    val recentFamilies: Int = 0
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
    private val familyDao: FamilyDao,
    @ApplicationContext private val appContext: Context
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
        val id = member.familyMemberId.ifBlank { GenerateId.generateId("familyMember") }

        val toSave = member.copy(
            familyMemberId = id,
            createdAt = if (isNew) member.createdAt.takeIf { it != null } ?: now else member.createdAt,
            updatedAt = now,
            isDirty = true,
            isDeleted = false,
            version = if (isNew) (member.version + 1).coerceAtLeast(1) else (member.version + 1).coerceAtLeast(1)
        )

        familyDao.upsertFamilyMember(toSave)
        FamilyMemberSyncScheduler.enqueuePushNow(appContext)
        return id
    }

    override suspend fun softDeleteFamilyMember(familyMemberId: String) {
        familyDao.softDeleteFamilyMember(familyMemberId, Timestamp.now())
        FamilyMemberSyncScheduler.enqueuePushNow(appContext)
    }

    override fun observeMembersByFamily(familyId: String): Flow<List<FamilyMember>> =
        familyDao.observeActiveMembersByFamilyId(familyId)

    override suspend fun upsertFamily(family: Family, isNew: Boolean): String {
        val now = Timestamp.now()
        val id = family.familyId.ifBlank { GenerateId.generateId("family") }

        val toSave = family.copy(
            familyId = id,
            createdAt = if (isNew) family.createdAt else family.createdAt,
            updatedAt = now,
            isDirty = true,
            isDeleted = false,
            version = (family.version + 1).coerceAtLeast(1)
        )

        familyDao.upsertFamily(toSave)
        FamilySyncScheduler.enqueuePushNow(appContext)
        return id
    }

    override suspend fun softDeleteFamily(familyId: String) {
        familyDao.softDeleteFamilyCascade(familyId, Timestamp.now())
        FamilySyncScheduler.enqueuePushNow(appContext)
    }
}