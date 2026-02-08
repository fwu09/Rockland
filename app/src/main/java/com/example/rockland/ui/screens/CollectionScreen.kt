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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.data.model.CollectionItem
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.viewmodel.CollectionViewModel

@Composable
fun CollectionScreen(
    viewModel: CollectionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTabIndex = remember { mutableIntStateOf(0) } // 0 = Collections, 1 = Dictionary
    val selectedItem = remember { mutableStateOf<CollectionItem?>(null) }

    if (selectedTabIndex.intValue == 0 && selectedItem.value != null) {
        CollectionDetailScreen(
            item = selectedItem.value!!,
            onBack = { selectedItem.value = null },
            onSaveNotes = { customId, location, notes ->
                val current = selectedItem.value
                if (current != null) {
                    viewModel.updateCollectionItem(
                        itemId = current.id,
                        customId = customId,
                        locationLabel = location,
                        notes = notes,
                        imageUrls = current.imageUrls
                    )
                    selectedItem.value = current.copy(
                        customId = customId,
                        locationLabel = location,
                        notes = notes
                    )
                }
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "My Rocks",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(12.dp))

            TabRow(
                selectedTabIndex = selectedTabIndex.intValue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex.intValue == 0,
                    onClick = { selectedTabIndex.intValue = 0 },
                    text = { Text("Collections") }
                )
                Tab(
                    selected = selectedTabIndex.intValue == 1,
                    onClick = { selectedTabIndex.intValue = 1 },
                    text = { Text("Dictionary") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTabIndex.intValue) {
                0 -> CollectionsTabContent(
                    items = uiState.items,
                    onDelete = { item -> viewModel.removeFromCollection(item.id) },
                    onSelect = { item -> selectedItem.value = item }
                )

                1 -> DictionaryTabPlaceholder()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CollectionScreenPreview() {
    val sampleItems = listOf(
        CollectionItem(rockName = "Granite", notes = "Coarse-grained, light-colored rock."),
        CollectionItem(rockName = "Basalt", notes = "Dark volcanic rock found near the coast.")
    )

    CollectionsTabContent(
        items = sampleItems,
        onDelete = {},
        onSelect = {}
    )
}

@Composable
private fun CollectionsTabContent(
    items: List<CollectionItem>,
    onDelete: (CollectionItem) -> Unit,
    onSelect: (CollectionItem) -> Unit
) {
    // TODO: Backend - Integrate with rock identification API

    if (items.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your virtual collection is empty. Go out and identify your first rock!",
                color = Color(0xFF555555),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
private fun CollectionListItem(
    item: CollectionItem,
    onDelete: () -> Unit,
    onSelect: () -> Unit
) {
    val menuExpanded = remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
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
                    Text(
                        text = "Photo",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark
                    )
                }

                Column {
                    Text(
                        text = item.rockName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (item.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF777777),
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
private fun DictionaryTabPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Rock Dictionary (coming soon).",
            color = Color(0xFF777777)
        )
    }
}
