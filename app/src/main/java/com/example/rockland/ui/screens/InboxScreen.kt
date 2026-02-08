// Displays a basic inbox view and routes to the profile screen.
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.presentation.viewmodel.InboxNotification
import com.example.rockland.presentation.viewmodel.ReviewContentViewModel
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InboxScreen(
    userData: UserData?,
    onProfileClick: () -> Unit,
    reviewViewModel: ReviewContentViewModel = viewModel(factory = ReviewContentViewModel.Factory()),
    reviewTabIndex: Int? = null,
    onReviewTabChanged: ((Int) -> Unit)? = null
) {
    val showAgentDialog = remember { mutableStateOf(false) }
    val showFaqDialog = remember { mutableStateOf(false) }
    val requestFormVisible = remember { mutableStateOf(false) }
    val reviewScreenVisible = remember { mutableStateOf(false) }
    val internalReviewTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val currentReviewTabIndex = reviewTabIndex ?: internalReviewTabIndex.intValue
    val setReviewTabIndex: (Int) -> Unit =
        onReviewTabChanged ?: { internalReviewTabIndex.intValue = it }
    val reviewTab = if (currentReviewTabIndex == 1) ReviewTab.IMAGES else ReviewTab.COMMENTS
    val notificationsVisible = remember { mutableStateOf(false) }
    val pendingComments by reviewViewModel.pendingComments.collectAsState()
    val pendingPhotos by reviewViewModel.pendingPhotos.collectAsState()
    val notifications by reviewViewModel.notifications.collectAsState()

    LaunchedEffect(userData?.userId, userData?.role) {
        reviewViewModel.bindUser(userData?.userId, userData?.role)
    }

    val pendingCommentItems = remember(pendingComments) {
        pendingComments.mapIndexed { index, comment ->
            PendingComment(
                id = comment.commentId,
                number = "${index + 1}",
                locationId = comment.locationId,
                userId = comment.userId,
                author = comment.author,
                postedAt = formatDate(comment.timestamp),
                location = "Comment Location",
                preview = comment.text.take(80),
                fullText = comment.text
            )
        }
    }
    val photoGroup = remember(pendingPhotos) {
        pendingPhotos.groupBy { "${it.locationId}|${it.userId}" }
    }
    val pendingImageBatches = remember(pendingPhotos) {
        photoGroup.entries.mapIndexed { index, entry ->
            val photos = entry.value
            val first = photos.first()
            PendingImageBatch(
                id = "batch_${index + 1}",
                batchNumber = "${index + 1}",
                locationId = first.locationId,
                userId = first.userId,
                author = first.author,
                submittedAt = formatDate(first.timestamp),
                location = "Image Location",
                imageCount = photos.size,
                imageUrls = photos.map { it.imageUrl },
                photoIds = photos.map { it.locationPhotoId }
            )
        }
    }
    val commentsById = remember(pendingComments) {
        pendingComments.associateBy { it.commentId }
    }
    val photosByBatchId = remember(pendingImageBatches, photoGroup) {
        pendingImageBatches.associate { batch ->
            val photos = photoGroup["${batch.locationId}|${batch.userId}"].orEmpty()
            batch.id to photos
        }
    }

    if (reviewScreenVisible.value) {
        ReviewContentScreen(
            activeTab = reviewTab,
            pendingComments = pendingCommentItems,
            pendingImageBatches = pendingImageBatches,
            onApproveComment = { item ->
                val comment = commentsById[item.id] ?: return@ReviewContentScreen
                reviewViewModel.approveComment(comment, userData?.userId)
            },
            onRejectComment = { item ->
                val comment = commentsById[item.id] ?: return@ReviewContentScreen
                reviewViewModel.rejectComment(comment, userData?.userId)
            },
            onApproveImageBatch = { batch ->
                val photos = photosByBatchId[batch.id].orEmpty()
                reviewViewModel.approvePhotos(photos, userData?.userId)
            },
            onRejectImageBatch = { batch ->
                val photos = photosByBatchId[batch.id].orEmpty()
                reviewViewModel.rejectPhotos(photos, userData?.userId)
            },
            onTabSelected = { tab ->
                setReviewTabIndex(if (tab == ReviewTab.IMAGES) 1 else 0)
            },
            onBack = { reviewScreenVisible.value = false }
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onProfileClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                val displayName = when {
                    userData == null -> "Loading..."
                    userData.firstName.isBlank() && userData.lastName.isBlank() ->
                        userData.email.ifBlank { "Unknown User" }
                    else -> listOf(userData.firstName, userData.lastName)
                        .filter { it.isNotBlank() }
                        .joinToString(" ")
                }
                val initials = displayName
                    .trim()
                    .split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .map { it.first() }
                    .joinToString("")
                    .ifBlank { "UN" }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Rock3),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = TextDark,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = "View profile",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }

            val hasUnread = notifications.any { !it.isRead }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0xFFE8EAF1), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { notificationsVisible.value = true },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Notifications",
                        tint = TextDark
                    )
                }
                if (hasUnread) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(TextDark.copy(alpha = 0.8f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        HelpDeskCard(
            onAgentClick = {
                showAgentDialog.value = true
                requestFormVisible.value = true
            },
            onFaqClick = { showFaqDialog.value = true }
        )
        Spacer(modifier = Modifier.height(24.dp))

        val isVerifiedExpert = userData?.role?.trim()?.lowercase() == "verified_expert"
        if (isVerifiedExpert) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Content Review",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                ReviewEntryCard(
                    title = "Comment Review",
                    subtitle = "${pendingCommentItems.size} Comments Pending",
                    onClick = {
                        setReviewTabIndex(0)
                        reviewScreenVisible.value = true
                    }
                )
                ReviewEntryCard(
                    title = "Image Review",
                    subtitle = "${pendingImageBatches.sumOf { it.imageCount }} Images Pending",
                    onClick = {
                        setReviewTabIndex(1)
                        reviewScreenVisible.value = true
                    }
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }

    HelpDeskAgentDialog(
        visible = showAgentDialog.value,
        initiallyShowForm = requestFormVisible.value,
        onClose = {
            showAgentDialog.value = false
            requestFormVisible.value = false
        },
        onFaqRequest = { showFaqDialog.value = true }
    )

    if (notificationsVisible.value) {
        NotificationsDialog(
            notifications = notifications,
            onMarkRead = { id -> reviewViewModel.markNotificationRead(id) },
            onClearAll = { reviewViewModel.clearNotifications() },
            onClose = { notificationsVisible.value = false }
        )
    }

        HelpDeskFaqDialog(
            visible = showFaqDialog.value,
            onClose = { showFaqDialog.value = false }
        )
    }
}

