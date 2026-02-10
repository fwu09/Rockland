package com.example.rockland.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.rockland.data.repository.UserProfileRepository
import com.example.rockland.ui.theme.TextDark

data class PendingComment(
    val id: String,
    val number: String,
    val locationId: String,
    val userId: String,
    val author: String,
    val postedAt: String,
    val location: String,
    val preview: String,
    val fullText: String,
    val imageUrls: List<String> = emptyList(),
    val photoIds: List<String> = emptyList()
)

@Composable
fun ReviewContentScreen(
    pendingComments: List<PendingComment>,
    onApproveComment: (PendingComment) -> Unit,
    onRejectComment: (PendingComment) -> Unit,
    onBack: () -> Unit
) {
    val selectedComment = remember { mutableStateOf<PendingComment?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ReviewHeader(onBack = onBack)
            if (pendingComments.isEmpty()) {
                EmptyReviewState(message = "No pending submissions.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendingComments.forEach { comment ->
                        CommentReviewCard(
                            comment = comment,
                            onClick = { selectedComment.value = comment }
                        )
                    }
                }
            }
        }
    }

    selectedComment.value?.let { comment ->
        CommentReviewDialog(
            comment = comment,
            onApprove = {
                onApproveComment(comment)
                selectedComment.value = null
            },
            onReject = {
                onRejectComment(comment)
                selectedComment.value = null
            },
            onClose = { selectedComment.value = null }
        )
    }
}

@Composable
private fun ReviewHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextDark
                )
            }
            Column {
            Text(
                text = "Review Content",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Text(
                text = "Verified Expert Review",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.6f)
            )
            }
        }
    }
}

@Composable
private fun CommentReviewCard(
    comment: PendingComment,
    onClick: () -> Unit
) {
    var authorProfileUrl by remember(comment.userId) { mutableStateOf<String?>(null) }
    val repo = remember { UserProfileRepository() }
    LaunchedEffect(comment.userId) {
        authorProfileUrl = runCatching {
            repo.getUserProfile(comment.userId).profilePictureUrl
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallAvatar(profilePictureUrl = authorProfileUrl, displayName = comment.author)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${comment.author} · Comment No. ${comment.number}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    Text(
                        text = comment.postedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.7f)
                    )
                }
            }
            Text(
                text = comment.location,
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.8f)
            )
            if (comment.imageUrls.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.imageUrls.forEach { url ->
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEDEDED))
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Comment photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
            Text(
                text = comment.preview,
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun SmallAvatar(profilePictureUrl: String?, displayName: String) {
    val initials = displayName.trim().split(" ").filter { it.isNotBlank() }.take(2).map { it.first() }.joinToString("").ifBlank { "?" }
    Box(
        modifier = Modifier
            .size(40.dp)
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
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
        }
    }
}

@Composable
private fun CommentReviewDialog(
    comment: PendingComment,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comment No. ${comment.number}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
                Text(
                    text = "Comment Submitted by: ${comment.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "Commented on: ${comment.postedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = comment.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Text(
                    text = comment.fullText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                if (comment.imageUrls.isNotEmpty()) {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        comment.imageUrls.forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(Color(0xFF444444)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Comment photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("APPROVE")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onReject,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B2E2E)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("REJECT")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReviewState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = TextDark.copy(alpha = 0.6f)
        )
    }
}
