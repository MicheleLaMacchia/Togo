package it.togo.app.ui.theme

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/**
 * Spacing TOGO — Scala 4px/8px + touch target minimo (da DESIGN.md §4).
 *
 * Griglia modulare 4px/8px per coerenza layout.
 * Target touch minimo 48x48dp per ogni controllo interattivo.
 */
data class TogoSpacing(
    /** 4dp — unità base. */
    val space1: androidx.compose.ui.unit.Dp = 4.dp,
    /** 8dp — distanza tra card, padding interno standard. */
    val space2: androidx.compose.ui.unit.Dp = 8.dp,
    /** 12dp — padding medio. */
    val space3: androidx.compose.ui.unit.Dp = 12.dp,
    /** 16dp — margine laterale schermo standard. */
    val space4: androidx.compose.ui.unit.Dp = 16.dp,
    /** 24dp — separazione categorie, padding ampio. */
    val space5: androidx.compose.ui.unit.Dp = 24.dp,
    /** 32dp — spaziatura massima. */
    val space6: androidx.compose.ui.unit.Dp = 32.dp,
    /** 48dp — target tocco minimo (48x48dp). */
    val touchTargetMin: androidx.compose.ui.unit.Dp = 48.dp,
) {
    /** Accesso per indice 1..6 (es. `spacing[3]` → 12dp). */
    operator fun get(index: Int): androidx.compose.ui.unit.Dp = when (index) {
        1 -> space1
        2 -> space2
        3 -> space3
        4 -> space4
        5 -> space5
        6 -> space6
        else -> throw IllegalArgumentException("Indice spacing valido: 1..6, ricevuto $index")
    }
}

/** Istanza singleton dello spacing TOGO. */
val TogoSpacing = TogoSpacing()

/** CompositionLocal per lo spacing TOGO. */
internal val LocalTogoSpacing: CompositionLocal<TogoSpacing> =
    staticCompositionLocalOf {
        error("TogoSpacing non disponibile. Avvolgere il contenuto in TogoTheme { ... }")
    }

/** Estensione per accedere a TogoSpacing da qualsiasi composable dentro TogoTheme. */
val androidx.compose.ui.platform.CompositionLocalProvider.spacing: TogoSpacing
    get() = LocalTogoSpacing.current