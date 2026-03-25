package com.example.charityDept.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.charityDept.presentation.theme.CharityDeptTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildBasicInfoScreen(
    onSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var childId by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var leftHomeDate by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var lastClass by remember { mutableStateOf("") }
    var previousSchool by remember { mutableStateOf("") }
    var genderMenu by remember { mutableStateOf(false) }

    val colors = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(22.dp)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primary)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp) // tighter
    ) {
        SectionHeader("BASIC INFORMATION")

        TwoColumnRow {
            PillField(childId, { childId = it }, "Child ID", fieldShape)
            PillField(firstName, { firstName = it }, "First Name", fieldShape)
        }
        TwoColumnRow {
            PillField(lastName, { lastName = it }, "Last Name", fieldShape)

            ExposedDropdownMenuBox(
                expanded = genderMenu,
                onExpandedChange = { genderMenu = !genderMenu },
                modifier = Modifier.weight(1f)
            ) {
                TextField(
                    value = gender,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Gender") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderMenu) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .height(52.dp), // compact height
                    shape = fieldShape,
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = colors.surface,
                        focusedContainerColor = colors.surface,
                        unfocusedIndicatorColor = colors.surface,
                        focusedIndicatorColor = colors.surface,
                        disabledIndicatorColor = colors.surface,
                        cursorColor = colors.onSurface,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface,
                        focusedLabelColor = colors.onSurface,
                        unfocusedLabelColor = colors.onSurface
                    )
                )
                ExposedDropdownMenu(expanded = genderMenu, onDismissRequest = { genderMenu = false }) {
                    listOf("Male", "Female", "Other").forEach {
                        DropdownMenuItem(text = { Text(it) }, onClick = {
                            gender = it; genderMenu = false
                        })
                    }
                }
            }
        }

        SectionHeader("BACKGROUND INFO")

        TwoColumnRow {
            PillField(leftHomeDate, { leftHomeDate = it }, "Left Home Date", fieldShape,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            PillField(reason, { reason = it }, "Reason", fieldShape)
        }

        SectionHeader("EDUCATION HISTORY")

        TwoColumnRow {
            PillField(lastClass, { lastClass = it }, "Last Class", fieldShape)
            PillField(previousSchool, { previousSchool = it }, "Previous School", fieldShape)
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp), // slightly shorter button
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor   = MaterialTheme.colorScheme.onSecondary
            )
        ) {
            Text(
                text = "Save",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.secondary,
        contentColor = colors.onSecondary,
        shape = RoundedCornerShape(18.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), // tighter
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Start
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PillField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    shape: RoundedCornerShape,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier

            .fillMaxWidth()
            .height(52.dp), // compact height
        singleLine = true,
        shape = shape,
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = colors.surface,
            focusedContainerColor = colors.surface,
            unfocusedIndicatorColor = colors.surface,
            focusedIndicatorColor = colors.surface,
            disabledIndicatorColor = colors.surface,
            cursorColor = colors.onSurface,
            focusedTextColor = colors.onSurface,
            unfocusedTextColor = colors.onSurface,
            focusedLabelColor = colors.onSurface,
            unfocusedLabelColor = colors.onSurface
        )
    )
}

@Composable
private fun TwoColumnRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp), // tighter
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/* ---- Previews ---- */
@Preview(showBackground = true, backgroundColor = 0xFF162D0D)
@Composable
private fun PreviewChildBasicInfo_Light() {
    CharityDeptTheme(darkTheme = false, dynamicColor = false) {
        ChildBasicInfoScreen()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF162D0D)
@Composable
private fun PreviewChildBasicInfo_Dark() {
    CharityDeptTheme(darkTheme = true, dynamicColor = false) {
        ChildBasicInfoScreen()
    }
}

