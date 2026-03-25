package com.example.charityDept.data.local.seed

//package com.example.charityDept.data.seed

import android.content.Context
import com.example.charityDept.data.local.dao.UgAdminDao
//import com.example.charityDept.data.dao.UgAdminDao
import com.example.charityDept.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UgAdminSeeder(
    private val context: Context,
    private val dao: UgAdminDao
) {
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.regionsCount() > 0) return@withContext

        dao.insertRegions(readRegions("ug_admin/ug_regions.csv"))
        dao.insertDistricts(readDistricts("ug_admin/ug_2022_districts_updated.csv"))
        dao.insertCounties(readCounties("ug_admin/ug_2022_counties.csv"))
        dao.insertSubcounties(readSubcounties("ug_admin/ug_2022_subcounties.csv"))
        dao.insertParishes(readParishes("ug_admin/ug_2022_parishes.csv"))
        dao.insertVillages(readVillages("ug_admin/ug_2022_villages.csv"))
    }

    private fun readLines(assetPath: String): List<String> =
        context.assets.open(assetPath).bufferedReader().useLines { it.toList() }
            .drop(1) // drop header
            .filter { it.isNotBlank() }

    private fun readRegions(path: String): List<UgRegionEntity> =
        readLines(path).map { line ->
            val (code, name) = line.split(",", limit = 2)
            UgRegionEntity(regionCode = code.trim(), regionName = name.trim())
        }

    private fun readDistricts(path: String): List<UgDistrictEntity> =
        readLines(path).map { line ->
            val parts = line.split(",", limit = 3)
            UgDistrictEntity(
                districtCode = parts[0].trim(),
                districtName = parts[1].trim(),
                regionCode = parts[2].trim()
            )
        }

    private fun readCounties(path: String): List<UgCountyEntity> =
        readLines(path).map { line ->
            val parts = line.split(",", limit = 3)
            UgCountyEntity(
                countyCode = parts[0].trim(),
                countyName = parts[1].trim(),
                districtCode = parts[2].trim()
            )
        }

    private fun readSubcounties(path: String): List<UgSubCountyEntity> =
        readLines(path).map { line ->
            val parts = line.split(",", limit = 3)
            UgSubCountyEntity(
                subCountyCode = parts[0].trim(),
                subCountyName = parts[1].trim(),
                countyCode = parts[2].trim()
            )
        }

    private fun readParishes(path: String): List<UgParishEntity> =
        readLines(path).map { line ->
            val parts = line.split(",", limit = 3)
            UgParishEntity(
                parishCode = parts[0].trim(),
                parishName = parts[1].trim(),
                subCountyCode = parts[2].trim()
            )
        }

    private fun readVillages(path: String): List<UgVillageEntity> =
        readLines(path).map { line ->
            val parts = line.split(",", limit = 3)
            UgVillageEntity(
                villageCode = parts[0].trim(),
                villageName = parts[1].trim(),
                parishCode = parts[2].trim()
            )
        }
}

