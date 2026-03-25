// <app/src/main/java/com/example/zionkids/data/local/dao/EventDao.kt>
// /// CHANGED: Add loadDirtyBatch(limit) used by EventSyncWorker (alias of existing dirty query).
// /// CHANGED: Add markBatchPushed(ids, newVersion, newUpdatedAt: Timestamp) to clear dirty & bump version/time.
// /// CHANGED: Update softDelete to accept Firestore Timestamp (avoid Long) to keep Timestamp-only flow.
// /// CHANGED: Keep all existing APIs; no renames, minimal diff.

package com.example.charityDept.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.model.Event
import kotlinx.coroutines.flow.Flow
import com.google.firebase.Timestamp  // /// CHANGED: use Firestore Timestamp in DAO params

@Dao
interface EventDao {

    @Query("SELECT COUNT(*) FROM events WHERE isDeleted = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM events")
    fun streamAllAdmin(): Flow<List<Event>>

    // --- Observability / Paging (unchanged) ---
    @Query("""
        SELECT * FROM events
        WHERE isDeleted = 0
        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
    """)
    fun pagingActive(): PagingSource<Int, Event>

    // /// CHANGED: Simple LIKE-based search across common fields; uses existing indices on title/teamName.
    @Query("""
        SELECT * FROM events
        WHERE isDeleted = 0 AND (
            title      LIKE :needle ESCAPE '\' COLLATE NOCASE OR
            teamName   LIKE :needle ESCAPE '\' COLLATE NOCASE OR
            location   LIKE :needle ESCAPE '\' COLLATE NOCASE
        )
        ORDER BY eventDate DESC, updatedAt DESC, createdAt DESC
    """)
    fun pagingSearch(needle: String): PagingSource<Int, Event>

    @Query("""
        SELECT * FROM events
        WHERE isDeleted = 0
        ORDER BY eventDate DESC  --, createdAt DESC, updatedAt DESC
    """)
    fun observeAllActive(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    fun observeById(id: String): Flow<Event?>

    @Query("SELECT * FROM events WHERE eventId = :id LIMIT 1")
    suspend fun getOnce(id: String): Event?

    // --- Delta pulls (unchanged; Room will convert Timestamp fields via TypeConverters) ---
    @Query("""
        SELECT * FROM events
        WHERE (updatedAt > :afterUpdatedAtMillis) OR (version > :afterVersion)
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun getSince(afterUpdatedAtMillis: Long, afterVersion: Long, limit: Int): List<Event>

    // --- Dirty rows for push (existing; keep) ---
    @Query("""
        SELECT * FROM events
        WHERE isDirty = 1
        ORDER BY updatedAt ASC
        LIMIT :limit
    """)
    suspend fun getDirty(limit: Int): List<Event>

    // /// CHANGED: Add alias used by EventSyncWorker; delegates to the same SQL.
//    @Query("""
//        SELECT * FROM events
//        WHERE isDirty = 1
//        ORDER BY updatedAt ASC
//        LIMIT :limit
//    """)
//    suspend fun loadDirtyBatch(limit: Int): List<Event>
    @Query("""SELECT * FROM events WHERE isDirty = 1 ORDER BY updatedAt ASC LIMIT :limit""")
    suspend fun loadDirtyBatch(limit: Int): List<Event>

    @Query("SELECT COUNT(*) FROM events WHERE isDirty = 1")
    fun observeDirtyCount(): Flow<Int>

    // --- Upserts (unchanged) ---
    @Upsert
    suspend fun upsertAll(items: List<Event>)
//    @Upsert
//    suspend fun upsertAll(children: List<Child>)


    @Upsert
    suspend fun upsertOne(item: Event)

    @Query("UPDATE events SET isDirty = :dirty WHERE eventId IN (:ids)")
    suspend fun setDirty(ids: List<String>, dirty: Boolean)


    // Hard delete (unchanged)
    @Query("DELETE FROM events WHERE eventId = :id")
    suspend fun hardDelete(id: String)

    // /// CHANGED: markBatchPushed used by EventSyncWorker to finalize a successful push.
    // ///          Sets isDirty=false, bumps version, and writes updatedAt as Firestore Timestamp.
    @Query("""
    UPDATE events SET
        isDirty = 0,
        version = version + 1,
        updatedAt = :newUpdatedAt
    WHERE eventId IN (:ids)
""")
    suspend fun markBatchPushed(ids: List<String>, newUpdatedAt: Timestamp)

    // <app/src/main/java/com/example/zionkids/data/local/dao/EventDao.kt>
// /// CHANGED: batch fetch for pull-merge
    @Query("SELECT * FROM events WHERE eventId IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<Event>

    // <app/src/main/java/com/example/zionkids/data/local/dao/EventDao.kt>

//    @Query("""
//    UPDATE events SET
//        isDeleted = 1,
//        isDirty   = 1,
//        deletedAt = :now,
//        updatedAt = :now,
//        version   = version + 1
//    WHERE eventId = :id
//""")
//    suspend fun softDelete(id: String, now: Timestamp)

    // /// CHANGED: cascade tombstone by eventId (mirror childId cascade)
    @Query(
        """
    UPDATE events
    SET isDeleted = 1,
        deletedAt = :now,
        isDirty = 1,
        updatedAt = :now,
        version = version + 1
    WHERE eventId = :eventId AND isDeleted = 0
    """
    )
    suspend fun softDeleteByEventId(eventId: String, now: Timestamp)

    @Query(
        """
    UPDATE events
    SET isDeleted = 1,
        deletedAt = :now,
        isDirty = 1,
        updatedAt = :now,
        version = version + 1
    WHERE  eventParentId = :eventId AND isDeleted = 0
    """
    )
    suspend fun softDeleteChildByEventId(eventId: String, now: Timestamp)

    @Query("""
  DELETE FROM events
  WHERE isDeleted = 1 AND isDirty = 0
    AND deletedAt IS NOT NULL AND deletedAt <= :cutoff
""")
    suspend fun hardDeleteOldTombstones(cutoff: Timestamp): Int

    @Query("""
    SELECT * FROM events
    WHERE isDeleted = 0 AND isChild = 0
    ORDER BY eventDate DESC, updatedAt DESC
""")
    fun observeParentCandidates(): Flow<List<Event>>


}


