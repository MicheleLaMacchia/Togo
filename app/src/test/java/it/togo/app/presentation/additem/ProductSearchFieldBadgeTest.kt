package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.presentation.search.SearchResultItem
import it.togo.app.ui.theme.TogoTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test del badge "Personalizzato" (Story 7.1) nei suggerimenti di ricerca
 * e nei risultati della schermata di ricerca.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProductSearchFieldBadgeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val normalProduct = CanonicalProduct(
        id = 1L,
        name = "Latte intero",
        level3Id = 12L,
        isUserDefined = false,
    )

    private val userProduct = CanonicalProduct(
        id = 2L,
        name = "Latte di avena",
        level3Id = 12L,
        isUserDefined = true,
    )

    @Before
    fun setup() {
        stopKoin()
    }

    @Test
    fun `badge Personalizzato appare solo per il prodotto user-defined nei suggerimenti`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ProductSearchField(
                    query = "latte",
                    onQueryChange = {},
                    results = listOf(normalProduct, userProduct),
                    isSearching = false,
                    onProductClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Personalizzato").assertCountEquals(1)
        composeTestRule.onNodeWithText("Latte di avena").assertExists()
        composeTestRule.onNodeWithText("Latte intero").assertExists()
    }

    @Test
    fun `badge Personalizzato visibile nel risultato di ricerca user-defined`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                SearchResultItem(
                    product = userProduct,
                    query = "latte",
                    onClick = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Personalizzato").assertExists()
    }

    @Test
    fun `nessun badge nel risultato di ricerca catalogo`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                SearchResultItem(
                    product = normalProduct,
                    query = "latte",
                    onClick = {},
                )
            }
        }

        composeTestRule.onAllNodesWithText("Personalizzato").assertCountEquals(0)
    }
}
