package com.example.charityDept.presentation.viewModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.example.charityDept.data.model.Kid
//import com.example.charityDept.domain.KidRegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val  TAG = "KidRegistrationViewModel"

@HiltViewModel
class KidRegistrationViewModel @Inject constructor(
    ///private val kidRegisRepo: KidRegistrationRepository,
): ViewModel() {

//    private val _isLoggedIn: MutableStateFlow<Boolean> = MutableStateFlow(false)
//    var isLoggedIn: StateFlow<Boolean> = _isLoggedIn
//
//    private val _kids = MutableStateFlow<List<Kid>>(emptyList())
//    val kids: StateFlow<List<Kid>> get() = _kids
//
//    private val _uiState: MutableStateFlow<KidRegisUiState> = MutableStateFlow(KidRegisUiState.Loading)
//    val uiState: StateFlow<KidRegisUiState> = _uiState.asStateFlow()
//
//    init {
//        _isLoggedIn.value = true
//    }
//
//    fun setEditing(){
//        _uiState.tryEmit(KidRegisUiState.Loading)
//    }
//
//    fun fetchData() {
//        _uiState.tryEmit(KidRegisUiState.Loading)
//        viewModelScope.launch {
//            kidRegisRepo.fetchAllKids().catch {  exception ->
//                Log.d(TAG, "Fetching data failed: $exception")
//                _uiState.tryEmit(KidRegisUiState.Error("Fetching data failed: $exception"))
//            }.collect { allKids ->
//                _kids.value = allKids
//                _uiState.tryEmit(KidRegisUiState.Loaded)
//            }
//        }
//    }
//
//    fun saveData(kid: Kid){
//        _uiState.tryEmit(KidRegisUiState.Loading)
//        viewModelScope.launch {
//            kidRegisRepo.saveKid(kid)
//            _uiState.tryEmit(KidRegisUiState.Loaded)
//        }
//    }
//

}

sealed class KidRegisUiState{
    object Loading: KidRegisUiState()
    object Loaded : KidRegisUiState()
    object Editing: KidRegisUiState()
    class Error(val errorMessage: String) : KidRegisUiState()
}
