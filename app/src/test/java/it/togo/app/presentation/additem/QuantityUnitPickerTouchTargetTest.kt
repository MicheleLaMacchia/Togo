package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

/**
 * Test touch target dei chip unità in [QuantityUnitPicker] (Story 6.2).
 *
 * Verifica che ogni chip abbia area di tocco ≥48×48dp — in particolare i chip
 * brevi come "g" — e che il click selezioni davvero l'unità.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class QuantityUnitPickerTouchTargetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private var selected: StandardUnit = StandardUnit.GRAM

    private fun setPicker(units: List<StandardUnit>) {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                QuantityUnitPicker(
                    quantityText = "1",
                    onQuantityChange = {},
                    selectedUnit = selected,
                    onUnitChange = { selected = it },
                    compatibleUnits = units,
                    errorQuantity = null,
                    errorUnit = null,
                )
            }
        }
    }

    @Test
    fun `short unit chip g has at least 48dp touch target`() {
        setPicker(listOf(StandardUnit.GRAM, StandardUnit.KILOGRAM))

        composeTestRule
            .onNodeWithText("g")
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `long unit chip grows beyond 48dp without clipping`() {
        setPicker(listOf(StandardUnit.PACKAGE, StandardUnit.BOTTLE, StandardUnit.KILOGRAM))

        for (label in listOf("conf", "bt", "kg")) {
            composeTestRule
                .onNodeWithText(label)
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }
    }

    @Test
    fun `clicking a unit chip selects that unit`() {
        setPicker(listOf(StandardUnit.GRAM, StandardUnit.KILOGRAM))
        composeTestRule.onNodeWithText("kg").performClick()
        assertEquals(StandardUnit.KILOGRAM, selected)
    }

    @Test
    fun `unit chips are clickable controls`() {
        setPicker(listOf(StandardUnit.GRAM))
        composeTestRule.onNodeWithText("g").assertHasClickAction()
    }
}
