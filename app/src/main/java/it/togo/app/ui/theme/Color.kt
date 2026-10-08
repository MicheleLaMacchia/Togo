package it.togo.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Token colore TOGO — High-Contrast Utility (da DESIGN.md front-matter).
 *
 * Light Mode: contrasto ≥12:1 su surface-base (#FFFFFF) per ink-primary (#0F172A).
 * Dark Mode: surface-base-dark (#0B0F17), ink-primary-dark (#F8FAFC).
 *
 * Tutti i token sono definiti esplicitamente per entrambi i temi;
 * niente eredità automatica da Material3.
 */
object TogoColorTokens {

    // --- Light Mode ---
    val LightSurfaceBase = Color(0xFFFFFFFF)       // #FFFFFF
    val LightSurfaceSubtle = Color(0xFFF8FAFC)     // #F8FAFC
    val LightSurfaceCard = Color(0xFFFFFFFF)       // #FFFFFF
    val LightSurfaceInverse = Color(0xFF0F172A)    // #0F172A
    val LightInkPrimary = Color(0xFF0F172A)        // #0F172A
    val LightInkSecondary = Color(0xFF475569)      // #475569
    val LightInkMuted = Color(0xFF94A3B8)          // #94A3B8
    val LightInkInverse = Color(0xFFFFFFFF)        // #FFFFFF
    val LightBorderCrisp = Color(0xFF0F172A)       // #0F172A
    val LightBorderHairline = Color(0xFFCBD5E1)    // #CBD5E1
    val LightAccentAction = Color(0xFF0F172A)      // #0F172A
    val LightAccentSuccess = Color(0xFF15803D)     // #15803D
    val LightAccentHighlight = Color(0xFF2563EB)   // #2563EB
    val LightAccentWarning = Color(0xFFB45309)     // #B45309
    val LightBadgeBg = Color(0xFF0F172A)           // #0F172A
    val LightBadgeInk = Color(0xFFFFFFFF)          // #FFFFFF

    // --- Dark Mode ---
    val DarkSurfaceBase = Color(0xFF0B0F17)        // #0B0F17
    val DarkSurfaceCard = Color(0xFF131B2E)        // #131B2E
    val DarkInkPrimary = Color(0xFFF8FAFC)         // #F8FAFC
    val DarkInkSecondary = Color(0xFF94A3B8)       // #94A3B8
    val DarkBorderCrisp = Color(0xFF475569)        // #475569
    val DarkAccentHighlight = Color(0xFF60A5FA)    // #60A5FA

    // Token condivisi (identici in entrambi i temi)
    val AccentSuccess = LightAccentSuccess
    val AccentWarning = LightAccentWarning
    val BadgeBg = LightBadgeBg
    val BadgeInk = LightBadgeInk
}

/** Costruisce la [ColorScheme] Material3 Light per TOGO. */
private val LightTogoColorScheme: ColorScheme = lightColorScheme(
    primary = TogoColorTokens.LightInkPrimary,
    onPrimary = TogoColorTokens.LightInkInverse,
    primaryContainer = TogoColorTokens.LightSurfaceSubtle,
    onPrimaryContainer = TogoColorTokens.LightInkPrimary,
    secondary = TogoColorTokens.LightAccentHighlight,
    onSecondary = TogoColorTokens.LightInkInverse,
    secondaryContainer = TogoColorTokens.LightSurfaceSubtle,
    onSecondaryContainer = TogoColorTokens.LightInkPrimary,
    tertiary = TogoColorTokens.LightAccentSuccess,
    onTertiary = TogoColorTokens.LightInkInverse,
    tertiaryContainer = TogoColorTokens.LightSurfaceSubtle,
    onTertiaryContainer = TogoColorTokens.LightInkPrimary,
    error = TogoColorTokens.LightAccentWarning,
    onError = TogoColorTokens.LightInkInverse,
    errorContainer = TogoColorTokens.LightSurfaceSubtle,
    onErrorContainer = TogoColorTokens.LightInkPrimary,
    background = TogoColorTokens.LightSurfaceBase,
    onBackground = TogoColorTokens.LightInkPrimary,
    surface = TogoColorTokens.LightSurfaceCard,
    onSurface = TogoColorTokens.LightInkPrimary,
    surfaceVariant = TogoColorTokens.LightSurfaceSubtle,
    onSurfaceVariant = TogoColorTokens.LightInkSecondary,
    outline = TogoColorTokens.LightBorderCrisp,
    outlineVariant = TogoColorTokens.LightBorderHairline,
    shadow = TogoColorTokens.LightInkPrimary,
    scrim = TogoColorTokens.LightInkPrimary,
    inverseSurface = TogoColorTokens.LightSurfaceInverse,
    onInverseSurface = TogoColorTokens.LightInkInverse,
    inversePrimary = TogoColorTokens.LightInkInverse,
)

/** Costruisce la [ColorScheme] Material3 Dark per TOGO. */
private val DarkTogoColorScheme: ColorScheme = darkColorScheme(
    primary = TogoColorTokens.DarkInkPrimary,
    onPrimary = TogoColorTokens.DarkSurfaceBase,
    primaryContainer = TogoColorTokens.DarkBorderCrisp,
    onPrimaryContainer = TogoColorTokens.DarkInkPrimary,
    secondary = TogoColorTokens.DarkAccentHighlight,
    onSecondary = TogoColorTokens.DarkSurfaceBase,
    secondaryContainer = TogoColorTokens.DarkBorderCrisp,
    onSecondaryContainer = TogoColorTokens.DarkInkPrimary,
    tertiary = TogoColorTokens.AccentSuccess,
    onTertiary = TogoColorTokens.DarkSurfaceBase,
    tertiaryContainer = TogoColorTokens.DarkBorderCrisp,
    onTertiaryContainer = TogoColorTokens.DarkInkPrimary,
    error = TogoColorTokens.AccentWarning,
    onError = TogoColorTokens.DarkSurfaceBase,
    errorContainer = TogoColorTokens.DarkBorderCrisp,
    onErrorContainer = TogoColorTokens.DarkInkPrimary,
    background = TogoColorTokens.DarkSurfaceBase,
    onBackground = TogoColorTokens.DarkInkPrimary,
    surface = TogoColorTokens.DarkSurfaceCard,
    onSurface = TogoColorTokens.DarkInkPrimary,
    surfaceVariant = TogoColorTokens.DarkSurfaceCard,
    onSurfaceVariant = TogoColorTokens.DarkInkSecondary,
    outline = TogoColorTokens.DarkBorderCrisp,
    outlineVariant = TogoColorTokens.DarkBorderCrisp,
    shadow = TogoColorTokens.DarkInkPrimary,
    scrim = TogoColorTokens.DarkInkPrimary,
    inverseSurface = TogoColorTokens.LightSurfaceBase,
    onInverseSurface = TogoColorTokens.LightInkPrimary,
    inversePrimary = TogoColorTokens.LightInkPrimary,
)

/** Sealed interface per accesso tipizzato ai token TOGO (Light/Dark). */
sealed interface TogoColorScheme {
    val surfaceBase: Color
    val surfaceSubtle: Color
    val surfaceCard: Color
    val surfaceInverse: Color
    val inkPrimary: Color
    val inkSecondary: Color
    val inkMuted: Color
    val inkInverse: Color
    val borderCrisp: Color
    val borderHairline: Color
    val accentAction: Color
    val accentSuccess: Color
    val accentHighlight: Color
    val accentWarning: Color
    val badgeBg: Color
    val badgeInk: Color
}

/** Implementazione Light. */
data class LightTogoColorSchemeImpl(
    override val surfaceBase: Color = TogoColorTokens.LightSurfaceBase,
    override val surfaceSubtle: Color = TogoColorTokens.LightSurfaceSubtle,
    override val surfaceCard: Color = TogoColorTokens.LightSurfaceCard,
    override val surfaceInverse: Color = TogoColorTokens.LightSurfaceInverse,
    override val inkPrimary: Color = TogoColorTokens.LightInkPrimary,
    override val inkSecondary: Color = TogoColorTokens.LightInkSecondary,
    override val inkMuted: Color = TogoColorTokens.LightInkMuted,
    override val inkInverse: Color = TogoColorTokens.LightInkInverse,
    override val borderCrisp: Color = TogoColorTokens.LightBorderCrisp,
    override val borderHairline: Color = TogoColorTokens.LightBorderHairline,
    override val accentAction: Color = TogoColorTokens.LightAccentAction,
    override val accentSuccess: Color = TogoColorTokens.AccentSuccess,
    override val accentHighlight: Color = TogoColorTokens.LightAccentHighlight,
    override val accentWarning: Color = TogoColorTokens.AccentWarning,
    override val badgeBg: Color = TogoColorTokens.BadgeBg,
    override val badgeInk: Color = TogoColorTokens.BadgeInk,
) : TogoColorScheme

/** Implementazione Dark. */
data class DarkTogoColorSchemeImpl(
    override val surfaceBase: Color = TogoColorTokens.DarkSurfaceBase,
    override val surfaceSubtle: Color = Color(0xFF131B2E),   // Dark surfaceSubtle (#131B2E)
    override val surfaceCard: Color = TogoColorTokens.DarkSurfaceCard,
    override val surfaceInverse: Color = TogoColorTokens.LightSurfaceBase, // #FFFFFF
    override val inkPrimary: Color = TogoColorTokens.DarkInkPrimary,
    override val inkSecondary: Color = TogoColorTokens.DarkInkSecondary,
    override val inkMuted: Color = Color(0xFF64748B),         // Dark inkMuted (slate 500)
    override val inkInverse: Color = TogoColorTokens.LightInkPrimary,     // #0F172A per testo su inverse
    override val borderCrisp: Color = TogoColorTokens.DarkBorderCrisp,
    override val borderHairline: Color = Color(0xFF64748B),   // Dark hairline (slate 500)
    override val accentAction: Color = TogoColorTokens.DarkInkPrimary,    // #F8FAFC per action su dark
    override val accentSuccess: Color = TogoColorTokens.AccentSuccess,
    override val accentHighlight: Color = TogoColorTokens.DarkAccentHighlight,
    override val accentWarning: Color = TogoColorTokens.AccentWarning,
    override val badgeBg: Color = TogoColorTokens.BadgeBg,
    override val badgeInk: Color = TogoColorTokens.BadgeInk,
) : TogoColorScheme

/** CompositionLocal per il ColorScheme TOGO corrente (Light/Dark). */
internal val LocalTogoColorScheme: CompositionLocal<TogoColorScheme> =
    staticCompositionLocalOf {
        error("TogoColorScheme non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Estensione per accedere ai token TOGO da qualsiasi composable dentro TogoTheme. */
val androidx.compose.ui.platform.CompositionLocalProvider.colors: TogoColorScheme
    get() = LocalTogoColorScheme.current

/** Factory per creare lo [TogoColorScheme] appropriato dal tema corrente. */
fun togoColorSchemeFrom(darkTheme: Boolean): TogoColorScheme =
    if (darkTheme) DarkTogoColorSchemeImpl() else LightTogoColorSchemeImpl()

/** Colori Material3 "di appoggio" usati internamente da TogoTheme per MaterialTheme. */
val Material3LightColorScheme: ColorScheme = LightTogoColorScheme
val Material3DarkColorScheme: ColorScheme = DarkTogoColorScheme