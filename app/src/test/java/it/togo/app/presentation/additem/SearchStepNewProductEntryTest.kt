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
 * Test dell'entry point "Aggiungi al catalogo" nello step 1 (Story 7.1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchStepNewProductEntryTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val entryLabel = "Non trovi il prodotto? Aggiungilo al catalogo"

    @Before
    fun setup() {
        stopKoin()
    }

    @Test
    fun `entry point visibile con query di almeno due caratteri e invoca il callback`() {
        var clicked = 0
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                SearchStep(
                    query = "latte",
                    onQueryChange = {},
                    results = emptyList(),
                    categoryResults = emptyList(),
                    activeCategory = null,
                    isSearching = false,
                    onProductClick = {},
                    onCategoryClick = {},
                    onClearCategoryFilter = {},
                    onAddNewProduct = { clicked++ },
                )
            }
        }

        composeTestRule.onNodeWithText(entryLabel).assertIsDisplayed().performClick()

        assertEquals(1, clicked)
    }

    @Test
    fun `entry point non visibile con query troppo corta`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                SearchStep(
                    query = "a",
                    onQueryChange = {},
                    results = emptyList(),
                    categoryResults = emptyList(),
                    activeCategory = null,
                    isSearching = false,
                    onProductClick = {},
                    onCategoryClick = {},
                    onClearCategoryFilter = {},
                    onAddNewProduct = {},
                )
            }
        }

        composeTestRule.onNodeWithText(entryLabel).assertDoesNotExist()
    }
}
