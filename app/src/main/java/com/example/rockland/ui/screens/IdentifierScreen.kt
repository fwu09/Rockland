package com.example.rockland.ui.screens

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.rockland.R
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.presentation.viewmodel.CollectionViewModel
import com.example.rockland.presentation.viewmodel.CollectionEvent
import com.example.rockland.presentation.viewmodel.UserViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

private enum class AddState { Idle, Adding, Added }




// ---------------------------------------------------------------------
// RockClassifier: loads the TFLite model + labels and runs inference.
// ---------------------------------------------------------------------
class RockClassifier(private val context: Context) {

    private val imageSize = 224

    private val interpreter: Interpreter by lazy {
        val modelBytes = context.assets.open("rock_finetuned_v1.tflite").readBytes()
        val bb = ByteBuffer.allocateDirect(modelBytes.size)
        bb.order(ByteOrder.nativeOrder())
        bb.put(modelBytes)
        bb.rewind()
        Interpreter(bb)
    }

    private val labels: List<String> by lazy {
        val inputStream = context.assets.open("rock_finetuned_v1_labels.txt")
        val reader = BufferedReader(InputStreamReader(inputStream))
        reader.readLines().filter { it.isNotBlank() }
    }

    /**
     * Run classification on the given image URI.
     * Returns Pair<label, confidence>.
     */
    suspend fun classify(imageUri: Uri): Pair<String, Float> = withContext(Dispatchers.Default) {
        val bitmap = loadAndResizeBitmap(imageUri)
        classifyBitmap(bitmap)
    }

    suspend fun classifyAsset(assetPath: String): Pair<String, Float> = withContext(Dispatchers.Default) {
        val bitmap = loadAndResizeBitmapFromAssets(assetPath)
        classifyBitmap(bitmap)
    }

    private fun classifyBitmap(bitmap: Bitmap): Pair<String, Float> {
        val input = bitmapToBuffer(bitmap)
        val output = Array(1) { FloatArray(labels.size) }

        interpreter.run(input, output)
        val probs = output[0]

        var maxIdx = 0
        var maxVal = Float.NEGATIVE_INFINITY
        for (i in probs.indices) {
            if (probs[i] > maxVal) {
                maxVal = probs[i]
                maxIdx = i
            }
        }
        val label = labels.getOrElse(maxIdx) { "Unknown" }
        return label to maxVal
    }

    private fun loadAndResizeBitmap(uri: Uri): Bitmap {
        val original: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeBitmapApi28(uri)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        return Bitmap.createScaledBitmap(original, imageSize, imageSize, true)
    }

    private fun loadAndResizeBitmapFromAssets(assetPath: String): Bitmap {
        val original = context.assets.open(assetPath).use { input ->
            BitmapFactory.decodeStream(input)
        }
        requireNotNull(original) { "Failed to decode asset image: $assetPath" }
        return Bitmap.createScaledBitmap(original, imageSize, imageSize, true)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeBitmapApi28(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
            decoder.isMutableRequired = true
        }
    }

    private fun bitmapToBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer =
            ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3).order(ByteOrder.nativeOrder())
        val intValues = IntArray(imageSize * imageSize)
        bitmap.getPixels(intValues, 0, imageSize, 0, 0, imageSize, imageSize)

        var pixel = 0
        repeat(imageSize) {
            repeat(imageSize) {
                val v = intValues[pixel++]
                val r = ((v shr 16) and 0xFF) / 255f
                val g = ((v shr 8) and 0xFF) / 255f
                val b = (v and 0xFF) / 255f
                buffer.putFloat(r)
                buffer.putFloat(g)
                buffer.putFloat(b)
            }
        }
        buffer.rewind()
        return buffer
    }
}

// ---------------------------------------------------------------------
// UI state for the identifier flow.
// ---------------------------------------------------------------------
sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Loading(val imageUri: Uri) : ScanUiState
    data class LoadingAsset(val assetPath: String) : ScanUiState
    data class Success(
        val imageUri: Uri,
        val rockName: String,
        val confidence: Float
    ) : ScanUiState

    data class Error(val message: String) : ScanUiState
}

