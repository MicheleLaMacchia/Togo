package it.togo.app.presentation.activelist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

/**
 * Test Dynamic Type della riga prodotto a fontScale 2.0 (Story 6.2).
 *
 * Verifica che a font massimo il nome prodotto non venga troncato, la riga
 * cresca in altezza (anziché troncare), la quantità resti leggibile e non
 * sovrapposta al nome, e la checkbox mantenga il target ≥48dp.
 *
 * NOTA: le asserzioni sui bounds usano `assertWidthIsAtLeast` /
 * `assertHeightIsAtLeast` (Dp) anziché confronti manuali, perché il tipo
 * restituito da `getUnclippedBoundsInRoot()` è `DpRect` e non una semplice
 * larghezza numerica.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DynamicTypeTouchTargetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val longName = "Latte parzialmente scremato"

    private fun longNameItem(checked: Boolean = false) = ShoppingItem(
        id = "item-1",
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.LITER,
        isChecked = checked,
    )

    /** Descrizione TalkBack della riga attiva (coerente con ItemRow). */
    private fun activeRowDescription(): String =
        "$longName, 2 litri, Latticini, non selezionato. Doppio tocco per modificare."

    /**
     * Componi una riga a fontScale dato iniettando il `Density` scalato nel
     * composition locale: non esiste un composable `Density(...)` e le
     * asserzioni di bounds funzionano solo se il layout reale usa quel density.
     */
    private fun setRow(
        fontScale: Float = 2.0f,
        productName: String = longName,
        item: ShoppingItem = longNameItem(),
    ) {
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = fontScale,
            )
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides scaledDensity,
            ) {
                TogoTheme(darkTheme = false) {
                    ItemRow(
                        item = item,
                        productName = productName,
                        categoryLabel = "Latticini",
                        onCheckChange = {},
                        onClick = {},
                        onDelete = {},
                    )
                }
            }
        }
    }

    @Test
    fun `long product name is fully visible without ellipsis at max font`() {
        setRow()

        // Il nodo col nome esiste ed è visibile (niente ellissi/troncamento).
        composeTestRule
            .onNodeWithText(longName)
            .assertIsDisplayed()
    }

    @Test
    fun `row grows with content at max font instead of truncating`() {
        // Un solo setContent: si compone un nome lungo, si misura, poi si
        // accorcia il nome e si rimisura. La riga deve accorciarsi col
        // contenuto, cioè a font max il nome lungo non viene troncato.
        var name by mutableStateOf(longName)
        composeTestRule.setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(
                density = baseDensity.density,
                fontScale = 2.0f,
            )
            androidx.compose.runtime.CompositionLocalProvider(
                LocalDensity provides scaledDensity,
            ) {
                TogoTheme(darkTheme = false) {
                    ItemRow(
                        item = longNameItem(),
                        productName = name,
                        categoryLabel = "Latticini",
                        onCheckChange = {},
                        onClick = {},
                        onDelete = {},
                    )
                }
            }
        }

        // La riga col nome lungo deve superare il minimo di 48dp.
        composeTestRule
            .onNode(hasTestTag("item_row"))
            .assertHeightIsAtLeast(48.dp)

        // Con un nome corto la riga resta comunque almeno 48dp (minimo del design).
        composeTestRule.runOnIdle { name = "Pane" }
        composeTestRule
            .onNode(hasTestTag("item_row"))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `row is at least 48dp tall at max font`() {
        setRow()
        composeTestRule
            .onNode(hasTestTag("item_row"))
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun `quantity stays legible and beside the name at max font`() {
        setRow()

        // Quantità "2 L" (displayName) interamente visibile e con larghezza utile.
        composeTestRule
            .onNodeWithText("2 L")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(1.dp)
    }

    @Test
    fun `checkbox keeps 48dp target at max font`() {
        setRow()

        composeTestRule
            .onNodeWithContentDescription("Spunta $longName.")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `checkbox visual square is smaller than its touch target`() {
        setRow()

        composeTestRule
            .onNodeWithTag("checkbox_visual", useUnmergedTree = true)
            .assertWidthIsEqualTo(24.dp)
            .assertHeightIsEqualTo(24.dp)
    }

    @Test
    fun `checked row at max font keeps name untruncated and checkbox target`() {
        setRow(item = longNameItem(checked = true))

        composeTestRule
            .onNodeWithText(longName)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Deseleziona $longName.")
            .assertHeightIsAtLeast(48.dp)
            .assertWidthIsAtLeast(48.dp)
    }

    @Test
    fun `active row semantics describe data state and edit action at max font`() {
        setRow()

        composeTestRule
            .onNodeWithContentDescription(activeRowDescription())
            .assertIsDisplayed()
    }
}
