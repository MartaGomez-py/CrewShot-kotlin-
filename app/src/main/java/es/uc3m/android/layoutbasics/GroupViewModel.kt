package es.uc3m.android.layoutbasics

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class GroupViewModel : ViewModel() {

    private val _selectedGroupId = mutableStateOf<String?>(null)
    val selectedGroupId: State<String?> = _selectedGroupId

    fun selectGroup(id: String?) {
        _selectedGroupId.value = id
    }
}