package com.example.rockland.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.R
import com.example.rockland.firebase.UserData
import com.example.rockland.viewmodel.MapViewModel
import com.example.rockland.viewmodel.UserViewModel

// Main screen with bottom navigation
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    userViewModel: UserViewModel = viewModel(factory = UserViewModel.Factory())
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val userData by userViewModel.userData.collectAsState()

    MainScreenContent(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        userData = userData,
        onSettingsClick = onSettingsClick,
        onLogoutClick = onLogoutClick
    )
}

@Composable
fun MainScreenContent(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    userData: UserData?,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) },
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.map)) },
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.collection)) },
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.profile)) },
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) }
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
                0 -> HomeScreen()
                1 -> MapScreen(
                    viewModel = MapViewModel(),
                    onInfoDetailsClick = { /* TODO: future info flow */ },
                    onAddCommentClick = { /* TODO: add comment flow */ }
                )
                2 -> PlaceholderScreen("Collection Screen")
                3 -> {
                    ProfileScreen(
                        userData = userData,
                        onSettingsClick = onSettingsClick,
                        onLogoutClick = onLogoutClick
                    )
                }
            }
        }
    }
}

// Placeholder screen for screens not yet implemented
@Composable
fun PlaceholderScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 24.sp
        )
    }
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MainScreenContent(
        selectedTab = 0,
        onTabSelected = {},
        userData = UserData(firstName = "Preview", lastName = "User"),
        onSettingsClick = {},
        onLogoutClick = {}
    )
}
