package com.example.charityDept.presentation.screens.children

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.data.model.Child
import com.example.charityDept.presentation.screens.widgets.DeleteIconWithConfirm
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDetailsScreen(
    childIdArg: String,
    onEdit: (String) -> Unit,
    toChildrenDashboard: () -> Unit,
    toChildrenList: () -> Unit,
    vm: ChildDetailsViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val authUI by authVM.ui.collectAsStateWithLifecycle()
    val canEditChild = authUI.perms.canEditChild
    val canDeleteChild= authUI.perms.canDeleteEvent
    // load once
    LaunchedEffect(childIdArg) { vm.load(childIdArg) }

//    LaunchedEffect(Unit) {
//        vm.events.collect { ev ->
//            when (ev) {
//                is ChildDetailsViewModel.Event.Deleted -> toChildrenList()
//                is ChildDetailsViewModel.Event.Error -> snackbarHostState.showSnackbar(ev.msg)
//            }
//        }
//    }
        LaunchedEffect(Unit) {
               vm.events.collectLatest { ev ->
            when (ev) {
                is ChildDetailsViewModel.Event.Deleted -> {
                    snackbarHostState.showSnackbar("Child deleted")
                    toChildrenList()
                }
                is ChildDetailsViewModel.Event.Error -> {
                    toChildrenList()
                    snackbarHostState.showSnackbar(ev.msg)

                }
            }
        }
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(ui.child?.fName?.trim().orEmpty().ifBlank { "Child details" }) },
                navigationIcon = {
                    IconButton(onClick = toChildrenDashboard) {
                        Icon(
                            Icons.Filled.ArrowCircleLeft, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.secondary
                            )
                    }
                },
                actions = {
                    val child = ui.child
                    if (child != null) {
                        if(canEditChild){
                            IconButton(onClick = { onEdit(child.childId) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                            }
                        }
                        IconButton(onClick = { vm.refresh(child.childId) }, enabled = !ui.deleting) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
//                 Reusable delete for CHILD
                       if(canDeleteChild){
                           DeleteIconWithConfirm(
                               label = "child ${child.fName} ${child.lName}".trim(),
                               deleting = ui.deleting,
                               onDelete = {
                                   vm.deleteChildOptimistic()
                               }
                           )
                       }

                        IconButton(onClick = toChildrenList) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    "Error: ${ui.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ui.child != null -> DetailsContent(ui.child!!)
            }
        }
    }


}

