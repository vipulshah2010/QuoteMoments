package com.quotemoments.data.model

data class Quote(
    val text: String,
    val category: String
)

data class QuoteData(
    val categories: Map<String, List<String>>
)