package it.togo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialThemeDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RoundedCornerSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tema TOGO — Entry point del Design System "High-Contrast Utility".
 *
 * Avvolge [MaterialTheme] fornendo:
 * - [TogoColorScheme] custom (Light/Dark) via [CompositionLocal]
 * - [TogoTypography] 5 ruoli via [CompositionLocal]
 * - [TogoSpacing] scala 4px/8px + touch target via [CompositionLocal]
 * - [TogoShapes] raggi angolo via [CompositionLocal]
 * - [TogoComponentTokens] 8 componenti via [CompositionLocal]
 *
 * Uso:
 * ```kotlin
 * TogoTheme {
 *     Surface { Text("Hello", style = typography.itemName, color = colors.inkPrimary) }
 * }
 * ```
 *
 * Cold-load: zero I/O, solo costanti → istantaneo.
 */
@Composable
fun TogoTheme(
    darkTheme: Boolean = androidx.compose.material3.isSystemInDarkTheme(),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val materialColorScheme = if (darkTheme) Material3DarkColorScheme else Material3LightColorScheme
    val togoColorScheme = togoColorSchemeFrom(darkTheme)
    val togoTypography = TogoTypography
    val togoSpacing = TogoSpacing
    val togoShapes = TogoShapes
    val togoComponentTokens = remember {
        TogoComponentTokens.from(togoColorScheme, togoShapes)
    }

    // Typography Material3 derivata dai token TOGO (per componenti Material3 che la usano internamente)
    val materialTypography = Typography(
        headlineMedium = togoTypography.titleScreen,
        titleMedium = togoTypography.sectionHeader,
        bodyLarge = togoTypography.itemName,
        labelLarge = togoTypography.itemMeta,
        bodySmall = togoTypography.caption,
        // Ruoli Material3 non usati da TOGO: fallback a default
        displayLarge = MaterialThemeDefaults.typography.displayLarge,
        displayMedium = MaterialThemeDefaults.typography.displayMedium,
        displaySmall = MaterialThemeDefaults.typography.displaySmall,
        headlineLarge = MaterialThemeDefaults.typography.headlineLarge,
        headlineSmall = MaterialThemeDefaults.typography.headlineSmall,
        titleLarge = MaterialThemeDefaults.typography.titleLarge,
        titleSmall = MaterialThemeDefaults.typography.titleSmall,
        bodyMedium = MaterialThemeDefaults.typography.bodyMedium,
        bodySmall = MaterialThemeDefaults.typography.bodySmall,
        labelMedium = MaterialThemeDefaults.typography.labelMedium,
        labelSmall = MaterialThemeDefaults.typography.labelSmall,
    )

    // Shapes Material3 derivati dai token TOGO
    val materialShapes = androidx.compose.material3.Shapes(
        extraSmall = togoShapes.none,
        small = togoShapes.sm,
        medium = togoShapes.md,
        large = togoShapes.lg,
        extraLarge = togoShapes.lg, // Chip/altri usano lg (12dp), non full (cerchio)
    )

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
            contentAlignment = contentAlignment,
            content = content,
        )
    }
}

/**
 * Accesso rapido ai colori TOGO dal contesto composable.
 * Equivalente a `MaterialTheme.colorScheme` ma per token TOGO.
 */
@Composable
fun togoColors(): TogoColorScheme = LocalTogoColorScheme.current

/**
 * Accesso rapido alla tipografia TOGO.
 */
@Composable
fun togoTypography(): TogoTypography = LocalTogoTypography.current

/**
 * Accesso rapido allo spacing TOGO.
 */
@Composable
fun togoSpacing(): TogoSpacing = LocalTogoSpacing.current

/**
 * Accesso rapido alle shape TOGO.
 */
@Composable
fun togoShapes(): TogoShapes = LocalTogoShapes.current

/**
 * Accesso rapido ai token component TOGO.
 */
@Composable
fun togoComponentTokens(): TogoComponentTokens = LocalTogoComponentTokens.current