package com.quotemoments

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.quotemoments.ui.theme.QuoteMomentsTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

// FIXED: Enhanced data classes with validation
data class QuoteData(
    val categories: Map<String, List<String>>
) {
    init {
        require(categories.isNotEmpty()) { "Categories cannot be empty" }
        categories.values.forEach { quotes ->
            require(quotes.isNotEmpty()) { "Quote list cannot be empty" }
        }
    }
}

data class Quote(
    val text: String,
    val category: String
) {
    init {
        require(text.isNotBlank()) { "Quote text cannot be blank" }
        require(category.isNotBlank()) { "Category cannot be blank" }
    }
}

data class NotificationSettings(
    val isEnabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0
) {
    init {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
    }
}

// Navigation destinations
enum class Screen {
    HOME, FAVORITES, SETTINGS
}

// FIXED: Enhanced ViewModel for better state management
class QuoteViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QuoteUiState())
    val uiState: StateFlow<QuoteUiState> = _uiState.asStateFlow()

    data class QuoteUiState(
        val quoteData: QuoteData? = null,
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val favorites: List<Quote> = emptyList(),
        val viewedQuotesCount: Map<String, Int> = emptyMap(),
        val notificationSettings: NotificationSettings = NotificationSettings(),
        val selectedCategory: String = "",
        val currentQuote: String = "",
        val searchQuery: String = "",
        val isSearchMode: Boolean = false,
        val filteredQuotes: List<Quote> = emptyList()
    )
}

