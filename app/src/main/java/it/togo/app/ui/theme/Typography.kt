package it.togo.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Data class contenente i token tipografici TOGO.
 *
 * Definisce 5 ruoli di testo ottimizzati per la lista della spesa:
 * - titleScreen: titoli principali
 * - sectionHeader: intestazioni di sezione/categoria
 * - itemName: nomi dei prodotti
 * - itemMeta: quantità e unità di misura
 * - caption: note e testi secondari
 */
data class TogoTypographyTokens(
    val titleScreen: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    val sectionHeader: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    val itemName: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    val itemMeta: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    val caption: TextStyle = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
)

/** Istanza singleton dei token tipografici TOGO. */
val TogoTypography = TogoTypographyTokens()

/**
 * CompositionLocal per distribuire i token tipografici attraverso l'albero dei composable.
 * Usato da [TogoTheme] per fornire i tipi di testo predefiniti.
 */
internal val LocalTogoTypography: CompositionLocal<TogoTypographyTokens> =
    staticCompositionLocalOf {
        error("TogoTypography non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/**
 * Funzione composable per accedere ai token tipografici TOGO.
 * Uso: val tokens = togoTypographyTokens()
 */
@Composable
fun togoTypographyTokens(): TogoTypographyTokens = LocalTogoTypography.current