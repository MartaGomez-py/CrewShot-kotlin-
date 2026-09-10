package es.uc3m.android.layoutbasics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth

@Composable
fun FeedScreen(
    postViewModel: PostViewModel = viewModel(),
    leaderboardViewModel: LeaderboardViewModel = viewModel(),
    onGroupSelected: (String?) -> Unit
) {
    val groups by leaderboardViewModel.groups.collectAsState()

    val groupViewModel: GroupViewModel = viewModel()
    val selectedGroupId by groupViewModel.selectedGroupId

    // Automatic selection of the first available group
    LaunchedEffect(groups) {
        if (selectedGroupId == null && groups.isNotEmpty()) {
            val firstId = groups.first().id
            groupViewModel.selectGroup(firstId)
            onGroupSelected(firstId)
        } else if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
            val newId = groups.firstOrNull()?.id
            groupViewModel.selectGroup(newId)
            onGroupSelected(newId)
        }
    }

    LaunchedEffect(selectedGroupId) {
        if (selectedGroupId != null) {
            postViewModel.listenToPosts(selectedGroupId)
            onGroupSelected(selectedGroupId)
        }
    }

    val posts = postViewModel.posts
    var selectedPost by remember { mutableStateOf<Post?>(null) }
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(10000)
            currentTime = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CrewShot",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 2.sp
                        )
                    )
                }

                if (groups.isNotEmpty()) {
                    val selectedIndex = groups.indexOfFirst { it.id == selectedGroupId }.coerceAtLeast(0)
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        edgePadding = 16.dp,
                        containerColor = Color.Transparent,
                        divider = {}
                    ) {
                        groups.forEach { group ->
                            Tab(
                                selected = selectedGroupId == group.id,
                                onClick = { groupViewModel.selectGroup(group.id) },
                                text = { 
                                    Text(
                                        text = group.name, 
                                        fontWeight = if (selectedGroupId == group.id) FontWeight.Bold else FontWeight.Normal
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "You haven't joined any group yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { onGroupSelected(null) }, 
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text("Create or Join a Group", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (posts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("There are no photos in this group yet", color = Color.Gray)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(posts) { post ->
                        PostItem(
                            post = post,
                            currentTime = currentTime,
                            postViewModel = postViewModel,
                            groups = groups,
                            posts = posts,
                            onClick = { selectedPost = post }
                        )
                    }
                }
            }

            selectedPost?.let {
                FullscreenViewer(
                    post = it,
                    onClose = { selectedPost = null }
                )
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentTime: Long,
    postViewModel: PostViewModel,
    groups: List<Group>,
    posts: List<Post>,
    onClick: () -> Unit
) {
    val timeText = formatTime(post.timestamp, currentTime)
    val groupName = groups.find { it.id == post.groupId }?.name ?: "Group"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = post.user,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$groupName • $timeText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable { onClick() },
                contentScale = ContentScale.Crop
            )

            LikeButton(post = post, postViewModel = postViewModel, posts = posts)
        }
    }
}

@Composable
fun LikeButton(
    post: Post,
    postViewModel: PostViewModel,
    posts: List<Post>
) {
    val currentUser = FirebaseAuth.getInstance().currentUser

    val alreadyVotedAnyPost = posts.any {
        it.votedBy.contains(currentUser?.uid)
    }

    val isVotedThisPost = post.votedBy.contains(currentUser?.uid)

    var showAlert by remember { mutableStateOf(false) }

    if (showAlert) {
        AlertDialog(
            onDismissRequest = { showAlert = false },
            title = { Text("Oops") },
            text = { Text("You can only vote once per round") },
            confirmButton = {
                TextButton(onClick = { showAlert = false }) {
                    Text("OK")
                }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    ) {

        IconButton(
            onClick = {
                if (currentUser?.uid == post.authorId) {
                    showAlert = true
                } else if (alreadyVotedAnyPost && !isVotedThisPost) {
                    showAlert = true
                } else {
                    postViewModel.voteForPost(post.id, post.authorId)
                }
            }
        ) {
            Icon(
                imageVector = if (isVotedThisPost)
                    Icons.Filled.Favorite
                else
                    Icons.Outlined.FavoriteBorder,
                contentDescription = "Vote",
                tint = if (isVotedThisPost) Color.Red else Color.Gray,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = "${post.votes}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = if (post.votes > 0)
                MaterialTheme.colorScheme.primary
            else
                Color.Gray
        )
    }
}

@Composable
fun FullscreenViewer(
    post: Post,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = post.imageUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.7f),
            contentScale = ContentScale.Fit
        )
    }
}

fun formatTime(timestamp: Long, now: Long): String {
    val diff = now - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    return when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours < 24 -> "${hours}h"
        else -> "old"
    }
}
