package it.togo.app.presentation.additem

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.domain.model.ResolutionOption
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Test JVM per DuplicateResolutionDialog (Story 3.5).
 */
class DuplicateResolutionDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `dialog shows product name and quantities`() {
        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                onSelect = {},
            )
        }

        composeTestRule.onNodeWithText("Prodotto già in lista").assertExists()
        composeTestRule.onNodeWithText("Latte UHT").assertExists()
    }

    @Test
    fun `canSum true shows sum option`() {
        var selectedOption: ResolutionOption? = null

        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                canSum = true,
                onSelect = { selectedOption = it },
            )
        }

        composeTestRule.onNodeWithText(org.hamcrest.CoreMatchers.containsString("Somma quantità")).assertExists()
        composeTestRule.onNodeWithText(org.hamcrest.CoreMatchers.containsString("Somma quantità")).performClick()
        assertEquals(ResolutionResult.SUM, selectedOption)
    }

    @Test
    fun `canSum false hides sum option`() {
        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                canSum = false,
                onSelect = {},
            )
        }

        composeTestRule.onNodeWithText(org.hamcrest.CoreMatchers.containsString("Somma quantità")).assertDoesNotExist()
    }

    @Test
    fun `replace option calls onSelect with REPLACE`() {
        var selectedOption: ResolutionOption? = null

        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                onSelect = { selectedOption = it },
            )
        }

        composeTestRule.onNodeWithText(org.hamcrest.CoreMatchers.containsString("Sostituisci quantità")).performClick()
        assertEquals(ResolutionResult.REPLACE, selectedOption)
    }

    @Test
    fun `separate option calls onSelect with SEPARATE`() {
        var selectedOption: ResolutionOption? = null

        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                onSelect = { selectedOption = it },
            )
        }

        composeTestRule.onNodeWithText("Crea voce separata").performClick()
        assertEquals(ResolutionResult.SEPARATE, selectedOption)
    }

    @Test
    fun `cancel option calls onSelect with CANCEL`() {
        var selectedOption: ResolutionOption? = null

        composeTestRule.setContent {
            DuplicateResolutionDialog(
                productName = "Latte UHT",
                currentQuantity = 1.0,
                newQuantity = 2.0,
                onSelect = { selectedOption = it },
            )
        }

        composeTestRule.onNodeWithText("Annulla").performClick()
        assertEquals(ResolutionResult.CANCEL, selectedOption)
    }
}