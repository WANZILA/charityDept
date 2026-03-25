package com.example.charityDept.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.local.projection.EduCount
import com.example.charityDept.data.local.projection.NameCount
import com.example.charityDept.data.model.Child
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.RegistrationStatus
import com.example.charityDept.data.model.Reply
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

data class KeyCount(
    val name: String,
    val count: Int
)
@Dao
interface ChildDao {

    // -------- Upsert / Read --------
    @Upsert
    suspend fun upsert(child: Child)

    @Upsert
    suspend fun upsertAll(children: List<Child>)

    @Query("SELECT * FROM children WHERE childId = :id LIMIT 1")
    suspend fun getById(id: String): Child?

    @Query("SELECT * FROM children WHERE childId = :id LIMIT 1")
    fun observeById(id: String): Flow<Child?>

    @Query("SELECT * FROM children")
    fun streamAllAdmin(): Flow<List<Child>>

    // -------- List/Flow helpers for repository --------
    // /// CHANGED: new helpers that power offline-first repository reads
    @Query("SELECT * FROM children WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeAllActive(): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE isDeleted = 0 AND graduated = :graduated ORDER BY updatedAt DESC")
    fun observeAllByGraduated(graduated: Reply): Flow<List<Child>>

    @Query("SELECT * FROM children WHERE isDeleted = 0")
    suspend fun getAllActive(): List<Child>

    @Query("SELECT * FROM children WHERE isDeleted = 0 AND graduated = :graduated")
    suspend fun getAllByGraduated(graduated: Reply): List<Child>

    @Query("SELECT * FROM children WHERE isDeleted = 0 AND educationPreference = :pref ORDER BY updatedAt DESC")
    fun observeByEducationPreference(pref: EducationPreference): Flow<List<Child>>

    // -------- Lists / Paging (active = not deleted) --------
    @Query(
        """
        SELECT * FROM children
        WHERE isDeleted = 0
        ORDER BY updatedAt DESC
        """
    )
    fun pagingActive(): PagingSource<Int, Child>

    // /// CHANGED: DB-side LIKE search over first+last name (and individually), only active (not deleted)
    @Query(
        """
        SELECT * FROM children
        WHERE isDeleted = 0
          AND (:needle = '' OR
               LOWER(fName || ' ' || lName) LIKE '%' || :needle || '%' OR
               LOWER(fName) LIKE '%' || :needle || '%' OR
               LOWER(lName) LIKE '%' || :needle || '%')
        ORDER BY updatedAt DESC, createdAt DESC
        """
    )
    fun pagingSearch(needle: String): PagingSource<Int, Child>

    /*=== used paging 3 used for searching and counting the children names that appear in the searcy **/
    @Query("""
SELECT * FROM children
WHERE graduated = 0
  AND (:needle == '' OR LOWER(fName || ' ' || lName) LIKE '%' || :needle || '%')
ORDER BY fName COLLATE NOCASE, lName COLLATE NOCASE
""")
    fun pageNotGraduatedByName(
        needle: String
    ): PagingSource<Int, Child>

    @Query("""
SELECT COUNT(*) FROM children
WHERE graduated = 0
  AND (:needle == '' OR LOWER(fName || ' ' || lName) LIKE '%' || :needle || '%')
""")
    fun countNotGraduatedByName(
        needle: String
    ): kotlinx.coroutines.flow.Flow<Int>

//    @Query(
//        """
//        SELECT * FROM children
//        WHERE isDeleted = 0
//          AND (fName LIKE :q OR lName LIKE :q OR oName LIKE :q)
//        ORDER BY updatedAt DESC
//        """
//    )
//    fun pagingSearch(q: String): PagingSource<Int, Child>

    @Query(
        """
        SELECT * FROM children 
        WHERE isDeleted = 0 
          AND registrationStatus = :status
        ORDER BY updatedAt DESC
        """
    )
    fun pagingByRegistrationStatus(status: RegistrationStatus): PagingSource<Int, Child>

