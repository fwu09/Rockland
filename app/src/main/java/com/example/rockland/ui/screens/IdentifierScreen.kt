package com.example.rockland.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rockland.presentation.model.UiBanner
import com.example.rockland.presentation.viewmodel.UserViewModel
import com.example.rockland.ui.components.TopBannerHost
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.presentation.viewmodel.CollectionViewModel
import androidx.compose.runtime.collectAsState
import com.example.rockland.presentation.viewmodel.AwardsViewModel
import com.example.rockland.util.ImageValidationUtil



sealed interface ScanUiState {
    object Idle : ScanUiState
    data class Loading(val imageUri: Uri) : ScanUiState
    data class Success(val imageUri: Uri, val rockName: String, val confidence: Float) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

private enum class AddState { Idle, Adding, Added }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdentifierScreen(userViewModel: UserViewModel) {
    val context = LocalContext.current
    val collectionViewModel: CollectionViewModel = viewModel()
    var uiState by remember { mutableStateOf<ScanUiState>(ScanUiState.Idle) }
    val uid = userViewModel.currentUser.collectAsState().value?.uid
    val awardsViewModel: AwardsViewModel = viewModel(
        factory = AwardsViewModel.Factory(uid)
    )
    var lastCountedUri by remember { mutableStateOf<Uri?>(null) }


    // recent scans (in-memory)
    val recentScans = remember { mutableStateListOf<String>() }

    // call cloud function after image selected
    LaunchedEffect(uiState) {
        when (val s = uiState) {
            is ScanUiState.Loading -> {
                try {
                    val (label, confidence) = scanRockWithCloudFunction(context, s.imageUri)
                    uiState = ScanUiState.Success(s.imageUri, label, confidence)
                } catch (e: Exception) {
                    uiState = ScanUiState.Error(
                        "Failed to identify rock. Please try again.\n${e.message ?: ""}"
                    )
                }
            }
            else -> Unit
        }
    }

    // push to recent scans on success
    LaunchedEffect(uiState) {
        val s = uiState
        if (s is ScanUiState.Success) {
            val text = "${s.rockName} • ${(s.confidence * 100f).coerceIn(0f, 100f).roundToInt()}%"
            if (recentScans.firstOrNull() != text) {
                recentScans.add(0, text)
                if (recentScans.size > 5) recentScans.removeAt(recentScans.lastIndex)
            }
        }
    }

    // for achievements
    LaunchedEffect(uiState) {
        val s = uiState
        if (s is ScanUiState.Success) {
            val uid = userViewModel.currentUser.value?.uid
            if (!uid.isNullOrBlank() && lastCountedUri != s.imageUri) {
                lastCountedUri = s.imageUri
                // match Firestore trigger
                awardsViewModel.applyTrigger("identify_rock", 1)
            }
        }
    }

    var bannerState by remember { mutableStateOf<UiBanner?>(null) }
    LaunchedEffect(Unit) {
        userViewModel.banners.collect { banner -> bannerState = banner }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "RockLand Scanner",
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Scan a rock to unlock details",
                            color = TextDark.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TopBannerHost(
                banner = bannerState,
                onDismiss = { bannerState = null },
                modifier = Modifier.align(Alignment.TopCenter)
            )

            when (val state = uiState) {
                is ScanUiState.Idle -> IdentifierHome(
                    recentScans = recentScans,
                    onImageSelected = { uri -> uiState = ScanUiState.Loading(uri) },
                    onValidationError = { msg -> userViewModel.showError(msg) }
                )

                is ScanUiState.Loading -> IdentifierLoadingScreen()

                is ScanUiState.Success -> IdentifierResultScreen(
                    imageUri = state.imageUri,
                    rockName = state.rockName,
                    confidence = state.confidence,
                    onScanAgain = { uiState = ScanUiState.Idle },
                    userViewModel = userViewModel,
                    collectionViewModel = collectionViewModel
                )

                is ScanUiState.Error -> IdentifierErrorScreen(
                    message = state.message,
                    onRetry = { uiState = ScanUiState.Idle }
                )
            }
        }
    }
}

