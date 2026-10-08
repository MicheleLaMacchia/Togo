package it.togo.app.ui.theme

import androidx.compose.material3.TextStyle
import androidx.compose.material3.Typography
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * Tipografia TOGO — 5 ruoli (da DESIGN.md §3).
 *
 * Basata su Roboto / Google Sans (font di sistema Android Material3).
 * Nessun peso Light/Thin sui testi primari.
 * Scala lineare con Dynamic Type (fontScale di sistema).
 */
data class TogoTypography(
    /** Titolo schermata — 24sp SemiBold — Headline Medium equivalente. */
    val titleScreen: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = androidx.compose.ui.unit.sp(32),
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDecoration = TextDecoration.None,
    ),

    /** Intestazione categoria — 16sp Bold, uppercase tracking — Title Medium equivalente. */
    val sectionHeader: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = androidx.compose.ui.unit.sp(24),
        letterSpacing = 0.15.sp,
        textAlign = TextAlign.Start,
        textDecoration = TextDecoration.None,
    ),

    /** Nome prodotto — 16sp Medium — Body Large equivalente. */
    val itemName: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = androidx.compose.ui.unit.sp(24),
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDecoration = TextDecoration.None,
    ),

    /** Quantità / Badge — 14sp SemiBold — Label Large equivalente (min 14sp). */
    val itemMeta: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = androidx.compose.ui.unit.sp(20),
        letterSpacing = 0.sp,
        textAlign = TextAlign.End,
        textDecoration = TextDecoration.None,
    ),

    /** Microcopy / Note — 12sp Regular — Body Small equivalente. */
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = androidx.compose.ui.unit.sp(16),
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDecoration = TextDecoration.None,
    ),
)

/** Istanza singleton dei token tipografici TOGO. */
val TogoTypography = TogoTypography()

/** CompositionLocal per la tipografia TOGO. */
internal val LocalTogoTypography: CompositionLocal<TogoTypography> =
    staticCompositionLocalOf {
        error("TogoTypography non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Estensione per accedere a TogoTypography da qualsiasi composable dentro TogoTheme. */
val androidx.compose.ui.platform.CompositionLocalProvider.typography: TogoTypography
    get() = LocalTogoTypography.current

/** Estensioni su [Typography] Material3 per accesso rapido ai ruoli TOGO. */
val Typography.titleScreen: TextStyle
    get() = TogoTypography.titleScreen
val Typography.sectionHeader: TextStyle
    get() = TogoTypography.sectionHeader
val Typography.itemName: TextStyle
    get() = TogoTypography.itemName
val Typography.itemMeta: TextStyle
    get() = TogoTypography.itemMeta
val Typography.caption: TextStyle
    get() = TogoTypography.caption