    @Query(
        """
        SELECT * FROM children 
        WHERE isDeleted = 0 
          AND graduated = :graduated
        ORDER BY updatedAt DESC
        """
    )
    fun pagingByGraduated(graduated: Reply): PagingSource<Int, Child>

    // -------- KPI / lightweight flows --------

    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM children WHERE isDirty = 1")
    fun observeDirtyCount(): Flow<Int>

    // -------- Delta / Sync helpers --------

    // For pull: track newest record we have locally to request remote delta (updatedAt/version > lastSynced)
    @Query("SELECT MAX(updatedAt) FROM children")
    suspend fun maxUpdatedAt(): Timestamp?

    @Query("SELECT MAX(version) FROM children")
    suspend fun maxVersion(): Long?

    // For push: fetch locally modified rows in small batches
    @Query(
        """
        SELECT * FROM children
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
        """
    )
    suspend fun loadDirtyBatch(limit: Int): List<Child>

    // Bulk mark as pushed (set clean + server-authoritative version/updatedAt)
    @Query(
        """
    UPDATE children
    SET isDirty = 0,
      --  version = version + 1,
        updatedAt = :newUpdatedAt
    WHERE childId IN (:ids)
    """
    )
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)
    // Soft delete locally (tombstone + mark dirty so it pushes)
    @Query("""
UPDATE children
SET isDeleted = 1,
    isDirty = 1,
    updatedAt = :now,
    version = version + 1
WHERE childId = :id
""")
    suspend fun softDelete(id: String, now: Timestamp)

    @Query("""
  DELETE FROM children
  WHERE isDeleted = 1 AND isDirty = 0
    AND deletedAt IS NOT NULL AND deletedAt <= :cutoff
""")
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int


    // Mark/clear dirty explicitly (e.g., after local edit or conflict resolution)
    @Query("""
UPDATE children
SET isDirty = 1,
    updatedAt = :now,
    version = version + 1
WHERE childId = :id
""")
    suspend fun markDirty(id: String, now: Timestamp)
    @Query("UPDATE children SET isDirty = 0 WHERE childId IN (:ids)")
    suspend fun clearDirty(ids: List<String>)

    /// CHANGED: quick health indicator for dashboard
    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0")
    suspend fun peekLocalCount(): Int

    /// CHANGED: fetch a set of entities for conflict resolution merge
    @Query("SELECT * FROM children WHERE childId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Child>

    @Query("DELETE FROM children WHERE childId = :id")
    suspend fun hardDelete(id: String)

    @Query("""
        UPDATE children
        SET isDirty = 0,
            updatedAt = :newUpdatedAt,
            version = :newVersion
        WHERE childId = :id
    """)
    suspend fun markCleanAndBumpVersion(
        id: String,
        newUpdatedAt: Timestamp,
        newVersion: Long
    )

    @Query("""
        UPDATE children
        SET isDirty = 0,
            updatedAt = :newUpdatedAt,
            version = :newVersion
        WHERE childId = :id
    """)
    suspend fun markCleanAndBumpVersion(
        id: String,
        newUpdatedAt: Timestamp,
        newVersion: Int
    )

    @Query("SELECT * FROM children WHERE isDirty = 1 AND isDeleted = 0")
    suspend fun getAllDirty(): List<Child>

    // Totals
    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0")
    fun observeTotal(): Flow<Int>



//    @Query("SELECT * FROM children WHERE isDirty = 1 AND isDeleted = 0 ORDER BY updatedAt LIMIT :limit")
//    suspend fun loadDirtyBatch(limit: Int): List<Child>

//    @Query("UPDATE children SET isDirty = 0, updatedAt = :newUpdatedAt, version = :newVersion WHERE childId IN (:ids)")
//    suspend fun markBatchPushed(ids: List<String>, newVersion: Long, newUpdatedAt: Timestamp)

//    @Query("SELECT COUNT(*) FROM children WHERE isDirty = 1")
//    fun observeDirtyCount(): kotlinx.coroutines.flow.Flow<Int>
    //Registration incomplete (in program)
//    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0 AND registrationStatus <> 'COMPLETE'")
//    fun observeInProgram(): Flow<Int>

    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0 AND resettled = 1")
    fun observeReunited(): Flow<Int>

    // Registration incomplete (in program)
    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0 AND registrationStatus <> 'COMPLETE'")
    fun observeInProgram(): Flow<Int>

    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0 AND graduated = 'YES'")
    fun observeGraduated(): Flow<Int>

    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0 AND partnershipForEducation = 1")
    fun observeSponsored(): Flow<Int>
    // New this month (assumes createdAt stored as INTEGER millis via TypeConverter)
    @Query("""
        SELECT COUNT(*) FROM children 
        WHERE isDeleted = 0 AND createdAt BETWEEN :startMs AND :endMs
    """)
    fun observeNewThisMonth(startMs: Long, endMs: Long): Flow<Int>

    // Average age ignoring 0
    @Query("SELECT AVG(NULLIF(age, 0)) FROM children WHERE isDeleted = 0")
    fun observeAvgAge(): Flow<Double?>

    // Education distribution
    @Query("""
        SELECT educationPreference AS pref, COUNT(*) AS cnt
        FROM children 
        WHERE isDeleted = 0
        GROUP BY educationPreference
    """)
    fun observeEduDist(): Flow<List<EduCount>>

    // Top regions (normalize empty to 'Unknown')
    @Query("""
        SELECT CASE WHEN TRIM(IFNULL(region, '')) = '' THEN 'Unknown' ELSE region END AS name,
               COUNT(*) AS cnt
        FROM children
        WHERE isDeleted = 0
        GROUP BY name
        ORDER BY cnt DESC, name ASC
        LIMIT 3
    """)
    fun observeTopRegions(): Flow<List<NameCount>>

    // Top streets (normalize empty to 'Unknown')
    @Query("""
        SELECT CASE WHEN TRIM(IFNULL(street, '')) = '' THEN 'Unknown' ELSE street END AS name,
               COUNT(*) AS cnt
        FROM children
        WHERE isDeleted = 0
        GROUP BY name
        ORDER BY cnt DESC, name ASC
        LIMIT 3
    """)
    fun observeTopStreets(): Flow<List<NameCount>>

    // “Stale updates” = older than cutoff
    @Query("""
        SELECT COUNT(*) FROM children 
        WHERE isDeleted = 0 AND updatedAt < :staleCutoffMs
    """)
    fun observeStaleCount(staleCutoffMs: Long): Flow<Int>

//    // Already present in your code; keep using for the debug banner:
//    @Query("SELECT COUNT(*) FROM children WHERE isDirty = 1")
//    fun observeDirtyCount(): Flow<Int>

    // Health quick check
//    @Query("SELECT COUNT(*) FROM children WHERE isDeleted = 0")
//    suspend fun peekLocalCount(): Int
//

  //for displaying the streets and districts

    @Query("""
    SELECT TRIM(street) AS name, COUNT(*) AS count
    FROM children
    WHERE isDeleted = 0
      AND TRIM(street) != ''
    GROUP BY name
    ORDER BY count DESC, name COLLATE NOCASE
""")
    fun watchStreetCounts(): Flow<List<KeyCount>>

    @Query("""
    SELECT TRIM(member1AncestralDistrict) AS name, COUNT(*) AS count
    FROM children
    WHERE isDeleted = 0
      AND TRIM(member1AncestralDistrict) != ''
    GROUP BY name
    ORDER BY count DESC, name COLLATE NOCASE
""")
    fun watchMember1AncestralDistrictCounts(): Flow<List<KeyCount>>

    @Query("""
    SELECT COUNT(*)
    FROM children
    WHERE isDeleted = 0
""")
    fun watchTotalChildren(): Flow<Int>


}

