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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.rockland.ui.theme.TextDark
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size

enum class ReviewTab {
    COMMENTS,
    IMAGES
}

data class PendingComment(
    val id: String,
    val number: String,
    val locationId: String,
    val userId: String,
    val author: String,
    val postedAt: String,
    val location: String,
    val preview: String,
    val fullText: String
)

data class PendingImageBatch(
    val id: String,
    val batchNumber: String,
    val locationId: String,
    val commentId: String,
    val userId: String,
    val author: String,
    val submittedAt: String,
    val location: String,
    val imageCount: Int,
    val imageUrls: List<String>,
    val photoIds: List<String>
)

@Composable
fun ReviewContentScreen(
    activeTab: ReviewTab,
    pendingComments: List<PendingComment>,
    pendingImageBatches: List<PendingImageBatch>,
    onApproveComment: (PendingComment) -> Unit,
    onRejectComment: (PendingComment) -> Unit,
    onApproveImageBatch: (PendingImageBatch) -> Unit,
    onRejectImageBatch: (PendingImageBatch) -> Unit,
    onTabSelected: (ReviewTab) -> Unit,
    onBack: () -> Unit
) {
    val selectedComment = remember { mutableStateOf<PendingComment?>(null) }
    val selectedImageBatch = remember { mutableStateOf<PendingImageBatch?>(null) }

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
    ReviewTabSelector(
                activeTab = activeTab,
                onTabSelected = onTabSelected
    )
            when (activeTab) {
        ReviewTab.COMMENTS -> {
            if (pendingComments.isEmpty()) {
                EmptyReviewState(message = "No pending comments.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendingComments.forEach { comment ->
                        CommentReviewCard(
                            comment = comment,
                            pendingImageBatches = pendingImageBatches,
                            onClick = { selectedComment.value = comment }
                        )
                    }
                }
            }
        }
        ReviewTab.IMAGES -> {
            if (pendingImageBatches.isEmpty()) {
                EmptyReviewState(message = "No pending image submissions.")
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendingImageBatches.forEach { batch ->
                        ImageBatchCard(
                            batch = batch,
                            onClick = { selectedImageBatch.value = batch }
                        )
                    }
                }
            }
        }
    }
        }
    }

    selectedComment.value?.let { comment ->
        CommentReviewDialog(
            comment = comment,
            pendingImageBatches = pendingImageBatches,
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

    selectedImageBatch.value?.let { batch ->
        ImageReviewDialog(
            batch = batch,
            onApprove = {
                onApproveImageBatch(batch)
                selectedImageBatch.value = null
            },
            onReject = {
                onRejectImageBatch(batch)
                selectedImageBatch.value = null
            },
            onBack = { selectedImageBatch.value = null }
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
private fun ReviewTabSelector(
    activeTab: ReviewTab,
    onTabSelected: (ReviewTab) -> Unit
) {
    TabRow(
        selectedTabIndex = ReviewTab.entries.indexOf(activeTab),
        modifier = Modifier.fillMaxWidth()
    ) {
        ReviewTab.entries.forEach { tab ->
            val title = if (tab == ReviewTab.COMMENTS) "Comments" else "Image"
            Tab(
                selected = tab == activeTab,
                onClick = { onTabSelected(tab) },
                text = { Text(title, color = TextDark) }
            )
        }
    }
}

@Composable
private fun CommentReviewCard(
    comment: PendingComment,
    pendingImageBatches: List<PendingImageBatch>,
    onClick: () -> Unit
) {
    val attachedUrls = pendingImageBatches
        .filter { it.commentId == comment.id }
        .flatMap { it.imageUrls }

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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Comment No. ${comment.number}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }
            Text(
                text = "Comment Submitted by: ${comment.author}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
            Text(
                text = "Commented on: ${comment.postedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
            Text(
                text = "Comment Location: ${comment.location}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.8f)
            )
            Text(
                text = "(Comment Preview) ${comment.preview}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )

            if (attachedUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(attachedUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageBatchCard(
    batch: PendingImageBatch,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Image Submission Batch No. ${batch.batchNumber}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFFEDEDED))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${batch.imageCount} IMAGES",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
            Text(
                text = "Review Submitted by: ${batch.author}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
            Text(
                text = "Submitted on: ${batch.submittedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.7f)
            )
            Text(
                text = batch.location,
                style = MaterialTheme.typography.bodySmall,
                color = TextDark.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun ImageReviewDialog(
    batch: PendingImageBatch,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onBack: () -> Unit
) {
    val currentIndex = remember { mutableIntStateOf(0) }
    val currentLabel = "${currentIndex.intValue + 1}/${batch.imageUrls.size}"
    val currentUrl = batch.imageUrls.getOrNull(currentIndex.intValue).orEmpty()
    Dialog(onDismissRequest = onBack) {
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
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Image Submission Batch No. ${batch.batchNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDark
                    )
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Color(0xFFF1F1F1)),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUrl.isBlank()) {
                        Text(
                            text = "Image $currentLabel",
                            color = TextDark.copy(alpha = 0.7f)
                        )
                    } else {
                        AsyncImage(
                            model = currentUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (currentIndex.intValue > 0) currentIndex.intValue -= 1
                        },
                        enabled = currentIndex.intValue > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDEDED), contentColor = TextDark)
                    ) {
                        Text("Prev")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = currentLabel,
                        color = TextDark.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (currentIndex.intValue < batch.imageUrls.size - 1) currentIndex.intValue += 1
                        },
                        enabled = currentIndex.intValue < batch.imageUrls.size - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEDEDED), contentColor = TextDark)
                    ) {
                        Text("Next")
                    }
                }
                Text(
                    text = "Review Submitted by: ${batch.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Text(
                    text = "Submitted on: ${batch.submittedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.7f)
                )
                Text(
                    text = batch.location,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDark.copy(alpha = 0.8f)
                )
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
private fun CommentReviewDialog(
    comment: PendingComment,
    pendingImageBatches: List<PendingImageBatch>,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onClose: () -> Unit
) {
    val attachedUrls = pendingImageBatches
        .filter { it.commentId == comment.id }
        .flatMap { it.imageUrls }
        .take(12) // optional limit

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

                if (attachedUrls.isNotEmpty()) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Text(
                        text = "Attached images (${attachedUrls.size})",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(attachedUrls) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(130.dp)
                                    .clip(MaterialTheme.shapes.medium)
                            )
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
