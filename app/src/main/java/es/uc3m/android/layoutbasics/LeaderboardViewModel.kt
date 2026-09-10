package es.uc3m.android.layoutbasics

import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.google.firebase.messaging.FirebaseMessaging

class LeaderboardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups

    private var groupsListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                prepareAndFetchGroups()
            } else {
                groupsListener?.remove()
                _groups.value = emptyList()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun prepareAndFetchGroups() {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                val username = document.getString("username")
                val finalUsername = username ?: currentUser.email?.substringBefore("@") ?: "User"
                startGroupsListener(finalUsername)
            }
    }

    private fun startGroupsListener(username: String) {
        groupsListener?.remove()
        groupsListener = db.collection("groups")
            .whereArrayContains("memberUsernames", username)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener
                val groupList = value?.mapNotNull { it.toObject<Group>().copy(id = it.id) } ?: emptyList()
                _groups.value = groupList
            }
    }

    fun createGroup(name: String, initialEmails: List<String>) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { userDoc ->
            val ownerUsername = userDoc.getString("username") ?: currentUser.email?.substringBefore("@") ?: "User"
            val initialMembers = listOf(hashMapOf("username" to ownerUsername, "points" to 0L, "position" to 1))
            val newGroup = hashMapOf("name" to name, "members" to initialMembers, "memberUsernames" to listOf(ownerUsername), "roundStart" to 0L)
            db.collection("groups").add(newGroup).addOnSuccessListener { docRef ->
                initialEmails.forEach { addMemberToGroup(docRef.id, it) }
            }
        }
    }

    fun addMemberToGroup(groupId: String, email: String) {
        val cleanEmail = email.trim().lowercase()
        db.collection("users").whereEqualTo("email", cleanEmail).get().addOnSuccessListener { result ->
            if (!result.isEmpty) {
                val username = result.documents[0].getString("username") ?: cleanEmail.substringBefore("@")
                db.collection("groups").document(groupId).get().addOnSuccessListener { groupDoc ->
                    val members = groupDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                    if (members.any { it["username"] == username }) return@addOnSuccessListener
                    val newUser = hashMapOf("username" to username, "points" to 0L, "position" to members.size + 1)
                    db.collection("groups").document(groupId).update(
                        "members", FieldValue.arrayUnion(newUser),
                        "memberUsernames", FieldValue.arrayUnion(username)
                    )
                }
            }
        }
    }

    fun leaveGroup(groupId: String) {
        val currentUser = auth.currentUser ?: return
        db.collection("users").document(currentUser.uid).get().addOnSuccessListener { userDoc ->
            val username = userDoc.getString("username") ?: currentUser.email?.substringBefore("@") ?: return@addOnSuccessListener
            val groupRef = db.collection("groups").document(groupId)
            groupRef.get().addOnSuccessListener { groupDoc ->
                if (!groupDoc.exists()) return@addOnSuccessListener
                val currentMembers = groupDoc.get("members") as? List<Map<String, Any>> ?: emptyList()
                val currentUsernames = groupDoc.get("memberUsernames") as? List<String> ?: emptyList()
                val updatedMembers = currentMembers.filter { it["username"] != username }
                val updatedUsernames = currentUsernames.filter { it != username }
                groupRef.update("members", updatedMembers, "memberUsernames", updatedUsernames)
                    .addOnSuccessListener {
                        // no subscribe from the channel
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("group_$groupId")
                            .addOnCompleteListener {
                                Log.d("FCM_DEBUG", "Desuscrito del canal del grupo: $groupId")
                            }
                    }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        groupsListener?.remove()
        authListener?.let { auth.removeAuthStateListener(it) }
    }
}
