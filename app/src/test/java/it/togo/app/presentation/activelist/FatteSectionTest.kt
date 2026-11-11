package it.togo.app.presentation.activelist

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ActionUrgency
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric) della sezione "Fatte (N)" (Story 9.1).
 *
 * Copre conteggio, default espansa, collapse/expand con stato semantico,
 * assenza della sezione quando N = 0 e deselezione tramite checkbox.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FatteSectionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun action(
        id: String,
        text: String,
        scope: ActionScope = ActionScope.CASA,
        urgency: ActionUrgency = ActionUrgency.A_TEMPO_PERSO,
        createdAt: Long = 1L,
        updatedAt: Long = 2L,
    ) = ActionItem(
        id = id,
        text = text,
        scope = scope,
        urgency = urgency,
        isChecked = true,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private fun render(
        items: List<ActionItem>,
        onUncheck: (String) -> Unit = {},
    ) {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                FatteSection(
                    items = items,
                    onUncheck = onUncheck,
                    colors = togoColors(),
                    typography = togoTypography(),
                    spacing = togoSpacing(),
                    tokens = componentTokens(),
                )
            }
        }
    }

    @Test
    fun `expanded by default with count heading and rows`() {
        render(
            listOf(
                action(id = "a", text = "Pagare la bolletta"),
                action(id = "b", text = "Chiamare l'idraulico"),
            )
        )

        composeTestRule.onNodeWithText("Fatte (2)").assertExists()
        composeTestRule.onNodeWithText("Pagare la bolletta").assertExists()
        composeTestRule.onNodeWithText("Chiamare l'idraulico").assertExists()

        val header = composeTestRule.onNodeWithContentDescription(
            "Fatte, 2 azioni. Doppio tocco per collassare."
        )
        header
            .assertExists()
            .assertHasClickAction()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "espansa",
                )
            )
    }

    @Test
    fun `singular count is announced with one action`() {
        render(listOf(action(id = "a", text = "Pagare la bolletta")))

        composeTestRule.onNodeWithText("Fatte (1)").assertExists()
        composeTestRule
            .onNodeWithContentDescription("Fatte, 1 azione. Doppio tocco per collassare.")
            .assertExists()
    }

    @Test
    fun `header click collapses and expands the rows`() {
        render(
            listOf(
                action(id = "a", text = "Pagare la bolletta"),
                action(id = "b", text = "Chiamare l'idraulico"),
            )
        )

        composeTestRule
            .onNodeWithContentDescription("Fatte, 2 azioni. Doppio tocco per collassare.")
            .performClick()

        composeTestRule
            .onNodeWithContentDescription("Fatte, 2 azioni. Doppio tocco per espandere.")
            .assertExists()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "collassata",
                )
            )
        composeTestRule.onNodeWithText("Pagare la bolletta").assertDoesNotExist()
        composeTestRule.onNodeWithText("Chiamare l'idraulico").assertDoesNotExist()
        // L'header resta visibile con il conteggio.
        composeTestRule.onNodeWithText("Fatte (2)").assertExists()

        composeTestRule
            .onNodeWithContentDescription("Fatte, 2 azioni. Doppio tocco per espandere.")
            .performClick()

        composeTestRule.onNodeWithText("Pagare la bolletta").assertExists()
        composeTestRule.onNodeWithText("Chiamare l'idraulico").assertExists()
    }

    @Test
    fun `empty section emits no nodes`() {
        render(emptyList())

        composeTestRule.onNodeWithTag("fatto_section").assertDoesNotExist()
        composeTestRule.onNodeWithText("Fatte (0)").assertDoesNotExist()
    }

    @Test
    fun `checkbox unchecks the action`() {
        var uncheckedId: String? = null
        render(listOf(action(id = "a", text = "Pagare la bolletta"))) { id ->
            uncheckedId = id
        }

        composeTestRule
            .onNodeWithContentDescription("Deseleziona Pagare la bolletta.")
            .assertExists()
            .performClick()

        assertEquals("a", uncheckedId)
    }
}
