package com.example.charityDept.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.charityDept.data.model.AssignedRole
import com.example.charityDept.data.model.Reply
import com.example.charityDept.migration.MigrationJsonlAllInOneScreen
import com.example.charityDept.presentation.screens.*
import com.example.charityDept.presentation.screens.Register.RegisterDashBoardScreen
import com.example.charityDept.presentation.screens.admin.AdminDashboardScreen
import com.example.charityDept.presentation.screens.admin.HomeDashboardScreen
import com.example.charityDept.presentation.screens.admin.UserDetailScreen
import com.example.charityDept.presentation.screens.admin.UserFormScreen
import com.example.charityDept.presentation.screens.admin.UserListScreen
import com.example.charityDept.presentation.screens.admin.questions.QuestionBankScreen
import com.example.charityDept.presentation.screens.admin.questions.QuestionFormScreen
import com.example.charityDept.presentation.screens.attendance.*
import com.example.charityDept.presentation.screens.children.*
import com.example.charityDept.presentation.screens.children.assessments.ChildAssessmentDetailScreen
import com.example.charityDept.presentation.screens.children.assessments.ChildAssessmentHistoryScreen
import com.example.charityDept.presentation.screens.children.childAttendanceHist.ChildEventHistoryScreen
import com.example.charityDept.presentation.screens.children.childDashboard.ChildDashboardScreen
import com.example.charityDept.presentation.screens.events.*
import com.example.charityDept.presentation.screens.reports.ReportScreen
import com.example.charityDept.presentation.screens.splash.SplashScreen
import com.example.charityDept.presentation.screens.admin.streets.StreetsScreen
import com.example.charityDept.presentation.screens.admin.taxonomy.TaxonomyBankScreen
import com.example.charityDept.presentation.screens.admin.taxonomy.TaxonomyFormScreen
import com.example.charityDept.presentation.screens.families.FamilyDashboardScreen
import com.example.charityDept.presentation.screens.families.FamilyDetailsScreen
import com.example.charityDept.presentation.screens.families.FamilyFormScreen
import com.example.charityDept.presentation.screens.families.FamilyListScreen
import com.example.charityDept.presentation.screens.technicalSkills.TechnicalSkillsScreen
import com.example.charityDept.presentation.screens.users.UsersDashboardScreen
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel

