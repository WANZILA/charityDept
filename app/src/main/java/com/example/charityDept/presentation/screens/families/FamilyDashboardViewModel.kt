package com.example.charityDept.presentation.screens.families

import androidx.lifecycle.ViewModel
import com.example.charityDept.data.local.dao.FamilyDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class FamilyDashboardUi(
    val familiesTotal: Int = 0,
    val familyMembersTotal: Int = 0
)

@HiltViewModel
class FamilyDashboardViewModel @Inject constructor(
    familyDao: FamilyDao
) : ViewModel() {

    val ui: Flow<FamilyDashboardUi> = combine(
        familyDao.observeActiveFamilyCount(),
        familyDao.observeActiveFamilyMemberCount()
    ) { familiesTotal, familyMembersTotal ->
        FamilyDashboardUi(
            familiesTotal = familiesTotal,
            familyMembersTotal = familyMembersTotal
        )
    }
}