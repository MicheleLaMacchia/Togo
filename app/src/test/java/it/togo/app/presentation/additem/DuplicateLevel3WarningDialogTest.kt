package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test del dialog di avviso duplicato per il Livello 3 (Story 7.2): deve
 * mostrare candidato e nuovo nome e consentire "Procedi" o "Annulla".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DuplicateLevel3WarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        stopKoin()
    }

    @Test
    fun `mostra candidato e nuovo nome`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                DuplicateLevel3WarningDialog(
                    candidateName = "Bevande vegetali",
                    newLevel3Name = "Bevande bio",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Possibile duplicato").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Esiste già il Livello 3 «Bevande vegetali» in questa categoria. " +
                "Vuoi creare comunque «Bevande bio»?",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Procedi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annulla").assertIsDisplayed()
    }

    @Test
    fun `procedi invoca la conferma`() {
        var confirmed = 0
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                DuplicateLevel3WarningDialog(
                    candidateName = "Bevande vegetali",
                    newLevel3Name = "Bevande bio",
                    onConfirm = { confirmed++ },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Procedi").performClick()

        assertEquals(1, confirmed)
    }

    @Test
    fun `annulla invoca la chiusura`() {
        var dismissed = 0
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                DuplicateLevel3WarningDialog(
                    candidateName = "Bevande vegetali",
                    newLevel3Name = "Bevande bio",
                    onConfirm = {},
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Annulla").performClick()

        assertEquals(1, dismissed)
    }
}
