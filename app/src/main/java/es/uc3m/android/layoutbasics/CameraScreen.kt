package es.uc3m.android.layoutbasics

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun CameraScreen(
    petViewModel: PetViewModel,
    postViewModel: PostViewModel = viewModel(),
    leaderboardViewModel: LeaderboardViewModel = viewModel(),
    selectedGroupId: String?
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    
    val groups by leaderboardViewModel.groups.collectAsState()
    var username by remember { mutableStateOf("Usuario") }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    // Resolve the actual group to upload to
    val targetGroupId = selectedGroupId ?: groups.firstOrNull()?.id
    val targetGroupName = groups.find { it.id == targetGroupId }?.name ?: "No group selected"

    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    username = document.getString("username") ?: "Usuario"
                }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Updated to avoid deprecated Bundle.get()
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                result.data?.extras?.getParcelable("data", Bitmap::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.extras?.getParcelable<Bitmap>("data")
            }

            if (bitmap != null) {
                imageBitmap = bitmap

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    if (targetGroupId == null) {
                        Log.e("CameraScreen", "Cannot upload: No group available")
                        return@rememberLauncherForActivityResult
                    }

                    postViewModel.uploadPost(username, bitmap, targetGroupId) { error ->
                        if (error == null) {
                            Log.d("CameraScreen", "Post uploaded successfully to group: $targetGroupName ($targetGroupId)")
                        } else {
                            errorMessage = error
                        }
                    }
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCamera(cameraLauncher)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (groups.isEmpty()) {
            Text(
                text = "Join a group first to upload photos!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Target Group:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = targetGroupName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        launchCamera(cameraLauncher)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Take & Upload Photo", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        imageBitmap?.let {
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Last captured image",
                    modifier = Modifier.size(250.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Last capture sent!", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        errorMessage?.let {

            AlertDialog(
                onDismissRequest = {
                    errorMessage = null
                },
                title = {
                    Text("Oops")
                },
                text = {
                    Text(it)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            errorMessage = null
                        }
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

fun launchCamera(launcher: ActivityResultLauncher<Intent>) {
    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
    launcher.launch(intent)
}
