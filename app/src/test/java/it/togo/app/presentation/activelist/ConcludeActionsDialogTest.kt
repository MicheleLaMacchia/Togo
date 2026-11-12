package it.togo.app.presentation.activelist

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Test JVM (Robolectric + Compose) del dialog "Concludi attività" (Story 9.2).
 *
 * Copre titolo, corpo singolare/plurale, primario distruttivo con icona
 * warning decorativa, secondario "Annulla", callback e touch target.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ConcludeActionsDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun render(
        count: Int,
        darkTheme: Boolean = false,
        onConfirm: () -> Unit = {},
        onDismiss: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            TogoTheme(darkTheme = darkTheme) {
                ConcludeActionsDialog(
                    count = count,
                    onConfirm = onConfirm,
                    onDismiss = onDismiss,
                )
            }
        }
    }

    @Test
    fun `shows title and plural body with count`() {
        render(count = 3)

        composeTestRule.onNodeWithText("Concludere le attività?").assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                "Verranno eliminate 3 attività spuntate. " +
                    "L'operazione non si può annullare."
            )
            .assertIsDisplayed()
    }

    @Test
    fun `uses singular body for one action`() {
        render(count = 1)

        composeTestRule
            .onNodeWithText(
                "Verrà eliminata 1 attività spuntata. " +
                    "L'operazione non si può annullare."
            )
            .assertIsDisplayed()
    }

    @Test
    fun `title is announced as heading`() {
        render(count = 2)

        composeTestRule
            .onNode(
                hasText("Concludere le attività?") and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
            )
            .assertExists()
    }

    @Test
    fun `confirm is a destructive full action with warning icon and 48dp target`() {
        render(count = 2)

        composeTestRule
            .onNodeWithTag("conclude_actions_confirm_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithText("Concludi attività").assertExists()
        composeTestRule
            .onNodeWithTag("conclude_actions_warning_icon", useUnmergedTree = true)
            .assertExists()
    }

    @Test
    fun `warning icon is decorative`() {
        render(count = 2)

        composeTestRule.onNodeWithContentDescription("Warning").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Attenzione").assertDoesNotExist()
    }

    @Test
    fun `confirm invokes onConfirm only`() {
        var confirmed = 0
        var dismissed = 0
        render(
            count = 1,
            onConfirm = { confirmed++ },
            onDismiss = { dismissed++ },
        )

        composeTestRule.onNodeWithTag("conclude_actions_confirm_button").performClick()

        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun `cancel invokes onDismiss only and has 48dp target`() {
        var confirmed = 0
        var dismissed = 0
        render(
            count = 1,
            onConfirm = { confirmed++ },
            onDismiss = { dismissed++ },
        )

        composeTestRule
            .onNodeWithTag("conclude_actions_cancel_button")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun `dark theme keeps content and actions`() {
        render(count = 2, darkTheme = true)

        composeTestRule.onNodeWithText("Concludere le attività?").assertIsDisplayed()
        composeTestRule
            .onNodeWithTag("conclude_actions_confirm_button")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeTestRule
            .onNodeWithTag("conclude_actions_cancel_button")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun `count zero throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            render(count = 0)
        }
    }

    @Test
    fun `negative count throws`() {
        assertThrows(IllegalArgumentException::class.java) {
            render(count = -1)
        }
    }

    @Test
    fun `body text is announced by TalkBack with count and irreversibility`() {
        render(count = 2)

        composeTestRule
            .onNodeWithText(
                "Verranno eliminate 2 attività spuntate. " +
                    "L'operazione non si può annullare."
            )
            .assert(
                androidx.compose.ui.test.SemanticsMatcher.expectValue(
                    androidx.compose.ui.semantics.SemanticsProperties.ContentDescription,
                    listOf("Verranno eliminate 2 attività spuntate. L'operazione non si può annullare."),
                )
            )
    }
}
