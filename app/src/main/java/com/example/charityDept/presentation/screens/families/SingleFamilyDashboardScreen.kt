package com.example.charityDept.presentation.screens.families

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.core.utils.DatesUtils.asHuman
import com.example.charityDept.data.model.Family
import com.example.charityDept.data.model.FamilyMember

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleFamilyDashboardScreen(
    familyIdArg: String,
    onEditFamily: (String) -> Unit,
    onAddMember: (String) -> Unit,
    onEditMember: (String, String) -> Unit,
    navigateUp: () -> Unit,
    vm: SingleFamilyDashboardViewModel = hiltViewModel()
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(familyIdArg) {
        if (familyIdArg.isNotBlank()) vm.load(familyIdArg)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Family Dashboard") },
                navigationIcon = {
                    IconButton(onClick = navigateUp) {
                        Icon(Icons.Outlined.Close, contentDescription = "Close")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                ui.loading -> LoadingSpinner()

                ui.error != null -> ErrorMessage(
                    message = ui.error ?: "An error occurred",
                    onRetry = { vm.load(familyIdArg) }
                )

                ui.family == null -> ErrorMessage(
                    message = "Family not found",
                    onRetry = { vm.load(familyIdArg) }
                )

                else -> {
                    val family = ui.family!!
                    val fid = family.familyId.ifBlank { familyIdArg }

                    val name = family.primaryContactHeadOfHousehold
                        .ifBlank { "Family" }

                    val initials = name
                        .trim()
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .mapNotNull { it.firstOrNull()?.uppercase() }
                        .joinToString("")
                        .ifBlank { "F" }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSecondary
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = fid,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardCardLike(
                                    title = "👤 Family Profile / Edit",
                                    value = "",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onEditFamily(fid)
                                }

                                DashboardCardLike(
                                    title = "➕ Add Member",
                                    value = "",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    onAddMember(fid)
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardCardLike(
                                    title = "👨‍👩‍👧‍👦 Members",
                                    value = ui.memberCount.toString(),
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}

                                DashboardCardLike(
                                    title = "📝 Assessment",
                                    value = "",
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // wire later
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardCardLike(
                                    title = "📄 Case Reference",
                                    value = family.caseReferenceNumber.ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}

                                DashboardCardLike(
                                    title = "📍 Address",
                                    value = family.addressLocation.ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardCardLike(
                                    title = "📞 Phone 1",
                                    value = family.personalPhone1.ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}

                                DashboardCardLike(
                                    title = "📞 Phone 2",
                                    value = family.personalPhone2.ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DashboardCardLike(
                                    title = "🆔 NIN",
                                    value = family.ninNumber.ifBlank { "-" },
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}

                                DashboardCardLike(
                                    title = "📅 Assessment Date",
                                    value = family.dateOfAssessment?.asHuman() ?: "-",
                                    modifier = Modifier.weight(1f),
                                    enabled = false
                                ) {}
                            }
                        }

                        item {
                            Text(
                                text = "Family Members (${ui.memberCount})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        if (ui.members.isEmpty()) {
                            item {
                                ElevatedCard {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "No family members yet",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        } else {
                            items(ui.members, key = { it.familyMemberId }) { member ->
                                MemberRow(
                                    member = member,
                                    onClick = {
                                        onEditMember(member.familyId, member.familyMemberId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCardLike(
    title: String,
    value: String = "",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { if (enabled) onClick() },
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (value.isNotBlank()) {
                Spacer(Modifier.padding(top = 6.dp))
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: FamilyMember,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = listOf(member.fName, member.lName)
                    .filter { it.isNotBlank() }
                    .joinToString(" ")
                    .ifBlank { "Unnamed Member" },
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Relationship: ${member.relationship.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Age: ${if (member.age > 0) member.age.toString() else "-"} • Gender: ${member.gender.ifBlank { "-" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "Tap to edit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun LoadingSpinner() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Spacer(modifier = Modifier.padding(top = 12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}