// ---------------------------------------------------------------------
// Entry point for the identify-rock feature.
// ---------------------------------------------------------------------
@Composable
fun IdentifierScreen(
    userViewModel: UserViewModel
) {
    val context = LocalContext.current
    val classifier = remember { RockClassifier(context) }

    var uiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }

    // When uiState becomes Loading, run the model.
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ScanUiState.Loading -> {
                try {
                    val (label, confidence) = classifier.classify(s.imageUri)
                    uiState = ScanUiState.Success(
                        imageUri = s.imageUri,
                        rockName = label,
                        confidence = confidence
                    )
                } catch (e: Exception) {
                    uiState = ScanUiState.Error(
                        "Failed to identify rock. Please try again.\n${e.message ?: ""}"
                    )
                }
            }
            is ScanUiState.LoadingAsset -> {
                try {
                    val (label, confidence) = classifier.classifyAsset(s.assetPath)
                    uiState = ScanUiState.Success(
                        imageUri = Uri.EMPTY,
                        rockName = label,
                        confidence = confidence
                    )
                } catch (e: Exception) {
                    uiState = ScanUiState.Error(
                        "Failed to identify rock. Please try again.\n${e.message ?: ""}"
                    )
                }
            }
            else -> Unit
        }
    }

    when (val state = uiState) {
        is ScanUiState.Idle -> {
            IdentifierHome(
                onImageSelected = { uri ->
                    uiState = ScanUiState.Loading(uri)
                },
                onSampleAssetSelected = { assetPath ->
                    uiState = ScanUiState.LoadingAsset(assetPath)
                }
            )
        }

        is ScanUiState.Loading -> {
            IdentifierLoadingScreen()
        }

        is ScanUiState.LoadingAsset -> {
            IdentifierLoadingScreen()
        }

        is ScanUiState.Success -> {
            IdentifierResultScreen(
                imageUri = state.imageUri,
                rockKey = state.rockName,
                rockName = state.rockName,
                confidence = state.confidence,
                onScanAgain = {
                    uiState = ScanUiState.Idle
                },
                userViewModel = userViewModel
            )
        }

        is ScanUiState.Error -> {
            IdentifierErrorScreen(
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
fun IdentifierHome(
    onImageSelected: (Uri) -> Unit,
    onSampleAssetSelected: (String) -> Unit
) {
    val context = LocalContext.current
    val isDebuggable = remember {
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    val sampleImages = remember {
        context.assets.list("test_images")?.toList().orEmpty()
    }
    var sampleMenuExpanded by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }


    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { _ ->
        // TODO: Save bitmap to file/Uri and call onImageSelected.
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
            // Fill image for the scan panel (not the whole page background).
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

        // Debug helper: choose a sample image from app assets to run the model without emulator gallery setup.
        if (isDebuggable && sampleImages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { sampleMenuExpanded = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TextDark)
                ) {
                    Text(
                        text = "Use Sample Image",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                DropdownMenu(
                    expanded = sampleMenuExpanded,
                    onDismissRequest = { sampleMenuExpanded = false }
                ) {
                    sampleImages.sorted().forEach { name ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                sampleMenuExpanded = false
                                onSampleAssetSelected("test_images/$name")
                            }
                        )
                    }
                }
            }
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
        ) {
            Text("Try Again")
        }
    }
}

// Result screen shown after a rock has been identified.

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
    // ---------- Firestore state ----------
    val db = Firebase.firestore

    // `collectionRockId` is what we will store in user's collection as the stable identifier.
    // Prefer numeric `rockID` from Firestore when available.
    var collectionRockId by remember(rockKey) { mutableStateOf(rockKey.trim().lowercase()) }

    var rockDesc by remember { mutableStateOf<String?>(null) }
    var rockLocation by remember { mutableStateOf<String?>(null) }
    var rockRarity by remember { mutableStateOf<String?>(null) }
    var rockImageUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var addState by remember { mutableStateOf(AddState.Idle) }
    var pendingAddRockId by remember { mutableStateOf<String?>(null) }
    // Fetch rock details whenever the detected name changes
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

    // Forward collection add results to the global top banner.
    LaunchedEffect(Unit) {
        collectionViewModel.events.collect { evt ->
            when (evt) {
                is CollectionEvent.Success -> {
                    userViewModel.showSuccess(evt.message)
                    if (evt.rockId != null && evt.rockId == pendingAddRockId) {
                        addState = AddState.Added
                    }
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

    // ---------- UI ----------
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
                    modifier = Modifier
                        .size(120.dp)
                        .alpha(0.5f),
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

            // Optional fields
            rockRarity?.let {
                Text(
                    text = "Rarity: $it",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            rockLocation?.let {
                Text(
                    text = "Typical location: $it",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
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
                        isLoading -> {
                            Text(
                                text = "Loading description...",
                                fontSize = 16.sp,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                        }

                        errorMsg != null -> {
                            Text(
                                text = errorMsg!!,
                                fontSize = 16.sp,
                                color = Color.Red
                            )
                        }

                        !rockDesc.isNullOrBlank() -> {
                            Text(
                                text = rockDesc!!,
                                fontSize = 16.sp,
                                color = TextDark.copy(alpha = 0.8f),
                                lineHeight = 24.sp
                            )
                        }

                        else -> {
                            Text(
                                text = "No description available yet.",
                                fontSize = 16.sp,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    when (addState) {
                        AddState.Added -> userViewModel.showInfo("Already in your collection.")
                        AddState.Adding -> userViewModel.showInfo("Adding to collection...")
                        AddState.Idle -> {
                            pendingAddRockId = collectionRockId
                            addState = AddState.Adding
                            collectionViewModel.addRockFromIdentification(
                                rockId = collectionRockId,
                                rockName = rockName,
                                thumbnailUrl = rockImageUrl
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rock1.copy(alpha = 0.9f))
            ) {
                Text(
                    text = when (addState) {
                        AddState.Added -> "Added"
                        AddState.Adding -> "Adding..."
                        AddState.Idle -> "Add to Collection"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = onScanAgain,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Rock1)
            ) {
                Text(
                    text = "Scan Again",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}






