package com.example.charityDept.presentation.screens.families

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleLeft
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.core.utils.DatesUtils.asHuman
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember
import com.example.charityDept.presentation.screens.widgets.DeleteIconWithConfirm
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyDetailsScreen(
    familyIdArg: String,
    onEdit: (String) -> Unit,
    onAddMember: (String) -> Unit,
    toFamilyList: () -> Unit,
    navigateUp: () -> Unit,
    vm: FamilyDetailsViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(familyIdArg) {
        vm.load(familyIdArg)
    }

    LaunchedEffect(Unit) {
        vm.events.collectLatest { ev ->
            when (ev) {
                is FamilyDetailsViewModel.Event.Deleted -> {
                    snackbarHostState.showSnackbar("Family deleted")
                    toFamilyList()
                }
                is FamilyDetailsViewModel.Event.Error -> {
                    snackbarHostState.showSnackbar(ev.msg)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        ui.family?.primaryContactHeadOfHousehold?.ifBlank { "Family details" }
                            ?: "Family details",
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Filled.ArrowCircleLeft, contentDescription = "Back")
                    }
                },
                actions = {
                    ui.family?.let { family ->
                        IconButton(onClick = { onEdit(family.familyId) }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { vm.refresh(family.familyId) }, enabled = !ui.deleting) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = { onAddMember(family.familyId) } ) {
                            Icon(Icons.Outlined.AddBox, contentDescription = "Add")
                        }

                        DeleteIconWithConfirm(
                            label = "family ${family.primaryContactHeadOfHousehold}".trim(),
                            deleting = ui.deleting,
                            onDelete = { vm.deleteFamilyOptimistic() }
                        )

                        IconButton(onClick = toFamilyList) {
                            Icon(Icons.Outlined.Close, contentDescription = "Close")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            when {
                ui.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                ui.error != null -> Text(
                    "Error: ${ui.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                ui.family != null -> FamilyDetailsContent(
                    family = ui.family!!,
                    members = ui.members
                )
            }
        }
    }
}

@Composable
private fun FamilyDetailsContent(
    family: Family,
    members: List<FamilyMember>
) {
    var openBasic by rememberSaveable { mutableStateOf(true) }
    var openAncestral by rememberSaveable { mutableStateOf(false) }
    var openRental by rememberSaveable { mutableStateOf(false) }
    var openMembers by rememberSaveable { mutableStateOf(true) }
    var openMeta by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CollapsibleSection("Basic Info", openBasic, { openBasic = !openBasic }) {
                Field("Head of household", family.primaryContactHeadOfHousehold.ifBlank { "-" })
                Field("Case reference", family.caseReferenceNumber.ifBlank { "-" })
                Field("Address / Location", family.addressLocation.ifBlank { "-" })
                Field("Born again", if (family.isBornAgain) "Yes" else "No")
                Field("Phone 1", family.personalPhone1.ifBlank { "-" })
                Field("Phone 2", family.personalPhone2.ifBlank { "-" })
                Field("NIN Number", family.ninNumber.ifBlank { "-" })
                Field("Date of assessment", family.dateOfAssessment?.asHuman() ?: "-")
            }
        }

        item {
            CollapsibleSection("Ancestral Location", openAncestral, { openAncestral = !openAncestral }) {
                Field("Country", family.memberAncestralCountry.ifBlank { "-" })
                Field("Region", family.memberAncestralRegion.ifBlank { "-" })
                Field("District", family.memberAncestralDistrict.ifBlank { "-" })
                Field("County", family.memberAncestralCounty.ifBlank { "-" })
                Field("Sub-county", family.memberAncestralSubCounty.ifBlank { "-" })
                Field("Parish", family.memberAncestralParish.ifBlank { "-" })
                Field("Village", family.memberAncestralVillage.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Rental Location", openRental, { openRental = !openRental }) {
                Field("Country", family.memberRentalCountry.ifBlank { "-" })
                Field("Region", family.memberRentalRegion.ifBlank { "-" })
                Field("District", family.memberRentalDistrict.ifBlank { "-" })
                Field("County", family.memberRentalCounty.ifBlank { "-" })
                Field("Sub-county", family.memberRentalSubCounty.ifBlank { "-" })
                Field("Parish", family.memberRentalParish.ifBlank { "-" })
                Field("Village", family.memberRentalVillage.ifBlank { "-" })
            }
        }

        item {
            CollapsibleSection("Family Members", openMembers, { openMembers = !openMembers }) {
                if (members.isEmpty()) {
                    Text("No family members yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    members.forEachIndexed { index, member ->
                        MemberBlock(member)
                        if (index < members.lastIndex) Divider()
                    }
                }
            }
        }

        item {
            CollapsibleSection("Meta", openMeta, { openMeta = !openMeta }) {
                Field("Family ID", family.familyId.ifBlank { "-" })
                Field("Created at", family.createdAt.asHuman())
                Field("Updated at", family.updatedAt.asHuman())
                Field("Version", family.version.toString())
                Field("Dirty", if (family.isDirty) "Yes" else "No")
                Field("Deleted", if (family.isDeleted) "Yes" else "No")
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
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun MemberBlock(member: FamilyMember) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Field("first Name", member.fName.ifBlank { "-" })
        Field("Last Name", member.lName.ifBlank { "-" })
        Field("Relationship", member.relationship.ifBlank { "-" })
        Field("Age", if (member.age > 0) member.age.toString() else "-")
        Field("Gender", member.gender.ifBlank { "-" })
        Field("Occupation / School Grade", member.occupationOrSchoolGrade.ifBlank { "-" })
        Field("Health / Disability Status", member.healthOrDisabilityStatus.ifBlank { "-" })
        Field("Phone 1", member.personalPhone1.ifBlank { "-" })
        Field("Phone 2", member.personalPhone2.ifBlank { "-" })
        Field("NIN Number", member.ninNumber.ifBlank { "-" })
    }
}