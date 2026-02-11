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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rockland.ui.theme.Rock1
import com.example.rockland.ui.theme.TextDark
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.example.rockland.data.repository.CollectionRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

@Composable
fun BoxesScreen(userId: String?) {
    val db = remember { FirebaseFirestore.getInstance() }
    val scope = rememberCoroutineScope()

    // ✅ Auth state listener (prevents "request.auth == null" race)
    val authUserIdState = remember { mutableStateOf<String?>(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { fbAuth ->
            authUserIdState.value = fbAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    // Prefer the passed-in userId only if you KNOW it equals auth uid.
    // Most projects store users/{uid}. So we use auth uid as source of truth.
    val resolvedUserId = authUserIdState.value ?: userId

    // Inventory state (from Firestore)
    val commonCount = rememberSaveable { mutableIntStateOf(0) }
    val rareCount = rememberSaveable { mutableIntStateOf(0) }
    val specialCount = rememberSaveable { mutableIntStateOf(0) }

    // UI state
    val isLoading = remember { mutableStateOf(true) }
    val errorMsg = remember { mutableStateOf<String?>(null) }
    val resultDialog = remember { mutableStateOf<BoxOpenResult?>(null) }
    val opening = remember { mutableStateOf(false) }

    // ✅ Only load after auth is ready (no permission denied flicker)
    LaunchedEffect(resolvedUserId) {
        if (resolvedUserId.isNullOrBlank()) {
            isLoading.value = false
            return@LaunchedEffect
        }

        isLoading.value = true
        errorMsg.value = null

        try {
            val userDoc = db.collection("users").document(resolvedUserId).get().await()
            val inv = userDoc.get("boxInventory") as? Map<*, *>

            commonCount.intValue = (inv?.get("common") as? Long ?: 0L).toInt()
            rareCount.intValue = (inv?.get("rare") as? Long ?: 0L).toInt()
            specialCount.intValue = (inv?.get("special") as? Long ?: 0L).toInt()
        } catch (e: Exception) {
            errorMsg.value = e.message ?: "Failed to load box inventory."
        } finally {
            isLoading.value = false
        }
    }

    // If auth not ready or user not logged in
    if (resolvedUserId.isNullOrBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Please login to view your boxes.")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If you are already logged in, wait 1–2 seconds and reopen Boxes tab.",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = "Boxes",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Earn boxes from missions. Open them to get rocks (duplicates possible).",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.75f)
            )

            Spacer(Modifier.height(12.dp))

            if (isLoading.value) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Loading inventory...", color = TextDark)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            } else if (errorMsg.value != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = errorMsg.value ?: "Error",
                            color = Color(0xFFB00020),
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "If this says PERMISSION_DENIED, you are either logged out or your users/{uid} doc ID is not the Auth UID.",
                            color = TextDark.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Spacer(Modifier.height(4.dp))
                InventoryCards(
                    commonCount = commonCount.intValue,
                    rareCount = rareCount.intValue,
                    specialCount = specialCount.intValue,
                    opening = opening.value,
                    onOpenCommon = {
                        if (commonCount.intValue <= 0) return@InventoryCards
                        scope.launch {
                            openBox(
                                db = db,
                                userId = resolvedUserId,
                                boxId = "common",
                                onStart = { opening.value = true },
                                onDone = { opening.value = false },
                                onError = { errorMsg.value = it },
                                onResult = { result ->
                                    commonCount.intValue = (commonCount.intValue - 1).coerceAtLeast(0)
                                    resultDialog.value = result
                                }
                            )
                        }
                    },
                    onOpenRare = {
                        if (rareCount.intValue <= 0) return@InventoryCards
                        scope.launch {
                            openBox(
                                db = db,
                                userId = resolvedUserId,
                                boxId = "rare",
                                onStart = { opening.value = true },
                                onDone = { opening.value = false },
                                onError = { errorMsg.value = it },
                                onResult = { result ->
                                    rareCount.intValue = (rareCount.intValue - 1).coerceAtLeast(0)
                                    resultDialog.value = result
                                }
                            )
                        }
                    },
                    onOpenSpecial = {
                        if (specialCount.intValue <= 0) return@InventoryCards
                        scope.launch {
                            openBox(
                                db = db,
                                userId = resolvedUserId,
                                boxId = "special",
                                onStart = { opening.value = true },
                                onDone = { opening.value = false },
                                onError = { errorMsg.value = it },
                                onResult = { result ->
                                    specialCount.intValue = (specialCount.intValue - 1).coerceAtLeast(0)
                                    resultDialog.value = result
                                }
                            )
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))
                InfoCard()
            }
        }

        resultDialog.value?.let { result ->
            BoxOpenResultDialog(
                result = result,
                onDismiss = { resultDialog.value = null }
            )
        }
    }
}

@Composable
private fun InventoryCards(
    commonCount: Int,
    rareCount: Int,
    specialCount: Int,
    opening: Boolean,
    onOpenCommon: () -> Unit,
    onOpenRare: () -> Unit,
    onOpenSpecial: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            listOf(
                BoxUi("Common Box", "Daily mission rewards", commonCount, Rock1.copy(alpha = 0.18f)),
                BoxUi("Rare Box", "Weekly mission rewards", rareCount, Rock1.copy(alpha = 0.22f)),
                BoxUi("Special Box", "Monthly leaderboard rewards", specialCount, Rock1.copy(alpha = 0.26f))
            )
        ) { item ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontSize = 12.sp,
                                color = TextDark.copy(alpha = 0.7f)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.badgeColor)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "x${item.count}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color(0xFFEAEAEA))
                    Spacer(Modifier.height(10.dp))

                    val canOpen = item.count > 0 && !opening
                    val buttonLabel = if (opening) "Opening..." else "Open"

                    Button(
                        onClick = {
                            when (item.title) {
                                "Common Box" -> onOpenCommon()
                                "Rare Box" -> onOpenRare()
                                "Special Box" -> onOpenSpecial()
                            }
                        },
                        enabled = canOpen,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Rock1,
                            contentColor = Color.White
                        )
                    ) {
                        Text(buttonLabel)
                    }

                    if (item.count <= 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "No boxes available. Complete missions to earn more.",
                            fontSize = 11.sp,
                            color = TextDark.copy(alpha = 0.65f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Rock1.copy(alpha = 0.14f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "How Boxes Work",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "• Boxes come from mission rewards.\n" +
                        "• Each box has weighted chances by rock rarity.\n" +
                        "• You can get duplicate rocks.",
                fontSize = 12.sp,
                color = TextDark.copy(alpha = 0.8f)
            )
        }
    }
}

