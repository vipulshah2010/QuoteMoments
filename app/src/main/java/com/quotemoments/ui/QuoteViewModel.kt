package com.quotemoments.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quotemoments.data.local.PreferencesManager
import com.quotemoments.data.model.Quote
import com.quotemoments.data.repository.QuoteRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuoteViewModel(
    private val repository: QuoteRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuoteUiState())
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    init {
        loadQuotes()
        loadFavorites()
    }

    private fun loadQuotes() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val quoteData = repository.loadQuotes()
                val initialQuote = repository.getRandomQuote(
                    _uiState.value.selectedCategory,
                    quoteData.categories
                ) ?: ""

                _uiState.update { state ->
                    state.copy(
                        quoteData = quoteData.categories,
                        currentQuote = Quote(initialQuote, state.selectedCategory),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load quotes: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val favorites = preferencesManager.getFavorites()
            _uiState.update { it.copy(favorites = favorites) }
        }
    }

    fun onEvent(event: QuoteEvent) {
        when (event) {
            is QuoteEvent.GetNewQuote -> getNewQuote()
            is QuoteEvent.SelectCategory -> selectCategory(event.category)
            is QuoteEvent.UpdateSearchQuery -> updateSearchQuery(event.query)
            is QuoteEvent.ToggleFavorite -> toggleFavorite(event.quote)
            is QuoteEvent.ClearSearch -> clearSearch()
        }
    }

    private fun getNewQuote() {
        val currentState = _uiState.value
        val newQuoteText = repository.getRandomQuoteExcluding(
            category = currentState.selectedCategory,
            quotes = currentState.quoteData,
            exclude = currentState.currentQuote.text
        ) ?: return

        _uiState.update { state ->
            state.copy(
                currentQuote = Quote(newQuoteText, state.selectedCategory)
            )
        }
    }

    private fun selectCategory(category: String) {
        val newQuoteText = repository.getRandomQuote(
            category = category,
            quotes = _uiState.value.quoteData
        ) ?: return

        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                currentQuote = Quote(newQuoteText, category),
                searchQuery = ""
            )
        }
    }

    private fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    private fun clearSearch() {
        _uiState.update { it.copy(searchQuery = "") }
    }

    private fun toggleFavorite(quote: Quote) {
        val currentFavorites = _uiState.value.favorites
        val newFavorites = if (currentFavorites.any { it.text == quote.text }) {
            currentFavorites.filter { it.text != quote.text }
        } else {
            currentFavorites + quote
        }

        _uiState.update { it.copy(favorites = newFavorites) }
        preferencesManager.saveFavorites(newFavorites)
    }

    fun shareQuote(quote: Quote) {
        viewModelScope.launch {
            _navigationEvents.emit(NavigationEvent.ShareQuote(quote))
        }
    }

    // ✅ ADD: Helper method for search results
    fun getFilteredQuotes(): List<Quote> {
        return repository.searchQuotes(_uiState.value.searchQuery, _uiState.value.quoteData)
    }
}

// Factory for ViewModel without Hilt
class QuoteViewModelFactory(
    private val repository: QuoteRepository,
    private val preferencesManager: PreferencesManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(QuoteViewModel::class.java)) {
            return QuoteViewModel(repository, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}