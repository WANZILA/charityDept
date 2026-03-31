package com.example.charityDept.presentation.navigation

import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.ClassGroup
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.data.model.Reply
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object HomeDashboard : Screen("home")
    object ChildrenDashboard : Screen("children")

    object RegisterDashboard : Screen("register")

    object FamiliesList : Screen("families_list")
    object FamilyForm : Screen("familyForm?familyId={familyId}") {
        fun newFamily() = "familyForm"
        fun edit(id: String) = "familyForm?familyId=$id"
    }

    object FamilyDetails : Screen("family_details/{familyId}") {
        fun createRoute(familyId: String) = "family_details/$familyId"
    }

    object FamilyMemberForm : Screen(
        "familyMemberForm?familyId={familyId}&familyMemberId={familyMemberId}"
    ) {
        fun newMember(familyId: String) = "familyMemberForm?familyId=$familyId"
        fun edit(familyId: String, familyMemberId: String) =
            "familyMemberForm?familyId=$familyId&familyMemberId=$familyMemberId"
    }

    object FamilyDashboard : Screen("family_dashboard")

    object SingleFamilyDashboard : Screen("single_family_dashboard/{familyId}") {
        fun createRoute(familyId: String) = "single_family_dashboard/$familyId"
    }


    object Migration: Screen("migration")

    /*** Admin  *
     *
     *
     *
     * **/
    object  AdminUsers : Screen("users")

    object UserForm : Screen("userForm?uid={uid}") {
        fun newUser() = "userForm"
        fun edit(id: String) = "userForm?uid=$id"
    }


    object UserDetails : Screen("user_details/{uid}") {
        fun createRoute(uid: String) = "user_details/$uid"
    }

    object UserList : Screen(
        "user_list" +
                "?userRole={userRole}" +
                "&disabled={disabled}"

    ) {
        fun all() = "user_list"
        fun byDisabled(yes: Boolean)                   = UserList.build(disabled = yes.toString())
        fun byUserRole(userRole: AssignedRole)               = UserList.build(userRole = userRole.name)
//        fun byEducation(pref: EducationPreference) = UserList.build(eduPref = pref.name)

        /**
         * Generic builder to compose any combination of filters.
         * Only non-blank args are included in the URL.
         */
        fun build(

            disabled: String? = null,     // "true"/"false"
            userRole: String? = null,     // "YES"/"NO"
               // "true"/"false"
        ): String {
            val params = mutableListOf<String>()
            fun add(k: String, v: String?) { if (!v.isNullOrBlank()) params += "$k=$v" }
            add("disabled", disabled)
            add("userRole", userRole)

            return if (params.isEmpty()) all() else "user_list?${params.joinToString("&")}"
        }

//        private fun enc(s: AssignedRole): String =
//            URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
    }


    /****************
     *
     * Children
     *
     *
     *
     *******************/
    object ChildForm : Screen("childForm?childId={childId}") {
        fun newChild() = "childForm"
        fun edit(id: String) = "childForm?childId=$id"
    }

    // Children list with ALL optional filters as query params
    object ChildrenList : Screen(
        "children_list" +
                "?eduPref={eduPref}" +
                "&street={street}" +
                "&region={region}" +
                "&sponsored={sponsored}" +
                "&graduated={graduated}" +
                "&classGroup={classGroup}" +
                "&accepted={accepted}" +
                "&resettled={resettled}" +
                "&dobVerified={dobVerified}"
    ) {
        fun all() = "children_list"

        // convenience helpers (single filter)
        fun byEducation(pref: EducationPreference) = build(eduPref = pref.name)
        fun byStreet(street: String)              = build(street = enc(street))
        fun byRegion(region: String)              = build(region = enc(region))
        fun sponsored(yes: Boolean)               = build(sponsored = yes.toString())
        fun graduated(reply: Reply)               = build(graduated = reply.name)
        fun classGroup(group: ClassGroup)         = build(classGroup = group.name)
        fun accepted(reply: Reply)                = build(accepted = reply.name)
        fun resettled(yes: Boolean)               = build(resettled = yes.toString())
        fun dobVerified(yes: Boolean)             = build(dobVerified = yes.toString())

        /**
         * Generic builder to compose any combination of filters.
         * Only non-blank args are included in the URL.
         */
        fun build(
            eduPref: String? = null,
            street: String? = null,
            region: String? = null,
            sponsored: String? = null,     // "true"/"false"
            graduated: String? = null,     // "YES"/"NO"
            classGroup: String? = null,    // e.g. "SERGEANT"
            accepted: String? = null,      // "YES"/"NO"
            resettled: String? = null,     // "true"/"false"
            dobVerified: String? = null    // "true"/"false"
        ): String {
            val params = mutableListOf<String>()
            fun add(k: String, v: String?) { if (!v.isNullOrBlank()) params += "$k=$v" }
            add("eduPref", eduPref)
            add("street", street)
            add("region", region)
            add("sponsored", sponsored)
            add("graduated", graduated)
            add("classGroup", classGroup)
            add("accepted", accepted)
            add("resettled", resettled)
            add("dobVerified", dobVerified)
            return if (params.isEmpty()) all() else "children_list?${params.joinToString("&")}"
        }

        private fun enc(s: String): String =
            URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
    }

    object ChildDetails : Screen("child_details/{childId}") {
        fun createRoute(childId: String) = "child_details/$childId"
    }

    object ChildDashboard : Screen("ChildDashboard?id={id}") {
        fun view(id: String) = "ChildDashboard?id=$id"
    }

    // /// ADDED: Question Bank
    object QuestionBank : Screen("question_bank")

    object QuestionForm : Screen("question_form?questionId={questionId}") {
        fun newQuestion() = "question_form"
        fun edit(id: String) = "question_form?questionId=$id"
    }

    object TaxonomyBank : Screen("taxonomy_bank")

    object TaxonomyForm : Screen("taxonomy_form?taxonomyId={taxonomyId}") {
        fun newTaxonomy() = "taxonomy_form"
        fun edit(id: String) = "taxonomy_form?taxonomyId=$id"
    }



    /***
     * Admin Dashboard
     */

    object AdminDashboard : Screen("admin_dashboard")
    object ChildAssessmentHistory : Screen("child_assessment_history/{childId}?mode={mode}") {
        fun qa(childId: String) = "child_assessment_history/$childId?mode=QA"
        fun observations(childId: String) = "child_assessment_history/$childId?mode=OBS"
        fun all(childId: String) = "child_assessment_history/$childId?mode=ALL"
    }

    object ChildAssessmentDetail : Screen("child_assessment_detail/{childId}/{generalId}?mode={mode}&assessmentKey={assessmentKey}") {
        fun open(
            childId: String,
            generalId: String,
            mode: String = "ALL",
            assessmentKey: String = ""
        ) = "child_assessment_detail/$childId/$generalId?mode=$mode&assessmentKey=$assessmentKey"
    }


    object Counts : Screen("counts/{mode}") {
        fun forRegions() = "counts/DISTRICTS"
        fun forStreets() = "counts/STREETS"
    }

    /** Events **/
    object EventsDashboard : Screen("events")

    object EventForm : Screen("eventForm?eventId={eventId}") {
        fun newEvent() = "eventForm"
        fun editEvent(id: String) = "eventForm?eventId=$id"
    }

    object EventsList : Screen("events_list")

    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: String) = "event_details/$eventId"
    }

    /** Attendance **/
    object AttendanceDashboard : Screen("attendance")

    object AttendanceRoster : Screen("attendance_roster/{eventId}") {
        fun createRoute(eventId: String) = "attendance_roster/$eventId"
    }

    object ConsecutiveAttendanceList : Screen("consecutiveAttendanceScreen")

    object ChildEventHistory : Screen("child_event_history/{id}") {
        fun createRoute(id: String) = "child_event_history/$id"
    }


    /**************
     *
     * Technical Skills
     *
     */
    object TechnicalSkillsDashboard : Screen("technicalSkills")


    /**************
     *
     * Streets
     *
     */
    object StreetsDashboard : Screen("streets")



    /**************
     *
     * Reports
     *
     */
    object ReportsDashboard : Screen("reports")





}

