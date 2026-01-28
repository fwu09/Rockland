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
import com.example.rockland.data.model.RockComment
import com.example.rockland.data.model.RockCommunityContent
import com.example.rockland.data.model.RockLocation
import com.example.rockland.data.model.RockPhoto
import com.example.rockland.data.repository.RockLocationRepository
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

private val RockLocation.coordinates: LatLng
    get() = LatLng(latitude, longitude)

@Composable
fun MapScreen(
    viewModel: MapViewModel = MapViewModel(RockLocationRepository()),
    userViewModel: UserViewModel? = null,
    onInfoDetailsClick: () -> Unit = {},
    onAddCommentClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val filtersExpanded = remember { mutableStateOf(false) }
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val recenterRequests by viewModel.recenterRequests.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val infoCardVisible = remember { mutableStateOf(false) }
    val cameraState = rememberCameraPositionState()
    val showDetailsDialog = remember { mutableStateOf(false) }
    val sectionScrollState = rememberScrollState()

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
            viewModel.awardMessages.collect { msg ->
                userViewModel.showSuccess(msg)
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
                            OutlinedButton(
                                onClick = {
                                    selectedLocation?.id?.let { viewModel.recordReadRockInfo(it) }
                                    onInfoDetailsClick()
                                    showDetailsDialog.value = true
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Text(
                                    "View Details",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 14.sp
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
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
                                    onClick = { viewModel.showPhotoForm() },
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
                                    onEditComment = { id, text -> viewModel.editComment(id, text) },
                                    onDeleteComment = { id -> viewModel.deleteComment(id) }
                                )
                            }
                        }

                        AnimatedVisibility(visible = showCommentForm) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(commentFormScroll)
                            ) {
                                CommunityInputForm(
                                    title = "Share a comment",
                                    placeholder = "Describe what you saw or learned",
                                    value = commentDraft.value,
                                    onValueChange = { commentDraft.value = it },
                                    onSubmit = {
                                        if (commentDraft.value.isNotBlank()) {
                                            viewModel.submitComment(commentDraft.value.trim())
                                            commentDraft.value = ""
                                        }
                                    },
                                    onCancel = {
                                        viewModel.hideCommentForm()
                                        commentDraft.value = ""
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
                                    title = "Add photo caption",
                                    placeholder = "What makes this shot meaningful?",
                                    value = photoCaptionDraft.value,
                                    onValueChange = { photoCaptionDraft.value = it },
                                    onSubmit = {
                                        if (photoCaptionDraft.value.isNotBlank()) {
                                            viewModel.submitPhoto(
                                                photoCaptionDraft.value.trim(),
                                                photoUri.value?.toString().orEmpty()
                                            )
                                            photoCaptionDraft.value = ""
                                            photoUri.value = null
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
    onEditComment: (String, String) -> Unit,
    onDeleteComment: (String) -> Unit
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
                    currentUserId = currentUserId,
                    onEdit = onEditComment,
                    onDelete = onDeleteComment
                )
                CommunityTab.PHOTOS -> PhotosSection(content.photos)
                CommunityTab.ANNOTATIONS -> AnnotationsSection(content.annotations)
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<RockComment>,
    currentUserId: String?,
    onEdit: (String, String) -> Unit,
    onDelete: (String) -> Unit
) {
    val editingCommentId = remember { mutableStateOf<String?>(null) }
    var editDraft by remember { mutableStateOf("") }

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
            var menuExpanded by remember(comment.id) { mutableStateOf(false) }
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
                                        text = { Text("Edit") },
                                        onClick = {
                                            menuExpanded = false
                                            editDraft = comment.text
                                            editingCommentId.value = comment.id
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete") },
                                        onClick = {
                                            menuExpanded = false
                                            onDelete(comment.id)
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

    if (editingCommentId.value != null) {
        Dialog(onDismissRequest = { editingCommentId.value = null }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Edit Comment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TextField(
                        value = editDraft,
                        onValueChange = { editDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Update your comment") }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { editingCommentId.value = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val commentId = editingCommentId.value ?: return@Button
                                onEdit(commentId, editDraft.trim())
                                editingCommentId.value = null
                            },
                            enabled = editDraft.isNotBlank()
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotosSection(photos: List<RockPhoto>) {
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
                        Text(text = "Photo", color = Rock1)
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
private fun AnnotationsSection(annotations: List<RockAnnotation>) {
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
                    Text(
                        text = annotation.note,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium
                    )
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