@Composable
private fun IdentifierHome(
    recentScans: List<String>,
    onImageSelected: (Uri) -> Unit,
    onValidationError: (String) -> Unit
) {
    val context = LocalContext.current
    var showHowTo by rememberSaveable { mutableStateOf(false) }

    // image input validation
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateForIdentification(context, uri)) {
            is ImageValidationUtil.Result.Ok -> onImageSelected(uri)
            is ImageValidationUtil.Result.Error -> onValidationError(res.message)
        }
    }
    // iamge input validation
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            val uri = saveBitmapToMediaStore(context, it)
            if (uri == null) return@rememberLauncherForActivityResult
            when (val res = ImageValidationUtil.validateForIdentification(context, uri)) {
                is ImageValidationUtil.Result.Ok -> onImageSelected(uri)
                is ImageValidationUtil.Result.Error -> onValidationError(res.message)
            }
        }
    }

    val infinite = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infinite.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = Rock1,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Tips for best results", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Good lighting • Keep centered • Avoid blur",
                        color = TextDark.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
                TextButton(onClick = { showHowTo = !showHowTo }) {
                    Text(if (showHowTo) "Hide" else "How to")
                }
            }

            AnimatedVisibility(visible = showHowTo) {
                Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    HorizontalDivider(color = Color(0xFFEDEDED))
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "1) Fill the frame with the rock\n" +
                                "2) Hold steady for 1–2 seconds\n" +
                                "3) Avoid reflections & shadows\n" +
                                "4) Try a second angle if unsure",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.75f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Scan a Rock",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Use your camera for the most accurate identification.",
                    color = TextDark.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.18f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Rock3.copy(alpha = 0.25f),
                                    Rock3.copy(alpha = 0.10f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = "file:///android_asset/scanner_background.jpg",
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(0.18f),
                        contentScale = ContentScale.Crop
                    )

                    ScannerFrameOverlay(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        alpha = 0.20f
                    )

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(Rock1.copy(alpha = pulseAlpha))
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text(
                            text = "Tap to open camera",
                            color = TextDark.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )

                        Button(
                            onClick = { cameraLauncher.launch(null) },
                            modifier = Modifier.size(76.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = Rock1),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Scan",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.80f)
                        ) {
                            Text(
                                text = "Hold steady • Avoid blur",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.75f),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDEDED))
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Gallery", fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = { showHowTo = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tips", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (recentScans.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Recent scans",
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(Modifier.height(8.dp))

                    recentScans.forEachIndexed { idx, s ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Rock1.copy(alpha = 0.85f))
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = s,
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.8f),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (idx != recentScans.lastIndex) Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }

        Text(
            text = "Tip: If the result looks wrong, try another angle or stronger lighting.",
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun ScannerFrameOverlay(modifier: Modifier = Modifier, alpha: Float = 0.2f) {
    Box(modifier = modifier) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = BorderStroke(1.dp, Rock1.copy(alpha = alpha))
        ) {}

        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CornerTick()
            CornerTick(mirrorX = true)
        }

        Row(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CornerTick(mirrorY = true)
            CornerTick(mirrorX = true, mirrorY = true)
        }
    }
}

