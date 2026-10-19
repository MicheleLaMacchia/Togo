package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.domain.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM per DuplicateResolutionDialog (Story 3.5 + Story 6.1).
 *
 * Verifica annuncio (titolo, messaggio, opzioni), stati e callback.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DuplicateResolutionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() { stopKoin() }

    private fun showDialog(
        canSum: Boolean = true,
        onSelect: (ResolutionOption) -> Unit = {},
    ) {
        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                canSum = canSum,
                onSelect = onSelect,
            )
        }
    }

    @Test
    fun `dialog shows product name and quantities`() {
        showDialog()

        composeTestRule.onNodeWithText("Prodotto già in lista").assertExists()
        // Il nome è dentro il messaggio: "\"Latte UHT\" è già presente con ..."
        composeTestRule.onNodeWithText("Latte UHT", substring = true).assertExists()
    }

    @Test
    fun `title and message are exposed as content descriptions`() {
        showDialog()

        composeTestRule
            .onNodeWithContentDescription("Prodotto già in lista")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription(
                "\"Latte UHT\" è già presente con 1.0. Nuova quantità: 2.0. Cosa vuoi fare?"
            )
            .assertExists()
    }

    @Test
    fun `sum option is clickable and invokes onSelect with SUM`() {
        var selectedOption: ResolutionOption? = null
        showDialog(canSum = true, onSelect = { selectedOption = it })

        composeTestRule
            .onNodeWithText("Somma quantità (+2.0 → 3.0)")
            .assertExists()
            .assertHasClickAction()
            .performClick()

        assertEquals(ResolutionOption.SUM, selectedOption)
    }

    @Test
    fun `canSum false hides the sum option so it is not actionable`() {
        showDialog(canSum = false)

        composeTestRule
            .onNodeWithText("Somma quantità (+2.0 → 3.0)")
            .assertDoesNotExist()
    }

    @Test
    fun `replace option is clickable and invokes onSelect with REPLACE`() {
        var selectedOption: ResolutionOption? = null
        showDialog(onSelect = { selectedOption = it })

        composeTestRule
            .onNodeWithText("Sostituisci quantità (2.0)")
            .assertHasClickAction()
            .performClick()

        assertEquals(ResolutionOption.REPLACE, selectedOption)
    }

    @Test
    fun `separate option is clickable and invokes onSelect with SEPARATE`() {
        var selectedOption: ResolutionOption? = null
        showDialog(onSelect = { selectedOption = it })

        composeTestRule
            .onNodeWithText("Crea voce separata")
            .assertHasClickAction()
            .performClick()

        assertEquals(ResolutionOption.SEPARATE, selectedOption)
    }

    @Test
    fun `cancel option is clickable and invokes onSelect with CANCEL`() {
        var selectedOption: ResolutionOption? = null
        showDialog(onSelect = { selectedOption = it })

        composeTestRule
            .onNodeWithText("Annulla")
            .assertHasClickAction()
            .performClick()

        assertEquals(ResolutionOption.CANCEL, selectedOption)
    }
}
