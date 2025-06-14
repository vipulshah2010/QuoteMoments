package com.quotemoments.data.local

import android.content.Context
import androidx.core.content.edit
import com.quotemoments.data.model.Quote
import org.json.JSONArray
import org.json.JSONObject

class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("quote_prefs", Context.MODE_PRIVATE)

    fun getFavorites(): List<Quote> {
        val favoritesJson = prefs.getString("favorites", "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(favoritesJson)
            List(jsonArray.length()) { i ->
                val item = jsonArray.getJSONObject(i)
                Quote(
                    text = item.getString("text"),
                    category = item.getString("category")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveFavorites(favorites: List<Quote>) {
        val jsonArray = JSONArray().apply {
            favorites.forEach { quote ->
                put(JSONObject().apply {
                    put("text", quote.text)
                    put("category", quote.category)
                })
            }
        }
        prefs.edit {
            putString("favorites", jsonArray.toString())
        }
    }

    fun getThemePreference(): Boolean {
        return prefs.getBoolean("is_dark_theme", false)
    }

    fun saveThemePreference(isDarkTheme: Boolean) {
        prefs.edit {
            putBoolean("is_dark_theme", isDarkTheme)
        }
    }
}