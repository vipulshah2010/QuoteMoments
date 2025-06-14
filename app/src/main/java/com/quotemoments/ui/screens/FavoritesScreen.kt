package com.quotemoments.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quotemoments.data.model.Quote
import com.quotemoments.ui.screens.components.FavoriteCategoryChips
import com.quotemoments.ui.screens.components.QuoteCard

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    favorites: List<Quote>,
    onToggleFavorite: (Quote) -> Unit,
    onShare: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf("all") }

    val availableCategories by remember(favorites) {
        derivedStateOf {
            favorites.map { it.category }.distinct().sorted()
        }
    }

    val filteredFavorites by remember(favorites, selectedCategory) {
        derivedStateOf {
            if (selectedCategory == "all") {
                favorites
            } else {
                favorites.filter { it.category == selectedCategory }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (favorites.isEmpty()) {
            EmptyFavoritesState()
        } else {
            if (availableCategories.isNotEmpty()) {
                FavoriteCategoryChips(
                    favorites = favorites,
                    availableCategories = availableCategories,
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it }
                )
            }

            Text(
                text = if (selectedCategory == "all") {
                    "${favorites.size} favorite ${if (favorites.size == 1) "quote" else "quotes"}"
                } else {
                    "${filteredFavorites.size} from ${selectedCategory.replaceFirstChar { it.uppercase() }}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (filteredFavorites.isEmpty()) {
                EmptyCategoryState(category = selectedCategory)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(
                        items = filteredFavorites,
                        key = { it.text }
                    ) { quote ->
                        QuoteCard(
                            quote = quote,
                            isFavorite = true,
                            onToggleFavorite = onToggleFavorite,
                            onShare = { onShare(quote) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FavoriteBorder,
            contentDescription = "No Favorites",
            modifier = Modifier.padding(16.dp)
        )
        Text(
            text = "No favorite quotes yet",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Start adding quotes from the home screen",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EmptyCategoryState(category: String) {
    Text(
        text = "No favorites in ${category.replaceFirstChar { it.uppercase() }}",
        modifier = Modifier.padding(32.dp),
        textAlign = TextAlign.Center
    )
}