package es.uc3m.android.layoutbasics

import android.graphics.Bitmap

// En Post.kt
// C:/Users/Usuario/AndroidStudioProjects/android-examples/LayoutBasics/app/src/main/java/es/uc3m/android/layoutbasics/Post.kt
data class Post(
    val id: String = "",
    val authorId: String = "", // <--- NUEVO CAMPO
    val user: String = "",     // Nombre para mostrar
    val imageUrl: String = "",
    val timestamp: Long = 0L,
    val votes: Int = 0,
    val votedBy: List<String> = emptyList(),
    val groupId: String = ""
)
