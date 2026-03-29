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

        val seeded = listOf(
            // FAMILY NEEDS ASSESSMENT
            AssessmentTaxonomy(
                taxonomyId = "FAMILY_NEEDS_OBS_GENERAL",
                assessmentKey = "FAMILY_NEEDS",
                assessmentLabel = "Family Needs Assessment",
                categoryKey = "OBS",
                categoryLabel = "Observation",
                subCategoryKey = "FN_OBS_GENERAL",
                subCategoryLabel = "General Observation",
                indexNum = 10
            ),
            AssessmentTaxonomy(
                taxonomyId = "FAMILY_NEEDS_QA_HOUSING",
                assessmentKey = "FAMILY_NEEDS",
                assessmentLabel = "Family Needs Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "FN_QA_HOUSING",
                subCategoryLabel = "Housing & Environment",
                indexNum = 20
            ),
            AssessmentTaxonomy(
                taxonomyId = "FAMILY_NEEDS_QA_HEALTH",
                assessmentKey = "FAMILY_NEEDS",
                assessmentLabel = "Family Needs Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "FN_QA_HEALTH",
                subCategoryLabel = "Health & Nutrition",
                indexNum = 30
            ),
            AssessmentTaxonomy(
                taxonomyId = "FAMILY_NEEDS_QA_CHILD_PROTECTION",
                assessmentKey = "FAMILY_NEEDS",
                assessmentLabel = "Family Needs Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "FN_QA_CHILD_PROTECTION",
                subCategoryLabel = "Child Protection & Education",
                indexNum = 40
            ),
            AssessmentTaxonomy(
                taxonomyId = "FAMILY_NEEDS_QA_ACTION_PLAN",
                assessmentKey = "FAMILY_NEEDS",
                assessmentLabel = "Family Needs Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "FN_QA_ACTION_PLAN",
                subCategoryLabel = "Summary & Action Plan",
                indexNum = 50
            ),

            // SKILLING ASSESSMENT
            AssessmentTaxonomy(
                taxonomyId = "SKILLING_OBS_GENERAL",
                assessmentKey = "SKILLING",
                assessmentLabel = "Skilling Assessment",
                categoryKey = "OBS",
                categoryLabel = "Observation",
                subCategoryKey = "SK_OBS_GENERAL",
                subCategoryLabel = "General Observation",
                indexNum = 110
            ),
            AssessmentTaxonomy(
                taxonomyId = "SKILLING_QA_APTITUDE",
                assessmentKey = "SKILLING",
                assessmentLabel = "Skilling Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SK_QA_APTITUDE",
                subCategoryLabel = "Interest & Aptitude Profile",
                indexNum = 120
            ),
            AssessmentTaxonomy(
                taxonomyId = "SKILLING_QA_PSYCHOSOCIAL",
                assessmentKey = "SKILLING",
                assessmentLabel = "Skilling Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SK_QA_PSYCHOSOCIAL",
                subCategoryLabel = "Psychosocial Assessment",
                indexNum = 130
            ),
            AssessmentTaxonomy(
                taxonomyId = "SKILLING_QA_BARRIERS",
                assessmentKey = "SKILLING",
                assessmentLabel = "Skilling Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SK_QA_BARRIERS",
                subCategoryLabel = "Barriers to Participation",
                indexNum = 140
            ),
            AssessmentTaxonomy(
                taxonomyId = "SKILLING_QA_PLACEMENT",
                assessmentKey = "SKILLING",
                assessmentLabel = "Skilling Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SK_QA_PLACEMENT",
                subCategoryLabel = "Suggested Placement & Goals",
                indexNum = 150
            ),

            // SOCIAL WORK ASSESSMENT
            AssessmentTaxonomy(
                taxonomyId = "SOCIAL_WORK_OBS_GENERAL",
                assessmentKey = "SOCIAL_WORK",
                assessmentLabel = "Social Work Assessment",
                categoryKey = "OBS",
                categoryLabel = "Observation",
                subCategoryKey = "SW_OBS_GENERAL",
                subCategoryLabel = "General Observation",
                indexNum = 210
            ),
            AssessmentTaxonomy(
                taxonomyId = "SOCIAL_WORK_QA_FAMILY_BACKGROUND",
                assessmentKey = "SOCIAL_WORK",
                assessmentLabel = "Social Work Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SW_QA_FAMILY_BACKGROUND",
                subCategoryLabel = "Family and Background",
                indexNum = 220
            ),
            AssessmentTaxonomy(
                taxonomyId = "SOCIAL_WORK_QA_NEEDS_PRIORITIES",
                assessmentKey = "SOCIAL_WORK",
                assessmentLabel = "Social Work Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SW_QA_NEEDS_PRIORITIES",
                subCategoryLabel = "About Needs and Priorities",
                indexNum = 230
            ),
            AssessmentTaxonomy(
                taxonomyId = "SOCIAL_WORK_QA_HEALTH",
                assessmentKey = "SOCIAL_WORK",
                assessmentLabel = "Social Work Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SW_QA_HEALTH",
                subCategoryLabel = "Health",
                indexNum = 240
            ),
            AssessmentTaxonomy(
                taxonomyId = "SOCIAL_WORK_QA_EDUCATION",
                assessmentKey = "SOCIAL_WORK",
                assessmentLabel = "Social Work Assessment",
                categoryKey = "QA",
                categoryLabel = "Questions",
                subCategoryKey = "SW_QA_EDUCATION",
                subCategoryLabel = "Education",
                indexNum = 250
            )
        )

        dao.upsertAll(seeded)
    }
}
//package com.example.charityDept.data.local.seed
//
//import com.example.charityDept.data.local.dao.AssessmentTaxonomyDao
//import com.example.charityDept.data.model.AssessmentTaxonomy
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//
//class AssessmentTaxonomySeeder(
//    private val dao: AssessmentTaxonomyDao
//) {
//    suspend fun seedIfEmpty() = withContext(Dispatchers.IO) {
//        if (dao.countAll() > 0) return@withContext
//  //sample testing
//        val seeded = listOf(
//            // OBS
//            AssessmentTaxonomy(
//                taxonomyId = "OBS_GENERAL",
//                categoryKey = "OBS", categoryLabel = "Observation",
//                subCategoryKey = "OBS_GENERAL", subCategoryLabel = "General",
//                indexNum = 10
//            ),
//            AssessmentTaxonomy(
//                taxonomyId = "OBS_SPIRITUAL",
//                categoryKey = "OBS", categoryLabel = "Observation",
//                subCategoryKey = "OBS_SPIRITUAL", subCategoryLabel = "Spiritual",
//                indexNum = 20
//            ),
//            AssessmentTaxonomy(
//                taxonomyId = "OBS_PHYSICAL",
//                categoryKey = "OBS", categoryLabel = "Observation",
//                subCategoryKey = "OBS_PHYSICAL", subCategoryLabel = "Physical",
//                indexNum = 30
//            ),
//
//            // QA
//            AssessmentTaxonomy(
//                taxonomyId = "QA_FAMILY_BACKGROUND",
//                categoryKey = "QA", categoryLabel = "Questions",
//                subCategoryKey = "QA_FAMILY_BACKGROUND", subCategoryLabel = "FAMILY AND BACKGROUND",
//                indexNum = 110
//            ),
//            AssessmentTaxonomy(
//                taxonomyId = "QA_NEEDS_PRIORITIES",
//                categoryKey = "QA", categoryLabel = "Questions",
//                subCategoryKey = "QA_NEEDS_PRIORITIES", subCategoryLabel = "ABOUT NEEDS AND PRIORITIES",
//                indexNum = 120
//            ),
//            AssessmentTaxonomy(
//                taxonomyId = "QA_HEALTH",
//                categoryKey = "QA", categoryLabel = "Questions",
//                subCategoryKey = "QA_HEALTH", subCategoryLabel = "HEALTH",
//                indexNum = 130
//            ),
//            AssessmentTaxonomy(
//                taxonomyId = "QA_EDUCATION",
//                categoryKey = "QA", categoryLabel = "Questions",
//                subCategoryKey = "QA_EDUCATION", subCategoryLabel = "EDUCATION",
//                indexNum = 140
//            )
//        )
//
//        dao.upsertAll(seeded)
//    }
//}
//
