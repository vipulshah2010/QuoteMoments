package com.quotemoments.data.repository

import android.content.Context
import com.quotemoments.data.model.Quote
import com.quotemoments.data.model.QuoteData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class QuoteRepository(private val context: Context) {

    suspend fun loadQuotes(): QuoteData = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open("quotes.json")
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val jsonObject = JSONObject(jsonString)
        val quotesObject = jsonObject.getJSONObject("quotes")

        val categories = buildMap {
            quotesObject.keys().forEach { category ->
                val quotesArray = quotesObject.getJSONArray(category)
                val quotesList = List(quotesArray.length()) { i ->
                    quotesArray.getString(i)
                }
                put(category, quotesList)
            }
        }

        QuoteData(categories)
    }

    fun getRandomQuote(category: String, quotes: Map<String, List<String>>): String? {
        return quotes[category]?.randomOrNull()
    }

    fun getRandomQuoteExcluding(
        category: String,
        quotes: Map<String, List<String>>,
        exclude: String
    ): String? {
        val categoryQuotes = quotes[category] ?: return null  // ✅ FIXED TYPO
        return if (categoryQuotes.size > 1) {
            categoryQuotes.filter { it != exclude }.randomOrNull()
        } else {
            categoryQuotes.randomOrNull()
        }
    }

    fun searchQuotes(query: String, quotes: Map<String, List<String>>): List<Quote> {
        if (query.isBlank()) return emptyList()

        return buildList {
            quotes.forEach { (category, categoryQuotes) ->
                categoryQuotes.forEach { quote ->
                    if (quote.contains(query, ignoreCase = true)) {
                        add(Quote(quote, category))
                    }
                }
            }
        }
    }
}