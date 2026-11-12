package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import it.togo.app.domain.model.ActionItem
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveListPagerContractTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `pager contains exactly shopping then actions`() {
        assertEquals(
            listOf(ActiveListPage.SHOPPING, ActiveListPage.ACTIONS),
            ActiveListPage.pagerPages,
        )
    }

    @Test
    fun `cold start selects shopping`() {
        assertEquals(ActiveListPage.SHOPPING, ActiveListUiState().currentPage)
    }

    @Test
    fun `rendered pager has two pages and shows selected page in defined order`() {
        lateinit var pagerState: PagerState

        composeTestRule.setContent {
            val selectedPage = remember { mutableStateOf(ActiveListPage.SHOPPING) }
            pagerState = rememberActiveListPagerState(selectedPage.value)

            Column {
                Button(onClick = { selectedPage.value = ActiveListPage.ACTIONS }) {
                    Text("Seleziona Da fare")
                }
                ActiveListPager(state = pagerState) { page ->
                    Text(page.label, Modifier.testTag("page_${page.name}"))
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.runOnIdle {
            assertEquals(2, pagerState.pageCount)
            assertEquals(0, pagerState.currentPage)
        }
        composeTestRule.onNodeWithTag("page_SHOPPING").assertIsDisplayed()

        composeTestRule.onNodeWithText("Seleziona Da fare").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.runOnIdle {
            assertEquals(1, pagerState.currentPage)
        }
        composeTestRule.onNodeWithTag("page_ACTIONS").assertIsDisplayed()
    }

    @Test
    fun `fixed header shows page counts arrows and contextual global actions`() {
        composeTestRule.setContent {
            val selectedPage = remember { mutableStateOf(ActiveListPage.SHOPPING) }
            TogoTheme(darkTheme = false) {
                ActiveListHeader(
                    page = selectedPage.value,
                    activeCount = 2,
                    activeActionsCount = 3,
                    swipeModifier = Modifier,
                    onEvent = { event ->
                        if (event is UiEvent.SelectPage) selectedPage.value = event.page
                    },
                    onShare = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Pagina precedente").assertExists()
        composeTestRule.onNodeWithContentDescription("Pagina successiva").assertExists()
        composeTestRule.onNodeWithContentDescription("Storico").assertExists()
        composeTestRule.onNodeWithContentDescription("Condividi lista").assertExists()
        composeTestRule.onNodeWithContentDescription("Impostazioni").assertExists()

        composeTestRule.onNodeWithContentDescription("Pagina successiva").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Da fare (3)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Storico").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Condividi lista").assertExists()
        composeTestRule.onNodeWithContentDescription("Impostazioni").assertExists()

        composeTestRule.onNodeWithContentDescription("Pagina precedente").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Storico").assertExists()
    }

    @Test
    fun `actions count excludes checked actions`() {
        val state = ActiveListUiState(
            actions = listOf(ActionItem(text = "Comprare il pane")),
            checkedActions = listOf(ActionItem(text = "Chiamare", isChecked = true)),
        )

        assertEquals(1, state.activeActionsCount)
    }
}
