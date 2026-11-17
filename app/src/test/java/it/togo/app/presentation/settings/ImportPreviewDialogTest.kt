package it.togo.app.presentation.settings

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsProperties
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.usecase.ImportExecutionResult
import it.togo.app.domain.usecase.ImportPlan
import it.togo.app.domain.usecase.PlannedImportItem
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImportPreviewDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val plan = ImportPlan(
        items = listOf(
            PlannedImportItem(
                level2Id = 1L,
                level2Name = "Pasta secca",
                level3Id = null,
                level3Name = "Penne",
                level1Name = "Alimentari",
                productId = null,
                productName = "Penne",
                quantity = 2.0,
                unit = StandardUnit.PIECE,
                brand = null,
                variant = null,
                lineNumber = 4,
            ),
        ),
        actions = listOf(
            ActionItem(
                text = "Chiamare il medico",
                scope = ActionScope.CASA,
                urgency = ActionUrgency.URGENTE,
            ),
        ),
        skippedDuplicateActions = 2,
    )

    @Test
    fun `anteprima annuncia la regione e mostra tutti i conteggi e la conferma`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ImportPreviewDialog(
                    plan = plan,
                    isImporting = false,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Anteprima importazione")
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        composeTestRule.onNode(
            SemanticsMatcher.expectValue(
                SemanticsProperties.PaneTitle,
                "Anteprima importazione",
            ),
            useUnmergedTree = true,
        ).assertExists()
        composeTestRule.onNodeWithText("Prodotti da importare: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Azioni da importare: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Azioni duplicate saltate: 2.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Categorie da creare: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Prodotti da creare: 1.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Importa 1 prodotto e 1 azione").assertIsDisplayed()
    }

    @Test
    fun `messaggio di esito riporta import duplicati aggiornamenti e creazioni`() {
        val message = buildImportSuccessMessage(
            ImportExecutionResult.Success(
                imported = 2,
                importedActions = 1,
                updatedExisting = 1,
                createdProducts = 1,
                createdCategories = 1,
                skippedDuplicateActions = 2,
            ),
        )

        assertEquals(
            "Importazione completata. Prodotti importati: 2. Azione importata: 1. " +
                "Azioni duplicate saltate: 2. Voce aggiornata: 1. " +
                "Categoria creata: 1. Prodotto creato: 1.",
            message,
        )
    }

    @Test
    fun `conferma mantiene numero e nome di prodotti e azioni`() {
        var confirmed = false
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ImportPreviewDialog(
                    plan = plan,
                    isImporting = false,
                    onConfirm = { confirmed = true },
                    onDismiss = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Importa 1 prodotto e 1 azione").performClick()

        assertEquals(true, confirmed)
    }
}
