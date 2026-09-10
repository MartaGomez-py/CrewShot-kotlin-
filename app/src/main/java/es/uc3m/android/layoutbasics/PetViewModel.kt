package es.uc3m.android.layoutbasics

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userListener: ListenerRegistration? = null

    private val prefs = getApplication<Application>()
        .getSharedPreferences("pet_prefs", Context.MODE_PRIVATE)

    //  XP (Synced with Firestore)
    private val _xp = mutableStateOf(prefs.getInt("xp", 0))
    val xp: State<Int> = _xp

    val level: Int
        get() = _xp.value / 100

    //  PET NAME
    private val _petName = mutableStateOf(prefs.getString("pet_name", "Mochi") ?: "Mochi")
    val petName: State<String> = _petName

    //  SELECTED PET
    private val _selectedPet = mutableStateOf(prefs.getInt("selected_pet", 1))
    val selectedPet: State<Int> = _selectedPet

    init {
        startUserSync()
    }

    private fun startUserSync() {
        auth.addAuthStateListener { firebaseAuth ->
            val currentUser = firebaseAuth.currentUser
            userListener?.remove()
            
            if (currentUser != null) {
                userListener = db.collection("users").document(currentUser.uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("PetViewModel", "Error syncing user data", e)
                            return@addSnapshotListener
                        }
                        
                        if (snapshot != null && snapshot.exists()) {
                            val firestoreXp = snapshot.getLong("xp")?.toInt() ?: 0
                            if (_xp.value != firestoreXp) {
                                _xp.value = firestoreXp
                                prefs.edit().putInt("xp", firestoreXp).apply()
                            }
                        }
                    }
            }
        }
    }

    fun addXp(amount: Int) {
        val newXp = _xp.value + amount
        _xp.value = newXp
        prefs.edit().putInt("xp", newXp).apply()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users").document(currentUser.uid)
                .update("xp", newXp)
                .addOnFailureListener { e ->
                    Log.e("PetViewModel", "Error updating XP in Firestore", e)
                }
        }
    }

    fun changeName(newName: String) {
        _petName.value = newName
        prefs.edit().putString("pet_name", newName).apply()
    }

    fun changePet(index: Int) {
        _selectedPet.value = index
        prefs.edit().putInt("selected_pet", index).apply()
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
