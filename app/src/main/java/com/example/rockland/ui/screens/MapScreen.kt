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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.OutlinedTextField
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
import com.example.rockland.data.repository.UserProfileRepository
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import com.example.rockland.util.ImageValidationUtil
import com.example.rockland.util.TextValidationUtil

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
    val openInfoCardForLocationId by viewModel.openInfoCardForLocationId.collectAsState()
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
    data class DeletePrompt(
        val contentTypeLabel: String,
        val confirmLabel: String,
        val onConfirm: () -> Unit
    )
    val deletePrompt = remember { mutableStateOf<DeletePrompt?>(null) }

    LaunchedEffect(openInfoCardForLocationId, selectedLocation?.id) {
        val targetId = openInfoCardForLocationId ?: return@LaunchedEffect
        val location = selectedLocation ?: return@LaunchedEffect
        if (location.id != targetId) return@LaunchedEffect
        infoCardVisible.value = true
        cameraState.move(CameraUpdateFactory.newLatLng(location.coordinates))
        viewModel.consumeOpenInfoCardRequest()
    }

    val addAnnotationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateTypeAndSize(context, uri)) {
            is ImageValidationUtil.Result.Ok -> annotationImageUri.value = uri
            is ImageValidationUtil.Result.Error -> {
                userViewModel?.showError(res.message)
                annotationImageUri.value = null
            }
        }
    }

    val editAnnotationPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateTypeAndSize(context, uri)) {
            is ImageValidationUtil.Result.Ok -> editAnnotationImageUri.value = uri
            is ImageValidationUtil.Result.Error -> {
                userViewModel?.showError(res.message)
                editAnnotationImageUri.value = null
            }
        }
    }

    val userData = userViewModel?.userData?.collectAsState()?.value
    val normalizedRole = userData?.role?.trim()?.lowercase()
    val isVerifiedExpert = normalizedRole == "verified_expert"
    val isAdmin = normalizedRole == "admin" || normalizedRole == "user_admin"

    val hasLocationPermission = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission.value = fineGranted || coarseGranted
    }

    LaunchedEffect(Unit) {
        launch {
            viewModel.locationCrudError.collect { msg ->
                userViewModel?.showError(msg)
            }
        }
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
    val showAddLocationForm by viewModel.showAddLocationForm.collectAsState()
    val showEditLocation by viewModel.showEditLocation.collectAsState()
    val locationToDelete by viewModel.locationToDelete.collectAsState()
    val selectedLocationCategoryLatest by viewModel.selectedLocationCategoryLatest.collectAsState()

    val commentDraft = remember { mutableStateOf("") }
    val photoCaptionDraft = remember { mutableStateOf("") }
    val photoUri = remember { mutableStateOf<Uri?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateTypeAndSize(context, uri)) {
            is ImageValidationUtil.Result.Ok -> photoUri.value = uri
            is ImageValidationUtil.Result.Error -> {
                userViewModel?.showError(res.message)
                photoUri.value = null
            }
        }
    }

     val commentFormScroll = rememberScrollState()
    val photoFormScroll = rememberScrollState()
    val commentPhotoUris = remember { mutableStateOf<List<Uri>>(emptyList())}


    val commentPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateTypeAndSize(context, uri)) {
            is ImageValidationUtil.Result.Ok -> {
                val current = commentPhotoUris.value
                if (current.size < 3) commentPhotoUris.value = current + uri
            }
            is ImageValidationUtil.Result.Error -> userViewModel?.showError(res.message)
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
                    if (linkedComment != null) {
                        Text(linkedComment.text, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "- ${linkedComment.author}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    } else {
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

    val addLocationName = remember { mutableStateOf("") }
    val addLocationDesc = remember { mutableStateOf("") }
    val addLocationLat = remember { mutableStateOf("") }
    val addLocationLng = remember { mutableStateOf("") }

    if (showAddLocationForm) {
        AddLocationDialog(
            name = addLocationName.value,
            onNameChange = { addLocationName.value = it },
            description = addLocationDesc.value,
            onDescriptionChange = { addLocationDesc.value = it },
            latitude = addLocationLat.value,
            onLatitudeChange = { addLocationLat.value = it },
            longitude = addLocationLng.value,
            onLongitudeChange = { addLocationLng.value = it },
            categoryDisplay = "unverified",
            onDismiss = {
                viewModel.hideAddLocationForm()
                addLocationName.value = ""
                addLocationDesc.value = ""
                addLocationLat.value = ""
                addLocationLng.value = ""
            },
            onSubmit = {
                val nameTrim = addLocationName.value.trim()
                val descTrim = addLocationDesc.value.trim()
                val latParsed = addLocationLat.value.trim().toDoubleOrNull()
                val lngParsed = addLocationLng.value.trim().toDoubleOrNull()
                when {
                    nameTrim.isBlank() -> userViewModel?.showError("Name is required.")
                    descTrim.isBlank() -> userViewModel?.showError("Description is required.")
                    latParsed == null -> userViewModel?.showError("Latitude must be a number.")
                    lngParsed == null -> userViewModel?.showError("Longitude must be a number.")
                    latParsed !in -90.0..90.0 -> userViewModel?.showError("Latitude must be between -90 and 90.")
                    lngParsed !in -180.0..180.0 -> userViewModel?.showError("Longitude must be between -180 and 180.")
                    else -> {
                        val latRounded = "%.6f".format(latParsed).toDouble()
                        val lngRounded = "%.6f".format(lngParsed).toDouble()
                        viewModel.createLocation(nameTrim, descTrim, latRounded, lngRounded)
                        addLocationName.value = ""
                        addLocationDesc.value = ""
                        addLocationLat.value = ""
                        addLocationLng.value = ""
                    }
                }
            }
        )
    }

    val editLocation = showEditLocation
    val editCategoryDisplay = selectedLocationCategoryLatest ?: editLocation?.category ?: ""
    if (editLocation != null) {
        EditLocationDialog(
            location = editLocation,
            categoryDisplay = editCategoryDisplay,
            onDismiss = viewModel::hideEditLocation,
            onSubmit = { name, description, lat, lng ->
                val nameTrim = name.trim()
                val descTrim = description.trim()
                val latParsed = lat.trim().toDoubleOrNull()
                val lngParsed = lng.trim().toDoubleOrNull()
                when {
                    nameTrim.isBlank() -> userViewModel?.showError("Name is required.")
                    descTrim.isBlank() -> userViewModel?.showError("Description is required.")
                    latParsed == null -> userViewModel?.showError("Latitude must be a number.")
                    lngParsed == null -> userViewModel?.showError("Longitude must be a number.")
                    latParsed !in -90.0..90.0 -> userViewModel?.showError("Latitude must be between -90 and 90.")
                    lngParsed !in -180.0..180.0 -> userViewModel?.showError("Longitude must be between -180 and 180.")
                    else -> {
                        val latRounded = "%.6f".format(latParsed).toDouble()
                        val lngRounded = "%.6f".format(lngParsed).toDouble()
                        viewModel.updateLocation(editLocation.id, nameTrim, descTrim, latRounded, lngRounded)
                    }
                }
            }
        )
    }

    locationToDelete?.let { loc ->
        Dialog(onDismissRequest = viewModel::cancelDeleteLocation) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Delete Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "Are you sure you want to delete this location?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = viewModel::cancelDeleteLocation) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge, color = Rock1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.deleteLocation(loc.id) },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B2E2E),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Delete", style = MaterialTheme.typography.labelLarge)
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

            userLocation?.let { coordinates ->
                Marker(
                    state = MarkerState(coordinates),
                    title = "You are here"
                )
            }
        }

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

        if (isVerifiedExpert || isAdmin) {
            IconButton(
                onClick = { viewModel.showAddLocationForm() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 26.dp)
                    .size(56.dp)
                    .background(Rock1.copy(alpha = 0.85f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add rock location",
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
                )
            }
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
                        FilterOption("All") { viewModel.filterRocks("all") }
                        FilterOption("Verified") { viewModel.filterRocks("verified") }
                        FilterOption("Unverified") { viewModel.filterRocks("unverified") }
                    }
                }
            }
        }

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
                            if ((isVerifiedExpert || isAdmin) && selectedLocation != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            selectedLocation?.let { viewModel.showEditLocation(it) }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp), tint = TextDark)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Edit", style = MaterialTheme.typography.labelSmall, fontSize = 13.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            selectedLocation?.let { viewModel.requestDeleteLocation(it) }
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF8B2E2E))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Delete", style = MaterialTheme.typography.labelSmall, fontSize = 13.sp, color = Color(0xFF8B2E2E))
                                    }
                                }
                            }
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 13.sp
                                )
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
                                if (isVerifiedExpert) {
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
                                tabs = listOf(CommunityTab.COMMENTS, CommunityTab.ANNOTATIONS),
                                activeTab = if (activeCommunityTab == CommunityTab.PHOTOS) CommunityTab.COMMENTS else activeCommunityTab,
                                onTabSelected = viewModel::setCommunityTab
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(sectionScrollState)
                            ) {
                                CommunityContentSection(
                                    tab = if (activeCommunityTab == CommunityTab.PHOTOS) CommunityTab.COMMENTS else activeCommunityTab,
                                    content = communityContent,
                                    currentUserId = currentUserId,
                                    isVerifiedExpert = isVerifiedExpert,
                                    isAdmin = isAdmin,
                                    onDeleteComment = { id ->
                                        deletePrompt.value = DeletePrompt(
                                            contentTypeLabel = "Comment",
                                            confirmLabel = "Yes"
                                        ) {
                                            viewModel.deleteComment(id)
                                            userViewModel?.showInfo("Comment has successfully been deleted!")
                                        }
                                    },
                                    onAdminDeleteComment = { id ->
                                        deletePrompt.value = DeletePrompt(
                                            contentTypeLabel = "Comment",
                                            confirmLabel = "Yes"
                                        ) {
                                            viewModel.deleteComment(id)
                                            userViewModel?.showInfo("Comment has successfully been deleted!")
                                        }
                                    },
                                    onPhotoClick = { photo -> selectedPhotoForDialog.value = photo },
                                    onAdminDeletePhoto = { photo ->
                                        deletePrompt.value = DeletePrompt(
                                            contentTypeLabel = "Image",
                                            confirmLabel = "Yes"
                                        ) {
                                            viewModel.deletePhoto(photo.locationPhotoId)
                                            userViewModel?.showInfo("Image has successfully been deleted!")
                                        }
                                    },
                                    onEditAnnotation = { annotation ->
                                        editAnnotationDraft.value = annotation.note
                                        editAnnotationImageUri.value = null
                                        editingAnnotation.value = annotation
                                    },
                                    onDeleteAnnotation = { annotation ->
                                        deletePrompt.value = DeletePrompt(
                                            contentTypeLabel = "Annotation",
                                            confirmLabel = "Yes"
                                        ) {
                                            viewModel.deleteAnnotation(annotation.id)
                                            userViewModel?.showInfo("Annotation has successfully been deleted!")
                                        }
                                    },
                                    onAdminDeleteAnnotation = { annotation ->
                                        deletePrompt.value = DeletePrompt(
                                            contentTypeLabel = "Annotation",
                                            confirmLabel = "Yes"
                                        ) {
                                            viewModel.deleteAnnotation(annotation.id)
                                            userViewModel?.showInfo("Annotation has successfully been deleted!")
                                        }
                                    }
                                )
                            }
                        }

                        AnimatedVisibility(visible = showCommentForm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .verticalScroll(commentFormScroll)
                            ) {
                                CommunityInputForm(
                                    title = "Share a comment!",
                                    placeholder = "Any interesting observations?",
                                    value = commentDraft.value,
                                    onValueChange = { commentDraft.value = it },
                                    onSubmit = {
                                        val text = commentDraft.value.trim()

                                        when (val res = TextValidationUtil.validateMapComment(text)) {
                                            is TextValidationUtil.Result.Error -> {
                                                userViewModel?.showError(res.message)
                                                return@CommunityInputForm
                                            }
                                            is TextValidationUtil.Result.Ok -> {
                                                val photos = commentPhotoUris.value
                                                if (photos.isNotEmpty()) {
                                                    viewModel.submitCommentWithPhotos(context, text, photos)
                                                } else {
                                                    viewModel.submitComment(text)
                                                }

                                                commentDraft.value = ""
                                                commentPhotoUris.value = emptyList()
                                            }
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
                                        when (val res = TextValidationUtil.validateExpertAnnotation(trimmed)) {
                                            is TextValidationUtil.Result.Error -> {
                                                userViewModel?.showError(res.message)
                                                return@CommunityInputForm
                                            }

                                            is TextValidationUtil.Result.Ok -> {
                                                viewModel.addAnnotationWithImage(
                                                    context = context,
                                                    note = trimmed,
                                                    imageUri = annotationImageUri.value
                                                )

                                                userViewModel?.showInfo("Annotation saved.")
                                                annotationDraft.value = ""
                                                annotationImageUri.value = null
                                                showAnnotationForm.value = false
                                            }
                                        }
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

                                                    when (val res = TextValidationUtil.validateExpertAnnotation(trimmed)) {
                                                        is TextValidationUtil.Result.Error -> {
                                                            userViewModel?.showError(res.message)
                                                            return@Button
                                                        }
                                                        is TextValidationUtil.Result.Ok -> {
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
                                                    }
                                                }
                                            ) {
                                                Text("Save")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        deletePrompt.value?.let { prompt ->
                            Dialog(onDismissRequest = { deletePrompt.value = null }) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(20.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "Delete ${prompt.contentTypeLabel}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )
                                        val messageText = when (prompt.contentTypeLabel.lowercase()) {
                                            "comment" -> "Are you sure you want to delete this comment?"
                                            "image" -> "Are you sure you want to delete this photo?"
                                            "annotation" -> "Are you sure you want to delete this annotation?"
                                            else -> "Are you sure you want to delete this item?"
                                        }
                                        Text(
                                            text = messageText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDark.copy(alpha = 0.8f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { deletePrompt.value = null }) {
                                                Text(
                                                    text = "Cancel",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = Rock1
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Button(
                                                onClick = {
                                                    prompt.onConfirm()
                                                    deletePrompt.value = null
                                                },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF8B2E2E),
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text(
                                                    text = prompt.confirmLabel,
                                                    style = MaterialTheme.typography.labelLarge
                                                )
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
                    Text(
                        text = tab.displayName(),
                        maxLines = 1
                    )
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
    isAdmin: Boolean,
    onDeleteComment: (String) -> Unit,
    onAdminDeleteComment: (String) -> Unit,
    onPhotoClick: (LocationPhoto) -> Unit,
    onAdminDeletePhoto: (LocationPhoto) -> Unit,
    onEditAnnotation: (RockAnnotation) -> Unit,
    onDeleteAnnotation: (RockAnnotation) -> Unit,
    onAdminDeleteAnnotation: (RockAnnotation) -> Unit
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
                    isAdmin = isAdmin,
                    onDelete = onDeleteComment,
                    onAdminDelete = onAdminDeleteComment,
                    onPhotoClick = onPhotoClick
                )

                CommunityTab.PHOTOS -> PhotosSection(
                    photos = content.photos,
                    isAdmin = isAdmin,
                    onPhotoClick = onPhotoClick,
                    onAdminDelete = onAdminDeletePhoto
                )

                CommunityTab.ANNOTATIONS -> AnnotationsSection(
                    annotations = content.annotations,
                    canEdit = { annotation ->
                        isVerifiedExpert && annotation.expertId == currentUserId
                    },
                    isAdmin = isAdmin,
                    onEdit = onEditAnnotation,
                    onDelete = onDeleteAnnotation,
                    onAdminDelete = onAdminDeleteAnnotation
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
    isAdmin: Boolean,
    onDelete: (String) -> Unit,
    onAdminDelete: (String) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        comments.forEach { comment ->
            var menuExpanded by remember(comment.commentId) { mutableStateOf(false) }
            val isOwner = comment.userId.isNotBlank() && comment.userId == currentUserId
            val canDelete = isOwner || isAdmin
            val attached = photos.filter { it.commentId == comment.commentId }
            var authorProfileUrl by remember(comment.userId) { mutableStateOf<String?>(null) }
            val profileRepo = remember { UserProfileRepository() }
            LaunchedEffect(comment.userId) {
                if (comment.userId.isNotBlank()) {
                    authorProfileUrl = runCatching { profileRepo.getUserProfile(comment.userId).profilePictureUrl }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                }
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CommentAvatar(profilePictureUrl = authorProfileUrl, displayName = comment.author)
                            Text(
                                text = comment.author,
                                color = TextDark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (canDelete) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More",
                                        tint = TextDark
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
                                            if (isAdmin && !isOwner) {
                                                onAdminDelete(comment.commentId)
                                            } else {
                                                onDelete(comment.commentId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = comment.text,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (attached.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(attached) { photo ->
                                AsyncImage(
                                    model = photo.imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(90.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { onPhotoClick(photo) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    val timeText = comment.updatedAt?.let {
                        "Edited ${TimeFormatter.formatLocal(it)}"
                    } ?: TimeFormatter.formatLocal(comment.timestamp)
                    Text(
                        text = timeText,
                        color = TextDark.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentAvatar(profilePictureUrl: String?, displayName: String) {
    val initials = displayName.trim().split(" ").filter { it.isNotBlank() }.take(2).map { it.first() }.joinToString("").ifBlank { "?" }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color(0xFFE8EAF1)),
        contentAlignment = Alignment.Center
    ) {
        if (!profilePictureUrl.isNullOrBlank()) {
            AsyncImage(
                model = profilePictureUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}
@Composable
private fun PhotosSection(
    photos: List<LocationPhoto>,
    isAdmin: Boolean,
    onPhotoClick: (LocationPhoto) -> Unit,
    onAdminDelete: (LocationPhoto) -> Unit
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
            var menuExpanded by remember(photo.locationPhotoId) { mutableStateOf(false) }
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
                    if (isAdmin) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Photo actions"
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
                                        onAdminDelete(photo)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnotationsSection(
    annotations: List<RockAnnotation>,
    canEdit: (RockAnnotation) -> Boolean,
    isAdmin: Boolean,
    onEdit: (RockAnnotation) -> Unit,
    onDelete: (RockAnnotation) -> Unit,
    onAdminDelete: (RockAnnotation) -> Unit
) {
    if (annotations.isEmpty()) {
        Text(
            text = "No expert annotations to show here yet.",
            color = TextDark.copy(alpha = 0.7f)
        )
        return
    }

    val profileRepo = remember { UserProfileRepository() }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        annotations.forEach { annotation ->
            var expertProfileUrl by remember(annotation.expertId) { mutableStateOf<String?>(null) }
            LaunchedEffect(annotation.expertId) {
                if (annotation.expertId.isNotBlank()) {
                    expertProfileUrl = runCatching { profileRepo.getUserProfile(annotation.expertId).profilePictureUrl }
                        .getOrNull()?.takeIf { it.isNotBlank() }
                }
            }
            val canEditOrAdmin = canEdit(annotation) || isAdmin
            var menuExpanded by remember(annotation.id) { mutableStateOf(false) }
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)),
                tonalElevation = 1.dp,
                shadowElevation = 2.dp,
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CommentAvatar(profilePictureUrl = expertProfileUrl, displayName = annotation.expertName)
                            Text(
                                text = annotation.expertName,
                                color = TextDark,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (canEditOrAdmin) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Annotation actions",
                                        tint = TextDark
                                    )
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (canEdit(annotation)) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                menuExpanded = false
                                                onEdit(annotation)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuExpanded = false
                                            if (isAdmin && !canEdit(annotation)) {
                                                onAdminDelete(annotation)
                                            } else {
                                                onDelete(annotation)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = annotation.note,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (annotation.imageUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(annotation.imageUrls.size) { idx ->
                                AsyncImage(
                                    model = annotation.imageUrls[idx],
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(height = 120.dp, width = 160.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = TimeFormatter.formatLocal(annotation.timestamp),
                        color = TextDark.copy(alpha = 0.5f),
                        style = MaterialTheme.typography.labelSmall
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
private fun AddLocationDialog(
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    latitude: String,
    onLatitudeChange: (String) -> Unit,
    longitude: String,
    onLongitudeChange: (String) -> Unit,
    categoryDisplay: String,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add Rock Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name (required)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description (required)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = latitude,
                    onValueChange = onLatitudeChange,
                    label = { Text("Latitude (-90 to 90, max 6 decimals)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = longitude,
                    onValueChange = onLongitudeChange,
                    label = { Text("Longitude (-180 to 180, max 6 decimals)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                Text(
                    text = "Category: $categoryDisplay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Rock1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSubmit,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rock1)
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}

@Composable
private fun EditLocationDialog(
    location: RockLocation,
    categoryDisplay: String,
    onDismiss: () -> Unit,
    onSubmit: (name: String, description: String, lat: String, lng: String) -> Unit
) {
    var name by remember(location.id) { mutableStateOf(location.name) }
    var description by remember(location.id) { mutableStateOf(location.description) }
    var latStr by remember(location.id) { mutableStateOf(location.latitude.toString()) }
    var lngStr by remember(location.id) { mutableStateOf(location.longitude.toString()) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Rock Location",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (required)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (required)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = latStr,
                    onValueChange = { latStr = it },
                    label = { Text("Latitude (-90 to 90, max 6 decimals)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lngStr,
                    onValueChange = { lngStr = it },
                    label = { Text("Longitude (-180 to 180, max 6 decimals)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                Text(
                    text = "Category (read-only): $categoryDisplay",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Rock1)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(name, description, latStr, lngStr) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Rock1)
                    ) {
                        Text("Save")
                    }
                }
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
        CommunityTab.ANNOTATIONS -> "Expert"
    }
}