@Composable
private fun ReviewEntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF2A2A2A))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "View",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun NotificationsDialog(
    notifications: List<InboxNotification>,
    onMarkRead: (String) -> Unit,
    onClearAll: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Notifications",
                            tint = TextDark
                        )
                    }
                }

                if (notifications.isEmpty()) {
                    Text(
                        text = "No notifications yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.6f)
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        notifications.forEach { notification ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onMarkRead(notification.id)
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (notification.isRead) {
                                        Color(0xFFF4F4F4)
                                    } else {
                                        Color(0xFFF9F9F9)
                                    }
                                ),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = notification.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDark
                                        )
                                        Text(
                                            text = notification.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextDark.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = notification.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextDark.copy(alpha = 0.6f)
                                        )
                                    }
                                    if (!notification.isRead) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(TextDark.copy(alpha = 0.6f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (notifications.isNotEmpty()) {
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear all")
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0L) return "—"
    return SimpleDateFormat("MM/dd/yyyy", Locale.US).format(Date(timestamp))
}

@Composable
private fun HelpDeskCard(onAgentClick: () -> Unit, onFaqClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Need help navigating Rockland?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Talk to Rocky or browse the FAQ to learn more about the app.",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onFaqClick,
                    modifier = Modifier
                        .weight(0.65f)
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEFEFF5),
                        contentColor = TextDark
                    )
                ) {
                    Text("See FAQ")
                }
                OutlinedButton(
                    onClick = onAgentClick,
                    modifier = Modifier
                        .weight(1.35f)
                        .height(44.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color(0xFF2A2A2A),
                contentColor = Color.White
                    )
                ) {
                    Text("Enter your question")
                }
            }
        }
    }
}

@Composable
private fun HelpDeskAgentDialog(
    visible: Boolean,
    initiallyShowForm: Boolean,
    onClose: () -> Unit,
    onFaqRequest: () -> Unit
) {
    if (!visible) return

    val conversation = remember { mutableStateListOf(
        "Rocky: Hey, I can help you with Rockland questions.",
        "Rocky: Tap FAQ for quick tips or send your own question."
    ) }
    val showForm = remember { mutableStateOf(initiallyShowForm) }
    LaunchedEffect(true) {
        if (visible) {
            showForm.value = initiallyShowForm
        }
    }
    val subject = remember { mutableStateOf("") }
    val details = remember { mutableStateOf("") }

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F3F7)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Rocky Help Desk",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextDark
                        )
                        Text(
                            text = "Can ask for assistance here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFFDBE2F2), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Help Desk",
                            tint = TextDark
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFDEE0E5))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    conversation.forEach { line ->
                        Text(
                            text = line,
                            color = TextDark,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onFaqRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDDE5F3),
                            contentColor = TextDark
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("See FAQ")
                    }
                }

                if (showForm.value) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = subject.value,
                            onValueChange = { subject.value = it },
                            placeholder = { Text("Subject") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        TextField(
                            value = details.value,
                            onValueChange = { details.value = it },
                            placeholder = { Text("Describe your question...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                showForm.value = false
                                subject.value = ""
                                details.value = ""
                            }) {
                                Text("Cancel")
                            }
                            Button(onClick = {
                                conversation.add("You: ${subject.value.ifBlank { "Help Request" }}")
                                conversation.add("Details: ${details.value}")
                                subject.value = ""
                                details.value = ""
                                showForm.value = false
                            }) {
                                Text("Send")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HelpDeskFaqDialog(visible: Boolean, onClose: () -> Unit) {
    if (!visible) return
    val faqEntries = listOf(
        "How do I collect rocks?" to "Use the Map screen to tap markers and log sightings.",
        "Where can I upload photos?" to "Open the Help Desk or Collection tab and choose Add Photo.",
        "Need to report an issue?" to "Use the 'Enter your question' form in Help Desk."
    )
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FAQ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close FAQ"
                        )
                    }
                }
                faqEntries.forEach { (question, answer) ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = question,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.8f)
                        )
                        HorizontalDivider(color = Color(0xFFE0E0E0))
                    }
                }
            }
        }
    }
}