private data class BoxUi(
    val title: String,
    val subtitle: String,
    val count: Int,
    val badgeColor: Color
)

private data class BoxOpenResult(
    val boxId: String,
    val rarity: String,
    val rockName: String
)

@Composable
private fun BoxOpenResultDialog(
    result: BoxOpenResult,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "You opened a ${result.boxId.replaceFirstChar { it.uppercase() }} Box!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Rarity: ${result.rarity.replace('_', ' ').uppercase()}",
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = result.rockName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Duplicates are possible.",
                    fontSize = 12.sp,
                    color = TextDark.copy(alpha = 0.7f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

private suspend fun openBox(
    db: FirebaseFirestore,
    userId: String,
    boxId: String,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onError: (String) -> Unit,
    onResult: (BoxOpenResult) -> Unit
) {
    onStart()
    try {
        // 1) Read box definition
        val boxDoc = db.collection("boxes").document(boxId).get().await()
        if (!boxDoc.exists()) {
            onError("Box '$boxId' not found in Firestore collection 'boxes'.")
            return
        }

        val rarityChances = (boxDoc.get("rarityChances") as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val value = (v as? Long ?: 0L).toInt()
                key to value
            }?.toMap() ?: emptyMap()

        val poolByRarity = (boxDoc.get("poolByRarity") as? Map<*, *>)
            ?.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val list = (v as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                key to list
            }?.toMap() ?: emptyMap()

        if (rarityChances.isEmpty() || poolByRarity.isEmpty()) {
            onError("Box '$boxId' is missing rarityChances or poolByRarity.")
            return
        }

        // 2) Pick rarity using weights
        val pickedRarity = weightedPick(rarityChances)
        val rocksForRarity = poolByRarity[pickedRarity].orEmpty()
        if (rocksForRarity.isEmpty()) {
            onError("No rocks found for rarity '$pickedRarity' in box '$boxId'.")
            return
        }

        // 3) Pick a rock within that rarity
        val rockName = rocksForRarity.random()

        // 4) Update user inventory (decrement) + log history + add to collection
        val userRef = db.collection("users").document(userId)

        // Read current inventory safely
        val userDoc = userRef.get().await()
        val inv = (userDoc.get("boxInventory") as? Map<*, *>)
        val current = (inv?.get(boxId) as? Long ?: 0L).toInt()

        if (current <= 0) {
            onError("No '$boxId' boxes left.")
            return
        }

        val newCount = current - 1

        // ✅ Update only the one key inside boxInventory (won't overwrite whole map)
        userRef.update("boxInventory.$boxId", newCount).await()

        // Log open history
        val historyData = mapOf(
            "boxId" to boxId,
            "rarity" to pickedRarity,
            "rockName" to rockName,
            "openedAt" to System.currentTimeMillis()
        )
        userRef.collection("boxOpenHistory").add(historyData).await()

        // ✅ Add to user's collection (duplicates -> increment count)
        addRockToUserCollection(
            db = db,
            userId = userId,
            rockName = rockName,
            sourceBoxId = boxId
        )

        onResult(BoxOpenResult(boxId = boxId, rarity = pickedRarity, rockName = rockName))
    } catch (e: Exception) {
        onError(e.message ?: "Failed to open box.")
    } finally {
        onDone()
    }
}
private suspend fun addRockToUserCollection(
    db: FirebaseFirestore,
    userId: String,
    rockName: String,
    sourceBoxId: String
) {
    // ✅ Use the SAME repository + schema as the rest of the app
    val repository = CollectionRepository()

    // ✅ Keep rockId stable and consistent.
    // If your dictionary uses rockName EXACTLY, keep it simple:
    val rockId = rockName.trim().lowercase().replace(" ", "_")

    try {
        // 1) Add to collection using your existing repository
        // IMPORTANT: this only works if addRockToCollection is a suspend function
        // or internally uses Tasks.await().
        repository.addRockToCollection(
            userId = userId,
            rockId = rockId,
            rockSource = "box:$sourceBoxId",
            rockName = rockName,
            thumbnailUrl = null
        )

        // 2) ✅ ALSO unlock it (many UIs hide rocks unless unlocked)
        // This is merge-safe and will not overwrite other fields.
        db.collection("users").document(userId)
            .set(
                mapOf("unlockedRockIds" to FieldValue.arrayUnion(rockId)),
                SetOptions.merge()
            )
            .await()

        // Optional: store rarity somewhere if your UI needs it later
        // db.collection("users").document(userId)
        //   .collection("collection_meta").document(rockId)
        //   .set(mapOf("rarity" to rarity), SetOptions.merge()).await()

    } catch (e: Exception) {
        // Bubble up so openBox() catch shows it in UI
        throw IllegalStateException("Failed to add '$rockName' to collection: ${e.message}", e)
    }
}




private fun weightedPick(weights: Map<String, Int>): String {
    val safe = weights.filterValues { it > 0 }
    val total = safe.values.sum()
    if (total <= 0) return safe.keys.firstOrNull() ?: "common"

    val r = Random.nextInt(1, total + 1)
    var acc = 0
    for ((k, w) in safe) {
        acc += w
        if (r <= acc) return k
    }
    return safe.keys.first()
}
