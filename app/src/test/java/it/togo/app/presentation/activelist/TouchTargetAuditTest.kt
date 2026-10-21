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
import it.togo.app.domain.model.ShoppingItem
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
 * Verifica che i controlli custom della lista attiva — intestazione "Presi",
 * checkbox di riga, pulsante checkout, pulsanti dello stato vuoto — abbiano
 * area di tocco ≥48×48dp. I controlli Material (TopAppBar IconButton,
 * FloatingActionButton) garantiscono già 48dp per default del design system.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TouchTargetAuditTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val checkedItem = ShoppingItem(
        id = "item-1",
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.PIECE,
        isChecked = true,
    )

    private fun setPresiAndRow() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                val colors = togoColors()
                val typography = togoTypography()
                val spacing = togoSpacing()
                val tokens = componentTokens()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(spacing.space2),
                ) {
                    PresiSection(
                        items = listOf(checkedItem),
                        productNames = mapOf(1L to "Mozzarella"),
                        onUncheck = {},
                        onCheckout = {},
                        colors = colors,
                        typography = typography,
                        spacing = spacing,
                        tokens = tokens,
                    )
                }
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
    fun `presi header toggle is at least 48dp`() {
        setPresiAndRow()

        // L'intestazione collassabile è il nodo con la descrizione "Presi, ...".
        val header = composeTestRule.onNode(hasContentDescription("Presi, 1 voce. Doppio tocco per collassare."))
        assertTarget(header)
    }

    @Test
    fun `presi row checkbox is at least 48dp`() {
        setPresiAndRow()

        val checkbox = composeTestRule.onNode(hasContentDescription("Deseleziona Mozzarella."))
        assertTarget(checkbox)
    }

    @Test
    fun `presi checkout button is at least 48dp`() {
        setPresiAndRow()

        val checkout = composeTestRule.onNode(hasTestTag("checkout_button"))
        assertTarget(checkout)
    }

    @Test
    fun `empty state add button is at least 48dp`() {
        setEmptyState()

        val add = composeTestRule.onNode(hasTestTag("add_manual_button"))
        assertTarget(add)
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
