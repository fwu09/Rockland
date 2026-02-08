// Shows nearby rock locations and lets users recenter or view details.
package com.example.rockland.ui.screens
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.rockland.data.model.RockAnnotation
import com.example.rockland.data.model.LocationComment
import com.example.rockland.data.model.RockCommunityContent
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.model.LocationPhoto
import com.example.rockland.ui.theme.BackgroundLight
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.presentation.viewmodel.CommunityTab
import com.example.rockland.presentation.viewmodel.MapViewModel
import com.example.rockland.presentation.viewmodel.UserViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.foundation.clickable
import com.example.rockland.util.TimeFormatter
import kotlinx.coroutines.launch

private val RockLocation.coordinates: LatLng
    get() = LatLng(latitude, longitude)

@Composable
fun MapScreen(
    viewModel: MapViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    userViewModel: UserViewModel? = null,
    onInfoDetailsClick: () -> Unit = {},
    onAddCommentClick: () -> Unit = {}
)
 {
    val context = LocalContext.current
    val filtersExpanded = remember { mutableStateOf(false) }
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val recenterRequests by viewModel.recenterRequests.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val isPosting by viewModel.isPosting.collectAsState()
    val infoCardVisible = remember { mutableStateOf(false) }
    val cameraState = rememberCameraPositionState()
    val showDetailsDialog = remember { mutableStateOf(false) }
    val sectionScrollState = rememberScrollState()
    val selectedPhotoForDialog = remember { mutableStateOf<LocationPhoto?>(null) }
    val suppressAwardUntil = remember { mutableLongStateOf(0L) }
    val showAnnotationForm = remember { mutableStateOf(false) }
    val editingAnnotation = remember { mutableStateOf<RockAnnotation?>(null) }
    val editAnnotationDraft = remember { mutableStateOf("") }
    val editAnnotationImageUri = remember { mutableStateOf<Uri?>(null) }
    val annotationDraft = remember { mutableStateOf("") }
    val annotationImageUri = remember { mutableStateOf<Uri?>(null) }
    val addAnnotationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val sizeOk = runCatching {
            val length = context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { it.length }
                ?: -1L
            length in 1..20L * 1024L * 1024L
        }.getOrDefault(false)
        val typeOk = mimeType == "image/jpeg" || mimeType == "image/png"
        if (!typeOk || !sizeOk) {
            userViewModel?.showError(
                "Upload failed. The image must be a JPEG or PNG and under 20MB."
            )
            annotationImageUri.value = null
        } else {
            annotationImageUri.value = uri
        }
    }
    val editAnnotationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val sizeOk = runCatching {
            val length = context.contentResolver
                .openAssetFileDescriptor(uri, "r")
                ?.use { it.length }
                ?: -1L
            length in 1..20L * 1024L * 1024L
        }.getOrDefault(false)
        val typeOk = mimeType == "image/jpeg" || mimeType == "image/png"
        if (!typeOk || !sizeOk) {
            userViewModel?.showError(
                "Upload failed. The image must be a JPEG or PNG and under 20MB."
            )
            editAnnotationImageUri.value = null
        } else {
            editAnnotationImageUri.value = uri
        }
    }
    val userData = userViewModel?.userData?.collectAsState()?.value
    val isVerifiedExpert = userData?.role?.trim()?.lowercase() == "verified_expert"

    // Location permission state
    val hasLocationPermission = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission.value = fineGranted || coarseGranted
    }

    LaunchedEffect(Unit) {
        if (userViewModel != null) {
            launch {
                viewModel.awardMessages.collect { msg ->
                    val now = System.currentTimeMillis()
                    if (now >= suppressAwardUntil.longValue) {
                        userViewModel.showSuccess(msg)
                    }
                }
            }
            launch {
                viewModel.submissionMessages.collect { msg ->
                    suppressAwardUntil.longValue = System.currentTimeMillis() + 4500L
                    userViewModel.showInfo(msg)
                }
            }
        }
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val granted = fineGranted || coarseGranted
        hasLocationPermission.value = granted

        if (!granted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

    }

    val communityContent by viewModel.communityContent.collectAsState()
    val activeCommunityTab by viewModel.activeCommunityTab.collectAsState()
    val showCommentForm by viewModel.showAddCommentForm.collectAsState()
    val showPhotoForm by viewModel.showAddPhotoForm.collectAsState()

    val commentDraft = remember { mutableStateOf("") }
    val photoCaptionDraft = remember { mutableStateOf("") }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
            photoUri.value = uri
        }
    val commentFormScroll = rememberScrollState()
    val photoFormScroll = rememberScrollState()
    val commentPhotoUris = remember { mutableStateOf<List<Uri>>(emptyList())}


    val commentPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val current = commentPhotoUris.value
            if (current.size < 3) {
                commentPhotoUris.value = current + uri
            }
        }
    }

    LaunchedEffect(recenterRequests) {
        if (recenterRequests > 0) {
            userLocation?.let {
                cameraState.animate(
                    CameraUpdateFactory.newLatLngZoom(it, 15f),
                    durationMs = 600
                )
            }
        }
    }

    val currentSelection = selectedLocation
    if (showDetailsDialog.value && currentSelection != null) {
        RockDetailsDialog(
            location = currentSelection,
            onDismiss = { showDetailsDialog.value = false }
        )
    }
    // show popup dialog when photo is selected in "Photos" section
    selectedPhotoForDialog.value?.let { photo ->
        val linkedComment = photo.commentId?.let { cid ->
            communityContent.comments.firstOrNull { it.commentId == cid }
        }
        val canDelete = photo.userId.isNotBlank() && photo.userId == currentUserId

        Dialog(onDismissRequest = { selectedPhotoForDialog.value = null }) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = photo.imageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    // show linked comment/original comment photo is from
                    if (linkedComment != null) {
                        Text(linkedComment.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "- ${linkedComment.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    } else { //or show the caption that came with the photo input
                        Text(photo.caption, style = MaterialTheme.typography.bodyMedium)
                    }

                    TextButton(
                        modifier = Modifier.align(Alignment.End),
                        onClick = { selectedPhotoForDialog.value = null }
                    ) {
                        Text("Close")
                    }

                    if (canDelete) {
                        TextButton(
                            onClick = {
                                viewModel.deletePhoto(photo.locationPhotoId)
                                selectedPhotoForDialog.value = null
                            }
                        ) {
                            Text("Delete photo")
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        GoogleMap(
            modifier = Modifier
                .fillMaxSize()
                .background(Rock3),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = hasLocationPermission.value),
            uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        ) {
            uiState.locations.forEach { location ->
                Marker(
                    state = MarkerState(location.coordinates),
                    title = location.name,
                    snippet = location.description,
                    onClick = {
                        viewModel.selectLocation(location.id)
                        cameraState.move(CameraUpdateFactory.newLatLng(location.coordinates))
                        infoCardVisible.value = true
                        true
                    }
                )
            }

            // User location marker (based on last "Locate me" press)
            userLocation?.let { coordinates ->
                Marker(
                    state = MarkerState(coordinates),
                    title = "You are here"
                )
            }
        }

        // Top search header with subtle card background
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.95f),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Search Rock Type",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextDark,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Locate button — moved to lower right to avoid overlapping compass
        IconButton(
            onClick = { viewModel.moveToUserLocation() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 120.dp)
                .size(56.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = "Locate me", tint = Rock1)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = { filtersExpanded.value = !filtersExpanded.value },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White, CircleShape)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Layers filters", tint = Rock1)
            }

            AnimatedVisibility(filtersExpanded.value) {
                Card(
                    modifier = Modifier.width(190.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterOption("Your Sightings") { viewModel.filterRocks("your-sighting") }
                        FilterOption("Verified Sightings") { viewModel.filterRocks("verified") }
                        FilterOption("Public Sightings") { viewModel.filterRocks("public") }
                    }
                }
            }
        }

        // Loading state
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(
                        color = Rock1,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Text(text = "Loading rock locations...", color = TextDark)
                }
            }
        } else if (uiState.locations.isEmpty()) {
            // No rock data available (only after loading finished)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "No rock distribution data found in this area. Be the first to log a discovery!.",
                    color = TextDark
                )
            }
        }

        if (isPosting) {
            Dialog(onDismissRequest = {}) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Rock1,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Uploading... Please wait",
                            color = TextDark,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Location error message
        locationError?.let { message ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(text = message, color = Color.Red)
            }
        }

        // Rock info card overlay, centered, dismissible by tapping outside
        AnimatedVisibility(
            visible = infoCardVisible.value && selectedLocation != null,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = tween(200)
            ),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(
                targetScale = 0.9f,
                animationSpec = tween(150)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Rock3.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Rock1
                                    )
                                }

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Rock Info",
                                        color = TextDark,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = selectedLocation?.name ?: "Unknown rock",
                                        color = TextDark,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    infoCardVisible.value = false
                                    viewModel.clearSelection()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close info card"
                                )
                            }
                        }

                        selectedLocation?.category?.takeIf { it.isNotBlank() }?.let { category ->
                            RockCategoryChip(category = category)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (isVerifiedExpert) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedLocation?.id?.let { viewModel.recordReadRockInfo(it) }
                                            onInfoDetailsClick()
                                            showDetailsDialog.value = true
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "View Details",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 13.sp
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.hideCommentForm()
                                            viewModel.hidePhotoForm()
                                            viewModel.setCommunityTab(CommunityTab.ANNOTATIONS)
                                            showAnnotationForm.value = true
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            "Add Annotation",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedLocation?.id?.let { viewModel.recordReadRockInfo(it) }
                                        onInfoDetailsClick()
                                        showDetailsDialog.value = true
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "View Details",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        showAnnotationForm.value = false
                                        viewModel.hidePhotoForm()
                                        viewModel.setCommunityTab(CommunityTab.COMMENTS)
                                        viewModel.showCommentForm()
                                        onAddCommentClick()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Add Comment",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 13.sp
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        showAnnotationForm.value = false
                                        viewModel.hideCommentForm()
                                        viewModel.setCommunityTab(CommunityTab.PHOTOS)
                                        viewModel.showPhotoForm()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Add Photo",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            color = Color(0xFFF0F0F0),
                            thickness = 1.dp
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TabSelector(
                                tabs = CommunityTab.entries.toList(),
                                activeTab = activeCommunityTab,
                                onTabSelected = viewModel::setCommunityTab
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(sectionScrollState)
                            ) {
                                CommunityContentSection(
                                    tab = activeCommunityTab,
                                    content = communityContent,
                                    currentUserId = currentUserId,
                                    isVerifiedExpert = isVerifiedExpert,
                                    onDeleteComment = { id -> viewModel.deleteComment(id) },
                                    onPhotoClick = { photo -> selectedPhotoForDialog.value = photo },
                                    onEditAnnotation = { annotation ->
                                        editAnnotationDraft.value = annotation.note
                                        editAnnotationImageUri.value = null
                                        editingAnnotation.value = annotation
                                    },
                                    onDeleteAnnotation = { annotation ->
                                        viewModel.deleteAnnotation(annotation.id)
                                        userViewModel?.showInfo("Annotation deleted.")
                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = showCommentForm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp) // a bit taller for preview
                                    .verticalScroll(commentFormScroll)
                            ) {
                                CommunityInputForm(
                                    title = "Share a comment!",
                                    placeholder = "Any interesting observations?",
                                    value = commentDraft.value,
                                    onValueChange = { commentDraft.value = it },
                                    onSubmit = {
                                        val text = commentDraft.value.trim()
                                        if (text.isNotBlank()) {
                                            val photos = commentPhotoUris.value

                                            if (photos.isNotEmpty()) {
                                                viewModel.submitCommentWithPhotos(context, text, photos)
                                            } else {
                                                viewModel.submitComment(text)
                                            }

                                            commentDraft.value = ""
                                            commentPhotoUris.value = emptyList()
                                        }
                                    },
                                    onCancel = {
                                        viewModel.hideCommentForm()
                                        commentDraft.value = ""
                                        commentPhotoUris.value = emptyList()
                                    },
                                    additionalContent = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { if (commentPhotoUris.value.size < 3) commentPhotoPickerLauncher.launch("image/*") },
                                                enabled = commentPhotoUris.value.size < 3,
                                                colors = ButtonDefaults.buttonColors(containerColor = Rock3),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(if (commentPhotoUris.value.size < 3) "Attach photo (${commentPhotoUris.value.size}/3)" else "Max 3 photos")
                                            }

                                            if (commentPhotoUris.value.isEmpty()) {
                                                Text("No photos selected", color = TextDark.copy(alpha = 0.6f))
                                            } else {
                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    items(commentPhotoUris.value) { uri ->
                                                        Box {
                                                            AsyncImage(
                                                                model = uri,
                                                                contentDescription = null,
                                                                modifier = Modifier
                                                                    .size(90.dp)
                                                                    .clip(RoundedCornerShape(10.dp))
                                                            )
                                                            TextButton(
                                                                onClick = {
                                                                    commentPhotoUris.value = commentPhotoUris.value.filterNot { it == uri }
                                                                },
                                                                modifier = Modifier.align(Alignment.TopEnd)
                                                            ) {
                                                                Text("✕")
                                                            }
                                                        }
                                                    }
                                                }

                                                TextButton(
                                                    onClick = { commentPhotoUris.value = emptyList() }
                                                ) {
                                                    Text("Remove all")
                                                }
                                            }
                                        }

                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPhotoForm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(photoFormScroll)
                            ) {
                                CommunityInputForm(
                                    title = "Add Photo Caption",
                                    placeholder = "Any remarks about the rock identified?",
                                    value = photoCaptionDraft.value,
                                    onValueChange = { photoCaptionDraft.value = it },
                                    onSubmit = {
                                        val caption = photoCaptionDraft.value.trim()
                                        val uri = photoUri.value

                                        if (caption.isNotBlank() && uri != null) {
                                            viewModel.submitPhoto(
                                                context = context,
                                                caption = caption,
                                                imageUri = uri
                                            )
                                            photoCaptionDraft.value = ""
                                            photoUri.value = null
                                        } else {
                                            if (uri == null) userViewModel?.showError("Please pick a photo first")
                                            if (caption.isBlank()) userViewModel?.showError("Please enter a caption")
                                        }
                                    },
                                    onCancel = {
                                        viewModel.hidePhotoForm()
                                        photoCaptionDraft.value = ""
                                        photoUri.value = null
                                    },
                                    submitLabel = "Upload",
                                    additionalContent = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { photoPickerLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Rock3),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Pick from gallery")
                                            }
                                            photoUri.value?.let { uri ->
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            } ?: Text(
                                                text = "No photo selected",
                                                color = TextDark.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                        AnimatedVisibility(visible = showAnnotationForm.value) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(commentFormScroll)
                            ) {
                                CommunityInputForm(
                                    title = "Expert Annotation",
                                    placeholder = "Share your expert insight",
                                    value = annotationDraft.value,
                                    onValueChange = { annotationDraft.value = it },
                                    onSubmit = {
                                        val trimmed = annotationDraft.value.trim()
                                        if (trimmed.length !in 10..1000) {
                                            userViewModel?.showError(
                                                "Please enter between 10 and 1000 characters."
                                            )
                                            return@CommunityInputForm
                                        }
                                        viewModel.addAnnotationWithImage(
                                            context = context,
                                            note = trimmed,
                                            imageUri = annotationImageUri.value
                                        )
                                        userViewModel?.showInfo("Annotation saved.")
                                        annotationDraft.value = ""
                                        annotationImageUri.value = null
                                        showAnnotationForm.value = false
                                    },
                                    onCancel = {
                                        annotationDraft.value = ""
                                        annotationImageUri.value = null
                                        showAnnotationForm.value = false
                                    },
                                    submitLabel = "Publish Annotation",
                                    additionalContent = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Button(
                                                onClick = { addAnnotationPickerLauncher.launch("image/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Rock3),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Upload image")
                                            }
                                            annotationImageUri.value?.let { uri ->
                                                AsyncImage(
                                                    model = uri,
                                                    contentDescription = null,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(120.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        editingAnnotation.value?.let { annotation ->
                            Dialog(onDismissRequest = { editingAnnotation.value = null }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 420.dp)
                                            .verticalScroll(rememberScrollState())
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "Edit Annotation",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )
                                        TextField(
                                            value = editAnnotationDraft.value,
                                            onValueChange = { editAnnotationDraft.value = it },
                                            placeholder = { Text("Update annotation...") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Button(
                                            onClick = { editAnnotationPickerLauncher.launch("image/*") },
                                            colors = ButtonDefaults.buttonColors(containerColor = Rock3),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Update image")
                                        }
                                        if (editAnnotationImageUri.value != null) {
                                            AsyncImage(
                                                model = editAnnotationImageUri.value,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        } else if (annotation.imageUrls.isNotEmpty()) {
                                            AsyncImage(
                                                model = annotation.imageUrls.first(),
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { editingAnnotation.value = null }) {
                                                Text("Cancel")
                                            }
                                            Button(
                                                onClick = {
                                                    val trimmed = editAnnotationDraft.value.trim()
                                                    if (trimmed.length !in 10..1000) {
                                                        userViewModel?.showError(
                                                            "Please enter between 10 and 1000 characters."
                                                        )
                                                        return@Button
                                                    }
                                                    viewModel.updateAnnotationWithImage(
                                                        context = context,
                                                        annotationId = annotation.id,
                                                        note = trimmed,
                                                        imageUri = editAnnotationImageUri.value,
                                                        existingImageUrls = annotation.imageUrls
                                                    )
                                                    userViewModel?.showInfo("Annotation updated.")
                                                    editingAnnotation.value = null
                                                }
                                            ) {
                                                Text("Save")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F2), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextDark)
        IconButton(onClick = onClick) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Rock1)
        }
    }
}

@Composable
private fun RockCategoryChip(category: String) {
    val (label, background, foreground) = when (category.lowercase()) {
        "verified" -> Triple("Verified Expert Sighting", Color(0xFFE3F2FD), Rock1)
        "public" -> Triple("Public Sighting", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "your-sighting" -> Triple("Your Sighting", Color(0xFFFFF3E0), Color(0xFFEF6C00))
        else -> Triple(category, Color(0xFFF2F2F2), TextDark)
    }

    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = foreground,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TabSelector(
    tabs: List<CommunityTab>,
    activeTab: CommunityTab,
    onTabSelected: (CommunityTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp)),
        color = Color(0xFFFAFAFA)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = tab == activeTab
                Button(
                    onClick = { onTabSelected(tab) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) Rock1.copy(alpha = 0.2f) else Color.White,
                        contentColor = if (isSelected) Rock1 else TextDark
                    )
                ) {
                    Text(tab.displayName())
                }
            }
        }
    }
}

@Composable
private fun CommunityContentSection(
    tab: CommunityTab,
    content: RockCommunityContent,
    currentUserId: String?,
    isVerifiedExpert: Boolean,
    onDeleteComment: (String) -> Unit,
    onPhotoClick: (LocationPhoto) -> Unit,
    onEditAnnotation: (RockAnnotation) -> Unit,
    onDeleteAnnotation: (RockAnnotation) -> Unit
) {

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF7F7F7)),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            when (tab) {
                CommunityTab.COMMENTS -> CommentsSection(
                    comments = content.comments,
                    photos = content.photos,
                    currentUserId = currentUserId,
                    onDelete = onDeleteComment,
                    onPhotoClick = onPhotoClick
                )

                CommunityTab.PHOTOS -> PhotosSection(
                    photos = content.photos,
                    onPhotoClick = onPhotoClick
                )

                CommunityTab.ANNOTATIONS -> AnnotationsSection(
                    annotations = content.annotations,
                    canEdit = { annotation ->
                        isVerifiedExpert && annotation.expertId == currentUserId
                    },
                    onEdit = onEditAnnotation,
                    onDelete = onDeleteAnnotation
                )
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<LocationComment>,
    photos: List<LocationPhoto>,
    currentUserId: String?,
    onDelete: (String) -> Unit,
    onPhotoClick: (LocationPhoto) -> Unit
)
 {

    if (comments.isEmpty()) {
        Text(
            text = "No comments yet. Share a quick note about this rock.",
            color = TextDark.copy(alpha = 0.7f)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        comments.forEach { comment ->
            var menuExpanded by remember(comment.commentId) { mutableStateOf(false) }
            val isOwner = comment.userId.isNotBlank() && comment.userId == currentUserId
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = comment.text,
                                color = TextDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "- ${comment.author}",
                                color = TextDark.copy(alpha = 0.6f),
                                style = MaterialTheme.typography.bodySmall
                            )
                            val timeText = comment.updatedAt?.let {
                                "Edited ${TimeFormatter.formatLocal(it)}"
                            } ?: TimeFormatter.formatLocal(comment.timestamp)

                            Text(
                                text = timeText,
                                color = TextDark.copy(alpha = 0.45f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        if (isOwner) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete(comment.commentId)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            val attached = photos.filter { it.commentId == comment.commentId }

            if (attached.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(attached) { photo ->
                        AsyncImage(
                            model = photo.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPhotoClick(photo) }
                        )
                    }
                }
            }
        }
    }
}
@Composable
private fun PhotosSection(
    photos: List<LocationPhoto>,
    onPhotoClick: (LocationPhoto) -> Unit
) {
    if (photos.isEmpty()) {
        Text(
            text = "No photos yet. Upload a snapshot to share your insight.",
            color = TextDark.copy(alpha = 0.7f)
        )
        return
    }

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        items(photos) { photo ->
            Surface(
                modifier = Modifier
                    .width(160.dp)
                    .clip(RoundedCornerShape(14.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Rock3.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photo.imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onPhotoClick(photo) }
                        )
                    }
                    Text(
                        text = photo.caption,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = photo.author,
                        color = TextDark.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun AnnotationsSection(
    annotations: List<RockAnnotation>,
    canEdit: (RockAnnotation) -> Boolean,
    onEdit: (RockAnnotation) -> Unit,
    onDelete: (RockAnnotation) -> Unit
) {
    if (annotations.isEmpty()) {
        Text(
            text = "No expert annotations to show here yet.",
            color = TextDark.copy(alpha = 0.7f)
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        annotations.forEach { annotation ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = annotation.note,
                                color = TextDark,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (annotation.imageUrls.isNotEmpty()) {
                                AsyncImage(
                                    model = annotation.imageUrls.first(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                        if (canEdit(annotation)) {
                            val menuExpanded = remember(annotation.id) { mutableStateOf(false) }
                            Box {
                                IconButton(onClick = { menuExpanded.value = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Annotation actions"
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded.value,
                                    onDismissRequest = { menuExpanded.value = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            menuExpanded.value = false
                                            onEdit(annotation)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuExpanded.value = false
                                            onDelete(annotation)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = annotation.expertName,
                        color = TextDark.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}


@Composable
private fun CommunityInputForm(
    title: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    submitLabel: String = "Post",
    additionalContent: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            color = TextDark,
            fontWeight = FontWeight.SemiBold
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        additionalContent?.invoke()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            Button(onClick = onSubmit) {
                Text(submitLabel)
            }
        }
    }
}

@Composable
private fun RockDetailsDialog(location: RockLocation, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = location.name.ifBlank { "Rock Details" },
                    style = MaterialTheme.typography.titleMedium,
                    color = TextDark
                )
                Text(
                    text = "Rock Information",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                HorizontalDivider(thickness = 1.dp, color = Color(0xFFEFEFEF))
                Text(
                    text = "Category: ${location.category.ifBlank { "Public Sighting" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
                Text(
                    text = "Location: ${location.latitude.format(3)}, ${location.longitude.format(3)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )
                Text(
                    text = location.description.ifBlank { "No additional description provided yet." },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }

}

private fun Double.format(decimals: Int): String {
    return "%.${decimals}f".format(this)
}

private fun CommunityTab.displayName(): String {
    return when (this) {
        CommunityTab.COMMENTS -> "Comments"
        CommunityTab.PHOTOS -> "Photos"
        CommunityTab.ANNOTATIONS -> "Expert Annotations"
    }
}