//package com.example.charityDept.presentation.navigation
//
//import com.example.charityDept.data.model.EducationPreference
//
//sealed class Screen(val route: String) {
//    object Splash : Screen("splash")
//    object Login : Screen("login")
//    object HomeDashboard: Screen("home")
//    object ChildrenDashboard : Screen("children")
////    object Profile : Screen("profile")
//    object ChildForm : Screen("childForm?childId={childId}"){
//        fun newChild() = "childForm"                   // no id -> create
//        fun edit(id: String) = "childForm?childId=$id"
//        //fun createRoute(childId: String) = "add_child/$childId"
//    }
//
////    object ChildrenList: Screen("children_list")
//object ChildrenList : Screen("children_list?eduPref={eduPref}&street={street}&region={region}") {
//    fun all() = "children_list"
//    fun byEducation(pref: EducationPreference) = "children_list?eduPref=${pref.name}"
//    fun byStreet(street: String) = "children_list?street=${street.encode()}"
//    fun byRegion(region: String) = "children_list?region=${region.encode()}"
//    private fun String.encode() = this.replace(" ", "%20")
//}
//    object ChildDetails : Screen("child_details/{childId}") {
//        fun createRoute(childId: String) = "child_details/$childId"
//    }
//
//    object Counts : Screen("counts/{mode}") {
//        fun forRegions() = "counts/REGIONS"
//        fun forStreets() = "counts/STREETS"
//    }
//
//
//
//
//    /**
//     * Events
//     * **/
//    object  EventsDashboard: Screen("events")
//
//    object EventForm: Screen("eventForm?eventId={eventId}"){
//        fun newEvent() = "eventForm"                   // no id -> create
//        fun editEvent(id: String) = "eventForm?eventId=$id"
//        //fun createRoute(childId: String) = "add_child/$childId"
//    }
//
//    object EventsList: Screen("events_list")
//
//    object EventDetails : Screen("event_details/{eventId}") {
//        fun createRoute(eventId: String) = "event_details/$eventId"
//    }
//
//    /**
//     * Attendance
//     * **/
//    object  AttendanceDashboard: Screen("attend")
//
//    /**
//     * Attendance List
//     */
//    object  AttendanceRoster: Screen("attendance_roster/{eventId}") {
//            fun createRoute(eventId: String) = "attendance_roster/$eventId"
//    }
//
//
//    object  ConsecutiveAttendanceList : Screen("consecutiveAttendanceScreen")
//
//
//}

