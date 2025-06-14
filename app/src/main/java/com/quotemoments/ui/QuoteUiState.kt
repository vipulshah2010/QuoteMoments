package com.quotemoments.ui

import com.quotemoments.data.model.Quote

data class QuoteUiState(
    val quoteData: Map<String, List<String>> = emptyMap(),
    val currentQuote: Quote = Quote("", ""),
    val selectedCategory: String = "motivation",
    val favorites: List<Quote> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

sealed interface QuoteEvent {
    data object GetNewQuote : QuoteEvent
    data class SelectCategory(val category: String) : QuoteEvent
    data class UpdateSearchQuery(val query: String) : QuoteEvent
    data class ToggleFavorite(val quote: Quote) : QuoteEvent
    data object ClearSearch : QuoteEvent
}

sealed interface NavigationEvent {
    data class ShareQuote(val quote: Quote) : NavigationEvent
}