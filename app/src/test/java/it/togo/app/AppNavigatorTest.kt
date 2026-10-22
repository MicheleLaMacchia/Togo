package it.togo.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.LearnedRulesRepository
import it.togo.app.domain.usecase.LearnedRuleManager
import it.togo.app.presentation.activelist.UiEvent
import it.togo.app.presentation.rules.LearnedRulesScreen
import it.togo.app.presentation.rules.LearnedRulesViewModel
import it.togo.app.ui.theme.TogoTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric) per l'host di navigazione a stato.
 *
 * Verifica che Impostazioni apra le Regole apprese e che sia il pulsante
 * "Indietro" sia il Back di sistema riportino alla Lista Attiva.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppNavigatorTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @After
    fun tearDown() { stopKoin() }

    private fun setNavigatorContent() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        Column {
                            Text("Lista attiva")
                            Button(onClick = { onEvent(UiEvent.NavigateToHistory) }) {
                                Text("Apri storico")
                            }
                            Button(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                                Text("Apri impostazioni")
                            }
                        }
                    },
                    learnedRulesContent = { onBack ->
                        Column {
                            Text("Regole apprese")
                            Button(onClick = onBack) {
                                Text("Indietro")
                            }
                        }
                    },
                    historyContent = { onBack ->
                        Column {
                            Text("Storico")
                            Button(onClick = onBack) {
                                Text("Indietro")
                            }
                        }
                    },
                )
            }
        }
    }

    @Test
    fun `settings opens learned rules and back button returns to active list`() {
        setNavigatorContent()

        composeTestRule.onNodeWithText("Lista attiva").assertExists()
        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()

        composeTestRule.onNodeWithText("Indietro").performClick()
        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }

    @Test
    fun `settings composes real learned rules screen and back returns`() {
        val catalogRepository = mockk<CatalogRepository>()
        val rulesRepository = mockk<LearnedRulesRepository>()
        every { rulesRepository.getAll() } returns flowOf(emptyList())
        coEvery { catalogRepository.getById(any()) } returns null
        val rulesViewModel = LearnedRulesViewModel(
            learnedRuleManager = mockk<LearnedRuleManager>(relaxed = true),
            catalogRepository = catalogRepository,
            learnedRulesRepository = rulesRepository,
        )

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        Button(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                            Text("Apri impostazioni")
                        }
                    },
                    learnedRulesContent = { onBack ->
                        LearnedRulesScreen(viewModel = rulesViewModel, onBack = onBack)
                    },
                    historyContent = { onBack ->
                        Button(onClick = onBack) { Text("Indietro") }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()
        composeTestRule.onNodeWithContentDescription("Indietro").performClick()
        composeTestRule.onNodeWithText("Apri impostazioni").assertExists()
    }

    @Test
    fun `active list saveable state survives settings navigation`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        var counter by rememberSaveable { mutableStateOf(0) }
                        Column {
                            Text("Contatore: $counter")
                            Button(onClick = { counter++ }) { Text("Incrementa") }
                            Button(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                                Text("Apri impostazioni")
                            }
                        }
                    },
                    learnedRulesContent = { onBack ->
                        Button(onClick = onBack) { Text("Indietro") }
                    },
                    historyContent = { onBack ->
                        Button(onClick = onBack) { Text("Indietro") }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Incrementa").performClick()
        composeTestRule.onNodeWithText("Contatore: 1").assertExists()

        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Indietro").performClick()

        composeTestRule.onNodeWithText("Contatore: 1").assertExists()
    }

    @Test
    fun `learned rules destination survives recreation`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        Button(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                            Text("Apri impostazioni")
                        }
                    },
                    learnedRulesContent = { onBack ->
                        Column {
                            Text("Regole apprese")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                    historyContent = { onBack ->
                        Column {
                            Text("Storico")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Regole apprese").assertExists()
    }

    @Test
    fun `system back from learned rules returns to active list`() {
        setNavigatorContent()

        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }

    @Test
    fun `history opens from active list and back button returns`() {
        setNavigatorContent()

        composeTestRule.onNodeWithText("Apri storico").performClick()
        composeTestRule.onNodeWithText("Storico").assertExists()

        composeTestRule.onNodeWithText("Indietro").performClick()
        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }

    @Test
    fun `system back from history returns to active list`() {
        setNavigatorContent()

        composeTestRule.onNodeWithText("Apri storico").performClick()
        composeTestRule.onNodeWithText("Storico").assertExists()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }

    @Test
    fun `history destination survives recreation`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        restorationTester.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        Button(onClick = { onEvent(UiEvent.NavigateToHistory) }) {
                            Text("Apri storico")
                        }
                    },
                    learnedRulesContent = { onBack ->
                        Button(onClick = onBack) { Text("Indietro") }
                    },
                    historyContent = { onBack ->
                        Column {
                            Text("Storico")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Apri storico").performClick()
        composeTestRule.onNodeWithText("Storico").assertExists()

        restorationTester.emulateSavedInstanceStateRestore()

        composeTestRule.onNodeWithText("Storico").assertExists()
    }

    @Test
    fun `history event is handled by navigator and not forwarded to active list`() {
        val received = mutableListOf<UiEvent>()

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = { received += it },
                    activeListContent = { onEvent ->
                        Button(onClick = { onEvent(UiEvent.NavigateToHistory) }) {
                            Text("Apri storico")
                        }
                    },
                    learnedRulesContent = { onBack ->
                        Button(onClick = onBack) { Text("Indietro") }
                    },
                    historyContent = { onBack ->
                        Column {
                            Text("Storico")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Apri storico").performClick()
        composeTestRule.onNodeWithText("Storico").assertExists()

        assertTrue(
            "NavigateToHistory è di navigazione e non va inoltrato al ViewModel",
            received.none { it is UiEvent.NavigateToHistory },
        )
    }
}
