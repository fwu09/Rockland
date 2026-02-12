// Simple screen that highlights the user's achievements in the UI layer.
package com.example.rockland.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.rotate
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.example.rockland.data.model.AchievementDefinition
import com.example.rockland.data.model.LeaderboardEntry
import com.example.rockland.data.model.MissionDefinition
import com.example.rockland.data.model.MissionWithProgress
import com.example.rockland.presentation.viewmodel.AwardsViewModel
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.TextDark
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults

// buttons ui improvement
@Composable
private fun NiceTopTabs(
    tabs: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val displayTabs = remember(tabs) {
        tabs.map {
            when (it) {
                "Leaderboard" -> "Leaderboard"
                "In-Progress" -> "In-Progress"
                "Achievements" -> "Achievement"
                else -> it // "All"
            }
        }
    }
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        displayTabs.forEachIndexed { index, title ->
            val selected = selectedIndex == index

            SegmentedButton(
                selected = selected,
                onClick = { onSelected(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = displayTabs.size),
                label = {
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                },
                modifier = Modifier
                    // KEY: don’t let each item expand equally
                    .padding(horizontal = 1.dp)
            )
        }
    }
}


@Composable
fun AchievementScreen(
    userId: String? = null,
    viewModel: AwardsViewModel = viewModel(factory = AwardsViewModel.Factory(userId)),
    selectedTabIndex: Int? = null,
    onTabSelected: ((Int) -> Unit)? = null,
    isAdmin: Boolean = false
) {
    val tabs = listOf("Leaderboard", "In-Progress", "All", "Achievements")
    val internalTab = rememberSaveable { mutableIntStateOf(0) }
    val currentTab = selectedTabIndex ?: internalTab.intValue
    val setTab: (Int) -> Unit = onTabSelected ?: { internalTab.intValue = it }
    val uiState by viewModel.uiState.collectAsState()
    val inProgressMissions = remember(uiState.missions) {
        uiState.missions.filter { !it.completed }
    }
    val allMissions = remember(uiState.missions) {
        uiState.missions.map { it.mission }
    }
    val showAdminForm = remember { mutableStateOf(false) }
    val adminEditingMission = remember { mutableStateOf<MissionDefinition?>(null) }
    val adminEditingAchievement = remember { mutableStateOf<AchievementDefinition?>(null) }
    val missionToDelete = remember { mutableStateOf<MissionDefinition?>(null) }
    val achievementToDelete = remember { mutableStateOf<AchievementDefinition?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadAwards()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Rock Collection Missions",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            NiceTopTabs(
                tabs = tabs,
                selectedIndex = currentTab,
                onSelected = { setTab(it) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (currentTab) {
                0 -> LeaderboardTabContent(entries = uiState.leaderboard)
                1 -> InProgressTabContent(missions = inProgressMissions)
                2 -> AllTabContent(
                    achievementsSummary = uiState.achievementsSummary,
                    missionsSummary = uiState.missionsSummary,
                    completedAchievements = uiState.completedAchievements,
                    completedMissions = uiState.completedMissions,
                    allMissions = allMissions,
                    allAchievements = uiState.allAchievements,
                    isAdmin = isAdmin,
                    onEditMission = { mission ->
                        adminEditingMission.value = mission
                        adminEditingAchievement.value = null
                        showAdminForm.value = true
                    },
                    onEditAchievement = { achievement ->
                        adminEditingAchievement.value = achievement
                        adminEditingMission.value = null
                        showAdminForm.value = true
                    },
                    onDeleteMission = { mission -> missionToDelete.value = mission },
                    onDeleteAchievement = { achievement -> achievementToDelete.value = achievement }
                )
                3 -> AchievementsTabContent(allAchievements = uiState.allAchievements)
            }
        }

        if (isAdmin && currentTab == 2) {
            FloatingActionButton(
                onClick = {
                    adminEditingMission.value = null
                    adminEditingAchievement.value = null
                    showAdminForm.value = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 80.dp),
                containerColor = Color(0xFF2A2A2A)
            ) {
                Text(text = "+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showAdminForm.value) {
            AdminMissionAchievementDialog(
                initialMission = adminEditingMission.value,
                initialAchievement = adminEditingAchievement.value,
                onDismiss = { showAdminForm.value = false },
                onSaveMission = { def ->
                    viewModel.upsertMission(def)
                    showAdminForm.value = false
                },
                onSaveAchievement = { def ->
                    viewModel.upsertAchievement(def)
                    showAdminForm.value = false
                }
            )
        }

        missionToDelete.value?.let { mission ->
            SimpleConfirmDialog(
                title = "Delete Mission",
                message = "Are you sure you want to delete this mission?",
                onConfirm = {
                    viewModel.deleteMission(mission.id)
                    missionToDelete.value = null
                },
                onDismiss = { missionToDelete.value = null }
            )
        }

        achievementToDelete.value?.let { achievement ->
            SimpleConfirmDialog(
                title = "Delete Achievement",
                message = "Are you sure you want to delete this achievement?",
                onConfirm = {
                    viewModel.deleteAchievement(achievement.id)
                    achievementToDelete.value = null
                },
                onDismiss = { achievementToDelete.value = null }
            )
        }
    }
}

@Composable
private fun LeaderboardTabContent(entries: List<LeaderboardEntry>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.22f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Monthly Points Leaderboard",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total Points Ranking",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Top point earners this month",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (entries.isEmpty()) {
            item {
                EmptyState(text = "No leaderboard data yet.")
            }
        } else {
            items(entries) { entry ->
                LeaderboardRow(entry = entry)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${entry.rank}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.width(24.dp)
            )
            Text(
                text = entry.name,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${entry.points}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}

@Composable
private fun InProgressTabContent(missions: List<MissionWithProgress>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (missions.isEmpty()) {
            item {
                EmptyState(text = "No active missions.")
            }
        } else {
            items(missions, key = { it.mission.id }) { mission ->
                MissionCard(mission = mission)
            }
        }
    }
}

@Composable
private fun MissionCard(mission: MissionWithProgress) {
    val progressCapped = mission.progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressCapped,
        animationSpec = tween(800, easing = LinearEasing),
        label = "mission_progress"
    )

    // Expand state
    var expanded by rememberSaveable(mission.mission.id) { mutableStateOf(false) }

    // Rotate arrow
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrow_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Title + dropdown icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = mission.mission.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextDark.copy(alpha = 0.8f),
                    modifier = Modifier.rotate(arrowRotation)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Mission Progress: ${mission.current}/${mission.mission.target}",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.7f)
            )
            Text(
                text = "Time Expiry: ${mission.expiryLabel}",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Rock1,
                trackColor = Rock1.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "+${mission.mission.rewardPoints} points",
                fontSize = 12.sp,
                color = Rock1
            )

            // Expandable criteria section
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(120)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Criteria",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Description
                    if (mission.mission.description.isNotBlank()) {
                        Text(
                            text = "• ${mission.mission.description}",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Trigger condition
                    if (mission.mission.trigger.isNotBlank()) {
                        Text(
                            text = "• Trigger: ${mission.mission.trigger}",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Target requirement
                    Text(
                        text = "• Requirement: Do ${mission.mission.target} time(s)",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.85f)
                    )

                    // Optional: Rock ID requirement
                    mission.mission.rockId?.let { rockId ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Rock ID: $rockId",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                    }

                    // Optional: Type
                    if (mission.mission.type.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Type: ${mission.mission.type}",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun AllTabContent(
    achievementsSummary: String,
    missionsSummary: String,
    completedAchievements: List<AchievementDefinition>,
    completedMissions: List<MissionWithProgress>,
    allMissions: List<MissionDefinition>,
    allAchievements: List<AchievementDefinition>,
    isAdmin: Boolean,
    onEditMission: (MissionDefinition) -> Unit,
    onEditAchievement: (AchievementDefinition) -> Unit,
    onDeleteMission: (MissionDefinition) -> Unit,
    onDeleteAchievement: (AchievementDefinition) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            val horizontalPadding = 16.dp * 2
            val gap = 12.dp
            val cardWidth = (screenWidth - horizontalPadding - gap) / 2
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                SummaryCard(
                    modifier = Modifier
                        .width(cardWidth)
                        .padding(end = 6.dp),
                    title = "Achievements",
                    value = achievementsSummary
                )
                SummaryCard(
                    modifier = Modifier
                        .width(cardWidth)
                        .padding(start = 6.dp),
                    title = "Missions",
                    value = missionsSummary
                )
            }
        }

        item {
            SectionHeader(title = "Completed Achievements")
        }

        if (completedAchievements.isEmpty()) {
            item { EmptyState(text = "No achievements completed yet.") }
        } else {
            items(completedAchievements, key = { it.id }) { achievement ->
                ExpandableAchievementRow(achievement = achievement)
            }
        }

        item {
            SectionHeader(title = "Completed Missions")
        }

        if (completedMissions.isEmpty()) {
            item { EmptyState(text = "No missions completed yet.") }
        } else {
            items(completedMissions, key = { it.mission.id }) { mission ->
                MissionCard(mission = mission)
            }
        }

        if (isAdmin) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Admin Missions")
            }
            if (allMissions.isEmpty()) {
                item {
                    EmptyState(text = "No missions defined yet.")
                }
            } else {
                items(allMissions) { mission ->
                    AdminMissionRow(
                        mission = mission,
                        onEdit = { onEditMission(mission) },
                        onDelete = { onDeleteMission(mission) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "Admin Achievements")
            }
            if (allAchievements.isEmpty()) {
                item {
                    EmptyState(text = "No achievements defined yet.")
                }
            } else {
                items(allAchievements) { achievement ->
                    AdminAchievementRow(
                        achievement = achievement,
                        onEdit = { onEditAchievement(achievement) },
                        onDelete = { onDeleteAchievement(achievement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMissionRow(
    mission: MissionDefinition,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = mission.title.ifBlank { "(Untitled Mission)" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = "Reward: ${mission.rewardPoints} pts • Target: ${mission.target}",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
                if (mission.type.isNotBlank()) {
                    Text(
                        text = "Type: ${mission.type}",
                        fontSize = 11.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mission actions",
                        tint = TextDark.copy(alpha = 0.9f)
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
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminAchievementRow(
    achievement: AchievementDefinition,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = achievement.title.ifBlank { "(Untitled Achievement)" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = "Reward: ${achievement.rewardPoints} pts • Target: ${achievement.target}",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Achievement actions",
                        tint = TextDark.copy(alpha = 0.9f)
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
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String
) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextDark
    )
}

@Composable
private fun EmptyState(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDark.copy(alpha = 0.7f),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun SimpleConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.8f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onConfirm) {
                        Text("Yes")
                    }
                }
            }
        }
    }
}

private enum class AdminEntityKind {
    MISSION,
    ACHIEVEMENT
}

// Simple in-memory cache to avoid resolving the same badge URL repeatedly.
private val achievementBadgeUrlCache = mutableMapOf<String, String>()

// achievement improved UI
@Composable
private fun ExpandableAchievementRow(achievement: AchievementDefinition) {
    var expanded by rememberSaveable(achievement.id) { mutableStateOf(false) }
    val badgeUrlState = remember(achievement.imageFile) { mutableStateOf<String?>(null) }

    LaunchedEffect(achievement.id, achievement.imageFile) {
        val raw = achievement.imageFile.trim()
        if (raw.isBlank()) {
            badgeUrlState.value = null
            return@LaunchedEffect
        }

        // First check in-memory cache so we do not hit storage repeatedly.
        val cached = achievementBadgeUrlCache[raw]
        if (cached != null) {
            badgeUrlState.value = cached
            return@LaunchedEffect
        }

        if (raw.startsWith("gs://")) {
            runCatching {
                val ref = FirebaseStorage.getInstance().getReferenceFromUrl(raw)
                val url = ref.downloadUrl.await()
                val resolved = url.toString()
                achievementBadgeUrlCache[raw] = resolved
                badgeUrlState.value = resolved
            }.onFailure {
                badgeUrlState.value = null
            }
        } else {
            achievementBadgeUrlCache[raw] = raw
            badgeUrlState.value = raw
        }
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrow_rotation_achievement"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = achievement.title.ifBlank { "(Untitled Achievement)" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )

                val badgeUrl = badgeUrlState.value
                if (badgeUrl != null) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(badgeUrl)
                            .crossfade(false) // avoid re-fade on scroll when loaded from cache
                            .build(),
                        contentDescription = "${achievement.title} badge",
                        contentScale = ContentScale.Fit,
                        filterQuality = FilterQuality.High,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = TextDark.copy(alpha = 0.8f),
                    modifier = Modifier.rotate(arrowRotation)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Rock1.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Completed",
                    fontSize = 11.sp,
                    color = Rock1
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(120)) + expandVertically(tween(180)),
                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "Criteria",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (achievement.description.isNotBlank()) {
                        Text(
                            text = "• ${achievement.description}",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    if (achievement.trigger.isNotBlank()) {
                        Text(
                            text = "• Trigger: ${achievement.trigger}",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = "• Requirement: Do ${achievement.target} time(s)",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.85f)
                    )

                    achievement.rockId?.let { rockId ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Rock ID: $rockId",
                            fontSize = 12.sp,
                            color = TextDark.copy(alpha = 0.85f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "+${achievement.rewardPoints} points",
                        fontSize = 12.sp,
                        color = Rock1
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMissionAchievementDialog(
    initialMission: MissionDefinition?,
    initialAchievement: AchievementDefinition?,
    onDismiss: () -> Unit,
    onSaveMission: (MissionDefinition) -> Unit,
    onSaveAchievement: (AchievementDefinition) -> Unit
) {
    val isEditingMission = initialMission != null
    val isEditingAchievement = initialAchievement != null
    val canSwitchType = !isEditingMission && !isEditingAchievement

    val initialKind = when {
        isEditingMission -> AdminEntityKind.MISSION
        isEditingAchievement -> AdminEntityKind.ACHIEVEMENT
        else -> AdminEntityKind.MISSION
    }

    val rewardOptions = listOf(10, 20, 50, 75, 100, 200, 500)
    val missionTypeOptions = listOf("Daily", "Weekly")

    val triggerOptions = listOf(
        "collect_rock" to "Identify or collect a rock",
        "post_comment" to "Post a comment",
        "read_rock_info" to "Read rock information",
        "upload_photo" to "Upload a photo",
        "visit_location" to "View a map location",
        "edit_personal_notes" to "Edit personal notes"
    )
    val triggerMenuExpanded = remember { mutableStateOf(false) }

    val startOptions = listOf("none", "today", "custom_existing")
    val startOptionLabels = mapOf(
        "none" to "No start",
        "today" to "Start today",
        "custom_existing" to "Keep existing start date"
    )

    val endOptions = listOf("none", "7d", "30d", "custom_existing")
    val endOptionLabels = mapOf(
        "none" to "No end",
        "7d" to "End in 7 days",
        "30d" to "End in 30 days",
        "custom_existing" to "Keep existing end date"
    )

    val titleState =
        remember { mutableStateOf(initialMission?.title ?: initialAchievement?.title ?: "") }
    val descriptionState = remember {
        mutableStateOf(
            initialMission?.description ?: initialAchievement?.description ?: ""
        )
    }

    val initialReward = initialMission?.rewardPoints
        ?: initialAchievement?.rewardPoints
        ?: rewardOptions.first()
    val rewardState = remember { mutableIntStateOf(initialReward) }
    val rewardMenuExpanded = remember { mutableStateOf(false) }

    val initialTarget = initialMission?.target ?: initialAchievement?.target ?: 0
    val targetTextState =
        remember { mutableStateOf(if (initialTarget > 0) initialTarget.toString() else "") }

    val initialTrigger = initialMission?.trigger ?: initialAchievement?.trigger ?: ""
    val triggerState = remember { mutableStateOf(initialTrigger) }
    val triggerDisplayLabel = remember(triggerState.value) {
        triggerOptions.find { it.first == triggerState.value }?.second ?: triggerState.value.ifBlank { "Select trigger condition" }
    }

    val rockIdInitial = initialMission?.rockId ?: initialAchievement?.rockId
    val rockIdTextState = remember { mutableStateOf(rockIdInitial?.toString().orEmpty()) }

    val missionTypeInitial =
        initialMission?.type?.takeIf { it.isNotBlank() } ?: missionTypeOptions.first()
    val missionTypeState = remember { mutableStateOf(missionTypeInitial) }
    val missionTypeMenuExpanded = remember { mutableStateOf(false) }

    val initialStartOption = when {
        initialMission == null || initialMission.startAt == 0L -> "none"
        else -> "custom_existing"
    }
    val startOptionState = remember { mutableStateOf(initialStartOption) }
    val startMenuExpanded = remember { mutableStateOf(false) }

    val initialEndOption = when {
        initialMission == null || initialMission.endAt == 0L -> "none"
        else -> "custom_existing"
    }
    val endOptionState = remember { mutableStateOf(initialEndOption) }
    val endMenuExpanded = remember { mutableStateOf(false) }

    val kindState = remember { mutableStateOf(initialKind) }
    val saveErrorState = remember { mutableStateOf<String?>(null) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(horizontal = 12.dp, vertical = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(18.dp)
        ) {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEditingMission || isEditingAchievement) "Edit Mission / Achievement" else "Add Mission / Achievement",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val missionSelected = kindState.value == AdminEntityKind.MISSION
                    val achievementSelected = kindState.value == AdminEntityKind.ACHIEVEMENT
                    TextButton(
                        onClick = {
                            if (canSwitchType) {
                                kindState.value = AdminEntityKind.MISSION
                            }
                        },
                        enabled = canSwitchType || missionSelected
                    ) {
                        Text(
                            text = "Mission",
                            fontWeight = if (missionSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    TextButton(
                        onClick = {
                            if (canSwitchType) {
                                kindState.value = AdminEntityKind.ACHIEVEMENT
                            }
                        },
                        enabled = canSwitchType || achievementSelected
                    ) {
                        Text(
                            text = "Achievement",
                            fontWeight = if (achievementSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                OutlinedTextField(
                    value = titleState.value,
                    onValueChange = { titleState.value = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = descriptionState.value,
                    onValueChange = { descriptionState.value = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Reward points",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            TextButton(onClick = { rewardMenuExpanded.value = true }) {
                                Text("${rewardState.intValue} pts")
                            }
                            DropdownMenu(
                                expanded = rewardMenuExpanded.value,
                                onDismissRequest = { rewardMenuExpanded.value = false }
                            ) {
                                rewardOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text("$option pts") },
                                        onClick = {
                                            rewardState.intValue = option
                                            rewardMenuExpanded.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = targetTextState.value,
                            onValueChange = { new ->
                                targetTextState.value = new.filter { it.isDigit() }
                            },
                            label = { Text("Target") },
                            singleLine = true
                        )
                    }
                }

                Column {
                    Text(
                        text = "Trigger condition",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedTextField(
                            value = triggerDisplayLabel,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Select trigger condition") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { triggerMenuExpanded.value = true },
                            trailingIcon = {
                                IconButton(onClick = { triggerMenuExpanded.value = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Open trigger menu"
                                    )
                                }
                            }
                        )
                        DropdownMenu(
                            expanded = triggerMenuExpanded.value,
                            onDismissRequest = { triggerMenuExpanded.value = false },
                            modifier = Modifier.heightIn(max = 320.dp)
                        ) {
                            triggerOptions.forEach { (key, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        triggerState.value = key
                                        triggerMenuExpanded.value = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = rockIdTextState.value,
                    onValueChange = { new ->
                        rockIdTextState.value = new.filter { it.isDigit() }
                    },
                    label = { Text("Rock ID (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                if (kindState.value == AdminEntityKind.MISSION) {
                    Text(
                        text = "Mission settings",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )

                    Column {
                        Text(
                            text = "Type",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            TextButton(onClick = { missionTypeMenuExpanded.value = true }) {
                                Text(missionTypeState.value)
                            }
                            DropdownMenu(
                                expanded = missionTypeMenuExpanded.value,
                                onDismissRequest = { missionTypeMenuExpanded.value = false }
                            ) {
                                missionTypeOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            missionTypeState.value = option
                                            missionTypeMenuExpanded.value = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start at",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                val label = startOptionLabels[startOptionState.value] ?: "No start"
                                TextButton(onClick = { startMenuExpanded.value = true }) {
                                    Text(label)
                                }
                                DropdownMenu(
                                    expanded = startMenuExpanded.value,
                                    onDismissRequest = { startMenuExpanded.value = false }
                                ) {
                                    startOptions.forEach { option ->
                                        val optionLabel = startOptionLabels[option] ?: option
                                        DropdownMenuItem(
                                            text = { Text(optionLabel) },
                                            onClick = {
                                                startOptionState.value = option
                                                startMenuExpanded.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "End at",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDark.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box {
                                val label = endOptionLabels[endOptionState.value] ?: "No end"
                                TextButton(onClick = { endMenuExpanded.value = true }) {
                                    Text(label)
                                }
                                DropdownMenu(
                                    expanded = endMenuExpanded.value,
                                    onDismissRequest = { endMenuExpanded.value = false }
                                ) {
                                    endOptions.forEach { option ->
                                        val optionLabel = endOptionLabels[option] ?: option
                                        DropdownMenuItem(
                                            text = { Text(optionLabel) },
                                            onClick = {
                                                endOptionState.value = option
                                                endMenuExpanded.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                saveErrorState.value?.let { errorMsg ->
                    Text(
                        text = errorMsg,
                        color = Color(0xFFB00020),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        saveErrorState.value = null
                        val title = titleState.value.trim()
                        val description = descriptionState.value.trim()
                        val trigger = triggerState.value.trim()
                        val target = targetTextState.value.toIntOrNull() ?: 0
                        val reward = rewardState.intValue
                        val rockId = rockIdTextState.value.toIntOrNull()

                        when {
                            title.isBlank() -> {
                                saveErrorState.value = "Title is required."
                                return@TextButton
                            }
                            description.isBlank() -> {
                                saveErrorState.value = "Description is required."
                                return@TextButton
                            }
                            target <= 0 -> {
                                saveErrorState.value = "Target must be greater than 0."
                                return@TextButton
                            }
                            trigger.isBlank() -> {
                                saveErrorState.value = "Trigger condition is required."
                                return@TextButton
                            }
                        }

                        val now = System.currentTimeMillis()
                        val millisPerDay = 24L * 60L * 60L * 1000L

                        if (kindState.value == AdminEntityKind.MISSION) {
                            val startAt = when (startOptionState.value) {
                                "none" -> 0L
                                "today" -> now
                                "signup" -> -1L
                                "custom_existing" -> initialMission?.startAt ?: 0L
                                else -> 0L
                            }
                            val endAt = when (endOptionState.value) {
                                "none" -> 0L
                                "7d" -> now + 7L * millisPerDay
                                "30d" -> now + 30L * millisPerDay
                                "custom_existing" -> initialMission?.endAt ?: 0L
                                else -> 0L
                            }
                            val mission = MissionDefinition(
                                id = initialMission?.id.orEmpty(),
                                title = title,
                                description = description,
                                type = missionTypeState.value,
                                target = target,
                                rewardPoints = reward,
                                startAt = startAt,
                                endAt = endAt,
                                trigger = trigger,
                                rockId = rockId
                            )
                            onSaveMission(mission)
                        } else {
                            val achievement = AchievementDefinition(
                                id = initialAchievement?.id.orEmpty(),
                                title = title,
                                description = description,
                                target = target,
                                rewardPoints = reward,
                                trigger = trigger,
                                rockId = rockId,
                                imageFile = initialAchievement?.imageFile ?: ""
                            )
                            onSaveAchievement(achievement)
                        }
                    }) {
                        Text("Save")
                    }
                }
            }
        }
    }



}@Composable
private fun AchievementsTabContent(allAchievements: List<AchievementDefinition>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.16f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Achievements",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Collect badges and points by completing missions and identifying rocks.",
                        fontSize = 12.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
        }

        if (allAchievements.isEmpty()) {
            item { EmptyState(text = "No achievements defined yet.") }
        } else {
            items(allAchievements, key = { it.id }) { achievement ->
                AchievementDataCard(achievement = achievement)
            }
        }
    }
}

@Composable
private fun AchievementDataCard(achievement: AchievementDefinition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = achievement.title.ifBlank { "(Untitled Achievement)" },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            if (achievement.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = achievement.description,
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Trigger: ${achievement.trigger.ifBlank { "N/A" }}",
                fontSize = 11.sp,
                color = TextDark.copy(alpha = 0.7f)
            )

            achievement.rockId?.let { rockId ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Rock ID: $rockId",
                    fontSize = 11.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Rock1.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Achievement Badge + ${achievement.rewardPoints} points",
                    fontSize = 11.sp,
                    color = Rock1,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

