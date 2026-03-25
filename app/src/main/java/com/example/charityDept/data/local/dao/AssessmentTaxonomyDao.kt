package com.example.charityDept.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.charityDept.data.model.AssessmentTaxonomy
import kotlinx.coroutines.flow.Flow

@Dao
interface AssessmentTaxonomyDao {

    @Upsert
    suspend fun upsert(item: AssessmentTaxonomy)

    @Upsert
    suspend fun upsertAll(items: List<AssessmentTaxonomy>)

    @Query("SELECT COUNT(*) FROM assessment_taxonomy")
    suspend fun countAll(): Int

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDeleted = 0 AND isActive = 1
        ORDER BY indexNum ASC
    """)
    fun observeActiveAll(): Flow<List<AssessmentTaxonomy>>

    @Query("""
        SELECT * FROM assessment_taxonomy
        WHERE isDeleted = 0 AND isActive = 1
        ORDER BY indexNum ASC
    """)
    suspend fun getActiveOnce(): List<AssessmentTaxonomy>
}