import kotlinx.coroutines.flow.first
// add this import with the other family screen imports
import com.example.charityDept.presentation.screens.families.FamilyMemberFormScreen
import com.example.charityDept.presentation.screens.families.SingleFamilyDashboardScreen
import com.example.charityDept.presentation.screens.families.assessments.FamilyAssessmentDetailScreen
import com.example.charityDept.presentation.screens.families.assessments.FamilyAssessmentHistoryScreen
val LocalNavController = staticCompositionLocalOf<NavController?> { null }

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CharityDeptNavHost(
    navController: NavHostController = rememberNavController(),
    authVm: AuthViewModel = hiltViewModel()
) {
//    val authUi by authVm.ui.collectAsStateWithLifecycle()

    val authUi by authVm.ui.collectAsStateWithLifecycle()

    val permsCanListUsers = authUi.perms.canListUsers

    // 🔑 Remount the WHOLE nav tree when the user account changes
    val sessionKey = authUi.profile?.uid ?: "anon"

    // Admin-only destinations
    val adminOnly = remember { setOf(Screen.AdminUsers.route, Screen.Migration.route) }

    val canManageQuestions = authUi.profile?.userRole == AssignedRole.ADMIN

    key(sessionKey) {
        // ⬇️ New NavController for this session
        val navController: NavHostController = rememberNavController()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val destination = backStackEntry?.destination

        // Show bottom bar only on these roots
        val bottomBarDestinations = remember {
            setOf(
                Screen.HomeDashboard.route,
                Screen.RegisterDashboard.route,
                Screen.ChildrenDashboard.route,
                Screen.EventsDashboard.route,
                Screen.AttendanceDashboard.route,
                Screen.AdminUsers.route,
                Screen.AdminDashboard.route,
            )
        }
        fun shouldShowBottomBar(): Boolean =
            destination?.hierarchy?.any { it.route in bottomBarDestinations } == true

        // Redirect to Login when signed out (clear everything, don’t restore)
//        LaunchedEffect(authUi.isLoggedIn) {
//            if (!authUi.isLoggedIn) {
//                navController.navigate(Screen.Login.route) {
//                    popUpTo(0)
//                    launchSingleTop = true
//                    restoreState = false
//                }
//            }
//        }
        // ✅ Redirect to login ONLY after NavHost has set the graph
        LaunchedEffect(authUi.isLoggedIn) {
            // Suspends until NavHost sets the graph and the first back stack entry exists
            navController.currentBackStackEntryFlow.first()

            if (!authUi.isLoggedIn) {
                navController.navigate("login") {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }

        // If roles change mid-session, nuke stack and land on HomeDashboard
        var lastRoles by remember { mutableStateOf(authUi.assignedRoles.toSet()) }
//        LaunchedEffect(authUi.assignedRoles) {
//            val newRoles = authUi.assignedRoles.toSet()
//            if (newRoles != lastRoles) {
//                lastRoles = newRoles
//                navController.navigate(Screen.HomeDashboard.route) {
//                    popUpTo(0)
//                    launchSingleTop = true
//                    restoreState = false
//                }
//            }
//        }

        // ✅ If roles change, also wait for graph before navigating
        LaunchedEffect(authUi.assignedRoles) {
            navController.currentBackStackEntryFlow.first()

            val destination = navController.currentDestination?.route ?: return@LaunchedEffect
            if (destination == "login") return@LaunchedEffect

//            val isAllowedNow = when (destination) {
//                "admin_dashboard" -> authUi.userRole == AssignedRole.ADMIN
//                else -> true
//            }
            val isAllowedNow = when (destination) {
                "admin_dashboard" -> authUi.canSeeAdminScreens
                else -> true
            }


            if (!isAllowedNow) {
                navController.navigate("dashboard") {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
        }

        // Guard admin-only screens if perms shrink
        LaunchedEffect(permsCanListUsers, destination?.route) {
            if (!permsCanListUsers && destination?.route in adminOnly) {
                navController.navigate(Screen.HomeDashboard.route) {
                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                    launchSingleTop = true
                    restoreState = false
                }
            }
        }

        CompositionLocalProvider(LocalNavController provides navController) {
            Scaffold(
                bottomBar = {
                    if (shouldShowBottomBar()) {
                        // Remount the entire bar when user/roles change
                        key(authUi.profile?.uid, authUi.assignedRoles) {
                            BottomNavigationBar(
                                navController = navController,
                                authUi = authUi
                            )
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            toLogin = { navController.navigate(Screen.Login.route) },
                            toAdmin = {
                                navController.navigate(Screen.HomeDashboard.route) {
                                    popUpTo(0); launchSingleTop = true; restoreState = false
                                }
                            }
                        )
                    }

//                    composable(Screen.Login.route) {
//                        LoginScreen(
//                            toAdminDashboard = {
//                                navController.navigate(Screen.HomeDashboard.route) {
//                                    popUpTo(0)
//                                    launchSingleTop = true
//                                    restoreState = false // ← do not restore prior session
//                                }
//                            }
//                        )
//                    }
                    composable(Screen.Login.route) {
                        LoginScreen(
                            toAdminDashboard = {
                                navController.navigate(Screen.HomeDashboard.route) {
                                    popUpTo(0)
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            vm = authVm   // 👈 use the SAME instance
                        )
                    }


                    // Admin-only: Migration (guard at call site as well)
                    composable(Screen.Migration.route) {
                        if (permsCanListUsers) {
                            MigrationJsonlAllInOneScreen(
                                navigateUp = {
                                    navController.navigate(Screen.AdminUsers.route){
                                        popUpTo(Screen.Migration.route){inclusive = true}
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    /** Home Dashboard */
                    composable(Screen.HomeDashboard.route) {
                        HomeDashboardScreen(
                            toChildrenList = {
                                navController.navigate(Screen.ChildrenList.route) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toEventsList = {
                                navController.navigate(Screen.EventsList.route) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAccepted = {
                                navController.navigate(Screen.ChildrenList.accepted(Reply.YES)) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toYetAccept = {
                                navController.navigate(Screen.ChildrenList.accepted(Reply.NO)) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toResettled = {
                                navController.navigate(Screen.ChildrenList.resettled(true)) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toBeResettled = {
                                navController.navigate(Screen.ChildrenList.resettled(false)) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toFamilyDashboard = {
                                navController.navigate(Screen.FamilyDashboard.route) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    /** Register Dashboard */
                    composable(Screen.RegisterDashboard.route) {
                        RegisterDashBoardScreen(
                            toChildrenDashboard = {
                                navController.navigate(Screen.ChildrenDashboard.route) {
                                    popUpTo(Screen.RegisterDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toFamilyDashboard = {
                                navController.navigate(Screen.FamilyDashboard.route) {
                                    popUpTo(Screen.RegisterDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },



                        )
                    }



                    /** Familu Dashboard */
                    composable(Screen.FamilyDashboard.route) {
                        FamilyDashboardScreen(
                            toFamiliesList = {
                                navController.navigate(Screen.FamiliesList.route) {
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = { navController.navigateUp() }
                        )
                    }

                    composable(Screen.FamiliesList.route) {
                        FamilyListScreen(
                            navigateUp = {
                                navController.navigate(Screen.RegisterDashboard.route)
                                         },
                            onAddFamily = {
                                navController.navigate(Screen.FamilyForm.newFamily()) {
                                    launchSingleTop = true
                                }
                            },
                            onFamilyClick = { familyId ->
                                navController.navigate(Screen.SingleFamilyDashboard.createRoute(familyId)) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.FamilyDetails.route,
                        arguments = listOf(
                            navArgument("familyId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val familyIdArg = backStackEntry.arguments?.getString("familyId").orEmpty()
                        FamilyDetailsScreen(
                            familyIdArg = familyIdArg,
                            onEdit = { familyId ->
                                navController.navigate(Screen.FamilyForm.edit(familyId)) {
                                    launchSingleTop = true
                                }
                            },
                            toFamilyList = {
                                navController.navigate(Screen.FamiliesList.route) {
                                    popUpTo(Screen.FamiliesList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onAddMember = { familyIdArg ->
                                navController.navigate(Screen.FamilyMemberForm.newMember(familyIdArg)) {
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = { navController.navigateUp() }
                        )
                    }

                    composable(
                        route = Screen.FamilyForm.route,
                        arguments = listOf(
                            navArgument("familyId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val familyIdArg = backStackEntry.arguments?.getString("familyId")

                        FamilyFormScreen(
                            familyIdArg = familyIdArg,
                            onFinished = { savedFamilyId ->
                                val target = if (familyIdArg.isNullOrBlank()) {
                                    Screen.FamiliesList.route
                                } else {
                                    Screen.SingleFamilyDashboard.createRoute(savedFamilyId)
                                }

                                navController.navigate(target) {
                                    popUpTo(Screen.FamilyForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.FamilyDashboard.route) {
                                    popUpTo(Screen.FamilyForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    /***family member **/
                    // place this composable right after the FamilyForm composable block
                    composable(
                        route = Screen.FamilyMemberForm.route,
                        arguments = listOf(
                            navArgument("familyId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("familyMemberId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val familyIdArg = backStackEntry.arguments?.getString("familyId").orEmpty()
                        val familyMemberIdArg = backStackEntry.arguments?.getString("familyMemberId")

                        FamilyMemberFormScreen(
                            familyIdArg = familyIdArg,
                            familyMemberIdArg = familyMemberIdArg,
                            onFinished = {
                                navController.navigate(Screen.SingleFamilyDashboard.createRoute(familyIdArg)) {
                                    popUpTo(Screen.FamilyMemberForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = { navController.navigateUp() }
                        )
                    }

                    /**single family dashboard ***/
                    composable(
                        route = Screen.SingleFamilyDashboard.route,
                        arguments = listOf(
                            navArgument("familyId") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val familyIdArg = backStackEntry.arguments?.getString("familyId").orEmpty()

                        SingleFamilyDashboardScreen(
                            familyIdArg = familyIdArg,
                            onEditFamily = { familyId ->
                                navController.navigate(Screen.FamilyDetails.createRoute(familyId)) {
                                    launchSingleTop = true
                                }
                            },
                            onAddMember = { familyId ->
                                navController.navigate(Screen.FamilyMemberForm.newMember(familyId)) {
                                    launchSingleTop = true
                                }
                            },
                            onEditMember = { familyId, familyMemberId ->
                                navController.navigate(Screen.FamilyMemberForm.edit(familyId, familyMemberId)) {
                                    launchSingleTop = true
                                }
                            },
                            onQa = { familyId ->
                                navController.navigate(Screen.FamilyAssessmentHistory.qa(familyId)) {
                                    launchSingleTop = true
                                }
                            },
                            onObservations = { familyId ->
                                navController.navigate(Screen.FamilyAssessmentHistory.observations(familyId)) {
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = { navController.navigateUp() }
                        )
                    }
                    composable(
                        route = Screen.FamilyAssessmentHistory.route,
                        arguments = listOf(
                            navArgument("familyId") { type = NavType.StringType },
                            navArgument("mode") { type = NavType.StringType; defaultValue = "ALL" }
                        )
                    ) { backStackEntry ->
                        val familyId = backStackEntry.arguments?.getString("familyId") ?: return@composable
                        val mode = backStackEntry.arguments?.getString("mode") ?: "ALL"

                        FamilyAssessmentHistoryScreen(
                            familyId = familyId,
                            mode = mode,
                            navigateUp = {
                                navController.navigate(Screen.SingleFamilyDashboard.createRoute(familyId)) {
                                    popUpTo(Screen.FamilyAssessmentHistory.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenSession = { generalId, assessmentKey ->
                                navController.navigate(
                                    Screen.FamilyAssessmentDetail.open(
                                        familyId = familyId,
                                        generalId = generalId,
                                        mode = mode,
                                        assessmentKey = assessmentKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onStartNew = { newGeneralId, assessmentKey ->
                                navController.navigate(
                                    Screen.FamilyAssessmentDetail.open(
                                        familyId = familyId,
                                        generalId = newGeneralId,
                                        mode = mode,
                                        assessmentKey = assessmentKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.FamilyAssessmentDetail.route,
                        arguments = listOf(
                            navArgument("familyId") { type = NavType.StringType },
                            navArgument("generalId") { type = NavType.StringType },
                            navArgument("mode") { type = NavType.StringType; defaultValue = "ALL" },
                            navArgument("assessmentKey") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val familyId = backStackEntry.arguments?.getString("familyId") ?: return@composable
                        val generalId = backStackEntry.arguments?.getString("generalId") ?: return@composable
                        val mode = backStackEntry.arguments?.getString("mode") ?: "ALL"
                        val assessmentKey = backStackEntry.arguments?.getString("assessmentKey") ?: ""

                        FamilyAssessmentDetailScreen(
                            familyId = familyId,
                            generalId = generalId,
                            mode = mode,
                            assessmentKey = assessmentKey,
                            navigateUp = {
                                val backRoute = when (mode) {
                                    "QA" -> Screen.FamilyAssessmentHistory.qa(familyId)
                                    "OBS" -> Screen.FamilyAssessmentHistory.observations(familyId)
                                    else -> Screen.FamilyAssessmentHistory.all(familyId)
                                }
                                navController.navigate(backRoute) {
                                    popUpTo(Screen.FamilyAssessmentDetail.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }


                    /***
                     * Admin dasboard
                     */
                    composable(Screen.AdminDashboard.route) {
                        if (canManageQuestions) {
                            AdminDashboardScreen(
                                toMigrationToolKit = {
                                    navController.navigate(Screen.Migration.route) {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toUsersDashboard = {
                                    navController.navigate(Screen.AdminUsers.route) {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toQuestionBank = {

                                    navController.navigate("question_bank") {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toTaxonomyBank = {
                                    navController.navigate(Screen.TaxonomyBank.createRoute()) {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toChildrenDashboard = {},
                                toEventsDashboard = {},
                                toAttendanceDashboard = {},
                                toReportsDashboard = {

                                },
                                toStreetsDashboard = {
                                    navController.navigate(Screen.StreetsDashboard.route) {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toTechnicalSkillsDashboard = {
                                    navController.navigate(Screen.TechnicalSkillsDashboard.route) {
                                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },

                                navigateUp = {}
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }
                    // Admin Users
                    composable(Screen.AdminUsers.route) {
                        if (permsCanListUsers) {
                            UsersDashboardScreen(
                                toUsersList = {
                                    navController.navigate(Screen.UserList.all()){
                                        popUpTo(Screen.AdminUsers.route){ inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toAddUser = {
                                    navController.navigate(Screen.UserForm.newUser()) {
                                        popUpTo(Screen.AdminUsers.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toUsersByRole = { userRole ->
                                    navController.navigate(Screen.UserList.byUserRole(userRole)){
                                        popUpTo(Screen.AdminUsers.route){ inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toActiveUsers = {
                                    navController.navigate(Screen.UserList.byDisabled(false)){
                                        popUpTo(Screen.AdminUsers.route){ inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                toDisabledUsers =  {
                                    navController.navigate(Screen.UserList.byDisabled(true)){
                                        popUpTo(Screen.AdminUsers.route){ inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) {
                                navController.navigate(Screen.HomeDashboard.route) {
                                    popUpTo(Screen.HomeDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }
                        }
                    }

                    composable(
                        Screen.UserForm.route,
                        arguments = listOf(navArgument("uid") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("uid")
                        UserFormScreen(
                            uidArg = uid,
                            toList = {
                                navController.navigate(Screen.UserList.all()){
                                    popUpTo( Screen.UserForm.route){inclusive = true}
                                    launchSingleTop = true
                                }
                            },
                            toDetail = { uidArg ->
                                navController.navigate(Screen.UserDetails.createRoute(uidArg)) {
                                    popUpTo(Screen.UserForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }

                            },
                            navigateUp = {
                                navController.navigate(Screen.AdminUsers.route) {
                                    popUpTo(Screen.UserForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.UserDetails.route,
                        arguments = listOf(navArgument("uid") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
                        UserDetailScreen(
                            uidArg = uid,
                            toEdit = { uidArg ->
                                navController.navigate(Screen.UserForm.edit(uidArg)) {
                                    popUpTo(Screen.UserDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toDashboard = {
                                navController.navigate(Screen.AdminUsers.route) {
                                    popUpTo(Screen.UserDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toList = {
//                                navController.navigate(Screen.UserList.route) {
//                                    popUpTo(Screen.UserDetails.route) { inclusive = true }
//                                    launchSingleTop = true
//                                }
                            },
                        )
                    }

                    composable(
                        route = Screen.UserList.route,
                        arguments = listOf(
                            navArgument("userRole")     { defaultValue = "" },
                            navArgument("disabled")      { defaultValue = "" },
//                            navArgument("volunteer")      { defaultValue = "" },
//                            navArgument("sponsor")   { defaultValue = "" },

                        )
                    ) {
                        UserListScreen(
                            toUserForm = {
                                navController.navigate(Screen.UserForm.newUser()) {
                                    popUpTo(Screen.UserList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.AdminUsers.route) {
                                    popUpTo(Screen.UserList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onUserClick = { uidArg ->
                                navController.navigate(Screen.UserDetails.createRoute(uidArg)) {
                                    popUpTo(Screen.UserList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onClearFilter = {
                                navController.navigate(Screen.UserList.all()) {
                                    popUpTo(Screen.UserList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    //Question Bank

                    composable(Screen.QuestionBank.route) {
                        if (canManageQuestions) {
                            QuestionBankScreen(
                                navigateUp = {
                                    navController.navigate(Screen.AdminDashboard.route) {
                                        popUpTo(Screen.QuestionBank.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onAdd = { initialAssessmentKey, initialAssessmentLabel ->
                                    navController.navigate(
                                        Screen.QuestionForm.newQuestion(
                                            initialAssessmentKey = initialAssessmentKey,
                                            initialAssessmentLabel = initialAssessmentLabel
                                        )
                                    ) {
                                        popUpTo(Screen.QuestionBank.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onEdit = { qid ->
                                    navController.navigate(Screen.QuestionForm.edit(qid)){
                                        popUpTo(Screen.QuestionBank.route) { inclusive = true }
                                        launchSingleTop = true
                                    } }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }



                    composable(
                        route = Screen.TaxonomyForm.route,
                        arguments = listOf(
                            navArgument("taxonomyId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("initialAssessmentKey") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("initialAssessmentLabel") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        if (canManageQuestions) {
                            val taxonomyId = backStackEntry.arguments?.getString("taxonomyId")
                            val initialAssessmentKey = backStackEntry.arguments?.getString("initialAssessmentKey")
                            val initialAssessmentLabel = backStackEntry.arguments?.getString("initialAssessmentLabel")

                            TaxonomyFormScreen(
                                taxonomyIdArg = taxonomyId,
                                initialAssessmentKeyArg = initialAssessmentKey,
                                initialAssessmentLabelArg = initialAssessmentLabel,
                                navigateUp = {
                                    navController.navigate(
                                        Screen.TaxonomyBank.createRoute(
                                            selectedAssessmentKey = initialAssessmentKey,
                                            selectedAssessmentLabel = initialAssessmentLabel
                                        )
                                    ) {
                                        popUpTo("taxonomy_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onDone = {
                                    navController.navigate(
                                        Screen.TaxonomyBank.createRoute(
                                            selectedAssessmentKey = initialAssessmentKey,
                                            selectedAssessmentLabel = initialAssessmentLabel
                                        )
                                    ) {
                                        popUpTo("taxonomy_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable(
                        route = Screen.TaxonomyForm.route,
                        arguments = listOf(
                            navArgument("taxonomyId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        if (canManageQuestions) {
                            val taxonomyId = backStackEntry.arguments?.getString("taxonomyId")
                            TaxonomyFormScreen(
                                taxonomyIdArg = taxonomyId,
                                navigateUp = {
                                    navController.navigate(Screen.TaxonomyBank.route) {
                                        popUpTo(Screen.TaxonomyForm.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                             },
                                onDone = {
                                    navController.navigate(Screen.TaxonomyBank.route) {
                                        popUpTo(Screen.TaxonomyForm.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable(
                        route = Screen.TaxonomyBank.route,
                        arguments = listOf(
                            navArgument("selectedAssessmentKey") {
                                type = NavType.StringType
                                defaultValue = ""
                                nullable = true
                            },
                            navArgument("selectedAssessmentLabel") {
                                type = NavType.StringType
                                defaultValue = ""
                                nullable = true
                            }
                        )
                    ) { backStackEntry ->
                        if (canManageQuestions) {
                            val selectedAssessmentKey =
                                backStackEntry.arguments?.getString("selectedAssessmentKey").orEmpty()
                            val selectedAssessmentLabel =
                                backStackEntry.arguments?.getString("selectedAssessmentLabel").orEmpty()

                            TaxonomyBankScreen(
                                initialSelectedAssessmentKey = selectedAssessmentKey.ifBlank { null },
                                initialSelectedAssessmentLabel = selectedAssessmentLabel.ifBlank { null },
                                navigateUp = {
                                    navController.navigate(Screen.AdminDashboard.route) {
                                        popUpTo(Screen.TaxonomyBank.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onAddClick = { assessmentKey, assessmentLabel ->
                                    navController.navigate(
                                        Screen.TaxonomyForm.newTaxonomy(
                                            initialAssessmentKey = assessmentKey,
                                            initialAssessmentLabel = assessmentLabel
                                        )
                                    )
                                },
                                onEditClick = { taxonomyId ->
                                    navController.navigate(
                                        Screen.TaxonomyForm.edit(taxonomyId)
                                    )
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }

                    composable(
                        route = "question_form?initialAssessmentKey={initialAssessmentKey}&initialAssessmentLabel={initialAssessmentLabel}",
                        arguments = listOf(
                            navArgument("initialAssessmentKey") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("initialAssessmentLabel") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        if (canManageQuestions) {
                            QuestionFormScreen(
                                questionIdArg = null,
                                initialAssessmentKeyArg = backStackEntry.arguments?.getString("initialAssessmentKey"),
                                initialAssessmentLabelArg = backStackEntry.arguments?.getString("initialAssessmentLabel"),
                                navigateUp = {
                                    navController.navigate(Screen.QuestionBank.route) {
                                        popUpTo("question_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onDone = {
                                    navController.navigate("question_bank") {
                                        popUpTo("question_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }



                    composable(
                        route = Screen.TaxonomyForm.route,
                        arguments = listOf(
                            navArgument("taxonomyId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("initialAssessmentKey") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("initialAssessmentLabel") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        if (canManageQuestions) {
                            val taxonomyId = backStackEntry.arguments?.getString("taxonomyId")
                            val initialAssessmentKey = backStackEntry.arguments?.getString("initialAssessmentKey")
                            val initialAssessmentLabel = backStackEntry.arguments?.getString("initialAssessmentLabel")

                            TaxonomyFormScreen(
                                taxonomyIdArg = taxonomyId,
                                initialAssessmentKeyArg = initialAssessmentKey,
                                initialAssessmentLabelArg = initialAssessmentLabel,
                                navigateUp = {
                                    navController.navigate(
                                        Screen.TaxonomyBank.createRoute(
                                            selectedAssessmentKey = initialAssessmentKey,
                                            selectedAssessmentLabel = initialAssessmentLabel
                                        )
                                    ) {
                                        popUpTo("taxonomy_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                },
                                onDone = {
                                    navController.navigate(
                                        Screen.TaxonomyBank.createRoute(
                                            selectedAssessmentKey = initialAssessmentKey,
                                            selectedAssessmentLabel = initialAssessmentLabel
                                        )
                                    ) {
                                        popUpTo("taxonomy_form") { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            )
                        } else {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        }
                    }




                    /** Children */
                    composable(Screen.ChildrenDashboard.route) {
                        ChildrenDashboardScreen(
                            toChildrenList = {
                                navController.navigate(Screen.ChildrenList.all()) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAddChild = {
                                navController.navigate(Screen.ChildForm.newChild()) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toChildrenByEducation = { pref ->
                                navController.navigate(Screen.ChildrenList.byEducation(pref)) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toReunited = {
                                navController.navigate(Screen.ChildrenList.resettled(true)) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAllRegions = {
                                navController.navigate(Screen.Counts.forRegions()) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAllStreets = {
                                navController.navigate(Screen.Counts.forStreets()) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toNavigateUp = {
                                navController.navigate(Screen.RegisterDashboard.route) {
                                    popUpTo(Screen.ChildrenDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },

                        )
                    }

                    composable(
                        route = Screen.Counts.route,
                        arguments = listOf(navArgument("mode") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val modeArg = backStackEntry.arguments?.getString("mode") ?: "STREETS"
                        CountsScreen(
                            navigateUp = {
                                navController.navigate(Screen.ChildrenDashboard.route) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onItemClick = { name ->
                                when (modeArg) {
                                    "STREETS" -> navController.navigate(Screen.ChildrenList.byStreet(name)) {
                                        popUpTo(Screen.Counts.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    "DISTRICTS" -> navController.navigate(Screen.ChildrenList.byRegion(name)) {
                                        popUpTo(Screen.Counts.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                    else -> {}
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.ChildrenList.route,
                        arguments = listOf(
                            navArgument("eduPref")     { defaultValue = "" },
                            navArgument("street")      { defaultValue = "" },
                            navArgument("region")      { defaultValue = "" },
                            navArgument("sponsored")   { defaultValue = "" },
                            navArgument("graduated")   { defaultValue = "" },
                            navArgument("classGroup")  { defaultValue = "" },
                            navArgument("accepted")    { defaultValue = "" },
                            navArgument("resettled")   { defaultValue = "" },
                            navArgument("dobVerified") { defaultValue = "" }
                        )
                    ) {
                        ChildrenListScreen(
                            toChildForm = {
                                navController.navigate(Screen.ChildForm.newChild()) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.ChildrenDashboard.route) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toChildDashboard = { childIdArg ->
                                navController.navigate(Screen.ChildDashboard.view(childIdArg)) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onClearFilter = {
                                navController.navigate(Screen.ChildrenList.all()) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    composable(
                        Screen.ChildForm.route,
                        arguments = listOf(navArgument("childId") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId")
                        ChildFormScreen(
                            childIdArg = childId,
                            onFinished = { childIdArg ->
                                navController.navigate(Screen.ChildDetails.createRoute(childIdArg)) {
                                    popUpTo(Screen.ChildForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onSave = {
                                navController.navigate(Screen.ChildForm.route) {
                                    popUpTo(Screen.ChildForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }

                                },
                            toList = {
                                navController.navigate(Screen.ChildrenList.route) {
                                    popUpTo(Screen.ChildForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.ChildrenDashboard.route) {
                                    popUpTo(Screen.ChildForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.ChildDetails.route,
                        arguments = listOf(navArgument("childId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
                        ChildDetailsScreen(
                            childIdArg = childId,
                            onEdit = { childIdArg ->
                                navController.navigate(Screen.ChildForm.edit(childIdArg)) {
                                    popUpTo(Screen.ChildDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toChildrenDashboard = {
                                navController.navigate(Screen.ChildDashboard.view(childId)) {
                                    popUpTo(Screen.ChildDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toChildrenList = {
                                navController.navigate(Screen.ChildrenList.route) {
                                    popUpTo(Screen.ChildDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    /**
                     * Single clientDasboard
                     */
                    composable(
                        Screen.ChildDashboard.route,
                        arguments = listOf(navArgument("id") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
//        val id = backStackEntry.arguments?.getString("id")?: 0
                        val id = backStackEntry.arguments?.getString("id") ?: return@composable

                        ChildDashboardScreen(
                            childIdArg = id,
                            toEditChild = {childIdArg ->
                                navController.navigate(Screen.ChildDetails.createRoute(childIdArg)) {
                                    popUpTo(Screen.ChildDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toEvents = { childIdArg ->
//                                navController.navigate(Screen.ChildDetails.createRoute(childIdArg)) {
//                                    popUpTo(Screen.ChildDashboard.route) { inclusive = true }
//                                    launchSingleTop = true
//                                }
                            },
                            toAttendance = { childIdArg ->
                                navController.navigate(Screen.ChildEventHistory.createRoute(childIdArg)) {
                                    popUpTo(Screen.ChildDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toQa = { childIdArg ->
                                navController.navigate(Screen.ChildAssessmentHistory.qa(childIdArg)) {
                                    popUpTo(Screen.ChildDashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            toObservations = { childIdArg ->
                                navController.navigate(Screen.ChildAssessmentHistory.observations(childIdArg)) {
                                    popUpTo(Screen.ChildDashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },

                            navigateUp = {
                                navController.navigate(Screen.ChildrenList.all()) {
                                    popUpTo(Screen.ChildDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }

                            },
                        )
                    }

                    composable(
                        route = Screen.ChildAssessmentHistory.route,
                        arguments = listOf(
                            navArgument("childId") { type = NavType.StringType },
                            navArgument("mode") { type = NavType.StringType; defaultValue = "ALL" }
                        )
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
                        val mode = backStackEntry.arguments?.getString("mode") ?: "ALL"

                        ChildAssessmentHistoryScreen(
                            childId = childId,
                            mode = mode,
                            navigateUp = {
                                navController.navigate(Screen.ChildDashboard.view(childId)) {
                                    popUpTo(Screen.ChildAssessmentHistory.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenSession = { generalId, assessmentKey ->
                                navController.navigate(
                                    Screen.ChildAssessmentDetail.open(
                                        childId = childId,
                                        generalId = generalId,
                                        mode = mode,
                                        assessmentKey = assessmentKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            },
                            onStartNew = { newGeneralId, assessmentKey ->
                                navController.navigate(
                                    Screen.ChildAssessmentDetail.open(
                                        childId = childId,
                                        generalId = newGeneralId,
                                        mode = mode,
                                        assessmentKey = assessmentKey
                                    )
                                ) {
                                    launchSingleTop = true
                                }
                            }

                        )
                    }

                    composable(
                        route = Screen.ChildAssessmentDetail.route,
                        arguments = listOf(
                            navArgument("childId") { type = NavType.StringType },
                            navArgument("generalId") { type = NavType.StringType },
                            navArgument("mode") { type = NavType.StringType; defaultValue = "ALL" },
                            navArgument("assessmentKey") { type = NavType.StringType; defaultValue = "" }
                        )
                    ) { backStackEntry ->
                        val childId = backStackEntry.arguments?.getString("childId") ?: return@composable
                        val generalId = backStackEntry.arguments?.getString("generalId") ?: return@composable
                        val mode = backStackEntry.arguments?.getString("mode") ?: "ALL"
                        val assessmentKey = backStackEntry.arguments?.getString("assessmentKey") ?: ""

                        ChildAssessmentDetailScreen(
                            childId = childId,
                            generalId = generalId,
                            mode = mode,
                            assessmentKey = assessmentKey,
                            navigateUp = {
                                val backRoute = when (mode) {
                                    "QA" -> Screen.ChildAssessmentHistory.qa(childId)
                                    "OBS" -> Screen.ChildAssessmentHistory.observations(childId)
                                    else -> Screen.ChildAssessmentHistory.all(childId)
                                }
                                navController.navigate(backRoute) {
                                    popUpTo(Screen.ChildAssessmentDetail.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }










                    /** Events */
                    composable(Screen.EventsDashboard.route) {
                        EventDashboardScreen(
                            toEventDetails = { eventIdArg ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventIdArg)) {
                                    popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onAddEvent = {
                                navController.navigate(Screen.EventForm.newEvent()) {
                                    popUpTo(Screen.EventForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toEventList = {
                                navController.navigate(Screen.EventsList.route) {
                                    popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenEvent = { eventIdArg ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventIdArg)) {
                                    popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toEventForm = {
                                navController.navigate(Screen.EventForm.newEvent()) {
                                    popUpTo(Screen.EventsDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        Screen.EventForm.route,
                        arguments = listOf(
                            navArgument("eventId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("parentEventId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument("isChild") {
                                type = NavType.BoolType
                                defaultValue = false
                            }
                        )
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId")
                        val parentEventId = backStackEntry.arguments?.getString("parentEventId")
                        val isChild = backStackEntry.arguments?.getBoolean("isChild") ?: false

                        EventFormScreen(
                            eventIdArg = eventId,
                            parentEventIdArg = parentEventId,
                            isChildArg = isChild,
                            onFinished = { eventIdArg ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventIdArg)) {
                                    popUpTo(Screen.EventForm.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                val returnEventId = eventId ?: parentEventId
                                if (!returnEventId.isNullOrBlank()) {
                                    navController.navigate(Screen.SingleEventDashboard.createRoute(returnEventId)) {
                                        popUpTo(Screen.EventForm.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                } else {
                                    navController.navigate(Screen.EventsDashboard.route) {
                                        popUpTo(Screen.EventForm.route) { inclusive = true }
                                        launchSingleTop = true
                                    }
                                }
                            }
                        )
                    }

                    composable(Screen.EventsList.route) {
                        EventListScreen(
                            toEventForm = {
                                navController.navigate(Screen.EventForm.newEvent()) {
                                    popUpTo(Screen.EventsList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.EventsDashboard.route) {
                                    popUpTo(Screen.EventsList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onEventClick = { eventIdArg ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventIdArg)) {
                                    popUpTo(Screen.EventsList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(
                        route = Screen.EventDetails.route,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                        EventDetailsScreen(
                            eventIdArg = eventId,
                            onEdit = { eventIdArg ->
                                navController.navigate(Screen.EventForm.editEvent(eventIdArg)) {
                                    popUpTo(Screen.ChildrenList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAttendanceRoster = { eventIdArg, _ ->
                                navController.navigate(Screen.AttendanceRoster.createRoute(eventIdArg)) {
                                    popUpTo(Screen.EventDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toEventList = {
                                navController.navigate(Screen.EventsList.route) {
                                    popUpTo(Screen.EventDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },

                            navigateUp = { eventIdArg ->
                                navController.navigate(
                                    Screen.SingleEventDashboard.createRoute(
                                        eventIdArg
                                    )
                                ) {
                                    popUpTo(Screen.EventDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                         ,
                        )
                    }

                    composable(
                        route = Screen.SingleEventDashboard.route,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                        SingleEventDashboardScreen(
                            eventIdArg = eventId,
                            onEditEvent = { eventIdArg ->
                                navController.navigate(Screen.EventForm.editEvent(eventIdArg)) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onAddChildEvent = { parentEventId ->
                                navController.navigate(Screen.EventForm.newChildEvent(parentEventId)) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenAttendance = { eventIdArg ->
                                navController.navigate(Screen.AttendanceRoster.createRoute(eventIdArg)) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenChildEvent = { childEventId ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(childEventId)) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onOpenFrequentAttendee = { childId ->
                                navController.navigate(Screen.ChildEventHistory.createRoute(childId)) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            navigateUp = {
                                navController.navigate(Screen.EventsList.route) {
                                    popUpTo(Screen.SingleEventDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }



                    /** Attendance */
                    composable(Screen.AttendanceDashboard.route) {
                        AttendanceDashboardScreen(
                            navigateUp = { /* no-op */ },
                            onContactGuardian = { },
                            toPresent = { _ ->
                                navController.navigate(Screen.ConsecutiveAttendanceList.route) {
                                    popUpTo(Screen.AttendanceDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toAbsent = { _ ->
                                navController.navigate(Screen.ConsecutiveAttendanceList.route) {
                                    popUpTo(Screen.AttendanceDashboard.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            toConsecutiveAbsentees = {
                                navController.navigate(Screen.ConsecutiveAttendanceList.route)
                            }
                        )
                    }

                    composable(
                        route = Screen.AttendanceRoster.route,
                        arguments = listOf(navArgument("eventId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                        AttendanceRosterScreen(
                            eventId = eventId,
                            adminId = "0",
                            navigateUp = {
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventId)) {
                                    popUpTo(Screen.EventDetails.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                        )
                    }

                    composable(Screen.ConsecutiveAttendanceList.route) {
                        ConsecutiveAttendanceScreen(
                            toAttendanceDashboard = {
                                navController.navigate(Screen.AttendanceDashboard.route) {
                                    popUpTo(Screen.ConsecutiveAttendanceList.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }


                    composable(
                        Screen.ChildEventHistory.route,
                        arguments = listOf(navArgument("id") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        })
                    ) { backStackEntry ->
//        val id = backStackEntry.arguments?.getString("id")?: 0
                        val id = backStackEntry.arguments?.getString("id") ?: return@composable

                        ChildEventHistoryScreen(
                            childIdArg = id,
                            navigateUp = {
                                navController.navigate(Screen.ChildDashboard.view(id)) {
                                    popUpTo(Screen.ChildEventHistory.route) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            onEventClick = { eventId ->
                                navController.navigate(Screen.SingleEventDashboard.createRoute(eventId)) {
                                    popUpTo(Screen.ChildEventHistory.route) { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                        )

                    }
                    /*****
                     * Technical skills
                     *
                     *
                     */

                    composable(Screen.TechnicalSkillsDashboard.route) {
                        TechnicalSkillsScreen(

                        )
                    }



                    /*****
                     * Streets
                     *
                     *
                     */

                    composable(Screen.StreetsDashboard.route) {
                        StreetsScreen(

                        )
                    }


                    composable(Screen.ReportsDashboard.route) {
                        ReportScreen(

                        )
                    }
                }
            }
        }
    }
}

