package com.example.rockland.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import com.example.rockland.data.auth.FirebaseAuthRepository
import com.example.rockland.data.model.CollectionItem
import com.example.rockland.data.repository.ContentReviewRepository
import com.example.rockland.data.repository.Rock
import com.example.rockland.data.repository.RockRepository
import com.example.rockland.presentation.viewmodel.CollectionEvent
import com.example.rockland.presentation.viewmodel.CollectionViewModel
import com.example.rockland.presentation.viewmodel.UserViewModel
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun CollectionScreen(
    userViewModel: UserViewModel? = null,
    viewModel: CollectionViewModel = viewModel(),
    selectedTabIndex: Int? = null,
    onTabSelected: ((Int) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val userData = userViewModel?.userData?.collectAsState()?.value
    val isVerifiedExpert = userData?.role?.trim()?.lowercase() == "verified_expert"
    val internalTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val currentTab = selectedTabIndex ?: internalTabIndex.intValue
    val setTab: (Int) -> Unit = onTabSelected ?: { internalTabIndex.intValue = it }
    val selectedItem = remember { mutableStateOf<CollectionItem?>(null) }

    LaunchedEffect(userViewModel) {
        if (userViewModel == null) return@LaunchedEffect
        viewModel.events.collect { evt ->
            when (evt) {
                is CollectionEvent.Success -> userViewModel.showSuccess(evt.message)
                is CollectionEvent.Error -> userViewModel.showError(evt.message)
            }
        }
    }

    if (currentTab == 0 && selectedItem.value != null) {
        CollectionDetailScreen(
            item = selectedItem.value!!,
            collectionViewModel = viewModel,
            onBack = { selectedItem.value = null },
            onSaveNotes = { customId, location, notes ->
                val current = selectedItem.value
                if (current != null) {
                    viewModel.updateCollectionItem(
                        itemId = current.id,
                        customId = customId,
                        locationLabel = location,
                        notes = notes,
                        userImageUrls = current.effectiveUserImageUrls()
                    )
                    selectedItem.value = current.copy(
                        customId = customId,
                        locationLabel = location,
                        notes = notes
                    )
                }
            },
            onItemUpdated = { updated -> selectedItem.value = updated }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "My Rocks",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Collected: ${uiState.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                TabRow(
                    selectedTabIndex = currentTab,
                    modifier = Modifier.fillMaxWidth(),
                    containerColor = Color.White,
                    indicator = {}
                ) {
                    val tabShape = RoundedCornerShape(14.dp)

                    Tab(
                        selected = currentTab == 0,
                        onClick = { setTab(0) },
                        text = {
                            Text(
                                "Collections",
                                fontWeight = if (currentTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(tabShape)
                            .background(if (currentTab == 0) Rock3.copy(alpha = 0.18f) else Color.Transparent)
                    )

                    Tab(
                        selected = currentTab == 1,
                        onClick = { setTab(1) },
                        text = {
                            Text(
                                "Dictionary",
                                fontWeight = if (currentTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier
                            .padding(6.dp)
                            .clip(tabShape)
                            .background(if (currentTab == 1) Rock3.copy(alpha = 0.18f) else Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (currentTab) {
                0 -> CollectionsTabContent(
                    items = uiState.items,
                    isLoading = uiState.isLoading,
                    onDelete = { item -> viewModel.removeFromCollection(item.id) },
                    onSelect = { item -> selectedItem.value = item }
                )

                1 -> DictionaryTabContent(
                    userViewModel = userViewModel,
                    viewModel = viewModel,
                    collectionItems = uiState.items,
                    isVerifiedExpert = isVerifiedExpert
                )
            }
        }
    }
}

@Composable
private fun CollectionsTabContent(
    items: List<CollectionItem>,
    isLoading: Boolean,
    onDelete: (CollectionItem) -> Unit,
    onSelect: (CollectionItem) -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))

    if (isLoading && items.isEmpty()) {
        CollectionSkeletonList()
        return
    }

    if (items.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your virtual collection is empty. Go out and identify your first rock!",
                color = Color(0xFF555555),
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                CollectionListItem(
                    item = item,
                    onDelete = { onDelete(item) },
                    onSelect = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
private fun CollectionSkeletonList() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(listOf(1, 2, 3, 4, 5, 6)) {
            CollectionSkeletonItem()
        }
    }
}

@Composable
private fun CollectionSkeletonItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Rock3.copy(alpha = 0.18f))
                )

                Column(modifier = Modifier.fillMaxWidth(0.85f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE6E6E6))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEEEEE))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEEEEE))
            )
        }
    }
}

