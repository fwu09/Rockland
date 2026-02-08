// Screen presenting the user's profile stats and navigation actions.
package com.example.rockland.ui.screens
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.R
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark

// Profile screen component
@Composable
fun ProfileScreen(
    userData: UserData? = null,
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val roleLabel = formatRoleLabel(userData?.role ?: "nature_enthusiast")
    val points = userData?.points ?: 0
    val missionsCompleted = userData?.missionsCompleted ?: 0
    val achievementsCompleted = userData?.achievementsCompleted ?: 0
    var showExpertDialog by remember { mutableStateOf(false) }

    var fullName by remember {
        mutableStateOf("${userData?.firstName ?: ""} ${userData?.lastName ?: ""}".trim())
    }
    var expertise by remember { mutableStateOf("") }
    var yearsOfExp by remember { mutableStateOf("") }
    var portfolioLink by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar with back button, title and settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
                Text(
                    text = "Profile",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.settings),
                    tint = Rock1
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Profile header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Rock1.copy(alpha = 0.18f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                    text = fullName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Rock1.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                }

                // Join date
                Text(
                    text = "${stringResource(R.string.joined)} ${userData?.joinDate ?: ""}",
                    fontSize = 14.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Gamification summary
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Rock1.copy(alpha = 0.12f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Progress Overview",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                SummaryStatItem(
                    modifier = Modifier.fillMaxWidth(),
                    value = points.toString(),
                    label = "Points"
                )

                Spacer(modifier = Modifier.height(10.dp))

                SummaryStatItem(
                    modifier = Modifier.fillMaxWidth(),
                    value = achievementsCompleted.toString(),
                    label = "Achievements"
                )

                Spacer(modifier = Modifier.height(10.dp))

                SummaryStatItem(
                    modifier = Modifier.fillMaxWidth(),
                    value = missionsCompleted.toString(),
                    label = "Missions"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Verified expert application entry
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Are you a Verified Expert?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Complete the application form to prove you are an expert and gain access to more permissions!",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { showExpertDialog = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8D2B5)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Apply",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E)
                    )
                }
            }
        }

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

    if (showExpertDialog) {
        VerifiedExpertDialog(
            fullName = fullName,
            expertise = expertise,
            yearsOfExp = yearsOfExp,
            portfolioLink = portfolioLink,
            notes = notes,
            onFullNameChange = { fullName = it },
            onExpertiseChange = { expertise = it },
            onYearsChange = { yearsOfExp = it },
            onPortfolioChange = { portfolioLink = it },
            onNotesChange = { notes = it },
            onDismiss = { showExpertDialog = false },
            onSubmit = { showExpertDialog = false }
        )
    }
}

@Composable
private fun SummaryStatItem(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
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

private fun formatRoleLabel(role: String): String {
    return when (role.lowercase()) {
        "verified_expert" -> "Verified Expert"
        "user_admin" -> "User Admin"
        else -> "Nature Enthusiast"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifiedExpertDialog(
    fullName: String,
    expertise: String,
    yearsOfExp: String,
    portfolioLink: String,
    notes: String,
    onFullNameChange: (String) -> Unit,
    onExpertiseChange: (String) -> Unit,
    onYearsChange: (String) -> Unit,
    onPortfolioChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: () -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
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
                    text = "Verified Expert Application",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    label = { Text("Full name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = expertise,
                    onValueChange = onExpertiseChange,
                    label = { Text("Expertise area") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = yearsOfExp,
                    onValueChange = onYearsChange,
                    label = { Text("Years of experience") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = portfolioLink,
                    onValueChange = onPortfolioChange,
                    label = { Text("Portfolio link (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    label = { Text("Notes for admin (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onSubmit,
                        colors = ButtonDefaults.buttonColors(containerColor = Rock1),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Submit")
                    }
                }
            }
        }
    }
}

