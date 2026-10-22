package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.ui.theme.TogoTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test Compose (Robolectric) del form "Nuovo prodotto" (Story 7.1).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NewProductFormTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val taxonomy = listOf(
        TaxonomyLevel.Level1(id = 1L, name = "Alimentari", sortOrder = 1),
        TaxonomyLevel.Level2(id = 21L, name = "Bevande", sortOrder = 1, level1Id = 1L),
        TaxonomyLevel.Level3(
            id = 12L,
            name = "Bevande vegetali",
            sortOrder = 1,
            level2Id = 21L,
            isUserDefined = false,
        ),
    )

    @Before
    fun setup() {
        stopKoin()
    }

    private fun setForm(
        name: String = "",
        level1Id: Long? = null,
        level2Id: Long? = null,
        level3Id: Long? = null,
        error: String? = null,
        isSaving: Boolean = false,
    ) {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                NewProductForm(
                    name = name,
                    taxonomy = taxonomy,
                    level1Id = level1Id,
                    level2Id = level2Id,
                    level3Id = level3Id,
                    error = error,
                    isSaving = isSaving,
                    onNameChange = {},
                    onLevel1Select = {},
                    onLevel2Select = {},
                    onLevel3Select = {},
                    onCancel = {},
                    onCreate = {},
                )
            }
        }
    }

    @Test
    fun `stato incompleto disabilita Crea prodotto`() {
        setForm(name = "Latte di avena", level1Id = 1L, level2Id = 21L)

        composeTestRule.onNodeWithText("Crea prodotto").assertIsNotEnabled()
    }

    @Test
    fun `nome vuoto disabilita Crea prodotto anche con tassonomia completa`() {
        setForm(name = "", level1Id = 1L, level2Id = 21L, level3Id = 12L)

        composeTestRule.onNodeWithText("Crea prodotto").assertIsNotEnabled()
    }

    @Test
    fun `stato completo abilita Crea prodotto`() {
        setForm(name = "Latte di avena", level1Id = 1L, level2Id = 21L, level3Id = 12L)

        composeTestRule.onNodeWithText("Crea prodotto").assertIsEnabled()
    }

    @Test
    fun `stato di salvataggio mostra Creazione e disabilita le azioni`() {
        setForm(
            name = "Latte di avena",
            level1Id = 1L,
            level2Id = 21L,
            level3Id = 12L,
            isSaving = true,
        )

        composeTestRule.onNodeWithText("Creazione...").assertExists().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Annulla").assertIsNotEnabled()
    }

    @Test
    fun `errore valorizzato e visibile`() {
        setForm(error = "Seleziona Livello 1, Livello 2 e Livello 3")

        composeTestRule.onNodeWithText("Seleziona Livello 1, Livello 2 e Livello 3").assertExists()
    }
}