@Composable
private fun CornerTick(mirrorX: Boolean = false, mirrorY: Boolean = false) {
    val w = 22.dp
    val t = 3.dp
    val color = Rock1.copy(alpha = 0.28f)

    Box(modifier = Modifier.size(26.dp)) {
        val align = when {
            mirrorX && mirrorY -> Alignment.BottomEnd
            mirrorX -> Alignment.TopEnd
            mirrorY -> Alignment.BottomStart
            else -> Alignment.TopStart
        }

        Box(
            modifier = Modifier
                .align(align)
                .width(w)
                .height(t)
                .background(color, RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .align(align)
                .width(t)
                .height(w)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun IdentifierLoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Rock1, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Identifying rock…",
            fontSize = 16.sp,
            color = TextDark,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "This usually takes a second.",
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun IdentifierErrorScreen(message: String, onRetry: () -> Unit) {
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
            tint = Color(0xFFB00020),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Couldn’t identify",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = Rock1),
            shape = RoundedCornerShape(14.dp)
        ) { Text("Try again") }
    }
}

@Composable
private fun IdentifierResultScreen(
    imageUri: Uri,
    rockName: String,
    confidence: Float,
    onScanAgain: () -> Unit,

    userViewModel: UserViewModel,
    collectionViewModel: CollectionViewModel
) {
    val percent = (confidence * 100f).coerceIn(0f, 100f).roundToInt()
    val context = LocalContext.current
    val db = Firebase.firestore
    // `collectionRockId` is what we will store in user's collection as the stable identifier.
    // Prefer numeric `rockID` from Firestore when available.
    var collectionRockId by remember(rockName) { mutableStateOf(rockName.trim().lowercase()) }

    var rockDesc by remember { mutableStateOf<String?>(null) }
    var rockLocation by remember { mutableStateOf<String?>(null) }
    var rockRarity by remember { mutableStateOf<String?>(null) }
    var rockImageUrl by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var addState by remember { mutableStateOf(AddState.Idle) }
    var pendingAddRockId by remember { mutableStateOf<String?>(null) }

    // Fetch rock details whenever detected rock changes
    LaunchedEffect(rockName) {
        isLoading = true
        errorMsg = null
        rockDesc = null
        rockLocation = null
        rockRarity = null
        rockImageUrl = null
        collectionRockId = rockName.trim().lowercase()

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
            val code = (e as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name
            errorMsg = if (code != null) {
                "Failed to load details ($code): ${e.message ?: "Unknown error"}"
            } else {
                "Failed to load details: ${e.message ?: "Unknown error"}"
            }
        } finally {
            isLoading = false
        }
    }

    // Forward collection add results to your global top banner
    LaunchedEffect(collectionViewModel) {
        collectionViewModel.events.collect { evt ->
            when (evt) {
                is com.example.rockland.presentation.viewmodel.CollectionEvent.Success -> {
                    userViewModel.showSuccess(evt.message)
                    if (evt.rockId != null && evt.rockId == pendingAddRockId) addState = AddState.Added
                }
                is com.example.rockland.presentation.viewmodel.CollectionEvent.Error -> {
                    userViewModel.showError(evt.message)
                    if (evt.rockId != null && evt.rockId == pendingAddRockId) {
                        addState =
                            if (evt.message.contains("Already in your collection", ignoreCase = true)) {
                                AddState.Added
                            } else {
                                AddState.Idle
                            }
                    }
                }
            }
        }
    }

    // Check if already in collection
    val collectionUiState by collectionViewModel.uiState.collectAsState()
    val alreadyInCollection =
        collectionUiState.items.any { item ->
            item.rockId == collectionRockId || item.rockName.equals(rockName, ignoreCase = true)
        }

    // Use Firestore image URL if available, else use scanned image
    val headerImageModel: Any? = when {
        !rockImageUrl.isNullOrBlank() -> rockImageUrl
        imageUri != Uri.EMPTY -> imageUri
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                // Header row: Result + confidence pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Result", fontWeight = FontWeight.SemiBold, color = TextDark)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(Rock3.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$percent%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.15f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Rock3.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (headerImageModel != null) {
                        AsyncImage(
                            model = headerImageModel,
                            contentDescription = "Scanned rock",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = rockName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Confidence: $percent%",
                    fontSize = 13.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )

                // -----------------------------
                // (1) INFO SECTION (replaces tips/next steps)
                // -----------------------------
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFEDEDED))
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Information",
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Spacer(Modifier.height(6.dp))

                when {
                    isLoading -> {
                        Text(
                            text = "Loading rock info…",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.75f)
                        )
                    }

                    errorMsg != null -> {
                        Text(
                            text = errorMsg!!,
                            fontSize = 12.sp,
                            color = Color(0xFFB00020)
                        )
                    }

                    else -> {
                        if (!rockRarity.isNullOrBlank()) {
                            Text(
                                text = "Rarity: $rockRarity",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                        }
                        if (!rockLocation.isNullOrBlank()) {
                            Text(
                                text = "Typical location: $rockLocation",
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = rockDesc?.takeIf { it.isNotBlank() }
                                ?: "No description available yet.",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.75f),
                            lineHeight = 18.sp
                        )
                    }
                }

                // -----------------------------
                // BUTTONS
                // -----------------------------
                Spacer(Modifier.height(14.dp))

                val actionHeight = 52.dp

                // 2) Save to Collection
                OutlinedButton(
                    onClick = {
                        if (alreadyInCollection) {
                            userViewModel.showInfo("Already in your collection.")
                            return@OutlinedButton
                        }

                        when (addState) {
                            AddState.Added -> userViewModel.showInfo("Already in your collection.")
                            AddState.Adding -> userViewModel.showInfo("Adding to collection…")
                            AddState.Idle -> {
                                pendingAddRockId = collectionRockId
                                addState = AddState.Adding

                                collectionViewModel.addRockFromIdentification(
                                    rockId = collectionRockId,
                                    rockName = rockName,
                                    thumbnailUrl = rockImageUrl,
                                    capturedImageUri = if (imageUri != Uri.EMPTY) imageUri else null,
                                    context = context
                                )
                            }
                        }
                    },
                    enabled = !alreadyInCollection && addState != AddState.Adding,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionHeight),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Rock3)
                ) {
                    Text(
                        text = when {
                            alreadyInCollection -> "Already in Collection"
                            addState == AddState.Added -> "Added"
                            addState == AddState.Adding -> "Adding…"
                            else -> "Save to Collection"
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }

                Spacer(Modifier.height(6.dp))

                // 2) Add Image to Personal Notes
                Button(
                    onClick = {
                        val uriToUpload = if (imageUri != Uri.EMPTY) imageUri else null
                        if (uriToUpload == null) {
                            userViewModel.showError(
                                "No image to save. Please scan with the camera or upload from gallery."
                            )
                            return@Button
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(actionHeight),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rock1)
                ) {
                    Text(
                        "Add Image to Personal Notes",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 3) Report Error
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            userViewModel.showInfo("Report submitted (TODO: wire to report flow).")
                        }
                    ) {
                        Text(
                            "Report error",
                            color = Color(0xFF8B2E2E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
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
    val payload = hashMapOf("image" to base64)

    val result = functions
        .getHttpsCallable("scanRockImage")
        .call(payload)
        .await()

    val map = (result.getData() as? Map<*, *>)
        ?: throw IllegalStateException("Invalid response from scanRockImage")

    val rockName = map["rockName"]?.toString() ?: "Unknown"
    val confidence = (map["confidence"] as? Number)?.toFloat() ?: 0f

    return rockName to confidence
}

