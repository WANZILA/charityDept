package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.charityDept.core.Utils.picker.PickerOption
import com.example.charityDept.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UgAdminDao {

    // --- reads (for pickers) LIVE ---
    @Query("SELECT * FROM ug_regions ORDER BY regionName")
    fun watchRegions(): Flow<List<UgRegionEntity>>

    @Query("SELECT * FROM ug_districts WHERE regionCode = :regionCode ORDER BY districtName")
    fun watchDistricts(regionCode: String): Flow<List<UgDistrictEntity>>

    @Query("SELECT * FROM ug_counties WHERE districtCode = :districtCode ORDER BY countyName")
    fun watchCounties(districtCode: String): Flow<List<UgCountyEntity>>

    @Query("SELECT * FROM ug_subcounties WHERE countyCode = :countyCode ORDER BY subcountyName")
    fun watchSubcounties(countyCode: String): Flow<List<UgSubCountyEntity>>

    @Query("SELECT * FROM ug_parishes WHERE subcountyCode = :subcountyCode ORDER BY parishName")
    fun watchParishes(subcountyCode: String): Flow<List<UgParishEntity>>

    @Query("SELECT * FROM ug_villages WHERE parishCode = :parishCode ORDER BY villageName")
    fun watchVillages(parishCode: String): Flow<List<UgVillageEntity>>

    // --- seed helpers (keep suspend) ---
    @Query("SELECT COUNT(*) FROM ug_regions")
    suspend fun regionsCount(): Int

    @Query("SELECT COUNT(*) FROM ug_districts")
    suspend fun districtsCount(): Int

    @Query("SELECT COUNT(*) FROM ug_counties")
    suspend fun countiesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_subcounties")
    suspend fun subcountiesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_parishes")
    suspend fun parishesCount(): Int

    @Query("SELECT COUNT(*) FROM ug_villages")
    suspend fun villagesCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegions(items: List<UgRegionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistricts(items: List<UgDistrictEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCounties(items: List<UgCountyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcounties(items: List<UgSubCountyEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParishes(items: List<UgParishEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillages(items: List<UgVillageEntity>)

    // =========================================================
    // Streets (derived from children.street) - Room only (KISS)
    // =========================================================

    // A–Z distinct streets (non-deleted children only)
    @Query("""
        SELECT DISTINCT TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        ORDER BY name COLLATE NOCASE
    """)
    fun watchStreetNames(): Flow<List<String>>

    @Query("""
        SELECT DISTINCT TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        ORDER BY name COLLATE NOCASE
    """)
    suspend fun getStreetNames(): List<String>

    // Most-used streets first (count desc), still distinct & trimmed
    @Query("""
        SELECT TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        GROUP BY name
        ORDER BY COUNT(*) DESC, name COLLATE NOCASE
    """)
    fun watchStreetNamesMostUsed(): Flow<List<String>>

    @Query("""
        SELECT TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        GROUP BY name
        ORDER BY COUNT(*) DESC, name COLLATE NOCASE
    """)
    suspend fun getStreetNamesMostUsed(): List<String>

    // Prefix search (A–Z). You can switch to "most used" by adding GROUP BY + COUNT ordering if you want.
    @Query("""
        SELECT DISTINCT TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
          AND TRIM(street) LIKE (:prefix || '%')
        ORDER BY name COLLATE NOCASE
        LIMIT :limit
    """)
    suspend fun searchStreetNamesByPrefix(prefix: String, limit: Int): List<String>

    // Picker versions (KISS: id == name)
    @Query("""
        SELECT DISTINCT TRIM(street) AS id, TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        ORDER BY name COLLATE NOCASE
    """)
    fun streetsPickerWatchAll(): Flow<List<PickerOption>>

    @Query("""
        SELECT TRIM(street) AS id, TRIM(street) AS name
        FROM children
        WHERE isDeleted = 0
          AND TRIM(street) != ''
        GROUP BY name
        ORDER BY COUNT(*) DESC, name COLLATE NOCASE
    """)
    fun streetsPickerWatchMostUsed(): Flow<List<PickerOption>>

    // =========================================================
// Member1 Ancestral District (derived from children.member1AncestralDistrict)
// =========================================================

    @Query("""
    SELECT DISTINCT TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    ORDER BY name COLLATE NOCASE
""")
    fun watchMember1AncestralDistricts(): Flow<List<String>>

    @Query("""
    SELECT DISTINCT TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    ORDER BY name COLLATE NOCASE
""")
    suspend fun getMember1AncestralDistricts(): List<String>

    @Query("""
    SELECT TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    GROUP BY name
    ORDER BY COUNT(*) DESC, name COLLATE NOCASE
""")
    fun watchMember1AncestralDistrictsMostUsed(): Flow<List<String>>

    @Query("""
    SELECT TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    GROUP BY name
    ORDER BY COUNT(*) DESC, name COLLATE NOCASE
""")
    suspend fun getMember1AncestralDistrictsMostUsed(): List<String>

    @Query("""
    SELECT DISTINCT TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
      AND TRIM(member1AncestralDistrict) LIKE (:prefix || '%')
    ORDER BY name COLLATE NOCASE
    LIMIT :limit
""")
    suspend fun searchMember1AncestralDistrictsByPrefix(prefix: String, limit: Int): List<String>

    @Query("""
    SELECT DISTINCT TRIM(member1AncestralDistrict) AS id, TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    ORDER BY name COLLATE NOCASE
""")
    fun member1AncestralDistrictPickerWatchAll(): Flow<List<PickerOption>>

    @Query("""
    SELECT TRIM(member1AncestralDistrict) AS id, TRIM(member1AncestralDistrict) AS name
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    GROUP BY name
    ORDER BY COUNT(*) DESC, name COLLATE NOCASE
""")
    fun member1AncestralDistrictPickerWatchMostUsed(): Flow<List<PickerOption>>

}