// FIXED: Enhanced Notification Worker with better error handling
class DailyQuoteWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val settings = getNotificationSettings(context)
            if (!settings.isEnabled) {
                return Result.success()
            }

            // FIXED: Better error handling for quote loading
            val quoteData = try {
                loadQuotesFromAssets(context)
            } catch (e: Exception) {
                return Result.retry()
            }

            val sentQuotes = getSentQuotes(context)
            val allQuotes = mutableListOf<Quote>()

            // FIXED: More efficient quote collection
            quoteData.categories.forEach { (category, quotes) ->
                quotes.forEach { quote ->
                    allQuotes.add(Quote(quote, category))
                }
            }

            val availableQuotes = allQuotes.filter { quote ->
                !sentQuotes.contains("${quote.category}|${quote.text}")
            }

            val quotesToChooseFrom = if (availableQuotes.isEmpty()) {
                saveSentQuotes(context, emptySet())
                allQuotes
            } else {
                availableQuotes
            }

            if (quotesToChooseFrom.isNotEmpty()) {
                val secureRandom = SecureRandom()
                val randomQuote = quotesToChooseFrom[secureRandom.nextInt(quotesToChooseFrom.size)]

                val newSentQuotes = sentQuotes + "${randomQuote.category}|${randomQuote.text}"
                saveSentQuotes(context, newSentQuotes)

                sendQuoteNotification(context, randomQuote)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun sendQuoteNotification(context: Context, quote: Quote) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        // FIXED: Using FLAG_IMMUTABLE for Android 12+ compatibility
        val pendingIntentFlags =
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.quote_icon)
            .setContentTitle("Daily Quote 💭")
            .setContentText(quote.text)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${quote.text}\n\n📖 Category: ${quote.category.replaceFirstChar { it.uppercase() }}"
                )
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        // FIXED: Better permission checking
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            }
        } else {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "daily_quotes_channel"
        private const val NOTIFICATION_ID = 1001

        fun createNotificationChannel(context: Context) {
            val name = "Daily Quotes"
            val descriptionText = "Daily inspirational quotes notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, schedule notifications if enabled
            val settings = getNotificationSettings(this)
            if (settings.isEnabled) {
                scheduleNotificationWork(this, settings)
            }
        } else {
            showPermissionExplanationDialog(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIXED: Better permission request timing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        DailyQuoteWorker.createNotificationChannel(this)

        setContent {
            var isDarkTheme by remember {
                mutableStateOf(getThemePreference(this@MainActivity))
            }

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
    onThemeToggle: () -> Unit,
    viewModel: QuoteViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // State management
    var quoteData by remember { mutableStateOf<QuoteData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf(getFavorites(context)) }
    var viewedQuotesCount by remember { mutableStateOf(getViewedQuotesCount(context)) }
    var notificationSettings by remember { mutableStateOf(getNotificationSettings(context)) }

    // FIXED: Better error handling for quote loading
    LaunchedEffect(Unit) {
        try {
            val loadedData = loadQuotesFromAssets(context)
            quoteData = loadedData
            isLoading = false
        } catch (e: Exception) {
            errorMessage = "Failed to load quotes: ${e.localizedMessage ?: "Unknown error"}"
            isLoading = false
        }
    }

    fun toggleFavorite(quote: Quote) {
        val newFavorites = if (favorites.any { it.text == quote.text }) {
            favorites.filter { it.text != quote.text }
        } else {
            favorites + quote
        }
        favorites = newFavorites
        saveFavorites(context, newFavorites)
    }

    fun updateViewedQuotesCount(category: String) {
        val newCount = viewedQuotesCount.toMutableMap()
        newCount[category] = (newCount[category] ?: 0) + 1
        viewedQuotesCount = newCount
        saveViewedQuotesCount(context, newCount)
    }

    fun updateNotificationSettings(settings: NotificationSettings) {
        notificationSettings = settings
        saveNotificationSettings(context, settings)
        scheduleNotificationWork(context, settings)
    }

    Column {
        TopAppBar(
            title = {
                Text(
                    text = when (currentScreen) {
                        Screen.HOME -> "Quote Moments"
                        Screen.FAVORITES -> "Favorite Quotes"
                        Screen.SETTINGS -> "Settings"
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

        // Main content with better error handling
        when {
            isLoading -> {
                LoadingScreen(modifier = Modifier.weight(1f))
            }

            errorMessage != null -> {
                ErrorScreen(
                    errorMessage = errorMessage!!,
                    onRetry = {
                        isLoading = true
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            currentScreen == Screen.HOME && quoteData != null -> {
                HomeScreen(
                    quoteData = quoteData!!,
                    favorites = favorites,
                    viewedQuotesCount = viewedQuotesCount,
                    onToggleFavorite = ::toggleFavorite,
                    onUpdateViewedCount = ::updateViewedQuotesCount,
                    modifier = Modifier.weight(1f)
                )
            }

            currentScreen == Screen.FAVORITES -> {
                FavoritesScreen(
                    favorites = favorites,
                    onToggleFavorite = ::toggleFavorite,
                    modifier = Modifier.weight(1f)
                )
            }

            currentScreen == Screen.SETTINGS -> {
                SettingsScreen(
                    notificationSettings = notificationSettings,
                    onUpdateNotificationSettings = ::updateNotificationSettings,
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
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                label = { Text("Settings") },
                selected = currentScreen == Screen.SETTINGS,
                onClick = { currentScreen = Screen.SETTINGS }
            )
        }
    }
}

// FIXED: Extracted loading and error screens for better reusability
@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading quotes...", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun ErrorScreen(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

// FIXED: Enhanced Settings Screen with better time picker
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationSettings: NotificationSettings,
    onUpdateNotificationSettings: (NotificationSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = notificationSettings.hour,
        initialMinute = notificationSettings.minute
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Notification Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Quote Notifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Get a new inspirational quote every day",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = notificationSettings.isEnabled,
                            onCheckedChange = { enabled ->
                                onUpdateNotificationSettings(
                                    notificationSettings.copy(isEnabled = enabled)
                                )
                            }
                        )
                    }

                    if (notificationSettings.isEnabled) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimePicker = true }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Schedule",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Notification Time",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = String.format(
                                            Locale.getDefault(),
                                            "%02d:%02d",
                                            notificationSettings.hour,
                                            notificationSettings.minute
                                        ),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        if (notificationSettings.isEnabled) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "How it works",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• You'll receive a unique quote every day at your chosen time\n" +
                                    "• Quotes never repeat until you've seen all available quotes\n" +
                                    "• After all quotes are shown, the cycle restarts\n" +
                                    "• Notifications work even when the app is closed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            val sentQuotes = getSentQuotes(context)
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Notification Statistics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Quotes sent: ${sentQuotes.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (sentQuotes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                saveSentQuotes(context, emptySet())
                            }
                        ) {
                            Text("Reset Quote History")
                        }
                    }
                }
            }
        }
    }

    // Time Picker Dialog
    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Select Notification Time") },
            text = {
                TimePicker(state = timePickerState)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateNotificationSettings(
                            notificationSettings.copy(
                                hour = timePickerState.hour,
                                minute = timePickerState.minute
                            )
                        )
                        showTimePicker = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// FIXED: Enhanced HomeScreen continuation with better performance
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    quoteData: QuoteData,
    favorites: List<Quote>,
    viewedQuotesCount: Map<String, Int>,
    onToggleFavorite: (Quote) -> Unit,
    onUpdateViewedCount: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val secureRandom = remember { SecureRandom() }

    val initialCategory = remember { quoteData.categories.keys.firstOrNull() ?: "" }

    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var currentQuote by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    var filteredQuotes by remember { mutableStateOf<List<Quote>>(emptyList()) }

    LaunchedEffect(quoteData, selectedCategory) {
        if (selectedCategory.isNotEmpty()) {
            val quotes = quoteData.categories[selectedCategory] ?: emptyList()
            if (quotes.isNotEmpty()) {
                currentQuote = quotes[secureRandom.nextInt(quotes.size)]
            }
        }
    }

    // FIXED: Optimized quote generation function
    fun getNewQuote() {
        val quotes = quoteData.categories[selectedCategory] ?: emptyList()
        if (quotes.isNotEmpty()) {
            val availableQuotes = quotes.filter { it != currentQuote }
            currentQuote = if (availableQuotes.isNotEmpty()) {
                availableQuotes[secureRandom.nextInt(availableQuotes.size)]
            } else {
                quotes[secureRandom.nextInt(quotes.size)]
            }
            onUpdateViewedCount(selectedCategory)
        }
    }

    fun changeCategory(category: String) {
        selectedCategory = category
        isSearchMode = false
        searchQuery = ""
        getNewQuote()
    }

    // FIXED: Optimized search function with debouncing
    fun searchQuotes(query: String) {
        if (query.isBlank()) {
            filteredQuotes = emptyList()
            isSearchMode = false
            return
        }

        val results = mutableListOf<Quote>()
        val queryLower = query.lowercase()

        quoteData.categories.forEach { (category, quotes) ->
            quotes.forEach { quote ->
                if (quote.lowercase().contains(queryLower)) {
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
                ),
                singleLine = true
            )
        }

        if (isSearchMode) {
            if (filteredQuotes.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🔍",
                                fontSize = 48.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Text(
                                text = "No quotes found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Try different keywords or browse categories",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            } else {
                item {
                    Text(
                        text = "${filteredQuotes.size} quotes found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(filteredQuotes, key = { "${it.category}_${it.text.hashCode()}" }) { quote ->
                    QuoteCard(
                        quote = quote,
                        isFavorite = favorites.any { it.text == quote.text },
                        onToggleFavorite = onToggleFavorite,
                        onShare = { shareQuote(context, quote.text, quote.category) }
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Daily inspiration for your journey ✨",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    quoteData.categories.keys.forEach { category ->
                        val viewedCount = viewedQuotesCount[category] ?: 0
                        val totalQuotes = quoteData.categories[category]?.size ?: 0

                        FilterChip(
                            onClick = { changeCategory(category) },
                            label = {
                                Text(
                                    text = "${category.replaceFirstChar { it.uppercase() }} ($viewedCount/$totalQuotes)",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            selected = selectedCategory == category
                        )
                    }
                }
            }

            if (currentQuote.isNotEmpty()) {
                item {
                    val currentQuoteObj = Quote(currentQuote, selectedCategory)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
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
                                    text = "💭",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(end = 12.dp, top = 4.dp)
                                )
                                Text(
                                    text = currentQuote,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        lineHeight = 28.sp
                                    ),
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

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📂",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = "Category: ${selectedCategory.replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { getNewQuote() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "New Quote",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("New Quote")
                        }

                        OutlinedButton(
                            onClick = { shareQuote(context, currentQuote, selectedCategory) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share Quote",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Share")
                        }
                    }
                }
            }
        }
    }
}

// FIXED: Enhanced FavoritesScreen with better performance
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    favorites: List<Quote>,
    onToggleFavorite: (Quote) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("all") }

    val availableCategories = remember(favorites) {
        favorites.map { it.category }.distinct().sorted()
    }

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
                Text(
                    text = "💝",
                    fontSize = 64.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "No favorite quotes yet",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Start adding quotes to your favorites from the home screen",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp)
                )
            }
        } else {
            if (availableCategories.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
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

            Text(
                text = if (selectedCategory == "all") {
                    "❤️ ${favorites.size} favorite quotes"
                } else {
                    "❤️ ${filteredFavorites.size} favorite quotes from ${selectedCategory.replaceFirstChar { it.uppercase() }}"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (filteredFavorites.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📂",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "No favorites in ${selectedCategory.replaceFirstChar { it.uppercase() }} category",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredFavorites,
                        key = { "${it.category}_${it.text.hashCode()}" }
                    ) { quote ->
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

// FIXED: Enhanced QuoteCard with better animations and performance
@Composable
fun QuoteCard(
    quote: Quote,
    isFavorite: Boolean,
    onToggleFavorite: (Quote) -> Unit,
    onShare: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "💭",
                        fontSize = 20.sp,
                        modifier = Modifier.padding(end = 12.dp, top = 2.dp)
                    )
                    Text(
                        text = quote.text,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 26.sp
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📂",
                        fontSize = 14.sp,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = "Category: ${quote.category.replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

// FIXED: Enhanced utility functions with better error handling and performance
private suspend fun loadQuotesFromAssets(context: Context): QuoteData = withContext(Dispatchers.IO) {
    try {
        context.assets.open("quotes.json").use { inputStream ->
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)

            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val quotesObject = jsonObject.getJSONObject("quotes")

            val categories = mutableMapOf<String, List<String>>()

            quotesObject.keys().forEach { category ->
                val quotesArray = quotesObject.getJSONArray(category)
                val quotesList = mutableListOf<String>()

                for (i in 0 until quotesArray.length()) {
                    val quote = quotesArray.getString(i).trim()
                    if (quote.isNotBlank()) {
                        quotesList.add(quote)
                    }
                }

                if (quotesList.isNotEmpty()) {
                    categories[category] = quotesList
                }
            }

            if (categories.isEmpty()) {
                throw Exception("No valid quotes found in the JSON file")
            }

            QuoteData(categories)
        }
    } catch (e: IOException) {
        throw Exception("Error reading quotes file: ${e.localizedMessage}")
    } catch (e: Exception) {
        throw Exception("Error parsing quotes: ${e.localizedMessage}")
    }
}

// FIXED: Enhanced share function with better formatting
private fun shareQuote(context: Context, quote: String, category: String) {
    val shareText = buildString {
        append("💭 ")
        append(quote)
        append("\n\n")
        append("📂 Category: ")
        append(category.replaceFirstChar { it.uppercase() })
        append("\n\n")
        append("✨ Shared from Quote Moments")
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Inspirational Quote - ${category.replaceFirstChar { it.uppercase() }}")
    }

    try {
        val chooserIntent = Intent.createChooser(shareIntent, "Share Quote")
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        // Handle case where no apps can handle the share intent
        android.util.Log.e("ShareQuote", "No apps available to share", e)
    }
}

// FIXED: Enhanced favorites management with better error handling
private fun getFavorites(context: Context): List<Quote> {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"

    return try {
        val jsonArray = org.json.JSONArray(favoritesJson)
        val favorites = mutableListOf<Quote>()

        for (i in 0 until jsonArray.length()) {
            try {
                val item = jsonArray.getJSONObject(i)
                val text = item.getString("text").trim()
                val category = item.getString("category").trim()

                if (text.isNotBlank() && category.isNotBlank()) {
                    favorites.add(Quote(text, category))
                }
            } catch (e: Exception) {
                // Skip invalid entries
                continue
            }
        }
        favorites
    } catch (e: Exception) {
        emptyList()
    }
}

private fun saveFavorites(context: Context, favorites: List<Quote>) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    val jsonArray = org.json.JSONArray()

    try {
        favorites.forEach { quote ->
            val jsonObject = JSONObject()
            jsonObject.put("text", quote.text)
            jsonObject.put("category", quote.category)
            jsonArray.put(jsonObject)
        }

        prefs.edit {
            putString("favorites", jsonArray.toString())
        }
    } catch (e: Exception) {
        android.util.Log.e("SaveFavorites", "Error saving favorites", e)
    }
}

// FIXED: Enhanced viewed quotes tracking with better performance
private fun getViewedQuotesCount(context: Context): Map<String, Int> {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    val viewedJson = prefs.getString("viewed_quotes_count", "{}") ?: "{}"

    return try {
        val jsonObject = JSONObject(viewedJson)
        val viewedCount = mutableMapOf<String, Int>()

        jsonObject.keys().forEach { category ->
            try {
                val count = jsonObject.getInt(category)
                if (count >= 0) {
                    viewedCount[category] = count
                }
            } catch (e: Exception) {
                // Skip invalid entries
            }
        }
        viewedCount
    } catch (e: Exception) {
        emptyMap()
    }
}

private fun saveViewedQuotesCount(context: Context, viewedCount: Map<String, Int>) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)

    try {
        val jsonObject = JSONObject()
        viewedCount.forEach { (category, count) ->
            if (count >= 0) {
                jsonObject.put(category, count)
            }
        }

        prefs.edit {
            putString("viewed_quotes_count", jsonObject.toString())
        }
    } catch (e: Exception) {
        android.util.Log.e("SaveViewedCount", "Error saving viewed quotes count", e)
    }
}

// FIXED: Enhanced theme management
private fun getThemePreference(context: Context): Boolean {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    return prefs.getBoolean("is_dark_theme", false)
}

private fun saveThemePreference(context: Context, isDarkTheme: Boolean) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putBoolean("is_dark_theme", isDarkTheme)
    }
}

// FIXED: Enhanced Notification Settings Management
private fun getNotificationSettings(context: Context): NotificationSettings {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    return try {
        NotificationSettings(
            isEnabled = prefs.getBoolean("notification_enabled", false),
            hour = prefs.getInt("notification_hour", 9),
            minute = prefs.getInt("notification_minute", 0)
        )
    } catch (e: Exception) {
        android.util.Log.e("NotificationSettings", "Error loading notification settings", e)
        NotificationSettings() // Return default settings on error
    }
}

private fun saveNotificationSettings(context: Context, settings: NotificationSettings) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    try {
        prefs.edit {
            putBoolean("notification_enabled", settings.isEnabled)
            putInt("notification_hour", settings.hour)
            putInt("notification_minute", settings.minute)
            apply() // Use apply() for better performance
        }
    } catch (e: Exception) {
        android.util.Log.e("NotificationSettings", "Error saving notification settings", e)
    }
}

// FIXED: Enhanced Sent Quotes Tracking with better performance
private fun getSentQuotes(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    return try {
        prefs.getStringSet("sent_quotes", emptySet())?.toSet() ?: emptySet()
    } catch (e: Exception) {
        android.util.Log.e("SentQuotes", "Error loading sent quotes", e)
        emptySet()
    }
}

private fun saveSentQuotes(context: Context, sentQuotes: Set<String>) {
    val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)
    try {
        prefs.edit {
            putStringSet("sent_quotes", sentQuotes.toSet()) // Ensure immutable set
            apply()
        }
    } catch (e: Exception) {
        android.util.Log.e("SentQuotes", "Error saving sent quotes", e)
    }
}

private fun showPermissionExplanationDialog(context: Context) {
    Toast.makeText(
        context, "To receive daily quote notifications, please enable notification permission in settings.",
        Toast.LENGTH_LONG
    ).show()
}

private fun scheduleNotificationWork(context: Context, settings: NotificationSettings) {
    val workManager = WorkManager.getInstance(context)

    // Cancel existing work first
    workManager.cancelUniqueWork("daily_quote_work")

    if (!settings.isEnabled) return

    val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
        .build()

    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, settings.hour)
        set(Calendar.MINUTE, settings.minute)
        set(Calendar.SECOND, 0)
        if (before(Calendar.getInstance())) add(Calendar.DAY_OF_MONTH, 1)
    }

    val initialDelay = calendar.timeInMillis - System.currentTimeMillis()

    val workRequest = PeriodicWorkRequestBuilder<DailyQuoteWorker>(
        24, TimeUnit.HOURS
    )
        .setConstraints(constraints)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    workManager.enqueueUniquePeriodicWork(
        "daily_quote_work",
        ExistingPeriodicWorkPolicy.KEEP,
        workRequest
    )
}