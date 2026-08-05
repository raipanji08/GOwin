package com.panjirai0110.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

enum class GowinBrand { User, Admin }

/* ──────────────────────────────────────────────
 * User Light & Dark Schemes
 * ────────────────────────────────────────────── */

private val UserLightColorScheme = lightColorScheme(
    primary = UserLightPrimary,
    onPrimary = UserLightOnPrimary,
    primaryContainer = UserLightPrimaryContainer,
    onPrimaryContainer = UserLightOnPrimaryContainer,
    secondary = UserLightSecondary,
    onSecondary = UserLightOnSecondary,
    secondaryContainer = UserLightSecondaryContainer,
    onSecondaryContainer = UserLightOnSecondaryContainer,
    tertiary = UserLightTertiary,
    onTertiary = UserLightOnTertiary,
    tertiaryContainer = UserLightTertiaryContainer,
    onTertiaryContainer = UserLightOnTertiaryContainer,
    error = UserLightError,
    onError = UserLightOnError,
    errorContainer = UserLightErrorContainer,
    onErrorContainer = UserLightOnErrorContainer,
    background = UserLightBackground,
    onBackground = UserLightOnBackground,
    surface = UserLightSurface,
    onSurface = UserLightOnSurface,
    surfaceVariant = UserLightSurfaceVariant,
    onSurfaceVariant = UserLightOnSurfaceVariant,
    outline = UserLightOutline,
    outlineVariant = UserLightOutlineVariant,
    inverseSurface = UserLightInverseSurface,
    inverseOnSurface = UserLightInverseOnSurface,
)

private val UserDarkColorScheme = darkColorScheme(
    primary = UserDarkPrimary,
    onPrimary = UserDarkOnPrimary,
    primaryContainer = UserDarkPrimaryContainer,
    onPrimaryContainer = UserDarkOnPrimaryContainer,
    secondary = UserDarkSecondary,
    onSecondary = UserDarkOnSecondary,
    secondaryContainer = UserDarkSecondaryContainer,
    onSecondaryContainer = UserDarkOnSecondaryContainer,
    tertiary = UserDarkTertiary,
    onTertiary = UserDarkOnTertiary,
    tertiaryContainer = UserDarkTertiaryContainer,
    onTertiaryContainer = UserDarkOnTertiaryContainer,
    error = UserDarkError,
    onError = UserDarkOnError,
    errorContainer = UserDarkErrorContainer,
    onErrorContainer = UserDarkOnErrorContainer,
    background = UserDarkBackground,
    onBackground = UserDarkOnBackground,
    surface = UserDarkSurface,
    onSurface = UserDarkOnSurface,
    surfaceVariant = UserDarkSurfaceVariant,
    onSurfaceVariant = UserDarkOnSurfaceVariant,
    outline = UserDarkOutline,
    outlineVariant = UserDarkOutlineVariant,
    inverseSurface = UserDarkInverseSurface,
    inverseOnSurface = UserDarkInverseOnSurface,
)

/* ──────────────────────────────────────────────
 * Admin Light & Dark Schemes
 * ────────────────────────────────────────────── */

private val AdminLightColorScheme = lightColorScheme(
    primary = AdminLightPrimary,
    onPrimary = AdminLightOnPrimary,
    primaryContainer = AdminLightPrimaryContainer,
    onPrimaryContainer = AdminLightOnPrimaryContainer,
    secondary = AdminLightSecondary,
    onSecondary = AdminLightOnSecondary,
    secondaryContainer = AdminLightSecondaryContainer,
    onSecondaryContainer = AdminLightOnSecondaryContainer,
    tertiary = AdminLightTertiary,
    onTertiary = AdminLightOnTertiary,
    tertiaryContainer = AdminLightTertiaryContainer,
    onTertiaryContainer = AdminLightOnTertiaryContainer,
    error = AdminLightError,
    onError = AdminLightOnError,
    background = AdminLightBackground,
    onBackground = AdminLightOnBackground,
    surface = AdminLightSurface,
    onSurface = AdminLightOnSurface,
    surfaceVariant = AdminLightSurfaceVariant,
    onSurfaceVariant = AdminLightOnSurfaceVariant,
    outline = AdminLightOutline,
)

private val AdminDarkColorScheme = darkColorScheme(
    primary = AdminDarkPrimary,
    onPrimary = AdminDarkOnPrimary,
    primaryContainer = AdminDarkPrimaryContainer,
    onPrimaryContainer = AdminDarkOnPrimaryContainer,
    secondary = AdminDarkSecondary,
    onSecondary = AdminDarkOnSecondary,
    secondaryContainer = AdminDarkSecondaryContainer,
    onSecondaryContainer = AdminDarkOnSecondaryContainer,
    tertiary = AdminDarkTertiary,
    onTertiary = AdminDarkOnTertiary,
    tertiaryContainer = AdminDarkTertiaryContainer,
    onTertiaryContainer = AdminDarkOnTertiaryContainer,
    error = AdminDarkError,
    onError = AdminDarkOnError,
    background = AdminDarkBackground,
    onBackground = AdminDarkOnBackground,
    surface = AdminDarkSurface,
    onSurface = AdminDarkOnSurface,
    surfaceVariant = AdminDarkSurfaceVariant,
    onSurfaceVariant = AdminDarkOnSurfaceVariant,
    outline = AdminDarkOutline,
)

/* ──────────────────────────────────────────────
 * GowinTheme Composable
 * Dynamic color DISABLED for fixed brand colours.
 * ────────────────────────────────────────────── */

@Composable
fun GowinTheme(
    brand: GowinBrand = GowinBrand.User,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when (brand) {
        GowinBrand.User -> if (darkTheme) UserDarkColorScheme else UserLightColorScheme
        GowinBrand.Admin -> if (darkTheme) AdminDarkColorScheme else AdminLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = GowinTypography,
        content = content,
    )
}
