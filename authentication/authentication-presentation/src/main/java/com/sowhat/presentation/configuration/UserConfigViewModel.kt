package com.sowhat.presentation.configuration

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UserConfigViewModel @Inject constructor() : ViewModel() {
    var id by mutableStateOf("")
    val isValid by derivedStateOf {
        id.length in (2..12)
    }


}