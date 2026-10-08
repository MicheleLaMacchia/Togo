package it.togo.app.ui.theme

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.RoundedCornerSize
import androidx.compose.ui.unit.dp

/**
 * Shape TOGO — Raggi d'angolo (da DESIGN.md §6).
 *
 * Flat + Crisp Strokes: zero ombre, profondità solo via bordi solidi.
 * Angoli definiti per comunicare precisione e solidità.
 */
data class TogoShapes(
    /** Nessun raggio (0dp). */
    val none: RoundedCornerSize = RoundedCornerSize(0.dp),
    /** 4dp — Checkbox, Badge quantità. */
    val sm: RoundedCornerSize = RoundedCornerSize(4.dp),
    /** 8dp — Card prodotto. */
    val md: RoundedCornerSize = RoundedCornerSize(8.dp),
    /** 12dp — Bottom Sheet, Duplicate Dialog. */
    val lg: RoundedCornerSize = RoundedCornerSize(12.dp),
    /** 9999dp — Cerchio perfetto (FAB vocale). */
    val full: RoundedCornerSize = RoundedCornerSize(9999.dp),
)

/** Istanza singleton delle shape TOGO. */
val TogoShapes = TogoShapes()

/** CompositionLocal per le shape TOGO. */
internal val LocalTogoShapes: CompositionLocal<TogoShapes> =
    staticCompositionLocalOf {
        error("TogoShapes non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Estensione per accedere a TogoShapes da qualsiasi composable dentro TogoTheme. */
val androidx.compose.ui.platform.CompositionLocalProvider.shapes: TogoShapes
    get() = LocalTogoShapes.current