@Composable
private fun DetailsContent(child: Child) {
    var openBasic by rememberSaveable { mutableStateOf(true) }
    var openBackground by rememberSaveable { mutableStateOf(false) }
    var openEducation by rememberSaveable { mutableStateOf(false) }
    var openResettlement by rememberSaveable { mutableStateOf(false) }
    var openMembers by rememberSaveable { mutableStateOf(false) }
    var openSpiritual by rememberSaveable { mutableStateOf(false) }
    var openSponsorship by rememberSaveable { mutableStateOf(false) }
    var openStatus by rememberSaveable { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CollapsibleSection("Basic Info", openBasic, { openBasic = !openBasic }) {
                Field("First name", child.fName.ifBlank { "-" })
                Field("Last name", child.lName.ifBlank { "-" })
                Field("Other name", child.oName.ifBlank { "-" })
                Field("Age", child.age.takeIf { it > 0 }?.toString() ?: "-")
                Field("Street", child.street.ifBlank { "-" })

                Field("NIN Number", child.ninNumber.ifBlank { "-" })
                Field("Child type", child.childType.name)
                Field("Program", child.program.name)
                Field("Child phone 1", child.personalPhone1.ifBlank { "-" })
                Field("Child phone 2", child.personalPhone2.ifBlank { "-" })

                Field("Class group", child.classGroup.name)
                Field("Invited by", child.invitedBy.name)
                Field("Invited by (ID)", child.invitedByIndividualId.ifBlank { "-" })
                Field("Invited by (Other)", child.invitedByTypeOther.ifBlank { "-" })
                Field("Education preference", child.educationPreference.name)
                Field("DOB verified", if (child.dobVerified) "Yes" else "No")
                Field("Date of birth", child.dob?.asHuman() ?: "-")
            }
        }

        item {
            CollapsibleSection("Background Info", openBackground, { openBackground = !openBackground }) {
                Field("Left home date", child.leftHomeDate?.asHuman() ?: "-")
                Field("Reason left home", child.reasonLeftHome.ifBlank { "-" })
                Field("Left street date", child.leaveStreetDate?.asHuman() ?: "-")
            }
        }

        item {
            CollapsibleSection("Education Info", openEducation, { openEducation = !openEducation }) {
                Field("Last class", child.lastClass.ifBlank { "-" })
                Field("Previous school", child.previousSchool.ifBlank { "-" })
                Field("Reason left school", child.reasonLeftSchool.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Family Resettlement", openResettlement, { openResettlement = !openResettlement }) {
                Field("Resettlement preference", child.resettlementPreference.name)
                Field("Resettlement preference (Other)", child.resettlementPreferenceOther.ifBlank { "-" })
                Field("Resettled", if (child.resettled) "Yes" else "No")
                Field("Resettlement date", child.resettlementDate?.asHuman() ?: "-")
                Field("Region", child.region.ifBlank { "-" })
                Field("District", child.district.ifBlank { "-" })
                Field("County", child.county.ifBlank { "-" })
                Field("Sub-county", child.subCounty.ifBlank { "-" })
                Field("Parish", child.parish.ifBlank { "-" })
                Field("Village", child.village.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Family Members", openMembers, { openMembers = !openMembers }) {
                MemberBlock(1, child.memberFName1, child.memberLName1, child.relationship1.name, child.telephone1a, child.telephone1b)
                Divider()
                MemberBlock(2, child.memberFName2, child.memberLName2, child.relationship2.name, child.telephone2a, child.telephone2b)
                Divider()
                MemberBlock(3, child.memberFName3, child.memberLName3, child.relationship3.name, child.telephone3a, child.telephone3b)
            }
        }

        item {
            CollapsibleSection("Spiritual Info", openSpiritual, { openSpiritual = !openSpiritual }) {
                Field("Accepted Jesus", child.acceptedJesus.name)
                Field("Accepted Jesus date", child.acceptedJesusDate?.asHuman() ?: "-")
                Field("Who prayed", child.whoPrayed.name)
                Field("Who prayed (Other)", child.whoPrayedOther.ifBlank { "-" })
                Field("Who prayed (ID)", child.whoPrayedId.ifBlank { "-" })
                Field("Outcome", child.outcome.ifBlank { "-" })
                Field("General comments", child.generalComments.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Sponsorship", openSponsorship, { openSponsorship = !openSponsorship }) {
                Field("Sponsored for education", if (child.partnershipForEducation) "Yes" else "No")
                Field("Sponsor ID", child.partnerId.ifBlank { "-" })
                Field("Sponsor first name", child.partnerFName.ifBlank { "-" })
                Field("Sponsor last name", child.partnerLName.ifBlank { "-" })
                Field("Sponsor phone 1", child.partnerTelephone1.ifBlank { "-" })
                Field("Sponsor phone 2", child.partnerTelephone2.ifBlank { "-" })
                Field("Sponsor email", child.partnerEmail.ifBlank { "-" })
                Field("Sponsor notes", child.partnerNotes.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Status", openStatus, { openStatus = !openStatus }) {
                Field("Registration step", child.registrationStatus.name)
                Field("Graduated", child.graduated.name)
                Field("Created at", child.createdAt.asHuman())
                Field("Updated at", child.updatedAt.asHuman())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsibleSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            Divider()
            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(160.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun MemberBlock(
    idx: Int,
    first: String?, last: String?,
    relation: String,
    phoneA: String?, phoneB: String?
) {
    Text("Member $idx", style = MaterialTheme.typography.titleSmall)
    Spacer(Modifier.height(4.dp))
    Field("Name", "${first ?: "-"} ${last ?: ""}".trim().ifBlank { "-" })
    Field("Relationship", relation)
    Field("Phone (A)", phoneA ?: "-")
    Field("Phone (B)", phoneB ?: "-")
}

private fun Timestamp.asHuman(): String {
    val sdf = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.getDefault())
    return sdf.format(this.toDate())
}

