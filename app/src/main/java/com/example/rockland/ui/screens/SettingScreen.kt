// NOTE: If you still want email editable, set `enabled = true` on the email field.

package com.example.rockland.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rockland.R
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import com.example.rockland.util.ImageValidationUtil
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.automirrored.filled.Logout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userData: UserData? = null,
    isUploadingPicture: Boolean = false,
    onBackClick: () -> Unit = {},
    onSaveClick: (String, String, String) -> Unit = { _, _, _ -> },
    onUploadProfilePicture: (Uri) -> Unit = {},
    onValidationError: (String) -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    var firstName by remember(userData?.firstName) { mutableStateOf(userData?.firstName ?: "") }
    var lastName by remember(userData?.lastName) { mutableStateOf(userData?.lastName ?: "") }
    var email by remember(userData?.email) { mutableStateOf(userData?.email ?: "") }

    val profilePictureUrl = userData?.profilePictureUrl.orEmpty()
    val displayName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ").ifBlank {
        userData?.email?.ifBlank { "Your profile" } ?: "Your profile"
    }

    val context = LocalContext.current

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        when (val res = ImageValidationUtil.validateTypeAndSize(context, uri)) {
            is ImageValidationUtil.Result.Ok -> onUploadProfilePicture(uri)
            is ImageValidationUtil.Result.Error -> onValidationError(res.message)
        }
    }

    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val bg = Brush.verticalGradient(
        colors = listOf(
            Rock1.copy(alpha = 0.10f),
            Color(0xFFF6F6F6),
            Color(0xFFF6F6F6)
        )
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.logout),
                            color = Color(0xFFE57373),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = TextDark
                ),
                scrollBehavior = topAppBarScrollBehavior
            )
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Button(
                    onClick = { onSaveClick(firstName.trim(), lastName.trim(), email.trim()) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rock1)
                ) {
                    Text(
                        text = stringResource(R.string.save_changes),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(paddingValues)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 92.dp), // space for bottom save card
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .clip(CircleShape)
                                    .background(Rock3)
                                    .clickable(enabled = !isUploadingPicture) {
                                        galleryLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                when {
                                    isUploadingPicture -> {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(34.dp),
                                            color = Rock1
                                        )
                                    }
                                    profilePictureUrl.isNotBlank() -> {
                                        AsyncImage(
                                            model = profilePictureUrl,
                                            contentDescription = "Profile photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Text(
                                            text = initialsFrom(firstName, lastName, email),
                                            fontSize = 26.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                }

                                // small edit badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Change photo",
                                        tint = Rock1,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = email.ifBlank { "—" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextDark.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isUploadingPicture) "Uploading photo…" else "Tap photo to change",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextDark.copy(alpha = 0.70f)
                                )
                            }
                        }
                    }
                }

                // Account details card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )

                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text(stringResource(R.string.first_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = settingsFieldColors()
                        )

                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text(stringResource(R.string.last_name)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = settingsFieldColors()
                        )

                        // Common UX: email is usually read-only (edit via auth flow).
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text(stringResource(R.string.email)) },
                            singleLine = true,
                            enabled = false, // ✅ set true if you want editable
                            modifier = Modifier.fillMaxWidth(),
                            colors = settingsFieldColors(disabled = true)
                        )

                        Text(
                            text = "Email changes are usually handled in the login/security flow.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.60f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isUploadingPicture) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.08f))
                )
            }
        }
    }
}

@Composable
private fun settingsFieldColors(
    disabled: Boolean = false
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Rock1,
    focusedLabelColor = Rock1,
    cursorColor = Rock1,
    unfocusedBorderColor = TextDark.copy(alpha = 0.25f),
    unfocusedLabelColor = TextDark.copy(alpha = 0.60f),
    disabledBorderColor = if (disabled) TextDark.copy(alpha = 0.12f) else TextDark.copy(alpha = 0.25f),
    disabledLabelColor = if (disabled) TextDark.copy(alpha = 0.35f) else TextDark.copy(alpha = 0.60f),
    disabledTextColor = TextDark.copy(alpha = 0.55f)
)

private fun initialsFrom(firstName: String, lastName: String, email: String): String {
    val a = firstName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val b = lastName.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    val combined = (a + b).ifBlank {
        email.trim().firstOrNull()?.uppercaseChar()?.toString().orEmpty()
    }
    return combined.ifBlank { "?" }
}
