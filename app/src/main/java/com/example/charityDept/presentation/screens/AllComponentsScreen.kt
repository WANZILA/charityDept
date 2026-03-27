package com.example.charityDept.presentation.screens

//import ZionKidAppTopBar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.charityDept.presentation.components.action.CharityDeptAppTopBar
//import com.example.charityDept.presentation.components.action.ZionKidAppTopBar
import com.example.charityDept.presentation.theme.CharityDeptTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllComponentsScreen(
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    var toggle by remember { mutableStateOf(false) }
    var check by remember { mutableStateOf(false) }
    var radio by remember { mutableStateOf("Option 1") }
    var sliderPos by remember { mutableStateOf(0f) }
    var textField by remember { mutableStateOf("") }
    var menuExpanded by remember { mutableStateOf(false) }
    val menuOptions = listOf("One", "Two", "Three")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor   = MaterialTheme.colorScheme.onBackground,
        topBar = {
            CharityDeptAppTopBar(
                canNavigateBack = true,
                navigateUp = { /* no-op */ },
            )
           // CenterAlignedTopAppBar(title = { Text("Component Gallery") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { /* show snackbar, etc. */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Buttons", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Filled") }
                OutlinedButton(onClick = {}) { Text("Outlined") }
                TextButton(onClick = {}) { Text("Text") }
                ElevatedButton(onClick = {}) { Text("Elevated") }
                FilledTonalButton(onClick = {}) { Text("Tonal") }
            }

            Text("Text Fields", style = MaterialTheme.typography.titleLarge)
            TextField(
                value = textField,
                onValueChange = { textField = it },
                label = { Text("Filled") }
            )
            OutlinedTextField(
                value = textField,
                onValueChange = { textField = it },
                label = { Text("Outlined") }
            )

            Text("Selection Controls", style = MaterialTheme.typography.titleLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Switch(checked = toggle, onCheckedChange = { toggle = it })
                Checkbox(checked = check, onCheckedChange = { check = it })
                RadioButton(selected = radio == "Option 1", onClick = { radio = "Option 1" })
                RadioButton(selected = radio == "Option 2", onClick = { radio = "Option 2" })
                Text("Selected: $radio")
            }

            Text("Slider", style = MaterialTheme.typography.titleLarge)
            Slider(value = sliderPos, onValueChange = { sliderPos = it })

            Text("Progress Indicators", style = MaterialTheme.typography.titleLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp))
                LinearProgressIndicator(progress = 0.5f, modifier = Modifier.fillMaxWidth(0.5f))
            }

            Text("Menus", style = MaterialTheme.typography.titleLarge)
            ExposedDropdownMenuBox(
                expanded = menuExpanded,
                onExpandedChange = { menuExpanded = !menuExpanded }
            ) {
                TextField(
                    value = radio,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Choose") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    menuOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                radio = option
                                menuExpanded = false
                            }
                        )
                    }
                }
            }

            Text("Cards", style = MaterialTheme.typography.titleLarge)
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Card title", style = MaterialTheme.typography.titleMedium)
                    Text("Card body text goes here.", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(Modifier.height(64.dp)) // bottom padding for FAB
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAllComponentsLight() {
    // Force brand colors (no dynamic theming)
    CharityDeptTheme(darkTheme = false, dynamicColor = false) {
        AllComponentsScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAllComponentsDark() {
    // Force brand colors (no dynamic theming)
    CharityDeptTheme(darkTheme = true, dynamicColor = false) {
        AllComponentsScreen()
    }
}

