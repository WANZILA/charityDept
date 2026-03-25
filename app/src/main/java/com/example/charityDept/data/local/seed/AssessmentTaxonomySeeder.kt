package com.example.charityDept.data.local.seed

import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
import com.example.charityDept.data.model.AssessmentTaxonomy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AssessmentTaxonomySeeder(
    private val dao: AssessmentTaxonomyDao
) {
    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
        if (dao.countAll() > 0) return@withContext
  //sample testing
        val seeded = listOf(
            // OBS
            AssessmentTaxonomy(
                taxonomyId = "OBS_GENERAL",
                categoryKey = "OBS", categoryLabel = "Observation",
                subCategoryKey = "OBS_GENERAL", subCategoryLabel = "General",
                indexNum = 10
            ),
            AssessmentTaxonomy(
                taxonomyId = "OBS_SPIRITUAL",
                categoryKey = "OBS", categoryLabel = "Observation",
                subCategoryKey = "OBS_SPIRITUAL", subCategoryLabel = "Spiritual",
                indexNum = 20
            ),
            AssessmentTaxonomy(
                taxonomyId = "OBS_PHYSICAL",
                categoryKey = "OBS", categoryLabel = "Observation",
                subCategoryKey = "OBS_PHYSICAL", subCategoryLabel = "Physical",
                indexNum = 30
            ),

            // QA
            AssessmentTaxonomy(
                taxonomyId = "QA_FAMILY_BACKGROUND",
                categoryKey = "QA", categoryLabel = "Questions",
                subCategoryKey = "QA_FAMILY_BACKGROUND", subCategoryLabel = "FAMILY AND BACKGROUND",
                indexNum = 110
            ),
            AssessmentTaxonomy(
                taxonomyId = "QA_NEEDS_PRIORITIES",
                categoryKey = "QA", categoryLabel = "Questions",
                subCategoryKey = "QA_NEEDS_PRIORITIES", subCategoryLabel = "ABOUT NEEDS AND PRIORITIES",
                indexNum = 120
            ),
            AssessmentTaxonomy(
                taxonomyId = "QA_HEALTH",
                categoryKey = "QA", categoryLabel = "Questions",
                subCategoryKey = "QA_HEALTH", subCategoryLabel = "HEALTH",
                indexNum = 130
            ),
            AssessmentTaxonomy(
                taxonomyId = "QA_EDUCATION",
                categoryKey = "QA", categoryLabel = "Questions",
                subCategoryKey = "QA_EDUCATION", subCategoryLabel = "EDUCATION",
                indexNum = 140
            )
        )

        dao.upsertAll(seeded)
    }
}

