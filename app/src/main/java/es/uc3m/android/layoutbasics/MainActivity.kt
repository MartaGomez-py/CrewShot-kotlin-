package es.uc3m.android.layoutbasics

import android.os.Bundle
import android.os.Build
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import es.uc3m.android.layoutbasics.ui.theme.MyAppTheme
import es.uc3m.android.layoutbasics.Notification
import es.uc3m.android.layoutbasics.FeedScreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.EmojiEvents

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Face
import androidx.lifecycle.viewmodel.compose.viewModel

import android.content.Context
import android.content.Intent
import com.google.firebase.messaging.FirebaseMessaging
import android.util.Log
class MainActivity : ComponentActivity() {
    private lateinit var notificationHandler: Notification
    private val leaderboardViewModel: LeaderboardViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //initialize notifications
        notificationHandler = Notification(this)
        notificationHandler.createNotificationChannel()
        // Ask for permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                101
            )
        }
        enableEdgeToEdge()
        setContent {
            MyAppTheme {
                val groups by leaderboardViewModel.groups.collectAsState()
                //this will be activate when creatng a group or leaving
                LaunchedEffect(groups) {
                    groups.forEach { group ->
                        FirebaseMessaging.getInstance().subscribeToTopic("group_${group.id}")
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d("FCM_DEBUG", "Suscrito con éxito a: ${group.name}")
                                }
                            }
                    }
                }
                var currentScreen by remember { mutableStateOf("login") }
                var currentSelectedGroupId by remember { mutableStateOf<String?>(null) }

                val petViewModel: PetViewModel = viewModel()
                val postViewModel: PostViewModel = viewModel()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen != "login" && currentScreen != "signup") {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = currentScreen == "feed",
                                    onClick = { currentScreen = "feed" },
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Feed") }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "leaderboard",
                                    onClick = {
                                        currentScreen = "leaderboard"
                                    },
                                    icon = {
                                        Icon(
                                            Icons.Default.EmojiEvents,
                                            contentDescription = "Ranking"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "camera",
                                    onClick = { currentScreen = "camera" },
                                    icon = {
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = "Camera"
                                        )
                                    }
                                )

                                NavigationBarItem(
                                    selected = currentScreen == "pet",
                                    onClick = { currentScreen = "pet" },
                                    icon = { Icon(Icons.Default.Face, contentDescription = "Pet") }
                                )

                            }
                        }

                    }

                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            "login" -> LoginScreen(
                                onSignupClick = { currentScreen = "signup" },
                                onLoginSuccess = { currentScreen = "feed" }
                            )
                            "signup" -> SignupScreen(
                                onLoginClick = { currentScreen = "login" },
                                onSignupSuccess = { currentScreen = "login" }
                            )
                            "leaderboard" -> LeaderboardScreen(
                                groups = groups,
                                onCreateGroup = { name, invitedEmails ->
                                    leaderboardViewModel.createGroup(name, invitedEmails)
                                },
                                onAddMember = { groupId, email ->
                                    leaderboardViewModel.addMemberToGroup(groupId, email)
                                },
                                onLeaveGroup = { groupId ->
                                    leaderboardViewModel.leaveGroup(groupId)
                                }
                            )
                            "camera" -> CameraScreen(
                                petViewModel = petViewModel,
                                postViewModel = postViewModel,
                                selectedGroupId = currentSelectedGroupId
                            )
                            "feed" -> FeedScreen(
                                postViewModel = postViewModel,
                                leaderboardViewModel = leaderboardViewModel,
                                onGroupSelected = { id ->
                                    currentSelectedGroupId = id
                                }
                            )
                            "pet" -> PetScreen(petViewModel)
                            else -> FeedScreen(
                                postViewModel = postViewModel,
                                leaderboardViewModel = leaderboardViewModel,
                                onGroupSelected = { id ->
                                    currentSelectedGroupId = id
                                }
                            )
                        }
                    }
                }
            }
        }
        // Inside onCreate, after setContent { ... }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                // Log a warning if the token could not be retrieved
                Log.w("FCM_DEBUG", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get the device token string
            val token = task.result

            // Print the token to the Logcat so you can copy it
            Log.d("FCM_DEBUG", "My device token is: $token")
        }
    }
}
