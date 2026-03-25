package com.example.charityDept.data.model


import com.google.firebase.Timestamp

data class AttendanceBuckets(
    val times1: Int,
    val times2: Int,
    val times3: Int,
    val times4plus: Int
)

data class MonthlyReport(
    val label: String,                 // e.g., "2025-10" or "2025-09-01 → 2025-09-30"
    val periodStart: Timestamp,
    val periodEnd: Timestamp,          // exclusive
    val totalChildren: Int,
    val attendanceBuckets: AttendanceBuckets,
    val percentAtLeast50: Double,      // 1 decimal place
    val newDecisions: Int,
    val totalAcceptedJesusToDate: Int,
    val sponsoredPrimaryP1toP7: Int,
    val skillsMechanicsEnrolled: Int,
    val resettledThisPeriod: Int,
    val resettledToDate: Int
)

data class QuarterlyReport(
    val label: String,                 // e.g., "2025-Q3"
    val periodStart: Timestamp,
    val periodEnd: Timestamp,          // exclusive
    val totalChildren: Int,
    val attendanceBuckets: AttendanceBuckets,
    val percentAtLeast50: Double,
    val newDecisions: Int,
    val totalAcceptedJesusToDate: Int,
    val sponsoredPrimaryP1toP7: Int,
    val skillsMechanicsEnrolled: Int,
    val resettledThisPeriod: Int,
    val resettledToDate: Int
)

