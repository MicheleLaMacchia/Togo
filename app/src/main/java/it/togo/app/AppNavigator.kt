package it.togo.app

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import it.togo.app.core.TogoLog
import it.togo.app.presentation.activelist.UiEvent
import it.togo.app.presentation.settings.AboutScreen
import it.togo.app.presentation.settings.SettingsScreen

/**
 * Schermate disponibili nell'app.
 *
 * L'app non usa NavHost: la navigazione è un semplice stato locale per evitare
 * l'introduzione di una nuova libreria.
 */
private sealed interface Screen {
    data object ActiveList : Screen
    data object Settings : Screen
    data object LearnedRules : Screen
    data object About : Screen
    data object History : Screen

    val saveKey: String
        get() = when (this) {
            Screen.ActiveList -> "active_list"
            Screen.Settings -> "settings"
            Screen.LearnedRules -> "learned_rules"
            Screen.About -> "about"
            Screen.History -> "history"
        }
}

/** Salva la schermata corrente tra le rotazioni. */
private val ScreenSaver = Saver<Screen, String>(
    save = { screen -> screen.saveKey },
    restore = { value ->
        when (value) {
            "settings" -> Screen.Settings
            "learned_rules" -> Screen.LearnedRules
            "about" -> Screen.About
            "history" -> Screen.History
            else -> Screen.ActiveList
        }
    },
)

/**
 * Host di navigazione a stato.
 *
 * - [UiEvent.NavigateToHistory] apre lo Storico.
 * - [UiEvent.NavigateToSettings] apre il menù Impostazioni.
 * - Da Impostazioni si accede alle Regole apprese e alle Informazioni su TOGO.
 * - Back da Regole apprese e Informazioni su TOGO torna a Impostazioni; back da
 *   Impostazioni e Storico torna alla lista attiva.
 * - Gli altri eventi sono delegati a [onActiveListEvent].
 *
 * @param onActiveListEvent gestore degli eventi della Lista Attiva non relativi
 *   alla navigazione (tipicamente `ActiveListViewModel::onEvent`).
 * @param activeListContent contenuto della Lista Attiva, con l'evento da inoltrare.
 * @param learnedRulesContent contenuto delle Regole apprese, con il callback di ritorno.
 * @param historyContent contenuto dello Storico, con il callback di ritorno.
 * @param settingsContent contenuto delle Impostazioni, con callback per aprire le
 *   Regole apprese, le Informazioni su TOGO e per tornare indietro. Il default usa
 *   [SettingsScreen].
 * @param aboutContent contenuto delle Informazioni su TOGO, con il callback di
 *   ritorno. Il default usa [AboutScreen].
 */
@Composable
fun AppNavigator(
    onActiveListEvent: (UiEvent) -> Unit,
    activeListContent: @Composable ((UiEvent) -> Unit) -> Unit,
    learnedRulesContent: @Composable (() -> Unit) -> Unit,
    historyContent: @Composable (() -> Unit) -> Unit,
    settingsContent: @Composable (() -> Unit, () -> Unit, () -> Unit) -> Unit =
        { onOpenLearnedRules, onOpenAbout, onBack ->
            SettingsScreen(
                onOpenLearnedRules = onOpenLearnedRules,
                onOpenAbout = onOpenAbout,
                onBack = onBack,
            )
        },
    aboutContent: @Composable (() -> Unit) -> Unit = { onBack ->
        AboutScreen(onBack = onBack)
    },
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.ActiveList)
    }

    val stateHolder = rememberSaveableStateHolder()

    fun navigateTo(target: Screen) {
        TogoLog.d("Navigazione: ${currentScreen.saveKey} -> ${target.saveKey}")
        currentScreen = target
    }

    when (currentScreen) {
        Screen.ActiveList -> stateHolder.SaveableStateProvider(Screen.ActiveList.saveKey) {
            activeListContent { event ->
                when (event) {
                    is UiEvent.NavigateToSettings -> navigateTo(Screen.Settings)
                    is UiEvent.NavigateToHistory -> navigateTo(Screen.History)
                    else -> onActiveListEvent(event)
                }
            }
        }

        Screen.Settings -> stateHolder.SaveableStateProvider(Screen.Settings.saveKey) {
            BackHandler { navigateTo(Screen.ActiveList) }
            settingsContent(
                { navigateTo(Screen.LearnedRules) },
                { navigateTo(Screen.About) },
                { navigateTo(Screen.ActiveList) },
            )
        }

        Screen.LearnedRules -> stateHolder.SaveableStateProvider(Screen.LearnedRules.saveKey) {
            BackHandler { navigateTo(Screen.Settings) }
            learnedRulesContent { navigateTo(Screen.Settings) }
        }

        Screen.About -> stateHolder.SaveableStateProvider(Screen.About.saveKey) {
            BackHandler { navigateTo(Screen.Settings) }
            aboutContent { navigateTo(Screen.Settings) }
        }

        Screen.History -> stateHolder.SaveableStateProvider(Screen.History.saveKey) {
            BackHandler { navigateTo(Screen.ActiveList) }
            historyContent { navigateTo(Screen.ActiveList) }
        }
    }
}
