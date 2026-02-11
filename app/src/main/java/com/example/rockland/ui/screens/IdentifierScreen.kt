package com.example.rockland.ui.screens

import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
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
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rockland.R
import com.example.rockland.presentation.model.UiBanner
import com.example.rockland.presentation.viewmodel.CollectionEvent
import com.example.rockland.presentation.viewmodel.CollectionViewModel
import com.example.rockland.presentation.viewmodel.UserViewModel
import com.example.rockland.ui.components.TopBannerHost
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

private enum class AddState { Idle, Adding, Added }


sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Loading(val imageUri: Uri) : ScanUiState
    data class Success(
        val imageUri: Uri,
        val rockName: String,
        val confidence: Float
    ) : ScanUiState
    data class Error(val message: String) : ScanUiState
}


@Composable
fun IdentifierScreen(
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    var uiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }

    // When uiState becomes Loading, call Cloud Function.
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ScanUiState.Loading -> {
                try {
                    val (label, confidence) = scanRockWithCloudFunction(context, s.imageUri)
                    uiState = ScanUiState.Success(
                        imageUri = s.imageUri,
                        rockName = label,
                        confidence = confidence
                    )
                } catch (e: Exception) {
                    uiState = ScanUiState.Error(
                        "Failed to identify rock (cloud). Please try again.\n${e.message ?: ""}"
                    )
                }
            }
            else -> Unit
        }
    }

    var bannerState by remember { mutableStateOf<UiBanner?>(null) }
    LaunchedEffect(Unit) {
        userViewModel.banners.collect { banner -> bannerState = banner }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TopBannerHost(
            banner = bannerState,
            onDismiss = { bannerState = null },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        when (val state = uiState) {
            is ScanUiState.Idle -> {
                IdentifierHome(
                    onImageSelected = { uri -> uiState = ScanUiState.Loading(uri) }
                )
            }

            is ScanUiState.Loading -> IdentifierLoadingScreen()

            is ScanUiState.Success -> {
                IdentifierResultScreen(
                    imageUri = state.imageUri,
                    rockKey = state.rockName,
                    rockName = state.rockName,
                    confidence = state.confidence,
                    onScanAgain = { uiState = ScanUiState.Idle },
                    userViewModel = userViewModel
                )
            }

            is ScanUiState.Error -> {
                IdentifierErrorScreen(
                    message = state.message,
                    onRetry = { uiState = ScanUiState.Idle }
                )
            }
        }
    }
}

@Composable
fun IdentifierHome(
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToMediaStore(context, it)
            if (uri != null) onImageSelected(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(Rock3)
            ) {
                AsyncImage(
                    model = "file:///android_asset/scanner_background.jpg",
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.32f),
                    contentScale = ContentScale.Crop
                )

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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun IdentifierLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Rock1, modifier = Modifier.size(48.dp))
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
fun IdentifierErrorScreen(message: String, onRetry: () -> Unit) {
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
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Rock1),
            shape = RoundedCornerShape(28.dp)
        ) { Text("Try Again") }
    }
}



private fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap): Uri? {
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "scan_${System.currentTimeMillis()}.jpg")
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
    }

    val uri = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    uri?.let {
        context.contentResolver.openOutputStream(it)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
    }

    return uri
}



private fun uriToBase64Jpeg(context: Context, uri: Uri, quality: Int = 80): String {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }

    val baos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
    return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
}


private suspend fun scanRockWithCloudFunction(
    context: Context,
    imageUri: Uri
): Pair<String, Float> {

    val functions = Firebase.functions("us-central1")

    val base64 = uriToBase64Jpeg(context, imageUri)
    val data = hashMapOf("image" to base64)

    val result = functions
        .getHttpsCallable("scanRockImage")
        .call(data)
        .await()

    val map = (result.getData() as? Map<*, *>)
        ?: throw IllegalStateException("Invalid response from scanRockImage")

    val rockName = map["rockName"]?.toString() ?: "Unknown"
    val confidence = (map["confidence"] as? Number)?.toFloat() ?: 0f

    return rockName to confidence
}


