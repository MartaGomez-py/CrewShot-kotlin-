package es.uc3m.android.layoutbasics

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Log

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // En MyFirebaseMessagingService.kt
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Extraer el ID del grupo de los datos de la notificación
        val groupId = remoteMessage.data["groupId"]
        val groupName = remoteMessage.data["groupName"] ?: "tu Crew"


        if (!groupId.isNullOrEmpty()) {

            val db = FirebaseFirestore.getInstance()

            db.collection("groups")
                .document(groupId)
                .update(
                    "roundStart",
                    System.currentTimeMillis()
                )

            clearGroupPostsInFirestore(groupId)
        }

        // Mostrar la notificación visual
        val notificationHandler = Notification(applicationContext)
        notificationHandler.sendChallengeNotification(
            "¡New challenge in $groupName!",
            remoteMessage.notification?.body ?: "¡Be fast, post you photo!"
        )
    }

    // Función interna para limpiar Firestore directamente desde el servicio
    private fun clearGroupPostsInFirestore(groupId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("posts")
            .whereEqualTo("groupId", groupId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    Log.d("FCM_SERVICE", "Feed del grupo $groupId limpiado para el nuevo reto")
                }
            }
            .addOnFailureListener { e ->
                Log.e("FCM_SERVICE", "Error al limpiar posts", e)
            }
    }
}