@Composable
private fun CollectionListItem(
    item: CollectionItem,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val menuExpanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Rock3.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = item.thumbnailUrl,
                            contentDescription = "${item.rockName} thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = "Photo",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = item.rockName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.65f),
                            maxLines = 2
                        )
                    }
                }
            }

            IconButton(onClick = { menuExpanded.value = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions"
                )
            }

            DropdownMenu(
                expanded = menuExpanded.value,
                onDismissRequest = { menuExpanded.value = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        menuExpanded.value = false
                        onDelete()
                    }
                )
            }
        }
    }
}

@Composable
private fun DictionaryTabContent(
    userViewModel: UserViewModel?,
    viewModel: CollectionViewModel,
    collectionItems: List<CollectionItem>,
    isVerifiedExpert: Boolean
) {
    val authRepo = remember { FirebaseAuthRepository.getInstance() }
    val user by authRepo.authState.collectAsState(initial = null)
    val userData = userViewModel?.userData?.collectAsState()?.value
    val normalizedRole = userData?.role?.trim()?.lowercase()
    val isAdmin = normalizedRole == "admin" || normalizedRole == "user_admin"

    val rockRepositoryOps = remember { RockRepository() }
    val reviewRepository = remember { ContentReviewRepository() }
    val scope = rememberCoroutineScope()

    val allRocks = remember { mutableStateOf<List<Rock>>(emptyList()) }
    val unlockedRockIds = remember { mutableStateOf<Set<String>>(emptySet()) }
    val isLoading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }
    val query = remember { mutableStateOf("") }
    val selectedRock = remember { mutableStateOf<Rock?>(null) }
    val showAddDialog = remember { mutableStateOf(false) }
    val showEditDialog = remember { mutableStateOf(false) }
    val editingRock = remember { mutableStateOf<Rock?>(null) }
    val rockToDelete = remember { mutableStateOf<Rock?>(null) }

    LaunchedEffect(user?.uid) {
        isLoading.value = true
        error.value = null

        runCatching {
            allRocks.value = viewModel.getDictionaryRocks()
        }.onFailure { e ->
            if (e is CancellationException) return@LaunchedEffect
            val msg = e.message ?: "Failed to load rock dictionary."
            error.value = msg
            userViewModel?.showError(msg)
        }

        isLoading.value = false
    }

    LaunchedEffect(user?.uid, collectionItems) {
        runCatching {
            val uid = user?.uid
            unlockedRockIds.value = viewModel.getUnlockedRockIds(uid, collectionItems)
        }.onFailure { e ->
            if (e is CancellationException) return@LaunchedEffect
        }
    }

    val q = query.value.trim()
    val filtered = if (q.isBlank()) allRocks.value
    else allRocks.value.filter { it.rockName.startsWith(q, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                OutlinedTextField(
                    value = query.value,
                    onValueChange = { query.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    label = { Text("Search rocks") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.value.isNotBlank()) {
                            IconButton(onClick = { query.value = "" }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Clear")
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading.value -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Loading...")
                    }
                }

                error.value != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(error.value!!, color = Color.Red)
                    }
                }

                filtered.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No matching rocks", fontWeight = FontWeight.SemiBold, color = TextDark)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Try a different spelling or clear the search.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                else -> {
                    val byRockId = remember(collectionItems) { collectionItems.associateBy { it.rockId } }
                    val byRockName = remember(collectionItems) { collectionItems.associateBy { it.rockName } }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.rockID }) { rock ->
                            val isCollected =
                                byRockId[rock.rockID.toString()] != null || byRockName[rock.rockName] != null

                            val unlocked =
                                unlockedRockIds.value.contains(rock.rockID.toString()) || isCollected

                            DictionaryRockCard(
                                rock = rock,
                                unlocked = unlocked,
                                onClick = { selectedRock.value = rock }
                            )
                        }
                    }

                    val rock = selectedRock.value
                    if (rock != null) {
                        val collectionItem = byRockId[rock.rockID.toString()] ?: byRockName[rock.rockName]
                        val isCollected = collectionItem != null
                        val isUnlocked = unlockedRockIds.value.contains(rock.rockID.toString()) || isCollected

                        RockDictionaryDialog(
                            rock = rock,
                            isUnlocked = isUnlocked,
                            collectionItem = collectionItem,
                            onDismiss = { selectedRock.value = null },
                            canEdit = isVerifiedExpert || isAdmin,
                            canDelete = isAdmin,
                            onEdit = {
                                editingRock.value = rock
                                showEditDialog.value = true
                                selectedRock.value = null
                            },
                            onDelete = {
                                rockToDelete.value = rock
                                selectedRock.value = null
                            }
                        )
                    }
                }
            }
        }

        if (isVerifiedExpert || isAdmin) {
            FloatingActionButton(
                onClick = { showAddDialog.value = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color(0xFF2A2A2A)
            ) {
                Text("+", color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
        }

        if (showAddDialog.value) {
            AddEditRockDialog(
                title = "Add New Rock",
                initialRock = null,
                onDismiss = { showAddDialog.value = false },
                onError = { msg -> userViewModel?.showError(msg) },
                onSubmit = { request ->
                    scope.launch {
                        runCatching {
                            val existing = rockRepositoryOps.getRockByName(request.rockName)
                            if (existing != null) {
                                userViewModel?.showError(
                                    "Existing rock already exists! Unable to add new Rock to Rock Dictionary."
                                )
                                showAddDialog.value = false
                                return@runCatching
                            }

                            val displayName = listOf(
                                userData?.firstName.orEmpty(),
                                userData?.lastName.orEmpty()
                            ).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                                userData?.email.orEmpty().ifBlank { "Verified Expert" }
                            }

                            val imageUrl = request.imageUri?.let { uri ->
                                reviewRepository.uploadRockDictionaryImage(
                                    userId = user?.uid.orEmpty(),
                                    imageUri = uri
                                )
                            } ?: request.existingImageUrl

                            reviewRepository.submitRockDictionaryRequest(
                                requestType = request.requestType,
                                rockID = request.rockID,
                                rockName = request.rockName,
                                rockRarity = request.rockRarity,
                                rockLocation = request.rockLocation,
                                rockDesc = request.rockDesc,
                                imageUrl = imageUrl,
                                submittedBy = displayName,
                                submittedById = user?.uid.orEmpty()
                            )

                            userViewModel?.showInfo(
                                "New Rock data has been saved and sent to the team for review. Please await a response from the Rockland Team."
                            )
                            showAddDialog.value = false
                        }.onFailure { e ->
                            userViewModel?.showError(e.message ?: "Failed to submit rock request.")
                        }
                    }
                }
            )
        }

        if (showEditDialog.value && editingRock.value != null) {
            AddEditRockDialog(
                title = "Edit Rock Information",
                initialRock = editingRock.value,
                onDismiss = {
                    showEditDialog.value = false
                    editingRock.value = null
                },
                onError = { msg -> userViewModel?.showError(msg) },
                onSubmit = { request ->
                    scope.launch {
                        runCatching {
                            val displayName = listOf(
                                userData?.firstName.orEmpty(),
                                userData?.lastName.orEmpty()
                            ).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
                                userData?.email.orEmpty().ifBlank { "Verified Expert" }
                            }

                            val imageUrl = request.imageUri?.let { uri ->
                                reviewRepository.uploadRockDictionaryImage(
                                    userId = user?.uid.orEmpty(),
                                    imageUri = uri
                                )
                            } ?: request.existingImageUrl

                            reviewRepository.submitRockDictionaryRequest(
                                requestType = request.requestType,
                                rockID = request.rockID,
                                rockName = request.rockName,
                                rockRarity = request.rockRarity,
                                rockLocation = request.rockLocation,
                                rockDesc = request.rockDesc,
                                imageUrl = imageUrl,
                                submittedBy = displayName,
                                submittedById = user?.uid.orEmpty()
                            )

                            userViewModel?.showInfo(
                                "Rock Information Update has been saved and sent to the team for review. Please await a response from the Rockland Team."
                            )
                            showEditDialog.value = false
                            editingRock.value = null
                        }.onFailure { e ->
                            userViewModel?.showError(e.message ?: "Failed to submit rock request.")
                        }
                    }
                }
            )
        }
    }

    rockToDelete.value?.let { rock ->
        Dialog(onDismissRequest = { rockToDelete.value = null }) {
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
                        text = "Delete Rock",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "Are you sure you want to delete this Rock?",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { rockToDelete.value = null }) {
                            Text("Cancel", style = MaterialTheme.typography.labelLarge, color = Rock1)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                rockToDelete.value = null
                                scope.launch {
                                    runCatching {
                                        val hasDeps = rockRepositoryOps.hasActiveDependencies(rock.rockID)
                                        if (hasDeps) {
                                            userViewModel?.showError(
                                                "This rock is used in active mission/achievement. Please remove dependencies first."
                                            )
                                        } else {
                                            rockRepositoryOps.deleteRock(rock.rockID)
                                            allRocks.value = allRocks.value.filterNot { it.rockID == rock.rockID }
                                            userViewModel?.showInfo("Rock has successfully been deleted!")
                                        }
                                    }.onFailure { e ->
                                        userViewModel?.showError(e.message ?: "Failed to delete rock.")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B2E2E),
                                contentColor = Color.White
                            )
                        ) {
                            Text("Yes", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

private data class PendingRockRequest(
    val requestType: String,
    val rockID: Int,
    val rockName: String,
    val rockRarity: String,
    val rockLocation: String,
    val rockDesc: String,
    val imageUri: Uri?,
    val existingImageUrl: String
)

@Composable
private fun AddEditRockDialog(
    title: String,
    initialRock: Rock?,
    onDismiss: () -> Unit,
    onError: (String) -> Unit,
    onSubmit: (PendingRockRequest) -> Unit
) {
    val context = LocalContext.current
    val rockName = remember { mutableStateOf(initialRock?.rockName ?: "") }
    val rockRarity = remember { mutableStateOf(initialRock?.rockRarity ?: "") }
    val rockLocation = remember { mutableStateOf(initialRock?.rockLocation ?: "") }
    val rockDesc = remember { mutableStateOf(initialRock?.rockDesc ?: "") }
    val imageUri = remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(
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
            onError("Upload failed. The image must be a JPEG or PNG and under 20MB.")
            imageUri.value = null
        } else {
            imageUri.value = uri
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                OutlinedTextField(
                    value = rockName.value,
                    onValueChange = { rockName.value = it },
                    label = { Text("Rock Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rockRarity.value,
                    onValueChange = { rockRarity.value = it },
                    label = { Text("Rock Rarity") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rockLocation.value,
                    onValueChange = { rockLocation.value = it },
                    label = { Text("Rock Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = rockDesc.value,
                    onValueChange = { rockDesc.value = it },
                    label = { Text("Rock Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Button(
                    onClick = { picker.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = Rock3),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Upload image")
                }
                if (imageUri.value != null) {
                    AsyncImage(
                        model = imageUri.value,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (initialRock?.rockImageUrl?.isNotBlank() == true) {
                    AsyncImage(
                        model = initialRock.rockImageUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val name = rockName.value.trim()
                        val rarity = rockRarity.value.trim()
                        val location = rockLocation.value.trim()
                        val desc = rockDesc.value.trim()
                        if (name.isBlank() || rarity.isBlank() || location.isBlank()) {
                            onError("Please fill in all required fields.")
                            return@Button
                        }
                        if (desc.length !in 10..1000) {
                            onError("Description must be between 10 and 1000 characters. Please edit your description and try again.")
                            return@Button
                        }
                        if (initialRock == null && imageUri.value == null) {
                            onError("You must upload a rock image before submitting.")
                            return@Button
                        }
                        val request = PendingRockRequest(
                            requestType = if (initialRock == null) "ADD" else "EDIT",
                            rockID = initialRock?.rockID ?: 0,
                            rockName = name,
                            rockRarity = rarity,
                            rockLocation = location,
                            rockDesc = desc,
                            imageUri = imageUri.value,
                            existingImageUrl = initialRock?.rockImageUrl.orEmpty()
                        )
                        onSubmit(request)
                    }) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

@Composable
private fun DictionaryRockCard(
    rock: Rock,
    unlocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Rock3.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                if (unlocked && rock.rockImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rock.rockImageUrl,
                        contentDescription = rock.rockName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                if (!unlocked) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Locked",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Text(
                    text = rock.rockName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    maxLines = 1
                )

                if (!unlocked) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Find to unlock details",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.65f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun RockDictionaryDialog(
    rock: Rock,
    isUnlocked: Boolean,
    collectionItem: CollectionItem?,
    onDismiss: () -> Unit,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            shape = RoundedCornerShape(22.dp),
            color = Color.White
        ) {
            RockDictionaryDialogContent(
                rock = rock,
                isUnlocked = isUnlocked,
                collectionItem = collectionItem,
                canEdit = canEdit,
                canDelete = canDelete,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun RockDictionaryDialogContent(
    rock: Rock,
    isUnlocked: Boolean,
    collectionItem: CollectionItem?,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    val collectedAt = collectionItem?.createdAt?.toDate()?.let { formatter.format(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (isUnlocked) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .background(Color(0xFFF0F0F0)),
                contentAlignment = Alignment.Center
            ) {
                if (rock.rockImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = rock.rockImageUrl,
                        contentDescription = rock.rockName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF3A3A3A), Color(0xFF7A7A7A))
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Not found yet",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = rock.rockName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = rock.rockDesc.ifBlank { "No description available." },
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEDEDED))
            Spacer(modifier = Modifier.height(10.dp))

            if (isUnlocked) {
                val statusText = if (collectionItem != null) "Collected" else "Unlocked"
                Text(
                    text = "Collection Status: $statusText",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold
                )
                if (collectedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Collection date: $collectedAt",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(label = "Rarity", value = rock.rockRarity)
                Spacer(modifier = Modifier.height(6.dp))
                InfoRow(label = "Location", value = rock.rockLocation)

                if (canEdit || canDelete) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (canEdit) {
                            OutlinedButton(
                                onClick = onEdit,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Edit Rock", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (canDelete) {
                            Button(
                                onClick = onDelete,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2E2E)),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Delete Rock", color = Color.White, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            } else {
                Text(
                    text = "Find this rock in the real world to unlock its full details (image, rarity, location).",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.65f)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark.copy(alpha = 0.85f),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = TextDark,
            textAlign = TextAlign.End
        )
    }
}
