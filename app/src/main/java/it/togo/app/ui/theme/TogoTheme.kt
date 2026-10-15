package it.togo.app.ui.theme

import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.provides
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Tema TOGO — Entry point del Design System "High-Contrast Utility".
 *
 * Avvolge [MaterialTheme] fornendo:
 * - [TogoColorScheme] custom (Light/Dark) via [CompositionLocal]
 * - [TogoTypographyTokens] 5 ruoli via [CompositionLocal]
 * - [TogoSpacingTokens] scala 4px/8px + touch target via [CompositionLocal]
 * - [TogoShapesTokens] raggi angolo via [CompositionLocal]
 * - [TogoComponentTokens] 8 componenti via [CompositionLocal]
 *
 * Uso:
 * ```kotlin
 * TogoTheme(darkTheme = isSystemDarkTheme()) {
 *     Surface { Text("Hello", style = typography.itemName, color = colors.inkPrimary) }
 * }
 * ```
 *
 * Cold-load: zero I/O, solo costanti → istantaneo.
 */
@Composable
fun TogoTheme(
    // darkTheme senza default: il caller decide (evita dipendenza da isSystemInDarkTheme)
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val materialColorScheme = if (darkTheme) Material3DarkColorScheme else Material3LightColorScheme
    val togoColorScheme: TogoColorScheme = togoColorSchemeFrom(darkTheme)
    val togoTypography: TogoTypographyTokens = TogoTypography
    val togoSpacing: TogoSpacingTokens = TogoSpacing
    val togoShapes: TogoShapesTokens = TogoShapes
    val togoComponentTokens = remember {
        togoComponentTokensFrom(togoColorScheme, togoShapes)
    }

    // Typography Material3 derivata dai token TOGO
    val materialTypography = Typography(
        headlineMedium = togoTypography.titleScreen,
        titleMedium = togoTypography.sectionHeader,
        bodyLarge = togoTypography.itemName,
        labelLarge = togoTypography.itemMeta,
        bodySmall = togoTypography.caption,
    )

    // Shapes Material3 derivati dai token TOGO
    val materialShapes = androidx.compose.material3.Shapes(
        extraSmall = togoShapes.none,
        small = togoShapes.sm,
        medium = togoShapes.md,
        large = togoShapes.lg,
        extraLarge = togoShapes.lg,
    )

    // CompositionLocalProvider usando infix 'provides' (API pubblica standard)
    CompositionLocalProvider(
        LocalTogoColorScheme provides togoColorScheme,
        LocalTogoTypography provides togoTypography,
        LocalTogoSpacing provides togoSpacing,
        LocalTogoShapes provides togoShapes,
        LocalTogoComponentTokens provides togoComponentTokens,
    ) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            typography = materialTypography,
            shapes = materialShapes,
            content = content,
        )
    }
}

/**
 * Helper per calcolare darkTheme da configurazione di sistema.
 * Sostituisce [isSystemInDarkTheme] per compatibilità versioni Compose.
 * Usa in: `TogoTheme(darkTheme = isSystemDarkTheme())`
 */
@Composable
fun isSystemDarkTheme(): Boolean {
    val config = LocalConfiguration.current
    return (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
}