package com.example.charityDept.presentation.screens.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.charityDept.data.model.Child

@Composable
fun ChildItem (
    child: Child,
    onClicked: (Child) -> Unit = {},
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClicked(child) }
    ) {
        Column(modifier = Modifier.weight(1f)){

            Text(
                text = child.fName,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = child.lName,
                fontSize = 30.sp,
                )
        }
    }
}
