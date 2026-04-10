// <app/src/main/java/com/example/zionkids/data/local/dao/AttendanceDao.kt>
package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.local.projection.EventAttendanceRow
import com.example.charityDept.data.model.Attendance
import com.example.charityDept.data.model.AttendanceStatus
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import com.example.charityDept.data.local.projection.EventFrequentAttendeeRow
@Dao
interface AttendanceDao {

    // ---------- Upserts ----------
    @Upsert
    suspend fun upsert(a: Attendance)

    @Upsert
    suspend fun upsertAll(list: List<Attendance>)

    @Query("SELECT * FROM attendances")
    fun streamAllAdmin(): Flow<List<Attendance>>
    // ---------- Single reads ----------
    @Query("SELECT * FROM attendances WHERE attendanceId = :id LIMIT 1")
    suspend fun getOnce(id: String): Attendance?

    // DAO
    @Query("SELECT * FROM attendances WHERE eventId = :eventId AND childId = :childId LIMIT 1")
    suspend fun getOneForChildAtEvent(eventId: String, childId: String): Attendance?


    // ---------- Lists (one-shot) ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId
        ORDER BY updatedAt DESC
    """)
    suspend fun getByEvent(eventId: String): List<Attendance>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId AND status = :status
        ORDER BY updatedAt DESC
    """)
    suspend fun getByEventAndStatus(eventId: String, status: AttendanceStatus): List<Attendance>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND childId = :childId
        ORDER BY updatedAt DESC
    """)
    suspend fun getForChild(childId: String): List<Attendance>

    // ---------- Streams (for UI) ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND eventId = :eventId
        ORDER BY updatedAt DESC
    """)
    fun observeByEvent(eventId: String): Flow<List<Attendance>>

    @Query("""
        SELECT * FROM attendances
        WHERE isDeleted = 0 AND childId = :childId
        ORDER BY updatedAt DESC
    """)
    fun observeByChild(childId: String): Flow<List<Attendance>>

    // ---------- Sync helpers ----------
    @Query("""
        SELECT * FROM attendances
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun loadDirtyBatch(limit: Int): List<Attendance>

    @Query("""
    UPDATE attendances SET
        isDirty = 0,
        version = version + 1,
        updatedAt = :newUpdatedAt
    WHERE attendanceId IN (:ids)
""")
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)

    @Query("""
        UPDATE attendances
        SET isDirty = 1,
            version  = version + 1,
            updatedAt = :now
        WHERE attendanceId = :id
    """)
    suspend fun markDirty(id: String, now: Timestamp)

    @Query("""
        UPDATE attendances
        SET isDeleted = 1,
            isDirty = 1,
            version  = version + 1,
            updatedAt = :now
        WHERE attendanceId = :id
    """)
    suspend fun softDelete(id: String, now: Timestamp)

    // ---------- Handy counters / diagnostics ----------
    @Query("SELECT COUNT(*) FROM attendances WHERE isDirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM attendances WHERE isDeleted = 0 AND eventId = :eventId")
    fun observeCountForEvent(eventId: String): Flow<Int>


    /** Total non-graduated children registered on/before cutoffMs. */
    /** Total non-graduated children registered on/before cutoffNanos. */
    @Query("""
        SELECT COUNT(*)
        FROM children c
        WHERE c.isDeleted = 0
          AND c.graduated = 0
          AND (c.createdAt IS NULL OR c.createdAt <= :cutoffNanos)
    """)
    suspend fun countTotalEligibleByCutoff(cutoffNanos: Long): Int
    /** selects the count of the children based on the eventDate **/
    /** Present count for an event among eligible (non-grad, registered on/before cutoffMs). */
//    @Query("""
//        SELECT COUNT(DISTINCT a.childId)
//        FROM attendances a
//        JOIN children c ON c.childId = a.childId
//        WHERE a.eventId = :eventId
//          AND a.status = :presentStatus
//          AND COALESCE(c.graduated, 'NO') = 'NO'
//          AND (c.createdAt IS NULL OR c.createdAt <= :cutoffMs)
//    """)
//    suspend fun countPresentEligibleForEvent(
//        eventId: String,
//        presentStatus: String,   // e.g., "PRESENT" if you store enum name
//        cutoffMs: Long
//    ): Int

    /** Present count for an event among eligible (non-grad, registered on/before cutoffNanos). */
    @Query("""
        SELECT COUNT(*)
        FROM attendances a
        INNER JOIN children c ON c.childId = a.childId
        WHERE a.eventId = :eventId
          AND a.status = :presentStatus
          AND c.isDeleted = 0
          AND c.graduated = 0
          AND (c.createdAt IS NULL OR c.createdAt <= :cutoffNanos)
    """)
    suspend fun countPresentEligibleForEvent(
        eventId: String,
        presentStatus: String,
        cutoffNanos: Long
    ): Int

    // --- Optional quick diagnostics (handy while verifying) ---
    @Query("SELECT COUNT(*) FROM children")
    suspend fun countAllChildren(): Int

    @Query("SELECT COUNT(*) FROM children WHERE COALESCE(graduated,0)=0")
    suspend fun countAllNonGraduated(): Int

    // Plain-English: mark all attendances for this child as deleted (tombstones) so other devices can see it.
    @Query(
        """
        UPDATE attendances
        SET isDeleted = 1,
            deletedAt = :now,
            isDirty = 1,
            updatedAt = :now,
            version = version + 1
        WHERE childId = :childId AND isDeleted = 0
        """
    )
    suspend fun softDeleteByChildId(childId: String, now: Timestamp)

    @Query("""
  DELETE FROM attendances
  WHERE isDeleted = 1 AND isDirty = 0
    AND deletedAt IS NOT NULL AND deletedAt <= :cutoff
""")
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int

    @Query(
        """
        DELETE FROM attendances
        WHERE childId = :childId
        """
    )
    suspend fun hardDeleteAllByChildId(childId: String)

    @Query("SELECT * FROM attendances WHERE attendanceId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Attendance>

    // <app/src/main/java/com/example/zionkids/data/local/dao/AttendanceDao.kt>
    @Query("""
    SELECT COUNT(*) FROM attendances
    WHERE isDeleted = 0 AND childId = :childId
""")
    fun observeCountForChild(childId: String): Flow<Int>

    @Query("""
    SELECT COUNT(*) FROM attendances
    WHERE isDeleted = 0 AND childId = :childId AND status = :status
""")
    fun observeCountForChildByStatus(childId: String, status: AttendanceStatus): Flow<Int>


    @Query("""
    SELECT 
        e.eventId AS eventId,
        e.title AS title,
        e.eventDate AS eventDate,
        e.teamName AS teamName,
        e.location AS location,
        a.status AS status,
        a.notes AS notes
    FROM attendances a
    JOIN events e ON e.eventId = a.eventId
    WHERE a.isDeleted = 0
      AND e.isDeleted = 0
      AND a.childId = :childId
      AND a.status = 'PRESENT'
    ORDER BY e.eventDate DESC, a.updatedAt DESC
""")
    fun observeAttendedEventsForChild(childId: String): Flow<List<EventAttendanceRow>>

    @Query("""
    SELECT
        e.eventId   AS eventId,
        e.title     AS title,
        e.eventDate AS eventDate,
        e.teamName  AS teamName,
        e.location  AS location,
        CASE
            WHEN a.attendanceId IS NULL THEN 'ABSENT'
            ELSE a.status
        END AS status,
        COALESCE(a.notes, '') AS notes
    FROM events e
    LEFT JOIN attendances a
        ON a.eventId = e.eventId
       AND a.childId = :childId
       AND a.isDeleted = 0
    WHERE e.isDeleted = 0
      AND (
          a.attendanceId IS NULL      -- no row recorded => treat as missed
          OR a.status = 'ABSENT'      -- explicitly marked absent
      )
    ORDER BY e.eventDate DESC,
             COALESCE(a.updatedAt, e.updatedAt) DESC
""")
    fun observeMissedEventsForChildWithReason(childId: String): Flow<List<EventAttendanceRow>>

    @Query("""
    SELECT 
        e.eventId AS eventId,
        e.title AS title,
        e.eventDate AS eventDate,
        e.teamName AS teamName,
        e.location AS location,
        a.status AS status,
        a.notes AS notes
    FROM attendances a
    JOIN events e ON e.eventId = a.eventId
    WHERE a.isDeleted = 0
      AND e.isDeleted = 0
      AND a.childId = :childId
      AND (
           a.status = 'PRESENT'
        OR a.status = 'EXCUSED'
        OR (a.status = 'ABSENT' AND TRIM(IFNULL(a.notes, '')) != '')
      )
    ORDER BY e.eventDate DESC, a.updatedAt DESC
""")
    fun observeAllRecordedEventsForChild(childId: String): Flow<List<EventAttendanceRow>>

    @Query("""
        SELECT
            c.childId AS childId,
            c.fName AS fName,
            c.lName AS lName,
            COUNT(a.attendanceId) AS presentCount
        FROM attendances a
        JOIN children c ON c.childId = a.childId
        WHERE a.isDeleted = 0
          AND c.isDeleted = 0
          AND a.status = 'PRESENT'
          AND a.eventId IN (:eventIds)
        GROUP BY c.childId, c.fName, c.lName
        ORDER BY presentCount DESC, c.fName ASC, c.lName ASC
    """)
    fun observeFrequentAttendeesForEvents(eventIds: List<String>): Flow<List<EventFrequentAttendeeRow>>

    // /// CHANGED: cascade tombstone by eventId (mirror childId cascade)
    @Query("""
    UPDATE attendances SET
        isDeleted = 1,
        isDirty   = 1,
        deletedAt = :now,
        updatedAt = :now,
        version   = version + 1
    WHERE eventId = :eventId
""")
    suspend fun softDeleteByEventId(eventId: String, now: Timestamp)


}

