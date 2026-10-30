package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.ListItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

/**
 * Audit dei touch target (Story 6.2).
 *
 * Verifica checkbox e pulsanti dello stato vuoto su touch target da almeno 48dp.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchTargetAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val activeItem = listItem(
        id = "item-1",
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.PIECE,
        isChecked = false,
    )

    private fun setActiveRow() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = activeItem,
                    productName = "Mozzarella",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }
    }
    private fun setEmptyState() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                EmptyState(
                    onAddManual = {},
                    onVoice = {},
                    colors = togoColors(),
                    typography = togoTypography(),
                    spacing = togoSpacing(),
                )
            }
        }
    }

    private fun assertTarget(node: SemanticsNodeInteraction) {
        node.assertWidthIsAtLeast(48.dp)
        node.assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `active row completion control is at least 48dp`() {
        setActiveRow()
        assertTarget(composeTestRule.onNode(hasContentDescription("Spunta Mozzarella.")))
    }
    @Test
    fun `empty state add button is at least 48dp`() {
        setEmptyState()
        assertTarget(composeTestRule.onNode(hasTestTag("add_manual_button")))
    }

    @Test
    fun `empty state voice button is at least 48dp`() {
        setEmptyState()

        val voice = composeTestRule.onNode(hasContentDescription("Aggiungi a voce"))
        assertTarget(voice)
    }

    @Test
    fun `every interactive node in empty state is at least 48dp`() {
        setEmptyState()

        // Ogni nodo interattivo (con azione click) deve rispettare il minimo.
        val interactive = composeTestRule.onAllNodes(hasClickAction())
        val count = interactive.fetchSemanticsNodes().size
        assertTrue("lo stato vuoto deve avere almeno 2 controlli interattivi", count >= 2)

        for (i in 0 until count) {
            assertTarget(interactive[i])
        }
    }
}