@Composable
fun IdentifierResultScreen(
    imageUri: Uri,
    rockKey: String,
    rockName: String,
    confidence: Float? = null,
    onScanAgain: () -> Unit,
    userViewModel: UserViewModel,
    collectionViewModel: CollectionViewModel = viewModel()
) {
    val db = Firebase.firestore
    val context = LocalContext.current

    var collectionRockId by remember(rockKey) { mutableStateOf(rockKey.trim().lowercase()) }

    var rockDesc by remember { mutableStateOf<String?>(null) }
    var rockLocation by remember { mutableStateOf<String?>(null) }
    var rockRarity by remember { mutableStateOf<String?>(null) }
    var rockImageUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var addState by remember { mutableStateOf(AddState.Idle) }
    var pendingAddRockId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rockName) {
        isLoading = true
        errorMsg = null
        collectionRockId = rockKey.trim().lowercase()
        rockDesc = null
        rockLocation = null
        rockRarity = null
        rockImageUrl = null

        try {
            val name = rockName.trim()
            val doc = db.collection("rock")
                .whereEqualTo("rockName", name)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()

            if (doc != null) {
                val rid = doc.getLong("rockID")?.toString()
                collectionRockId = rid ?: doc.id
                rockDesc = doc.getString("rockDesc")
                rockLocation = doc.getString("rockLocation")
                rockRarity = doc.getString("rockRarity")
                rockImageUrl = doc.getString("rockImageUrl")
            } else {
                errorMsg = "No details found for $rockName."
            }
        } catch (e: Exception) {
            val code = (e as? FirebaseFirestoreException)?.code?.name
            errorMsg = if (code != null) {
                "Failed to load details ($code): ${e.message ?: "Unknown error"}"
            } else {
                "Failed to load details: ${e.message ?: "Unknown error"}"
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        collectionViewModel.events.collect { evt ->
            when (evt) {
                is CollectionEvent.Success -> {
                    userViewModel.showSuccess(evt.message)
                    if (evt.rockId != null && evt.rockId == pendingAddRockId) addState = AddState.Added
                }
                is CollectionEvent.Error -> {
                    userViewModel.showError(evt.message)
                    if (evt.rockId != null && evt.rockId == pendingAddRockId) {
                        addState = if (evt.message.contains("Already in your collection", ignoreCase = true)) {
                            AddState.Added
                        } else {
                            AddState.Idle
                        }
                    }
                }
            }
        }
    }

    val uiState by collectionViewModel.uiState.collectAsState()
    val alreadyInCollection =
        uiState.items.any { item ->
            item.rockId == collectionRockId || item.rockName.equals(rockName, ignoreCase = true)
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "RockLand Scanner",
            fontSize = 20.sp,
            color = TextDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
        )

        val headerImageModel: Any? = when {
            !rockImageUrl.isNullOrBlank() -> rockImageUrl
            imageUri != Uri.EMPTY -> imageUri
            else -> null
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(Rock3),
            contentAlignment = Alignment.Center
        ) {
            if (headerImageModel != null) {
                AsyncImage(
                    model = headerImageModel,
                    contentDescription = "Rock image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = "Rock image placeholder",
                    modifier = Modifier.size(120.dp).alpha(0.5f),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(Rock1)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = rockName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                letterSpacing = 1.sp
            )

            Text(
                text = "(Detected)",
                fontSize = 16.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            confidence?.let {
                Text(
                    text = "Confidence: ${(it * 100f).coerceIn(0f, 100f).toInt()}%",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            } ?: Spacer(modifier = Modifier.height(16.dp))

            rockRarity?.let {
                Text("Rarity: $it", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 4.dp))
            }
            rockLocation?.let {
                Text("Typical location: $it", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 12.dp))
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "About $rockName",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    when {
                        isLoading -> Text("Loading description...", fontSize = 16.sp, color = TextDark.copy(alpha = 0.8f))
                        errorMsg != null -> Text(errorMsg!!, fontSize = 16.sp, color = Color.Red)
                        !rockDesc.isNullOrBlank() -> Text(
                            text = rockDesc!!,
                            fontSize = 16.sp,
                            color = TextDark.copy(alpha = 0.8f),
                            lineHeight = 24.sp
                        )
                        else -> Text("No description available yet.", fontSize = 16.sp, color = TextDark.copy(alpha = 0.8f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    if (alreadyInCollection) {
                        userViewModel.showInfo("Already in your collection.")
                        return@Button
                    }

                    when (addState) {
                        AddState.Added -> userViewModel.showInfo("Already in your collection.")
                        AddState.Adding -> userViewModel.showInfo("Adding to collection...")
                        AddState.Idle -> {
                            pendingAddRockId = collectionRockId
                            addState = AddState.Adding
                            collectionViewModel.addCapturedImageToPersonalNotesFromIdentification(
                                rockId = collectionRockId,
                                rockName = rockName,
                                rockSource = "identify",
                                thumbnailUrl = rockImageUrl,
                                imageUri = if (imageUri != Uri.EMPTY) imageUri else null,
                                context = context
                            )
                        }
                    }
                },
                enabled = !alreadyInCollection && addState != AddState.Adding,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rock1.copy(alpha = 0.9f))
            ) {
                Text(
                    text = when {
                        alreadyInCollection -> "You already have this Rock!"
                        addState == AddState.Added -> "Added"
                        addState == AddState.Adding -> "Adding..."
                        else -> "Add to Collection"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = {
                    val uriToUpload = if (imageUri != Uri.EMPTY) imageUri else null
                    if (uriToUpload == null) {
                        userViewModel.showError("No image to save. Please scan with the camera or upload from gallery.")
                        return@OutlinedButton
                    }
                    collectionViewModel.addCapturedImageToPersonalNotesFromIdentification(
                        rockId = collectionRockId,
                        rockName = rockName,
                        rockSource = "identify",
                        thumbnailUrl = rockImageUrl,
                        imageUri = uriToUpload,
                        context = context
                    )
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rock1)
            ) {
                Text("Add Image of Rock into Collection Entry", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { userViewModel.showInfo("<<insert popup window for reporting of rock identification errors>>") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red)
            ) {
                Text("Suspect an Error?", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onScanAgain,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rock1)
            ) {
                Text("Scan Again", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
