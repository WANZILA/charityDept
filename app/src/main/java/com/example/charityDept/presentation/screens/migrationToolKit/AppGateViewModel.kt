package com.example.charityDept.presentation.screens.migrationToolKit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.charityDept.core.di.AppVersionCode
import com.example.charityDept.domain.repositories.online.AppUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class UpdateUiState(
    val loading: Boolean = true,
    val forceUpdate: Boolean = false,
    val showSoftPrompt: Boolean = false,
    val message: String? = null,
    val downloadUrl: String? = null
)

@HiltViewModel
class AppGateViewModel @Inject constructor(
    private val updateRepo: AppUpdateRepository,
    @AppVersionCode private val appVersionCode: Int
) : ViewModel() {

    private val _ui = MutableStateFlow(UpdateUiState())
    val ui: StateFlow<UpdateUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            updateRepo.streamConfig().collect { cfg ->
                val current = appVersionCode
                val min = cfg?.minVersionCode?.toIntOrNull() ?: 1
                val latest = cfg?.latestVersionCode?.toIntOrNull()

                val forceBlock = (cfg?.force == true) && current < min
                val softPrompt = (latest != null && current < latest && !forceBlock)

                _ui.value = UpdateUiState(
                    loading = false,
                    forceUpdate = forceBlock,
                    showSoftPrompt = softPrompt,
                    message = (if (forceBlock) cfg?.forceMessage else cfg?.softMessage),
                    downloadUrl = cfg?.downloadUrl
                )
            }
        }
    }
}


