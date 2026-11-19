package it.togo.app.presentation.activelist

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertIsFocused
import it.togo.app.ui.theme.TogoTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActionSheetTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `urgenza and ambito expose labeled radio groups and selected options`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionSheet(onDismiss = {})
            }
        }

        composeTestRule.onNodeWithContentDescription("Selezione urgenza").assertExists()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        composeTestRule.onNodeWithContentDescription("Selezione ambito").assertExists()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
        composeTestRule.onNodeWithTag("action_sheet_pane")
            .assertIsFocused()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.PaneTitle,
                    "Nuova azione",
                )
            )

        composeTestRule.onNodeWithText("A tempo perso")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
        composeTestRule.onNodeWithText("Altro")
            .assertIsSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
        composeTestRule.onNodeWithText("Urgente")
            .assertIsNotSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
        composeTestRule.onNodeWithText("Salute")
            .assertIsNotSelected()
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)
            )
    }
}
