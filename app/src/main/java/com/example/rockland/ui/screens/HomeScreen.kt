package com.example.rockland.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import kotlinx.coroutines.delay

// UI State Definition
sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Loading(val imageUri: Uri) : ScanUiState
    data class Success(val imageUri: Uri) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

@Composable
fun HomeScreen() {
    var uiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }

    // Mock Logic: Simulate network request when state becomes Loading
    LaunchedEffect(uiState) {
        if (uiState is ScanUiState.Loading) {
            val loadingState = uiState as ScanUiState.Loading
            // Simulate network delay
            delay(2000)

            // TODO: Mock Data - Randomly succeed or fail for demonstration
            if (Math.random() > 0.2) { // 80% success rate
                uiState = ScanUiState.Success(loadingState.imageUri)
            } else {
                uiState = ScanUiState.Error("Failed to identify rock. Please try again.")
            }
        }
    }

    when (val state = uiState) {
        is ScanUiState.Idle -> {
            RockIdentifyHome(
                onImageSelected = { uri ->
                    uiState = ScanUiState.Loading(uri)
                }
            )
        }

        is ScanUiState.Loading -> {
            LoadingScreen()
        }

        is ScanUiState.Success -> {
            RockInfoScreen(
                imageUri = state.imageUri,
                onScanAgain = {
                    uiState = ScanUiState.Idle
                }
            )
        }

        is ScanUiState.Error -> {
            ErrorScreen(
                message = state.message,
                onRetry = {
                    uiState = ScanUiState.Idle
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RockIdentifyHome(onImageSelected: (Uri) -> Unit) {
    val context = LocalContext.current

    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // TODO: Backend Integration - Save bitmap to Uri and pass to onImageSelected
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = "RockLand Scanner",
            fontSize = 20.sp,
            color = TextDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        )

        // Camera Preview Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .background(Rock3)
        ) {
            // Camera Preview
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Camera Preview",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.3f)
                    .padding(48.dp),
                contentScale = ContentScale.Fit
            )

            // Scan Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Click to Scan",
                    color = TextDark,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Circular Scan Button
                Button(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Rock1),
                    elevation = ButtonDefaults.buttonElevation(8.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = "Scan",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Section: Upload from Gallery Button
        OutlinedButton(
            onClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, TextDark)
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = "Upload from Gallery",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Extra bottom padding for safety
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            color = Rock1,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Identifying Rock...",
            fontSize = 16.sp,
            color = TextDark,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Error",
            tint = Color.Red.copy(alpha = 0.7f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Oops!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 16.sp,
            color = TextDark.copy(alpha = 0.7f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Rock1),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Try Again")
        }
    }
}

@Composable
fun RockInfoScreen(imageUri: Uri, onScanAgain: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Text(
            text = "RockLand Scanner",
            fontSize = 20.sp,
            color = TextDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        )

        // Scanned Image Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(Rock3),
            contentAlignment = Alignment.Center
        ) {
            // TODO: Mock Data - Replace placeholder with actual image loading logic
            // AsyncImage(model = imageUri, ...)
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Scanned Rock",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(0.5f),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Rock1)
            )
            Text("Image Loaded", color = TextDark, modifier = Modifier.padding(top = 80.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Rock Info Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // TODO: Mock Data - Replace hardcoded text with backend response
            Text(
                text = "AMETHYST",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 1.sp
            )

            Text(
                text = "(Detected)",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Description Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Fun Fact and Description",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    // TODO: Mock Data - Replace with dynamic description from API
                    Text(
                        text = "This is a violet variety of quartz. The name comes from the Koine Greek αμέθυστος amethystos from α- a-, 'not' and μεθύσκω (Ancient Greek) methysko, 'intoxicate', a reference to the belief that the stone protected its owner from drunkenness.",
                        fontSize = 16.sp,
                        color = TextDark.copy(alpha = 0.8f),
                        lineHeight = 24.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(24.dp))

        // Scan Again Button
        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Rock1)
        ) {
            Text(
                text = "Scan Again",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}

@Preview(showBackground = true, name = "Result Screen")
@Composable
fun RockInfoScreenPreview() {
    RockInfoScreen(
        imageUri = Uri.EMPTY,
        onScanAgain = {}
    )
}

@Preview(showBackground = true, name = "Loading Screen")
@Composable
fun LoadingScreenPreview() {
    LoadingScreen()
}

@Preview(showBackground = true, name = "Error Screen")
@Composable
fun ErrorScreenPreview() {
    ErrorScreen(message = "Network connection failed", onRetry = {})
}
