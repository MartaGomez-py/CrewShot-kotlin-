package es.uc3m.android.layoutbasics

import androidx.compose.runtime.mutableStateListOf

object FeedRepository {

    val posts = mutableStateListOf<Post>()

    fun addPost(post: Post) {
        posts.add(0,post) //los posts nuevos se ven arriba segun se van subiendo y los mas antiguos abajo
    }

    fun getValidPosts(): List<Post> {

        val now = System.currentTimeMillis()

        return posts.filter {
            now - it.timestamp < 24 * 60 * 60 * 1000
        }
    }
}