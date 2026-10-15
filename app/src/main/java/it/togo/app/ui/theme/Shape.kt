package it.togo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Shape TOGO — Raggi d'angolo (da DESIGN.md §6).
 *
 * Flat + Crisp Strokes: zero ombre, profondità solo via bordi solidi.
 * Angoli definiti per comunicare precisione e solidità.
 */
data class TogoShapesTokens(
    /** Nessun raggio (0dp). */
    val none: androidx.compose.ui.graphics.RoundedCornerShape = androidx.compose.ui.graphics.RoundedCornerShape(0.dp),
    /** 4dp — Checkbox, Badge quantità. */
    val sm: androidx.compose.ui.graphics.RoundedCornerShape = androidx.compose.ui.graphics.RoundedCornerShape(4.dp),
    /** 8dp — Card prodotto. */
    val md: androidx.compose.ui.graphics.RoundedCornerShape = androidx.compose.ui.graphics.RoundedCornerShape(8.dp),
    /** 12dp — Bottom Sheet, Duplicate Dialog. */
    val lg: androidx.compose.ui.graphics.RoundedCornerShape = androidx.compose.ui.graphics.RoundedCornerShape(12.dp),
    /** 9999dp — Cerchio perfetto (FAB vocale). */
    val full: androidx.compose.ui.graphics.RoundedCornerShape = androidx.compose.ui.graphics.RoundedCornerShape(9999.dp),
)

/** Istanza singleton delle shape TOGO. */
val TogoShapes = TogoShapesTokens()

/** CompositionLocal per le shape TOGO. */
internal val LocalTogoShapes: CompositionLocal<TogoShapesTokens> =
    staticCompositionLocalOf {
        error("TogoShapes non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Funzione composable per accedere alle shape TOGO. */
@Composable
fun togoShapes(): TogoShapesTokens = LocalTogoShapes.current