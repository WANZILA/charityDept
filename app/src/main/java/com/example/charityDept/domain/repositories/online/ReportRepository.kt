// com/example/zionkids/domain/repositories/online/ReportRepository.kt
package com.example.charityDept.domain.repositories.online

import com.example.charityDept.core.di.AttendanceRef
import com.example.charityDept.core.di.ChildrenRef
import com.example.charityDept.core.di.EventsRef
import com.example.charityDept.data.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.round

@Singleton
class ReportRepository @Inject constructor(
    @ChildrenRef private val childrenRef: CollectionReference,
    @AttendanceRef private val attendancesRef: CollectionReference,
    @EventsRef private val eventsRef: CollectionReference,
) {

    // ===== PUBLIC APIS =====

    suspend fun buildMonthlyReport(year: Int, month1to12: Int): MonthlyReport {
        val (start, end) = monthBounds(year, month1to12)
        return buildPeriodReportMonthly(label = "%04d-%02d".format(year, month1to12), start, end)
    }

    suspend fun buildQuarterlyReport(year: Int, quarter1to4: Int): QuarterlyReport {
        val (start, end) = quarterBounds(year, quarter1to4)
        val core = buildPeriodCore(start, end)
        return QuarterlyReport(
            label = "%04d-Q%d".format(year, quarter1to4),
            periodStart = start,
            periodEnd = end,
            totalChildren = core.totalChildren,
            attendanceBuckets = core.attendanceBuckets,
            percentAtLeast50 = core.percentAtLeast50,
            newDecisions = core.newDecisions,
            totalAcceptedJesusToDate = core.totalAcceptedJesusToDate,
            sponsoredPrimaryP1toP7 = core.sponsoredPrimaryP1toP7,
            skillsMechanicsEnrolled = core.skillsMechanicsEnrolled,
            resettledThisPeriod = core.resettledThisPeriod,
            resettledToDate = core.resettledToDate
        )
    }

    /** Custom date-range report. `endExclusiveMillis` should be start-of-next-day. */
    suspend fun buildCustomReport(
        startInclusiveMillis: Long,
        endExclusiveMillis: Long
    ): MonthlyReport {
        val start = Timestamp(Date(startInclusiveMillis))
        val end = Timestamp(Date(endExclusiveMillis))
        val core = buildPeriodCore(start, end)
        val label = "${isoDate(startInclusiveMillis)} → ${isoDate(endExclusiveMillis - 1)}"
        return MonthlyReport(
            label = label,
            periodStart = start,
            periodEnd = end,
            totalChildren = core.totalChildren,
            attendanceBuckets = core.attendanceBuckets,
            percentAtLeast50 = core.percentAtLeast50,
            newDecisions = core.newDecisions,
            totalAcceptedJesusToDate = core.totalAcceptedJesusToDate,
            sponsoredPrimaryP1toP7 = core.sponsoredPrimaryP1toP7,
            skillsMechanicsEnrolled = core.skillsMechanicsEnrolled,
            resettledThisPeriod = core.resettledThisPeriod,
            resettledToDate = core.resettledToDate
        )
    }

    // ===== INTERNALS =====

    private suspend fun buildPeriodReportMonthly(label: String, start: Timestamp, end: Timestamp): MonthlyReport {
        val core = buildPeriodCore(start, end)
        return MonthlyReport(
            label = label,
            periodStart = start,
            periodEnd = end,
            totalChildren = core.totalChildren,
            attendanceBuckets = core.attendanceBuckets,
            percentAtLeast50 = core.percentAtLeast50,
            newDecisions = core.newDecisions,
            totalAcceptedJesusToDate = core.totalAcceptedJesusToDate,
            sponsoredPrimaryP1toP7 = core.sponsoredPrimaryP1toP7,
            skillsMechanicsEnrolled = core.skillsMechanicsEnrolled,
            resettledThisPeriod = core.resettledThisPeriod,
            resettledToDate = core.resettledToDate
        )
    }

    private data class PeriodCore(
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

    private suspend fun buildPeriodCore(start: Timestamp, end: Timestamp): PeriodCore {
        // 1) children
        val children = childrenRef.get().await().documents.mapNotNull { it.toObject(Child::class.java) }
        val totalChildren = children.size

        // 2) events in period (denominator for 50%)
        val events = eventsRef
            .whereGreaterThanOrEqualTo("eventDate", start)
            .whereLessThan("eventDate", end)
            .get().await().documents
            .mapNotNull { it.toObject(Event::class.java) }
        val eventsInPeriod = events.size

        // 3) attendances in period
        val attendances = attendancesRef
            .whereGreaterThanOrEqualTo("checkedAt", start)
            .whereLessThan("checkedAt", end)
            .get().await().documents
            .mapNotNull { it.toObject(Attendance::class.java) }

        // Attendance buckets: count PRESENT per child
        val presentCountsByChild = attendances
            .asSequence()
            .filter { it.status == AttendanceStatus.PRESENT }
            .groupBy { it.childId }
            .mapValues { (_, list) -> list.size }

        val buckets = AttendanceBuckets(
            times1 = presentCountsByChild.values.count { it == 1 },
            times2 = presentCountsByChild.values.count { it == 2 },
            times3 = presentCountsByChild.values.count { it == 3 },
            times4plus = presentCountsByChild.values.count { it >= 4 },
        )

        // ≥50% attendance
        val threshold = if (eventsInPeriod == 0) Int.MAX_VALUE else ceil(0.5 * eventsInPeriod).toInt()
        val numAtLeast50 = presentCountsByChild.values.count { it >= threshold }
        val percentAtLeast50 = if (totalChildren == 0) 0.0
        else round((numAtLeast50 * 1000.0 / totalChildren)) / 10.0

        // Spiritual growth
        val newDecisions = children.count { c ->
            c.acceptedJesus == Reply.YES &&
                    c.acceptedJesusDate != null &&
                    c.acceptedJesusDate!! >= start &&
                    c.acceptedJesusDate!! < end
        }
        val totalAcceptedJesusToDate = children.count { it.acceptedJesus == Reply.YES }

        // Education support P1..P7
        val p1to7 = setOf("P1","P2","P3","P4","P5","P6","P7")
        val sponsoredPrimaryP1toP7 = children.count { c ->
            c.partnershipForEducation &&
                    c.educationLevel == EducationLevel.PRIMARY &&
                    p1to7.contains(c.lastClass.trim().uppercase())
        }

        // Skills: mechanics
        val skillsMechanicsEnrolled = children.count { c ->
            c.educationPreference == EducationPreference.SKILLING &&
                    c.technicalSkills.contains("mechanic", ignoreCase = true)
        }

        // Family resettlement
        val resettledThisPeriod = children.count { c ->
            c.resettled && c.resettlementDate != null &&
                    c.resettlementDate!! >= start && c.resettlementDate!! < end
        }
        val resettledToDate = children.count { it.resettled }

        return PeriodCore(
            totalChildren = totalChildren,
            attendanceBuckets = buckets,
            percentAtLeast50 = percentAtLeast50,
            newDecisions = newDecisions,
            totalAcceptedJesusToDate = totalAcceptedJesusToDate,
            sponsoredPrimaryP1toP7 = sponsoredPrimaryP1toP7,
            skillsMechanicsEnrolled = skillsMechanicsEnrolled,
            resettledThisPeriod = resettledThisPeriod,
            resettledToDate = resettledToDate
        )
    }

    // ===== period helpers =====
    private fun monthBounds(year: Int, month1to12: Int): Pair<Timestamp, Timestamp> {
        val startLd = LocalDate.of(year, month1to12, 1)
        val endLd = startLd.plusMonths(1)
        return startLd.atStartOfDay().toTs() to endLd.atStartOfDay().toTs()
    }

    private fun quarterBounds(year: Int, quarter1to4: Int): Pair<Timestamp, Timestamp> {
        val startMonth = (quarter1to4 - 1) * 3 + 1
        val startLd = LocalDate.of(year, startMonth, 1)
        val endLd = startLd.plusMonths(3)
        return startLd.atStartOfDay().toTs() to endLd.atStartOfDay().toTs()
    }

    private fun java.time.LocalDateTime.toTs(): Timestamp =
        Timestamp(this.toInstant(ZoneOffset.UTC).epochSecond, 0)

    private fun isoDate(millis: Long): String =
        java.time.Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toString()
}

