// Simple screen that highlights the user's achievements in the UI layer.
package com.example.rockland.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.data.model.AchievementDefinition
import com.example.rockland.data.model.LeaderboardEntry
import com.example.rockland.data.model.MissionWithProgress
import com.example.rockland.presentation.viewmodel.AwardsViewModel
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.TextDark

@Composable
fun AchievementScreen(
    userId: String? = null,
    viewModel: AwardsViewModel = viewModel(factory = AwardsViewModel.Factory(userId))
) {
    val tabs = listOf("Leaderboard", "In-Progress", "All")
    val selectedTab = remember { mutableIntStateOf(0) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(selectedTab.intValue) {
        viewModel.loadAwards()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Awards",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(modifier = Modifier.height(12.dp))

        TabRow(
            selectedTabIndex = selectedTab.intValue,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab.intValue == index,
                    onClick = { selectedTab.intValue = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when (selectedTab.intValue) {
            0 -> LeaderboardTabContent(entries = uiState.leaderboard)
            1 -> InProgressTabContent(missions = uiState.missions.filter { !it.completed })
            2 -> AllTabContent(
                achievementsSummary = uiState.achievementsSummary,
                missionsSummary = uiState.missionsSummary,
                completedAchievements = uiState.completedAchievements,
                completedMissions = uiState.completedMissions
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
            items(missions) { mission ->
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

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = mission.mission.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
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
        }
    }
}

@Composable
private fun AllTabContent(
    achievementsSummary: String,
    missionsSummary: String,
    completedAchievements: List<AchievementDefinition>,
    completedMissions: List<MissionWithProgress>
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
            items(completedAchievements) { achievement ->
                SimpleListRow(text = achievement.title)
            }
        }

        item {
            SectionHeader(title = "Completed Missions")
        }

        if (completedMissions.isEmpty()) {
            item { EmptyState(text = "No missions completed yet.") }
        } else {
            items(completedMissions) { mission ->
                SimpleListRow(text = mission.mission.title)
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
private fun SimpleListRow(text: String) {
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
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Rock1)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
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
        }
    }
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
