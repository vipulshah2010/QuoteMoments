package com.quotemoments.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quotemoments.ui.QuoteViewModel

enum class Screen {
    HOME, FAVORITES
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteApp(
    viewModel: QuoteViewModel,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (currentScreen) {
                            Screen.HOME -> "Quote Moments"
                            Screen.FAVORITES -> "Favorite Quotes"
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onThemeToggle) {
                        Text(
                            text = if (isDarkTheme) "☀️" else "🌙",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home") },
                    selected = currentScreen == Screen.HOME,
                    onClick = { currentScreen = Screen.HOME }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Favorites") },
                    label = { Text("Favorites") },
                    selected = currentScreen == Screen.FAVORITES,
                    onClick = { currentScreen = Screen.FAVORITES }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                Screen.HOME -> {
                    when {
                        uiState.isLoading -> {
                            LoadingScreen()
                        }

                        uiState.error != null -> {
                            ErrorScreen(message = uiState.error!!)
                        }

                        else -> {
                            HomeScreen(
                                uiState = uiState,
                                viewModel = viewModel,  // ✅ FIXED: Pass ViewModel
                                onEvent = viewModel::onEvent,
                                onShare = { viewModel.shareQuote(it) }
                            )
                        }
                    }
                }

                Screen.FAVORITES -> {
                    FavoritesScreen(
                        favorites = uiState.favorites,
                        onToggleFavorite = {
                            viewModel.onEvent(com.quotemoments.ui.QuoteEvent.ToggleFavorite(it))
                        },
                        onShare = { viewModel.shareQuote(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Error",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}