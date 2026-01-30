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
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.ui.theme.Rock3
import com.example.rockland.ui.theme.TextDark

@Composable
fun InboxScreen(
    userData: UserData?,
    onProfileClick: () -> Unit
) {
    val showAgentDialog = remember { mutableStateOf(false) }
    val showFaqDialog = remember { mutableStateOf(false) }
    val requestFormVisible = remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Rock3),
                contentAlignment = Alignment.Center
            ) {
                val initials =
                    "${userData?.firstName?.firstOrNull() ?: 'U'}${userData?.lastName?.firstOrNull() ?: 'N'}"
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
                    text = "${userData?.firstName ?: "Unknown"} ${userData?.lastName ?: "User"}",
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

        Spacer(modifier = Modifier.height(32.dp))

        HelpDeskCard(
            onAgentClick = {
                showAgentDialog.value = true
                requestFormVisible.value = true
            },
            onFaqClick = { showFaqDialog.value = true }
        )
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                text = "Inbox",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
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

        HelpDeskFaqDialog(
            visible = showFaqDialog.value,
            onClose = { showFaqDialog.value = false }
        )
    }
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
                        containerColor = Color.Transparent,
                        contentColor = TextDark
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
