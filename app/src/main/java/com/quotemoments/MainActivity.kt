package com.quotemoments

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quotemoments.data.local.PreferencesManager
import com.quotemoments.data.repository.QuoteRepository
import com.quotemoments.ui.QuoteViewModel
import com.quotemoments.ui.QuoteViewModelFactory
import com.quotemoments.ui.screens.QuoteApp
import com.quotemoments.ui.theme.QuoteMomentsTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: QuoteViewModel by viewModels {
        QuoteViewModelFactory(
            repository = QuoteRepository(applicationContext),
            preferencesManager = PreferencesManager(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencesManager = PreferencesManager(this)

        setContent {
            var isDarkTheme by rememberSaveable {
                mutableStateOf(preferencesManager.getThemePreference())
            }

            // Apply edge-to-edge styling
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            // Collect navigation events
            LaunchedEffect(Unit) {
                viewModel.navigationEvents.collectLatest { event ->
                    when (event) {
                        is com.quotemoments.ui.NavigationEvent.ShareQuote -> {
                            shareQuote(event.quote)
                        }
                    }
                }
            }

            QuoteMomentsTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    QuoteApp(
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            isDarkTheme = !isDarkTheme
                            preferencesManager.saveThemePreference(isDarkTheme)
                        }
                    )
                }
            }
        }
    }

    private fun shareQuote(quote: com.quotemoments.data.model.Quote) {
        val shareText = buildString {
            append("💭 ${quote.text}\n\n")
            append("🏷️ Category: ${quote.category.replaceFirstChar { it.uppercase() }}\n\n")
            append("📱 Shared from Quote Moments")
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(
                Intent.EXTRA_SUBJECT,
                "Inspirational Quote - ${quote.category.replaceFirstChar { it.uppercase() }}"
            )
        }

        startActivity(Intent.createChooser(shareIntent, "Share Quote"))
    }
}