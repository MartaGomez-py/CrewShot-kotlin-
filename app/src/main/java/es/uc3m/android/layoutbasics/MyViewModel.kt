/*
package es.uc3m.android.layoutbasics

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyViewModel : ViewModel() {
    // Aquí inicializamos la conexión a Firestore
    private val firestore = FirebaseFirestore.getInstance()

    // El StateFlow que guardará tus datos
    private val _notes = MutableStateFlow<List<String>>(emptyList())
    val notes = _notes.asStateFlow()

    fun fetchData() {
        firestore.collection("notas")
            .get()
            .addOnSuccessListener { result ->
                // Guardamos los documentos en la lista
                val list = result.map { it.getString("titulo") ?: "" }
                _notes.value = list
            }
    }
}
*/