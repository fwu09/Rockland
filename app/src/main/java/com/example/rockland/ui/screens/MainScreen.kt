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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rockland.R
import com.example.rockland.viewmodel.UserViewModel

// Main screen with bottom navigation
@Composable
fun MainScreen(
    onSettingsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {},
    userViewModel: UserViewModel = viewModel(factory = UserViewModel.Factory())
) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.fillMaxWidth()
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.home)) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = null) },
                    label = { Text(stringResource(R.string.map)) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text(stringResource(R.string.collection)) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
                    label = { Text(stringResource(R.string.profile)) },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 }
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
                0 -> PlaceholderScreen("Home Screen")
                1 -> PlaceholderScreen("Map Screen")
                2 -> PlaceholderScreen("Collection Screen")
                3 -> {
                    val userData by userViewModel.userData.collectAsState()
                    com.example.rockland.ui.screens.ProfileScreen(
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


// Preview for MainScreen
@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    com.example.rockland.ui.theme.RocklandTheme {
        MainScreen()
    }
}