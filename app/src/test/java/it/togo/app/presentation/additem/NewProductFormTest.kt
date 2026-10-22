package it.togo.app.presentation.additem

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.domain.model.TaxonomyLevel
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
        isLevel3ProposalVisible: Boolean = false,
        newLevel3Name: String = "",
        level3ProposalError: String? = null,
        isCreatingLevel3: Boolean = false,
        onShowLevel3Proposal: () -> Unit = {},
        onHideLevel3Proposal: () -> Unit = {},
        onNewLevel3NameChange: (String) -> Unit = {},
        onCreateLevel3: () -> Unit = {},
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
                    isLevel3ProposalVisible = isLevel3ProposalVisible,
                    newLevel3Name = newLevel3Name,
                    level3ProposalError = level3ProposalError,
                    isCreatingLevel3 = isCreatingLevel3,
                    onNameChange = {},
                    onLevel1Select = {},
                    onLevel2Select = {},
                    onLevel3Select = {},
                    onShowLevel3Proposal = onShowLevel3Proposal,
                    onHideLevel3Proposal = onHideLevel3Proposal,
                    onNewLevel3NameChange = onNewLevel3NameChange,
                    onCreateLevel3 = onCreateLevel3,
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

    @Test
    fun `Proponi nuovo Livello 3 non visibile senza Livello 2`() {
        setForm(level1Id = 1L)

        composeTestRule.onNodeWithText("Proponi nuovo Livello 3").assertDoesNotExist()
    }

    @Test
    fun `Proponi nuovo Livello 3 visibile con Livello 2 selezionato`() {
        setForm(level1Id = 1L, level2Id = 21L)

        composeTestRule.onNodeWithText("Proponi nuovo Livello 3").assertExists()
    }

    @Test
    fun `click su Proponi nuovo Livello 3 invoca il callback`() {
        var clicks = 0
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            onShowLevel3Proposal = { clicks++ },
        )

        composeTestRule.onNodeWithText("Proponi nuovo Livello 3").performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `pannello aperto con nome vuoto disabilita Crea Livello 3`() {
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
        )

        composeTestRule.onNodeWithText("Nome nuovo Livello 3 *").assertExists()
        composeTestRule.onNodeWithText("Crea Livello 3").assertIsNotEnabled()
    }

    @Test
    fun `pannello con nome abilita Crea Livello 3 e invoca il callback`() {
        var clicks = 0
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
            newLevel3Name = "Bevande bio",
            onCreateLevel3 = { clicks++ },
        )

        composeTestRule.onNodeWithText("Crea Livello 3")
            .assertIsEnabled()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `errore della proposta Livello 3 visibile`() {
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
            level3ProposalError = "Inserisci il nome del nuovo Livello 3",
        )

        composeTestRule
            .onNodeWithText("Inserisci il nome del nuovo Livello 3")
            .assertExists()
    }

    @Test
    fun `non esistono azioni per creare Livello 1 o Livello 2`() {
        setForm(level1Id = 1L, level2Id = 21L)

        composeTestRule.onNodeWithText("Proponi nuovo Livello 1").assertDoesNotExist()
        composeTestRule.onNodeWithText("Proponi nuovo Livello 2").assertDoesNotExist()
    }

    @Test
    fun `creazione Livello 3 in corso mostra Creazione e disabilita i controlli`() {
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
            newLevel3Name = "Bevande bio",
            isCreatingLevel3 = true,
        )

        composeTestRule.onNodeWithText("Creazione...").assertExists().assertIsNotEnabled()
        composeTestRule.onNodeWithText("Crea prodotto").assertIsNotEnabled()
        composeTestRule.onNodeWithText("Bevande bio").assertIsNotEnabled()
    }

    @Test
    fun `salvataggio prodotto disabilita Crea Livello 3`() {
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
            newLevel3Name = "Bevande bio",
            isSaving = true,
        )

        composeTestRule.onNodeWithText("Crea Livello 3").assertIsNotEnabled()
    }

    @Test
    fun `Annulla proposta chiude solo il pannello e invoca il callback`() {
        var hides = 0
        setForm(
            level1Id = 1L,
            level2Id = 21L,
            isLevel3ProposalVisible = true,
            newLevel3Name = "Bevande bio",
            onHideLevel3Proposal = { hides++ },
        )

        composeTestRule.onNodeWithText("Annulla proposta").performClick()

        assertEquals(1, hides)
    }
}
