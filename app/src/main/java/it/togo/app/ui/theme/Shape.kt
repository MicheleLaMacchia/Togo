package it.togo.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Shape TOGO — Raggi d'angolo (da DESIGN.md §6).
 *
 * Flat + Crisp Strokes: zero ombre, profondità solo via bordi solidi.
 * Angoli definiti per comunicare precisione e solidità.
 */
data class TogoShapesTokens(
    /** Nessun raggio (0dp). */
    val none: RoundedCornerShape = RoundedCornerShape(0.dp),
    /** 4dp — Checkbox, Badge quantità. */
    val sm: RoundedCornerShape = RoundedCornerShape(4.dp),
    /** 8dp — Card prodotto. */
    val md: RoundedCornerShape = RoundedCornerShape(8.dp),
    /** 12dp — Bottom Sheet, Duplicate Dialog. */
    val lg: RoundedCornerShape = RoundedCornerShape(12.dp),
    /** 9999dp — Cerchio perfetto (FAB vocale). */
    val full: RoundedCornerShape = RoundedCornerShape(9999.dp),
)

/** Istanza singleton delle shape TOGO. */
val TogoShapes = TogoShapesTokens()

/** CompositionLocal per le shape TOGO. */
internal val LocalTogoShapes: ProvidableCompositionLocal<TogoShapesTokens> =
    staticCompositionLocalOf {
        error("TogoShapes non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Funzione composable per accedere alle shape TOGO. */
@Composable
fun togoShapes(): TogoShapesTokens = LocalTogoShapes.current
