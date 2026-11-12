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
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
}
