package com.quotemoments.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val QuoteLightColorScheme = lightColorScheme(
    primary = QuotePrimary,
    onPrimary = QuoteOnPrimary,
    primaryContainer = QuotePrimaryContainer,
    onPrimaryContainer = QuoteOnPrimaryContainer,
    secondary = QuoteSecondary,
    onSecondary = QuoteOnSecondary,
    secondaryContainer = QuoteSecondaryContainer,
    onSecondaryContainer = QuoteOnSecondaryContainer,
    tertiary = QuoteTertiary,
    onTertiary = QuoteOnTertiary,
    tertiaryContainer = QuoteTertiaryContainer,
    onTertiaryContainer = QuoteOnTertiaryContainer,
    error = QuoteError,
    errorContainer = QuoteErrorContainer,
    onError = QuoteOnError,
    onErrorContainer = QuoteOnErrorContainer,
    background = QuoteBackground,
    onBackground = QuoteOnBackground,
    surface = QuoteSurface,
    onSurface = QuoteOnSurface,
    surfaceVariant = QuoteSurfaceVariant,
    onSurfaceVariant = QuoteOnSurfaceVariant,
    outline = QuoteOutline,
    inverseOnSurface = QuoteInverseOnSurface,
    inverseSurface = QuoteInverseSurface,
    inversePrimary = QuoteInversePrimary,
    // Additional surface colors for better appearance
    surfaceContainer = QuoteSurfaceContainer,
    surfaceContainerHigh = QuoteSurfaceContainerHigh,
)

private val QuoteDarkColorScheme = darkColorScheme(
    primary = QuotePrimaryDark,
    onPrimary = QuoteOnPrimaryDark,
    primaryContainer = QuotePrimaryContainerDark,
    onPrimaryContainer = QuoteOnPrimaryContainerDark,
    secondary = QuoteSecondaryDark,
    onSecondary = QuoteOnSecondaryDark,
    secondaryContainer = QuoteSecondaryContainerDark,
    onSecondaryContainer = QuoteOnSecondaryContainerDark,
    tertiary = QuoteTertiaryDark,
    onTertiary = QuoteOnTertiaryDark,
    tertiaryContainer = QuoteTertiaryContainerDark,
    onTertiaryContainer = QuoteOnTertiaryContainerDark,
    error = QuoteErrorDark,
    errorContainer = QuoteErrorContainerDark,
    onError = QuoteOnErrorDark,
    onErrorContainer = QuoteOnErrorContainerDark,
    background = QuoteBackgroundDark,
    onBackground = QuoteOnBackgroundDark,
    surface = QuoteSurfaceDark,
    onSurface = QuoteOnSurfaceDark,
    surfaceVariant = QuoteSurfaceVariantDark,
    onSurfaceVariant = QuoteOnSurfaceVariantDark,
    outline = QuoteOutlineDark,
    inverseOnSurface = QuoteInverseOnSurfaceDark,
    inverseSurface = QuoteInverseSurfaceDark,
    inversePrimary = QuoteInversePrimaryDark,
    // Additional surface colors for better appearance
    surfaceContainer = QuoteSurfaceContainerDark,
    surfaceContainerHigh = QuoteSurfaceContainerHighDark,
)

@Composable
fun QuoteMomentsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Set to false to always use custom colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> QuoteDarkColorScheme
        else -> QuoteLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QuoteTypography,
        content = content
    )
}