package com.example.rockland.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.ui.theme.BackgroundDark
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.RocklandTheme
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.ui.theme.TextLight

// Profile screen component
@Composable
fun ProfileScreen(
    userData: UserData? = null,
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    // Calculate level based on experience points
    val level = calculateLevel(userData?.experience ?: 0)
    val currentLevelExp = calculateExpForLevel(level)
    val nextLevelExp = calculateExpForLevel(level + 1)
    val expProgress = calculateExpProgress(userData?.experience ?: 0, currentLevelExp, nextLevelExp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar with welcome message and settings button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text(
                    text = "Welcome back to your profile",
                    fontSize = 16.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = Rock1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Profile header with level info
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = BackgroundDark
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Rock1)
                        .align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Profile photo placeholder
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Rock3)
                        .shadow(4.dp, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${userData?.firstName?.firstOrNull() ?: ""}${userData?.lastName?.firstOrNull() ?: ""}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // User name
                Text(
                    text = "${userData?.firstName ?: ""} ${userData?.lastName ?: ""}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rank title
                Text(
                    text = getRankTitle(level),
                    fontSize = 16.sp,
                    color = Color(0xFFFFD700),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Join date
                Text(
                    text = "${stringResource(R.string.joined)} ${userData?.joinDate ?: ""}",
                    fontSize = 14.sp,
                    color = TextLight.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Experience progress
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Level Progress",
                        fontSize = 14.sp,
                        color = TextLight,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Animated progress bar
                    val animatedProgress by animateFloatAsState(
                        targetValue = expProgress,
                        animationSpec = tween(1000, easing = LinearEasing),
                        label = "exp_progress"
                    )

                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Rock1,
                        trackColor = Color.Gray.copy(alpha = 0.3f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${userData?.experience ?: 0}/${nextLevelExp} XP",
                        fontSize = 12.sp,
                        color = TextLight.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Game stats section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Game Stats",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats grid - top row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GameStatItem(
                        icon = Icons.Default.Landscape,
                        value = userData?.checkins ?: 0,
                        label = stringResource(R.string.checkins),
                        color = Rock1
                    )

                    GameStatItem(
                        icon = Icons.Default.Explore,
                        value = userData?.observations ?: 0,
                        label = stringResource(R.string.observations),
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats grid - bottom row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    GameStatItem(
                        icon = Icons.Default.Map,
                        value = userData?.states ?: 0,
                        label = stringResource(R.string.states),
                        color = Color(0xFF2196F3)
                    )

                    GameStatItem(
                        icon = Icons.Default.EmojiEvents,
                        value = userData?.countries ?: 0,
                        label = stringResource(R.string.countries),
                        color = Color(0xFFFFC107)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Achievements section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .animateContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Recent Achievements",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                // List of achievements
                AchievementItem(
                    title = "First Discovery",
                    description = "Found your first rock formation",
                    progress = 1.0f,
                    isCompleted = true
                )

                AchievementItem(
                    title = "Explorer",
                    description = "Visit 5 different locations",
                    progress = (userData?.checkins ?: 0) / 5f,
                    isCompleted = (userData?.checkins ?: 0) >= 5
                )

                AchievementItem(
                    title = "Geologist",
                    description = "Identify 10 different rock types",
                    progress = (userData?.observations ?: 0) / 10f,
                    isCompleted = (userData?.observations ?: 0) >= 10
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Next challenge card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Rock1.copy(alpha = 0.1f)
            ),
            border = BorderStroke(1.dp, Rock1),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Rock1,
                    modifier = Modifier.size(36.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Next Challenge",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Text(
                        text = "Create a check-in to level up!",
                        fontSize = 14.sp,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(16.dp))

        // Logout button
        Button(
            onClick = onLogoutClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336)
            ),
            shape = RoundedCornerShape(25.dp)
        ) {
            Text(
                text = stringResource(R.string.logout),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(56.dp))
    }
}

// Game stat item component with icon
@Composable
private fun GameStatItem(
    icon: ImageVector,
    value: Int,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = value.toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Text(
            text = label,
            fontSize = 12.sp,
            color = TextDark.copy(alpha = 0.7f)
        )
    }
}

// Achievement item component
@Composable
private fun AchievementItem(
    title: String,
    description: String,
    progress: Float,
    isCompleted: Boolean
) {
    val progressCapped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressCapped,
        animationSpec = tween(1000, easing = LinearEasing),
        label = "achievement_progress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) Color(0xFFECF6EC) else Color(0xFFF8F8F8)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) Color(0xFF388E3C) else TextDark
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = TextDark.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (isCompleted) Color(0xFF388E3C) else Rock1,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${(progressCapped * 100).toInt()}% Complete",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

// Helper functions for level calculations
private fun calculateLevel(exp: Int): Int {
    // Simple level formula: level = sqrt(exp / 100)
    return kotlin.math.sqrt(exp / 100.0).toInt() + 1
}

private fun calculateExpForLevel(level: Int): Int {
    // Experience required for a given level
    return 100 * (level - 1) * (level - 1)
}

private fun calculateExpProgress(currentExp: Int, currentLevelExp: Int, nextLevelExp: Int): Float {
    if (nextLevelExp <= currentLevelExp) return 1.0f
    val levelExpRange = nextLevelExp - currentLevelExp
    val userExpInLevel = currentExp - currentLevelExp
    return (userExpInLevel.toFloat() / levelExpRange).coerceIn(0f, 1f)
}

private fun getRankTitle(level: Int): String {
    return when {
        level < 3 -> "Novice Explorer"
        level < 5 -> "Rock Enthusiast"
        level < 10 -> "Geology Adventurer"
        level < 15 -> "Master Geologist"
        level < 20 -> "Rock Connoisseur"
        else -> "Legendary Explorer"
    }
}

// Extended UserData class for preview
private val previewUserData = UserData(
    userId = "123",
    firstName = "Test",
    lastName = "123",
    email = "xxxx@gmail.com",
    joinDate = "October 29, 2025",
    checkins = 5,
    observations = 10,
    states = 2,
    countries = 1,
    experience = 350
)

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    RocklandTheme {
        ProfileScreen(
            userData = previewUserData
        )
    }
}
