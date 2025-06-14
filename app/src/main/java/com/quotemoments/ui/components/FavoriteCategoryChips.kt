package com.quotemoments.ui.screens.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quotemoments.data.model.Quote

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoriteCategoryChips(
    favorites: List<Quote>,
    availableCategories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp)
    ) {
        // "All" chip
        FilterChip(
            onClick = { onCategorySelect("all") },
            label = {
                Text(
                    text = "All (${favorites.size})",
                    style = MaterialTheme.typography.labelMedium
                )
            },
            selected = selectedCategory == "all"
        )

        // Category-specific chips
        availableCategories.forEach { category ->
            val categoryCount = favorites.count { it.category == category }
            FilterChip(
                onClick = { onCategorySelect(category) },
                label = {
                    Text(
                        text = "${category.replaceFirstChar { it.uppercase() }} ($categoryCount)",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                selected = selectedCategory == category
            )
        }
    }
}