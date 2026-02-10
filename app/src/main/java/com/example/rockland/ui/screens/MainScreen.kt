// Screen orchestrating bottom navigation and child content in the UI layer.
package com.example.rockland.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.data.datasource.remote.UserData
import com.example.rockland.presentation.viewmodel.AwardsViewModel
import com.example.rockland.presentation.viewmodel.CollectionViewModel
import com.example.rockland.presentation.viewmodel.MapViewModel
import com.example.rockland.presentation.viewmodel.ReviewContentViewModel
import com.example.rockland.presentation.viewmodel.UserViewModel

// Main screen with bottom navigation.
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    userViewModel: UserViewModel = viewModel(factory = UserViewModel.Factory())
) {
    val selectedTabState = rememberSaveable { mutableIntStateOf(0) }
    val selectedTab = selectedTabState.intValue
    val userData by userViewModel.userData.collectAsState()

    MainScreenContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTabState.intValue = it },
        userData = userData,
        userViewModel = userViewModel,
        onSettingsClick = onSettingsClick,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun MainScreenContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    userData: UserData?,
    userViewModel: UserViewModel,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val mapViewModel = remember { MapViewModel() }
    val collectionViewModel: CollectionViewModel = viewModel()
    val awardsViewModel: AwardsViewModel = viewModel(
        key = "awards_${userData?.userId ?: "_anon"}",
        factory = AwardsViewModel.Factory(userData?.userId)
    )
    val reviewContentViewModel: ReviewContentViewModel =
        viewModel(factory = ReviewContentViewModel.Factory())

    val collectionTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val awardsTabIndex = rememberSaveable { mutableIntStateOf(0) }
    val inboxReviewTabIndex = rememberSaveable { mutableIntStateOf(0) }

    val normalizedRole = userData?.role?.trim()?.lowercase()
    val isAdmin = normalizedRole == "admin" || normalizedRole == "user_admin"

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 0 - Collection
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Collect") },
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                // 1 - Map
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text("Map") },
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                // 2 - Inbox
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Email, contentDescription = null) },
                    label = { Text("Inbox") },
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                // 3 - Identifier
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = { Text("Scan") },
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
                )
                // 4 - Awards
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Star, contentDescription = null) },
                    label = { Text("Awards") },
                    selected = selectedTab == 4,
                    onClick = { onTabSelected(4) }
                )
                // ✅ 5 - Boxes (NEW)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.CardGiftcard, contentDescription = null) },
                    label = { Text("Boxes") },
                    selected = selectedTab == 5,
                    onClick = { onTabSelected(5) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> CollectionScreen(
                    userViewModel = userViewModel,
                    viewModel = collectionViewModel,
                    selectedTabIndex = collectionTabIndex.intValue,
                    onTabSelected = { collectionTabIndex.intValue = it }
                )

                1 -> MapScreen(
                    viewModel = mapViewModel,
                    userViewModel = userViewModel,
                )

                2 -> InboxScreen(
                    userData = userData,
                    onProfileClick = { onTabSelected(6) }, // ✅ moved profile to 6
                    onGoToPage = { notification ->
                        val targetTab = notification.targetTab?.trim()?.lowercase()
                        when (targetTab) {
                            "map" -> {
                                onTabSelected(1)
                                val locationId = notification.targetLocationId.orEmpty()
                                if (locationId.isNotBlank()) {
                                    mapViewModel.focusLocation(locationId)
                                }
                            }
                            "dictionary" -> {
                                collectionTabIndex.intValue = 1
                                onTabSelected(0)
                            }
                            else -> {
                                if (notification.type == "rock_dictionary_approved" ||
                                    notification.title == "Rock Dictionary Update Approved"
                                ) {
                                    collectionTabIndex.intValue = 1
                                    onTabSelected(0)
                                }
                            }
                        }
                    },
                    reviewViewModel = reviewContentViewModel,
                    reviewTabIndex = inboxReviewTabIndex.intValue,
                    onReviewTabChanged = { inboxReviewTabIndex.intValue = it }
                )

                3 -> IdentifierScreen(userViewModel = userViewModel)

                4 -> AchievementScreen(
                    userId = userData?.userId,
                    viewModel = awardsViewModel,
                    selectedTabIndex = awardsTabIndex.intValue,
                    onTabSelected = { awardsTabIndex.intValue = it },
                    isAdmin = isAdmin
                )

                // ✅ 5 -> Boxes Screen (NEW)
                5 -> BoxesScreen(
                    userId = userData?.userId
                )

                // ✅ 6 -> Profile Screen (moved from 5)
                6 -> ProfileScreen(
                    userData = userData,
                    onSettingsClick = onSettingsClick,
                    onLogoutClick = onLogoutClick,
                    onBackClick = { onTabSelected(2) }
                )

                // Fallback to Collection if selectedTab is invalid
                else -> {
                    onTabSelected(0)
                    CollectionScreen()
                }
            }
        }
    }
}
