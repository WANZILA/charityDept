package com.example.charityDept.presentation.screens.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.charityDept.data.model.Child
import com.example.charityDept.presentation.components.common.ProfileAvatar


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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                profileImageLocalPath = child.profileImageLocalPath,
                profileImg = child.profileImg
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
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
}