package com.example.charityDept.presentation.screens.children

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.charityDept.R
import com.example.charityDept.data.model.ClassGroup
import com.example.charityDept.data.model.EducationPreference
import com.example.charityDept.presentation.components.action.ZionKidAppTopBar
import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.saveable.rememberSaveable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildrenDashboardScreen(
    toChildrenList: () -> Unit,
    toAddChild: () -> Unit,
    toChildrenByEducation: (EducationPreference) -> Unit,
    toReunited: () -> Unit,
    toAllRegions: () -> Unit,
    toAllStreets: () -> Unit,
    vm: ChildrenDashboardViewModel = hiltViewModel(),
    authVM: AuthViewModel = hiltViewModel()
) {
    // lifecycle-aware collection (works great with Room flows)
    val ui by vm.ui.collectAsStateWithLifecycle()
    val authUi by authVM.ui.collectAsStateWithLifecycle()
    val canCreateChild = authUi.perms.canCreateChild

    var classOpen by rememberSaveable { mutableStateOf(false) }
    var eduOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(classOpen) { vm.setClassExpanded(classOpen) }
    LaunchedEffect(eduOpen) { vm.setEduExpanded(eduOpen) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            ZionKidAppTopBar(
                canNavigateBack = false,
                navigateUp = { /* no-op */ },
                txtLabel = stringResource(R.string.children),
            )
        },
        // Optional FAB; uncomment if you want quick add
        // floatingActionButton = {
        //     if (canCreateChild) {
        //         FloatingActionButton(
        //             onClick = toAddChild,
        //             containerColor = MaterialTheme.colorScheme.secondary,
        //             contentColor   = MaterialTheme.colorScheme.onSecondary
        //         ) { Icon(Icons.Default.Add, contentDescription = "Add") }
        //     }
        // }
    ) { inner ->
        when {
            ui.loading -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            ui.error != null -> Box(
                Modifier
                    .padding(inner)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { Text("Error: ${ui.error}", color = MaterialTheme.colorScheme.error) }

            else -> {
                DebugSyncBanner() // shows Local/Dirty from Room
                LazyColumn(
                    modifier = Modifier
                        .padding(inner)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // KPI Cards
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
//                            StatCard("Total", ui.total.toString(), Modifier.weight(1f))
                            // inProgram = registration incomplete (not fully registered)
//                            StatCard("In Program", ui.inProgram.toString(), Modifier.weight(1f))
                        }
                    }

                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (canCreateChild) {
                                StatCard(
                                    title = "Children",
                                    value = "Add New",
                                    modifier = Modifier.weight(1f),
                                    onClick = toAddChild
                                )
                            }
                            StatCard(
                                title = "Children",
                                value = "View All",
                                modifier = Modifier.weight(1f),
                                onClick = toChildrenList
                            )
                        }
                    }
                    // Class Group distribution (Room-driven)
                    item {
                        ExpandableSectionCard(
                            title = "Class Group",
                            expanded = classOpen,
                            onToggle = { classOpen = !classOpen }
                        ) {
                            val total = ui.classDist.values.sum().let { if (it <= 0) 1 else it }

                            ClassGroup.values().forEach { g ->
                                val count = ui.classDist[g] ?: 0
                                ClassGroupRow(
                                    label = labelForClassGroupDashboard(g),
                                    count = count,
                                    total = total
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }


                    // Education distribution (Room-driven)
                    item {
                        ExpandableSectionCard(
                            title = "Education Preference",
                            expanded = eduOpen,
                            onToggle = { eduOpen = !eduOpen }
                        ) {
                            val total = ui.eduDist.values.sum().let { if (it <= 0) 1 else it }

                            EducationPreference.values().forEach { pref ->
                                val count = ui.eduDist[pref] ?: 0
                                EducationPreferenceRow(
                                    label = pref.name,
                                    count = count,
                                    total = total,
                                    onClick = { toChildrenByEducation(pref) }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }


                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "All",
                                value = "Streets",
                                modifier = Modifier.weight(1f),
                                onClick = toAllStreets
                            )
//                            StatCard(
//                                title = "All",
//                                value = "Regions",
//                                modifier = Modifier.weight(1f),
//                                onClick = toAllRegions
//                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    sub: String = "",
    onClick: (() -> Unit)? = null
) {
    ElevatedCard(
        modifier = modifier,
        onClick = { onClick?.invoke() },
        enabled = onClick != null
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotBlank()) {
                Text(
                    sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ExpandableSectionCard(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EducationPreferenceRow(
    label: String,
    count: Int,
    total: Int,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, enabled = true) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text("Count: $count", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (count / total.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

@Composable
fun DebugSyncBanner(vm: ChildrenDashboardViewModel = hiltViewModel()) {
    val health by vm.health.collectAsStateWithLifecycle()
    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Local: ${health.localCount}")
            Text("Dirty: ${health.dirtyCount}")
        }
    }
}

@Composable
private fun ClassGroupRow(
    label: String,
    count: Int,
    total: Int
) {
    val pct = ((count * 100f) / total.toFloat()).coerceIn(0f, 100f)

    ElevatedCard {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Count: $count (${String.format("%.0f", pct)}%)",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = (count / total.toFloat()).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}

private fun labelForClassGroupDashboard(v: ClassGroup): String = when (v) {
    ClassGroup.SERGEANT -> "Sergeant (0–5)"
    ClassGroup.LIEUTENANT -> "Lieutenant (6–9)"
    ClassGroup.CAPTAIN -> "Captain (10–12)"
    ClassGroup.GENERAL -> "General (13–18)"
    ClassGroup.MAJOR -> "Major (19–21)"
    ClassGroup.COMMANDER -> "Commander (22–25)"
}

//package com.example.charityDept.presentation.screens.children
//
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.dp
//import androidx.hilt.navigation.compose.hiltViewModel
//import androidx.lifecycle.compose.collectAsStateWithLifecycle
//import com.example.charityDept.R
//import com.example.charityDept.data.model.EducationPreference
//import com.example.charityDept.presentation.components.action.ZionKidAppTopBar
//import com.example.charityDept.presentation.viewModels.auth.AuthViewModel
//import com.example.charityDept.presentation.screens.children.ChildrenDashboardViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ChildrenDashboardScreen(
//    toChildrenList: () -> Unit,
//    toAddChild: () -> Unit,
//    toChildrenByEducation: (EducationPreference) -> Unit,
//    toReunited: () -> Unit,
//    toAllRegions: () -> Unit,
//    toAllStreets: () -> Unit,
//    vm: ChildrenDashboardViewModel = hiltViewModel(),
//    authVM: AuthViewModel = hiltViewModel()
//) {
//    // lifecycle-aware collection (works great with Room flows)
//    val ui by vm.ui.collectAsStateWithLifecycle()
//    val authUi by authVM.ui.collectAsStateWithLifecycle()
//    val canCreateChild = authUi.perms.canCreateChild
//
//    Scaffold(
//        containerColor = MaterialTheme.colorScheme.background,
//        contentColor   = MaterialTheme.colorScheme.onBackground,
//        topBar = {
//            ZionKidAppTopBar(
//                canNavigateBack = false,
//                navigateUp = { /* no-op */ },
//                txtLabel = stringResource(R.string.children),
//            )
//        },
//        // Optional FAB; uncomment if you want quick add
//        // floatingActionButton = {
//        //     if (canCreateChild) {
//        //         FloatingActionButton(
//        //             onClick = toAddChild,
//        //             containerColor = MaterialTheme.colorScheme.secondary,
//        //             contentColor   = MaterialTheme.colorScheme.onSecondary
//        //         ) { Icon(Icons.Default.Add, contentDescription = "Add") }
//        //     }
//        // }
//    ) { inner ->
//        when {
//            ui.loading -> Box(
//                Modifier
//                    .padding(inner)
//                    .fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) { CircularProgressIndicator() }
//
//            ui.error != null -> Box(
//                Modifier
//                    .padding(inner)
//                    .fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) { Text("Error: ${ui.error}", color = MaterialTheme.colorScheme.error) }
//
//            else -> {
//                DebugSyncBanner() // shows Local/Dirty from Room
//                LazyColumn(
//                    modifier = Modifier
//                        .padding(inner)
//                        .fillMaxSize(),
//                    contentPadding = PaddingValues(16.dp),
//                    verticalArrangement = Arrangement.spacedBy(12.dp)
//                ) {
//                    // KPI Cards
//                    item {
//                        Row(
//                            Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            StatCard("Total", ui.total.toString(), Modifier.weight(1f))
//                            // inProgram = registration incomplete (not fully registered)
//                            StatCard("In Program", ui.inProgram.toString(), Modifier.weight(1f))
//                        }
//                    }
//
//                    item {
//                        Row(
//                            Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            if (canCreateChild) {
//                                StatCard(
//                                    title = "Children",
//                                    value = "Add New",
//                                    modifier = Modifier.weight(1f),
//                                    onClick = toAddChild
//                                )
//                            }
//                            StatCard(
//                                title = "Children",
//                                value = "View All",
//                                modifier = Modifier.weight(1f),
//                                onClick = toChildrenList
//                            )
//                        }
//                    }
//
//                    // Education distribution (Room-driven)
//                    item {
//                        SectionCard(title = "Education Preference") {
//                            // guard divide-by-zero for progress bars
//                            val total = ui.eduDist.values.sum().let { if (it <= 0) 1 else it }
//                            EducationPreference.values().forEach { pref ->
//                                val count = ui.eduDist[pref] ?: 0
//                                EducationPreferenceRow(
//                                    label = pref.name,
//                                    count = count,
//                                    total = total,
//                                    onClick = { toChildrenByEducation(pref) }
//                                )
//                                Spacer(Modifier.height(6.dp))
//                            }
//                        }
//                    }
//
//                    item {
//                        Row(
//                            Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.spacedBy(12.dp)
//                        ) {
//                            StatCard(
//                                title = "All",
//                                value = "Streets",
//                                modifier = Modifier.weight(1f),
//                                onClick = toAllStreets
//                            )
//                            StatCard(
//                                title = "All",
//                                value = "Regions",
//                                modifier = Modifier.weight(1f),
//                                onClick = toAllRegions
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun StatCard(
//    title: String,
//    value: String,
//    modifier: Modifier = Modifier,
//    sub: String = "",
//    onClick: (() -> Unit)? = null
//) {
//    ElevatedCard(
//        modifier = modifier,
//        onClick = { onClick?.invoke() },
//        enabled = onClick != null
//    ) {
//        Column(Modifier.padding(12.dp)) {
//            Text(
//                title,
//                style = MaterialTheme.typography.labelMedium,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                value,
//                style = MaterialTheme.typography.headlineSmall,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
//            if (sub.isNotBlank()) {
//                Text(
//                    sub,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant
//                )
//            }
//        }
//    }
//}
//
//@Composable
//private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
//    ElevatedCard {
//        Column(Modifier.padding(12.dp)) {
//            Text(title, style = MaterialTheme.typography.titleMedium)
//            Spacer(Modifier.height(8.dp))
//            content()
//        }
//    }
//}
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//private fun EducationPreferenceRow(
//    label: String,
//    count: Int,
//    total: Int,
//    onClick: () -> Unit
//) {
//    ElevatedCard(onClick = onClick, enabled = true) {
//        Column(Modifier.padding(12.dp)) {
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
//                Text(label, style = MaterialTheme.typography.bodyMedium)
//                Text("Count: $count", style = MaterialTheme.typography.bodyMedium)
//            }
//            Spacer(Modifier.height(6.dp))
//            LinearProgressIndicator(
//                progress = (count / total.toFloat()).coerceIn(0f, 1f),
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(6.dp)
//            )
//        }
//    }
//}
//
//@Composable
//fun DebugSyncBanner(vm: ChildrenDashboardViewModel = hiltViewModel()) {
//    val health by vm.health.collectAsStateWithLifecycle()
//    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
//        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
//            Text("Local: ${health.localCount}")
//            Text("Dirty: ${health.dirtyCount}")
//        }
//    }
//}

