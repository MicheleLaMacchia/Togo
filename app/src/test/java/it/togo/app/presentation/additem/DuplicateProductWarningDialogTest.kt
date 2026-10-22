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
 * Test del dialog di avviso duplicato (Story 7.1): deve mostrare entrambi i
 * nomi e consentire sia "Procedi" sia "Annulla".
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DuplicateProductWarningDialogTest {

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
                DuplicateProductWarningDialog(
                    candidateName = "Latte di avena",
                    newProductName = "Latte d'avena",
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Possibile duplicato").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "Esiste già «Latte di avena» in questa categoria. " +
                "Vuoi creare comunque «Latte d'avena»?",
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText("Procedi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Annulla").assertIsDisplayed()
    }

    @Test
    fun `procedi invoca la conferma`() {
        var confirmed = 0
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                DuplicateProductWarningDialog(
                    candidateName = "Latte di avena",
                    newProductName = "Latte d'avena",
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
                DuplicateProductWarningDialog(
                    candidateName = "Latte di avena",
                    newProductName = "Latte d'avena",
                    onConfirm = {},
                    onDismiss = { dismissed++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Annulla").performClick()

        assertEquals(1, dismissed)
    }
}
