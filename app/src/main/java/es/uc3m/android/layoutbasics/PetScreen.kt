package es.uc3m.android.layoutbasics

// Compose core
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment

// Layout
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape

// Material
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh

// UI extras
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Lottie
import com.airbnb.lottie.compose.*

import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PetScreen(petViewModel: PetViewModel) {

    val xp by petViewModel.xp
    val level = xp / 100
    val progress = (xp % 100) / 100f

    val selectedPet by petViewModel.selectedPet
    val petName by petViewModel.petName

    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(petName) }


    var showSelector by remember { mutableStateOf(false) }

    val animationRes = when (selectedPet) {
        0 -> R.raw.bird
        1 -> R.raw.crocodile
        2 -> R.raw.gorilla
        else -> R.raw.otter
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {

        //  HEADER ARRIBA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Level $level",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
            )

            Text(
                text = "$xp XP",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    letterSpacing = 1.sp
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 50.dp)
        ) {

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${xp % 100}/100 XP",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.align(Alignment.End)
            )
        }

        //  MASCOTA CENTRADA
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            val composition by rememberLottieComposition(
                LottieCompositionSpec.RawRes(animationRes)
            )

            if (composition != null) {

                val progressAnim by animateLottieCompositionAsState(
                    composition,
                    iterations = LottieConstants.IterateForever
                )

                LottieAnimation(
                    composition = composition,
                    progress = progressAnim,
                    modifier = Modifier
                        .size(220.dp)
                        .clickable {
                            showSelector = true
                        }
                )

            } else {
                Text("Loading pet...")
            }
        }

        // SELECTOR DE MASCOTA (NUEVO)
        if (showSelector) {
            AlertDialog(
                onDismissRequest = { showSelector = false },
                confirmButton = {},
                title = { Text("Choose your pet") },
                text = {

                    val pets = listOf(
                        Pair("Bird", R.raw.bird),
                        Pair("Crocodile", R.raw.crocodile),
                        Pair("Gorilla", R.raw.gorilla),
                        Pair("Otter", R.raw.otter)
                    )

                    Column {

                        pets.forEachIndexed { index, (name, petRes) ->

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        petViewModel.changePet(index)
                                        showSelector = false
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                val comp by rememberLottieComposition(
                                    LottieCompositionSpec.RawRes(petRes)
                                )

                                val prog by animateLottieCompositionAsState(
                                    comp,
                                    iterations = LottieConstants.IterateForever
                                )

                                LottieAnimation(
                                    composition = comp,
                                    progress = prog,
                                    modifier = Modifier.size(60.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(text = name)
                            }
                        }
                    }
                }
            )
        }

        //  NOMBRE ABAJO
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if (isEditingName) {

                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    petViewModel.changeName(tempName)
                    isEditingName = false
                }) {
                    Text("Save")
                }

            } else {

                Text(
                    text = petName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.clickable {
                        tempName = petName
                        isEditingName = true
                    }
                )
            }
        }
    }
}