package com.quotemoments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quotemoments.data.model.Quote
import com.quotemoments.ui.QuoteEvent
import com.quotemoments.ui.QuoteUiState
import com.quotemoments.ui.QuoteViewModel
import com.quotemoments.ui.components.SearchBar
import com.quotemoments.ui.screens.components.CategoryChips
import com.quotemoments.ui.screens.components.CurrentQuoteCard
import com.quotemoments.ui.screens.components.QuoteActionButtons
import com.quotemoments.ui.screens.components.QuoteCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    uiState: QuoteUiState,
    viewModel: QuoteViewModel,  // ✅ ADDED: Pass ViewModel to get search results
    onEvent: (QuoteEvent) -> Unit,
    onShare: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    // Derived states
    val isSearchMode by remember(uiState.searchQuery) {
        derivedStateOf { uiState.searchQuery.isNotBlank() }
    }

    // ✅ FIXED: Get filtered quotes from ViewModel
    val filteredQuotes by remember(uiState.searchQuery, uiState.quoteData) {
        derivedStateOf {
            if (uiState.searchQuery.isBlank()) {
                emptyList()
            } else {
                viewModel.getFilteredQuotes()  // Use ViewModel method
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "search_bar") {
            SearchBar(
                query = uiState.searchQuery,
                onQueryChange = { onEvent(QuoteEvent.UpdateSearchQuery(it)) },
                onClear = {
                    onEvent(QuoteEvent.ClearSearch)
                    keyboardController?.hide()
                },
                onSearch = { keyboardController?.hide() }
            )
        }

        if (isSearchMode) {
            if (filteredQuotes.isEmpty()) {
                item(key = "no_results") {
                    Text(
                        text = "No quotes found for \"${uiState.searchQuery}\"",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                items(
                    items = filteredQuotes,
                    key = { it.text }
                ) { quote ->
                    QuoteCard(
                        quote = quote,
                        isFavorite = uiState.favorites.any { it.text == quote.text },
                        onToggleFavorite = { onEvent(QuoteEvent.ToggleFavorite(it)) },
                        onShare = { onShare(quote) }
                    )
                }
            }
        } else {
            item(key = "subtitle") {
                Text(
                    text = "Daily inspiration for your journey",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item(key = "categories") {
                CategoryChips(
                    categories = uiState.quoteData.keys.toList(),
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelect = { onEvent(QuoteEvent.SelectCategory(it)) }
                )
            }

            item(key = "current_quote") {
                CurrentQuoteCard(
                    quote = uiState.currentQuote,
                    isFavorite = uiState.favorites.any { it.text == uiState.currentQuote.text },
                    onToggleFavorite = { onEvent(QuoteEvent.ToggleFavorite(uiState.currentQuote)) }
                )
            }

            item(key = "actions") {
                QuoteActionButtons(
                    onNewQuote = { onEvent(QuoteEvent.GetNewQuote) },
                    onShare = { onShare(uiState.currentQuote) }
                )
            }
        }
    }
}