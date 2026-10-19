package it.togo.app

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.presentation.activelist.UiEvent
import it.togo.app.ui.theme.TogoTheme
import org.junit.After
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
    fun `system back from learned rules returns to active list`() {
        setNavigatorContent()

        composeTestRule.onNodeWithText("Apri impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()

        composeTestRule.runOnUiThread {
            composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        }

        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }
}
