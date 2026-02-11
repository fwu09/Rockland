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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewContentScreen(
    pendingComments: List<PendingComment>,
    onApproveComment: (PendingComment) -> Unit,
    onRejectComment: (PendingComment) -> Unit,
    onBack: () -> Unit
) {
    var selectedComment by remember { mutableStateOf<PendingComment?>(null) }

    Surface(color = Color(0xFFF6F7FB), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Content Review",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Text(
                            text = "Verified Expert Queue",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF6F7FB)
                )
            )

            if (pendingComments.isEmpty()) {
                EmptyReviewState(
                    message = "No pending submissions.",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }

                    items(pendingComments, key = { it.id }) { comment ->
                        CommentReviewCard(
                            comment = comment,
                            onClick = { selectedComment = comment }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    selectedComment?.let { comment ->
        CommentReviewDialog(
            comment = comment,
            onApprove = {
                onApproveComment(comment)
                selectedComment = null
            },
            onReject = {
                onRejectComment(comment)
                selectedComment = null
            },
            onClose = { selectedComment = null }
        )
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
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SmallAvatar(profilePictureUrl = authorProfileUrl, displayName = comment.author)

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = comment.author.ifBlank { "Unknown" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = comment.postedAt,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDark.copy(alpha = 0.65f)
                    )
                }

                // right-side chip
                Chip(
                    text = "No. ${comment.number}",
                    container = Color(0xFFEFF2FF),
                    content = TextDark
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Chip(
                    text = comment.location.ifBlank { "Unknown location" },
                    container = Color(0xFFF3F3F3),
                    content = TextDark
                )

                if (comment.imageUrls.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFF3F3F3))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            tint = TextDark.copy(alpha = 0.75f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "${comment.imageUrls.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextDark
                        )
                    }
                }
            }

            if (comment.imageUrls.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    comment.imageUrls.take(6).forEach { url ->
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF1F1F1))
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
                text = comment.preview.ifBlank { "Tap to view details" },
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.75f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SmallAvatar(profilePictureUrl: String?, displayName: String) {
    val initials = displayName
        .trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first() }
        .joinToString("")
        .ifBlank { "?" }

    Box(
        modifier = Modifier
            .size(42.dp)
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
private fun Chip(
    text: String,
    container: Color,
    content: Color
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = content,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(CircleShape)
            .background(container)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
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
                .padding(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
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
                            text = "Comment No. ${comment.number}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Text(
                            text = "${comment.author} • ${comment.postedAt}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextDark.copy(alpha = 0.65f)
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark
                        )
                    }
                }

                Chip(
                    text = comment.location.ifBlank { "Unknown location" },
                    container = Color(0xFFF3F3F3),
                    content = TextDark
                )

                HorizontalDivider(color = Color(0xFFE9E9E9))

                Text(
                    text = comment.fullText.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark
                )

                if (comment.imageUrls.isNotEmpty()) {
                    Text(
                        text = "Photos",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
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
                                    .size(92.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFF1F1F1))
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onReject,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF2F2F2)),
                    ) {
                        Text("Reject", color = TextDark)
                    }
                    Button(
                        onClick = onApprove,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A))
                    ) {
                        Text("Approve", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReviewState(
    message: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextDark.copy(alpha = 0.7f)
            )
        }
    }
}
