package es.uc3m.android.layoutbasics

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID
import android.content.Context

class PostViewModel : ViewModel() {
    private val _posts = mutableStateListOf<Post>()
    val posts: List<Post> = _posts
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentListener: com.google.firebase.firestore.ListenerRegistration? = null

    val currentTime = System.currentTimeMillis()
    fun uploadPost(
        username: String,
        bitmap: Bitmap,
        groupId: String?,
        onComplete: (String?) -> Unit
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fileName = "posts/${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val data = baos.toByteArray()

        firestore.collection("posts")
            .whereEqualTo("authorId", currentUserId)
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { result ->

                if (!result.isEmpty) {
                    onComplete("You already posted in this group")
                    return@addOnSuccessListener
                }

                storageRef.putBytes(data)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { uri ->
                            val imageUrl = uri.toString()
                            savePostToFirestore(username, imageUrl, groupId, onComplete)
                        }.addOnFailureListener { onComplete("Upload failed") }
                    }
                    .addOnFailureListener { e ->
                        Log.e("PostViewModel", "Error uploading image", e)
                        onComplete("Upload failed")
                    }
            }
    }

    private fun savePostToFirestore(
        username: String,
        imageUrl: String,
        groupId: String?,
        onComplete: (String?) -> Unit
    ) {
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: ""
        val db = FirebaseFirestore.getInstance()

        val postData = hashMapOf(
            "authorId" to userId,
            "user" to username,
            "imageUrl" to imageUrl,
            "timestamp" to System.currentTimeMillis(),
            "votes" to 0,
            "votedBy" to emptyList<String>(),
            "groupId" to groupId
        )

        // Add the post
        db.collection("posts").add(postData).addOnSuccessListener {
            // Reward the user for posting (20 XP)
            rewardUserForPosting(userId, username, groupId)
            onComplete(null)
        }.addOnFailureListener { onComplete("Upload failed") }
    }

    // Archivo: PostViewModel.kt

    private fun rewardUserForPosting(userId: String, username: String, groupId: String?) {
        val db = FirebaseFirestore.getInstance()
        val authorRef = db.collection("users").document(userId)
        val groupRef = if (!groupId.isNullOrEmpty()) db.collection("groups").document(groupId) else null

        val currentTime = System.currentTimeMillis()

        db.runTransaction { transaction ->
            val authorSnapshot = transaction.get(authorRef)
            val groupSnapshot = if (groupRef != null) transaction.get(groupRef) else null

            var reward = 1L

            // Lógica para el grupo
            if (groupRef != null && groupSnapshot != null && groupSnapshot.exists()) {
                val roundStart = groupSnapshot.getLong("roundStart") ?: 0L
                // Si sube la foto en los primeros 2 minutos, gana 3 puntos, si no 1
                val isOnTime = roundStart > 0L && currentTime - roundStart <= 2 * 60 * 1000
                reward = if (isOnTime) 3L else 1L

                @Suppress("UNCHECKED_CAST")
                val members = groupSnapshot.get("members") as? List<Map<String, Any>> ?: emptyList()

                var memberFound = false
                val updatedMembers = members.map { member ->
                    val m = member.toMutableMap()
                    val mUsername = m["username"]?.toString()?.trim() ?: ""

                    if (mUsername.equals(username.trim(), ignoreCase = true)) {
                        val p = (m["points"] as? Number)?.toLong() ?: 0L
                        m["points"] = p + reward
                        memberFound = true
                    }
                    m
                }

                if (memberFound) {
                    // Reordenar ranking
                    val sortedMembers = updatedMembers.sortedByDescending {
                        (it["points"] as? Number)?.toLong() ?: 0L
                    }
                    val finalMembers = sortedMembers.mapIndexed { index, m ->
                        val map = m.toMutableMap()
                        map["position"] = index + 1
                        map
                    }
                    transaction.update(groupRef, "members", finalMembers)
                }
            }

            // Actualizar XP global del usuario (Lectura ya hecha arriba)
            if (authorSnapshot.exists()) {
                val currentXp = authorSnapshot.getLong("xp") ?: 0L
                transaction.update(authorRef, "xp", currentXp + reward)
            }
        }.addOnFailureListener { e ->
            Log.e("XP_DEBUG", "La transacción falló: ", e)
        }
    }

    fun voteForPost(postId: String, authorId: String) {
        if (authorId.isEmpty()) return
        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        val postRef = db.collection("posts").document(postId)
        val authorRef = db.collection("users").document(authorId)

        db.runTransaction { transaction ->
            val postSnapshot = transaction.get(postRef)
            val authorSnapshot = transaction.get(authorRef)
            val authorUsername = authorSnapshot.getString("username")?.trim() ?: postSnapshot.getString("user")?.trim() ?: ""
            val groupId = postSnapshot.getString("groupId")
            val groupRef = if (!groupId.isNullOrEmpty()) db.collection("groups").document(groupId) else null
            val groupSnapshot = if (groupRef != null) transaction.get(groupRef) else null

            @Suppress("UNCHECKED_CAST")
            val votedBy = postSnapshot.get("votedBy") as? List<String> ?: emptyList()
            val hasVoted = votedBy.contains(currentUserId)

            val voteDelta = if (hasVoted) -1L else 1L
            val xpDelta = if (hasVoted) -1L else 1L

            // Post votes
            if (hasVoted) transaction.update(postRef, "votedBy", FieldValue.arrayRemove(currentUserId))
            else transaction.update(postRef, "votedBy", FieldValue.arrayUnion(currentUserId))
            transaction.update(postRef, "votes", (postSnapshot.getLong("votes") ?: 0L) + voteDelta)

            // Author global XP
            if (authorSnapshot.exists()) {
                transaction.update(authorRef, "xp", (authorSnapshot.getLong("xp") ?: 0L) + voteDelta)
            }

            // Group Leaderboard
            if (groupRef != null && groupSnapshot != null && groupSnapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                val members = groupSnapshot.get("members") as? List<Map<String, Any>> ?: emptyList()
                val updatedMembers = members.map { member ->
                    val m = member.toMutableMap()
                    if ((m["username"]?.toString()?.trim()).equals(authorUsername, ignoreCase = true)) {
                        m["points"] = ((m["points"] as? Number)?.toLong() ?: 0L) + voteDelta
                    }
                    m
                }
                val sorted = updatedMembers.sortedByDescending { (it["points"] as? Number)?.toLong() ?: 0L }
                val finalMembers = sorted.mapIndexed { i, m -> m.toMutableMap().apply { this["position"] = i + 1 } }
                transaction.update(groupRef, "members", finalMembers)
            }
        }
    }

    fun listenToPosts(groupId: String? = null) {
        currentListener?.remove()
        var query: Query = firestore.collection("posts").orderBy("timestamp", Query.Direction.DESCENDING)
        if (groupId != null) query = query.whereEqualTo("groupId", groupId)

        currentListener = query.addSnapshotListener { snapshot, e ->
            if (e != null || snapshot == null) return@addSnapshotListener
            _posts.clear()
            for (doc in snapshot.documents) {
                val post = Post(
                    id = doc.id,
                    authorId = doc.getString("authorId") ?: "",
                    user = doc.getString("user") ?: "Unknown",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    timestamp = doc.getLong("timestamp") ?: 0L,
                    votes = doc.getLong("votes")?.toInt() ?: 0,
                    votedBy = doc.get("votedBy") as? List<String> ?: emptyList(),
                    groupId = doc.getString("groupId") ?: ""
                )
                _posts.add(post)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        currentListener?.remove()
    }


}

