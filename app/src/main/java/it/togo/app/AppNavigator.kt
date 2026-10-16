package it.togo.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import it.togo.app.presentation.activelist.UiEvent

/**
 * Schermate disponibili nell'app.
 *
 * L'app non usa NavHost: la navigazione è un semplice stato locale per evitare
 * l'introduzione di una nuova libreria.
 */
private sealed interface Screen {
    data object ActiveList : Screen
    data object LearnedRules : Screen
}

/** Salva la schermata corrente tra le rotazioni. */
private val ScreenSaver = Saver<Screen, String>(
    save = { screen -> if (screen is Screen.LearnedRules) "learned_rules" else "active_list" },
    restore = { value -> if (value == "learned_rules") Screen.LearnedRules else Screen.ActiveList },
)

/**
 * Host di navigazione a stato.
 *
 * - Da [UiEvent.NavigateToSettings] apre le Regole apprese.
 * - Da Regole apprese il tasto Back di sistema/gesture torna alla lista.
 * - Gli altri eventi sono delegati a [onActiveListEvent].
 *
 * @param onActiveListEvent gestore degli eventi della Lista Attiva non relativi
 *   alla navigazione (tipicamente `ActiveListViewModel::onEvent`).
 * @param activeListContent contenuto della Lista Attiva, con l'evento da inoltrare.
 * @param learnedRulesContent contenuto delle Regole apprese, con il callback di ritorno.
 */
@Composable
fun AppNavigator(
    onActiveListEvent: (UiEvent) -> Unit,
    activeListContent: @Composable ((UiEvent) -> Unit) -> Unit,
    learnedRulesContent: @Composable (() -> Unit) -> Unit,
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.ActiveList)
    }

    when (currentScreen) {
        Screen.ActiveList -> activeListContent { event ->
            if (event is UiEvent.NavigateToSettings) {
                currentScreen = Screen.LearnedRules
            } else {
                onActiveListEvent(event)
            }
        }

        Screen.LearnedRules -> {
            BackHandler { currentScreen = Screen.ActiveList }
            learnedRulesContent { currentScreen = Screen.ActiveList }
        }
    }
}
