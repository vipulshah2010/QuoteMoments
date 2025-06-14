package com.quotemoments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quotemoments.ui.theme.QuoteMomentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import kotlin.random.Random

// Data classes for quotes
data class QuoteData(
    val categories: Map<String, List<String>>
)

data class Quote(
    val text: String,
    val category: String
)

// Navigation destinations
enum class Screen {
    HOME, FAVORITES
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(getThemePreference(this@MainActivity))
            }

            // Apply edge-to-edge styling based on current theme
            LaunchedEffect(isDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    },
                    navigationBarStyle = if (isDarkTheme) {
                        SystemBarStyle.dark(
                            scrim = android.graphics.Color.TRANSPARENT
                        )
                    } else {
                        SystemBarStyle.light(
                            scrim = android.graphics.Color.TRANSPARENT,
                            darkScrim = android.graphics.Color.TRANSPARENT
                        )
                    }
                )
            }

            QuoteMomentsTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QuoteApp(
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = {
                            isDarkTheme = !isDarkTheme
                            saveThemePreference(this@MainActivity, isDarkTheme)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteApp(
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current

    // State management
    var quoteData by remember { mutableStateOf<QuoteData?>(null) }
    var favorites by remember {
        mutableStateOf(getFavorites(context))
    }

    // Load quotes from JSON file
    LaunchedEffect(Unit) {
        try {
            val loadedData = loadQuotesFromAssets(context)
            quoteData = loadedData
        } catch (e: Exception) {
            // Handle error
        }
    }

    // Function to toggle favorite
    fun toggleFavorite(quote: Quote) {
        val newFavorites = if (favorites.any { it.text == quote.text }) {
            favorites.filter { it.text != quote.text }
        } else {
            favorites + quote
        }
        favorites = newFavorites
        saveFavorites(context, newFavorites)
    }

    Column {
        // Top App Bar
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

        // Main content
        when (currentScreen) {
            Screen.HOME -> {
                quoteData?.let { data ->
                    HomeScreen(
                        quoteData = data,
                        favorites = favorites,
                        onToggleFavorite = ::toggleFavorite,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Screen.FAVORITES -> {
                FavoritesScreen(
                    favorites = favorites,
                    onToggleFavorite = ::toggleFavorite,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Bottom Navigation
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    quoteData: QuoteData,
    favorites: List<Quote>,
    onToggleFavorite: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // State management
    var selectedCategory by remember { mutableStateOf("motivation") }
    var currentQuote by remember { mutableStateOf("Loading quotes...") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var filteredQuotes by remember { mutableStateOf<List<Quote>>(emptyList()) }

    // Set initial quote
    LaunchedEffect(quoteData, selectedCategory) {
        val quotes = quoteData.categories[selectedCategory] ?: emptyList()
        if (quotes.isNotEmpty()) {
            currentQuote = quotes[Random.nextInt(quotes.size)]
        }
    }

    // Function to get new quote from selected category
    fun getNewQuote() {
        val quotes = quoteData.categories[selectedCategory] ?: emptyList()
        if (quotes.isNotEmpty()) {
            var newQuote: String
            do {
                newQuote = quotes[Random.nextInt(quotes.size)]
            } while (newQuote == currentQuote && quotes.size > 1)
            currentQuote = newQuote
        }
    }

    // Function to change category
    fun changeCategory(category: String) {
        selectedCategory = category
        isSearchMode = false
        searchQuery = ""
        getNewQuote()
    }

    // Function to search quotes
    fun searchQuotes(query: String) {
        if (query.isBlank()) {
            filteredQuotes = emptyList()
            isSearchMode = false
            return
        }

        val results = mutableListOf<Quote>()
        quoteData.categories.forEach { (category, quotes) ->
            quotes.forEach { quote ->
                if (quote.contains(query, ignoreCase = true)) {
                    results.add(Quote(quote, category))
                }
            }
        }
        filteredQuotes = results
        isSearchMode = true
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    searchQuotes(it)
                },
                label = { Text("Search quotes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            searchQuotes("")
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { keyboardController?.hide() }
                )
            )
        }

        if (isSearchMode) {
            // Search Results
            if (filteredQuotes.isEmpty()) {
                item {
                    Text(
                        text = "No quotes found for \"$searchQuery\"",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(filteredQuotes) { quote ->
                    QuoteCard(
                        quote = quote,
                        isFavorite = favorites.any { it.text == quote.text },
                        onToggleFavorite = onToggleFavorite,
                        onShare = { shareQuote(context, quote.text, quote.category) }
                    )
                }
            }
        } else {
            // Regular Quote Display Mode
            item {
                // Subtitle
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

            item {
                // Category Selection
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    quoteData.categories.keys.forEach { category ->
                        FilterChip(
                            onClick = { changeCategory(category) },
                            label = {
                                Text(
                                    text = category.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selectedCategory == category
                        )
                    }
                }
            }

            item {
                // Current Quote Card
                val currentQuoteObj = Quote(currentQuote, selectedCategory)
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = currentQuote,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = { onToggleFavorite(currentQuoteObj) }
                            ) {
                                Icon(
                                    imageVector = if (favorites.any { it.text == currentQuote })
                                        Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (favorites.any { it.text == currentQuote })
                                        MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Category: ${selectedCategory.replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                // Action Buttons
                Column {
                    Button(
                        onClick = { getNewQuote() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "New Quote",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Get New Quote")
                    }

                    OutlinedButton(
                        onClick = { shareQuote(context, currentQuote, selectedCategory) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share Quote",
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Share This Quote")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    favorites: List<Quote>,
    onToggleFavorite: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // State for category filtering
    var selectedCategory by remember { mutableStateOf("all") }

    // Get unique categories from favorites
    val availableCategories = remember(favorites) {
        favorites.map { it.category }.distinct().sorted()
    }

    // Filter favorites based on selected category
    val filteredFavorites = remember(favorites, selectedCategory) {
        if (selectedCategory == "all") {
            favorites
        } else {
            favorites.filter { it.category == selectedCategory }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (favorites.isEmpty()) {
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
                    text = "Start adding quotes to your favorites from the home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        } else {
            // Category filter chips
            if (availableCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    // "All" chip
                    FilterChip(
                        onClick = { selectedCategory = "all" },
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
                            onClick = { selectedCategory = category },
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

            // Favorites count and category info
            Text(
                text = if (selectedCategory == "all") {
                    "${favorites.size} favorite quotes"
                } else {
                    "${filteredFavorites.size} favorite quotes from ${selectedCategory.replaceFirstChar { it.uppercase() }}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Favorites list
            if (filteredFavorites.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No favorites in ${selectedCategory.replaceFirstChar { it.uppercase() }} category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                LazyColumn {
                    items(filteredFavorites) { quote ->
                        QuoteCard(
                            quote = quote,
                            isFavorite = true,
                            onToggleFavorite = onToggleFavorite,
                            onShare = { shareQuote(context, quote.text, quote.category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuoteCard(
    quote: Quote,
    isFavorite: Boolean,
    onToggleFavorite: (Quote) -> Unit,
    onShare: () -> Unit
) {
    ElevatedCard(  // Changed from Card to ElevatedCard
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)  // Match main card elevation
    ) {
        Column(
            modifier = Modifier.padding(20.dp)  // Increased padding to match main card (was 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = quote.text,
                    style = MaterialTheme.typography.bodyLarge,  // Changed from bodyMedium to bodyLarge to match main card
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                IconButton(
                    onClick = { onToggleFavorite(quote) }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Toggle Favorite",
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))  // Add spacer like main card

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category: ${quote.category.replaceFirstChar { it.uppercase() }}", // Match main card format
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                IconButton(onClick = onShare) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Load quotes from JSON file in assets folder
 */
private suspend fun loadQuotesFromAssets(context: Context): QuoteData = withContext(Dispatchers.IO) {
    try {
        val inputStream = context.assets.open("quotes.json")
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        val jsonString = String(buffer, Charsets.UTF_8)
        val jsonObject = JSONObject(jsonString)
        val quotesObject = jsonObject.getJSONObject("quotes")

        val categories = mutableMapOf<String, List<String>>()

        quotesObject.keys().forEach { category ->
            val quotesArray = quotesObject.getJSONArray(category)
            val quotesList = mutableListOf<String>()

            for (i in 0 until quotesArray.length()) {
                quotesList.add(quotesArray.getString(i))
            }

            categories[category] = quotesList
        }

        QuoteData(categories)
    } catch (e: IOException) {
        throw Exception("Error reading quotes file: ${e.message}")
    }
}

/**
 * Function to share a quote using Android's share intent
 */
private fun shareQuote(context: Context, quote: String, category: String) {
    val shareText =
        "💭 $quote\n\n🏷️ Category: ${category.replaceFirstChar { it.uppercase() }}\n\n📱 Shared from Quote Moments"

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Inspirational Quote - ${category.replaceFirstChar { it.uppercase() }}")
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Share Quote")
    context.startActivity(chooserIntent)
}

// Favorites management functions
private fun getFavorites(context: Context): List<Quote> {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"

    return try {
        val jsonArray = org.json.JSONArray(favoritesJson)
        val favorites = mutableListOf<Quote>()

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            favorites.add(
                Quote(
                    text = item.getString("text"),
                    category = item.getString("category")
                )
            )
        }
        favorites
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveFavorites(context: Context, favorites: List<Quote>) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    val jsonArray = org.json.JSONArray()

    favorites.forEach { quote ->
        val jsonObject = org.json.JSONObject()
        jsonObject.put("text", quote.text)
        jsonObject.put("category", quote.category)
        jsonArray.put(jsonObject)
    }

    prefs.edit()
        .putString("favorites", jsonArray.toString())
        .apply()
}

// Theme management functions
private fun getThemePreference(context: Context): Boolean {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_dark_theme", false)
}

private fun saveThemePreference(context: Context, isDarkTheme: Boolean) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putBoolean("is_dark_theme", isDarkTheme)
        .apply()
}