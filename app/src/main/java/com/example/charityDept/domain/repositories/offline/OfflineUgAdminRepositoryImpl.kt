package com.example.charityDept.data.repositories.offline

import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.local.dao.UgAdminDao
import com.example.charityDept.data.model.*
import com.example.charityDept.domain.repositories.offline.OfflineUgAdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import com.example.charityDept.data.local.dao.DistrictRegionLookup
import com.example.charityDept.data.local.dao.VillageHierarchyLookup
class OfflineUgAdminRepositoryImpl @Inject constructor(
    private val dao: UgAdminDao
) : OfflineUgAdminRepository {

    override fun watchRegions(): Flow<List<UgRegionEntity>> = dao.watchRegions()
    override fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>> = dao.watchDistricts(regionCode)
    override fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>> = dao.watchCounties(districtCode)
    override fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>> = dao.watchSubcounties(countyCode)
    override fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>> = dao.watchParishes(subcountyCode)
    override fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>> = dao.watchVillages(parishCode)

    override fun watchAllDistricts(): Flow<List<UgDistrictEntity>> = dao.watchAllDistricts()
    override fun watchAllVillages(): Flow<List<UgVillageEntity>> = dao.watchAllVillages()

    override suspend fun regionsCount(): Int = dao.regionsCount()
    override suspend fun districtsCount(): Int = dao.districtsCount()
    override suspend fun countiesCount(): Int = dao.countiesCount()
    override suspend fun subcountiesCount(): Int = dao.subcountiesCount()
    override suspend fun parishesCount(): Int = dao.parishesCount()
    override suspend fun villagesCount(): Int = dao.villagesCount()

    override suspend fun insertRegions(items: List<UgRegionEntity>) = dao.insertRegions(items)
    override suspend fun insertDistricts(items: List<UgDistrictEntity>) = dao.insertDistricts(items)
    override suspend fun insertCounties(items: List<UgCountyEntity>) = dao.insertCounties(items)
    override suspend fun insertSubcounties(items: List<UgSubCountyEntity>) = dao.insertSubcounties(items)
    override suspend fun insertParishes(items: List<UgParishEntity>) = dao.insertParishes(items)
    override suspend fun insertVillages(items: List<UgVillageEntity>) = dao.insertVillages(items)


    override suspend fun getDistrictRegionByDistrictCode(districtCode: String): DistrictRegionLookup? {
        val code = districtCode.trim()
        if (code.isEmpty()) return null
        return dao.getDistrictRegionByDistrictCode(code)
    }

    override suspend fun getVillageHierarchyByVillageCode(villageCode: String): VillageHierarchyLookup? {
        val code = villageCode.trim()
        if (code.isEmpty()) return null
        return dao.getVillageHierarchyByVillageCode(code)
    }

    // =========================
    // Streets from children.street (Room only)
    // =========================

    override fun watchStreetNames(): Flow<List<String>> = dao.watchStreetNames()
    override suspend fun getStreetNames(): List<String> = dao.getStreetNames()

    override fun watchStreetNamesMostUsed(): Flow<List<String>> = dao.watchStreetNamesMostUsed()
    override suspend fun getStreetNamesMostUsed(): List<String> = dao.getStreetNamesMostUsed()

    override fun streetsPickerWatchAll(): Flow<List<PickerOption>> = dao.streetsPickerWatchAll()
    override fun streetsPickerWatchMostUsed(): Flow<List<PickerOption>> = dao.streetsPickerWatchMostUsed()

    override suspend fun searchStreetNamesByPrefix(prefix: String, limit: Int): List<String> {
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        return dao.searchStreetNamesByPrefix(p, limit)
    }

    // =========================
// Member1 Ancestral District (Room only)
// =========================

    override fun watchMember1AncestralDistricts(): Flow<List<String>> =
        dao.watchMember1AncestralDistricts()

    override suspend fun getMember1AncestralDistricts(): List<String> =
        dao.getMember1AncestralDistricts()

    override fun watchMember1AncestralDistrictsMostUsed(): Flow<List<String>> =
        dao.watchMember1AncestralDistrictsMostUsed()

    override suspend fun getMember1AncestralDistrictsMostUsed(): List<String> =
        dao.getMember1AncestralDistrictsMostUsed()

    override fun member1AncestralDistrictPickerWatchAll(): Flow<List<PickerOption>> =
        dao.member1AncestralDistrictPickerWatchAll()

    override fun member1AncestralDistrictPickerWatchMostUsed(): Flow<List<PickerOption>> =
        dao.member1AncestralDistrictPickerWatchMostUsed()

    override suspend fun searchMember1AncestralDistrictsByPrefix(prefix: String, limit: Int): List<String> {
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        return dao.searchMember1AncestralDistrictsByPrefix(p, limit)
    }

}

