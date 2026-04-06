package com.example.charityDept.domain.repositories.offline

import com.example.charityDept.core.utils.picker.PickerOption
import com.example.charityDept.data.model.*
import kotlinx.coroutines.flow.Flow
import com.example.charityDept.data.local.dao.DistrictRegionLookup
import com.example.charityDept.data.local.dao.VillageHierarchyLookup
interface OfflineUgAdminRepository {

    // LIVE picker streams
    fun watchRegions(): Flow<List<UgRegionEntity>>
    fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>>
    fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>>
    fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>>
    fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>>
    fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>>

    suspend fun getDistrictRegionByDistrictCode(districtCode: String): DistrictRegionLookup?
    suspend fun getVillageHierarchyByVillageCode(villageCode: String): VillageHierarchyLookup?


    fun watchAllDistricts(): Flow<List<UgDistrictEntity>>
    fun watchAllVillages(): Flow<List<UgVillageEntity>>
    // seed helpers
    suspend fun regionsCount(): Int
    suspend fun districtsCount(): Int
    suspend fun countiesCount(): Int
    suspend fun subcountiesCount(): Int
    suspend fun parishesCount(): Int
    suspend fun villagesCount(): Int

    suspend fun insertRegions(items: List<UgRegionEntity>)
    suspend fun insertDistricts(items: List<UgDistrictEntity>)
    suspend fun insertCounties(items: List<UgCountyEntity>)
    suspend fun insertSubcounties(items: List<UgSubCountyEntity>)
    suspend fun insertParishes(items: List<UgParishEntity>)
    suspend fun insertVillages(items: List<UgVillageEntity>)

    // =========================
    // Streets from children.street (Room only)
    // =========================
    fun watchStreetNames(): Flow<List<String>>
    suspend fun getStreetNames(): List<String>

    fun watchStreetNamesMostUsed(): Flow<List<String>>
    suspend fun getStreetNamesMostUsed(): List<String>

    fun streetsPickerWatchAll(): Flow<List<PickerOption>>
    fun streetsPickerWatchMostUsed(): Flow<List<PickerOption>>

    suspend fun searchStreetNamesByPrefix(prefix: String, limit: Int = 20): List<String>

    // =========================
// Member1 Ancestral District (Room only)
// =========================
    fun watchMember1AncestralDistricts(): Flow<List<String>>
    suspend fun getMember1AncestralDistricts(): List<String>

    fun watchMember1AncestralDistrictsMostUsed(): Flow<List<String>>
    suspend fun getMember1AncestralDistrictsMostUsed(): List<String>

    fun member1AncestralDistrictPickerWatchAll(): Flow<List<PickerOption>>
    fun member1AncestralDistrictPickerWatchMostUsed(): Flow<List<PickerOption>>

    suspend fun searchMember1AncestralDistrictsByPrefix(prefix: String, limit: Int = 20): List<String>

}

