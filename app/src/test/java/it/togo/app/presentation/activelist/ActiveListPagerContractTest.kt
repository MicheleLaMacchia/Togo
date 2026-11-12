package it.togo.app.presentation.activelist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.isFocusable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import it.togo.app.domain.model.ActionItem
import it.togo.app.domain.model.ActionScope
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.ui.theme.TogoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `pager swipe region accepts drags on group headers and empty space`() {
        var swipeCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionsPage(
                    uiState = ActiveListUiState(
                        actions = listOf(
                            ActionItem(text = "Telefonare", scope = ActionScope.ALTRO),
                        ),
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { swipeCount++ },
                    onEvent = {},
                    onEditAction = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("action_scope_header_ALTRO")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("actions_page")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(2, swipeCount)
    }

    @Test
    fun `header swipe changes the selected page`() {
        composeTestRule.setContent {
            val selectedPage = remember { mutableStateOf(ActiveListPage.SHOPPING) }
            val pagerState = rememberActiveListPagerState(selectedPage.value)
            TogoTheme(darkTheme = false) {
                Column(modifier = Modifier.fillMaxSize()) {
                    ActiveListHeader(
                        page = selectedPage.value,
                        activeCount = 2,
                        activeActionsCount = 3,
                        swipeModifier = rememberPagerSwipeModifier {
                            selectedPage.value = selectedPage.value.other()
                        },
                        onEvent = { event ->
                            if (event is UiEvent.SelectPage) selectedPage.value = event.page
                        },
                        onShare = {},
                    )
                    ActiveListPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                    ) { page ->
                        Text(page.label, Modifier.testTag("page_${page.name}"))
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("active_list_header")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Da fare (3)").assertIsDisplayed()

        composeTestRule.onNodeWithTag("active_list_header")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        composeTestRule.onNodeWithTag("page_SHOPPING").assertIsDisplayed()
    }

    @Test
    fun `active shopping row reveals its delete action without swiping pager`() {
        var pagerSwipeCount = 0
        var deleteEventCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ShoppingPage(
                    uiState = ActiveListUiState(
                        activeGroups = listOf(
                            CategoryGroup(
                                level1 = TaxonomyLevel.Level1(1L, "Alimentari", 1),
                                level2Groups = listOf(
                                    Level2Group(
                                        level2 = TaxonomyLevel.Level2(2L, "Latticini", 1, 1L),
                                        level3Groups = listOf(
                                            Level3Group(
                                                level3 = TaxonomyLevel.Level3(3L, "Formaggi", 1, 2L),
                                                items = listOf(
                                                    ShoppingItem(
                                                        productId = 1L,
                                                        quantity = 1.0,
                                                        unit = StandardUnit.PIECE,
                                                    ),
                                                ),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                        productNames = mapOf(1L to "Mozzarella"),
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = { event ->
                        if (event is UiEvent.DeleteItem) deleteEventCount++
                    },
                    onAddManual = {},
                    onVoice = {},
                    onEdit = {},
                )
            }
        }

        composeTestRule.mainClock.autoAdvance = false
        val row = composeTestRule.onNodeWithTag("item_row")
        val initialLeft = row.fetchSemanticsNode().boundsInRoot.left
        row.performTouchInput { swipeLeft() }
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Elimina Mozzarella").assertIsDisplayed()
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)
        composeTestRule.onNodeWithTag("item_row").assertIsDisplayed()

        composeTestRule.onNodeWithContentDescription("Elimina Mozzarella")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)

        composeTestRule.onNodeWithTag("item_row")
            .performTouchInput { swipeRight() }
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.waitForIdle()
        assertEquals(
            initialLeft,
            row
                .fetchSemanticsNode().boundsInRoot.left,
            1f,
        )
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)

        composeTestRule.onNodeWithTag("level1_header_1")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(1, pagerSwipeCount)
        composeTestRule.onNodeWithTag("item_row").assertIsDisplayed()

        composeTestRule.onNodeWithTag("shopping_page")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertEquals(2, pagerSwipeCount)

        // Verifica il reveal con un tap fisico: se il contenuto coprisse ancora
        // il cestino, il hit-test raggiungerebbe la riga e non il pulsante sotto.
        row.performTouchInput { swipeLeft() }
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Elimina Mozzarella")
            .performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertEquals(1, deleteEventCount)
        assertEquals(2, pagerSwipeCount)
    }

    @Test
    fun `active action row reveals its delete action without swiping pager`() {
        var pagerSwipeCount = 0
        var deleteEventCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionsPage(
                    uiState = ActiveListUiState(
                        actions = listOf(ActionItem(text = "Telefonare")),
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = { event ->
                        if (event is UiEvent.DeleteAction) deleteEventCount++
                    },
                    onEditAction = {},
                )
            }
        }

        composeTestRule.mainClock.autoAdvance = false
        val foreground = composeTestRule.onNodeWithTag("action_row_foreground")
        val initialLeft = foreground.fetchSemanticsNode().boundsInRoot.left
        foreground.performTouchInput { swipeLeft() }
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("Elimina Telefonare").assertIsDisplayed()
        val finalBounds = foreground.fetchSemanticsNode().boundsInRoot
        assertTrue(
            "action row content did not move: initial=$initialLeft final=${finalBounds.left}",
            finalBounds.left < initialLeft,
        )
        val deleteLeft = composeTestRule.onNodeWithContentDescription("Elimina Telefonare")
            .fetchSemanticsNode().boundsInRoot.left
        assertTrue(
            "action row still covers the revealed delete action",
            finalBounds.right <= deleteLeft + 1f,
        )
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)

        composeTestRule.onNodeWithContentDescription("Elimina Telefonare")
            .performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)

        foreground
            .performTouchInput { swipeRight() }
        composeTestRule.mainClock.advanceTimeBy(250)
        composeTestRule.waitForIdle()
        assertEquals(
            initialLeft,
            foreground
                .fetchSemanticsNode().boundsInRoot.left,
            1f,
        )
        assertEquals(0, pagerSwipeCount)
        assertEquals(0, deleteEventCount)
    }

    @Test
    fun `non deletable checked shopping row does not start pager swipe`() {
        var pagerSwipeCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ShoppingPage(
                    uiState = ActiveListUiState(
                        checkedItems = listOf(
                            ShoppingItem(
                                productId = 1L,
                                quantity = 1.0,
                                unit = StandardUnit.PIECE,
                                isChecked = true,
                            ),
                        ),
                        productNames = mapOf(1L to "Mozzarella"),
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = {},
                    onAddManual = {},
                    onVoice = {},
                    onEdit = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Deseleziona Mozzarella.")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(0, pagerSwipeCount)
    }

    @Test
    fun `checked action row does not start pager swipe`() {
        var pagerSwipeCount = 0
        var checkEventCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionsPage(
                    uiState = ActiveListUiState(
                        checkedActions = listOf(ActionItem(text = "Telefonare", isChecked = true)),
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = { event ->
                        if (event is UiEvent.CheckOff) checkEventCount++
                    },
                    onEditAction = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Deseleziona Telefonare.")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(0, pagerSwipeCount)
        assertEquals(0, checkEventCount)
    }

    @Test
    fun `shopping empty state accepts pager swipe`() {
        var pagerSwipeCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ShoppingPage(
                    uiState = ActiveListUiState(),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = {},
                    onAddManual = {},
                    onVoice = {},
                    onEdit = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("shopping_page")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(1, pagerSwipeCount)
    }

    @Test
    fun `actions empty state accepts pager swipe`() {
        var pagerSwipeCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionsPage(
                    uiState = ActiveListUiState(),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = {},
                    onEditAction = {},
                )
            }
        }

        composeTestRule.onNodeWithTag("actions_page")
            .performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()

        assertEquals(1, pagerSwipeCount)
    }

    @Test
    fun `vertical drag on populated actions page still scrolls the list`() {
        var pagerSwipeCount = 0

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActionsPage(
                    uiState = ActiveListUiState(
                        actions = (1..30).map { index ->
                            ActionItem(
                                text = "Azione ${index.toString().padStart(2, '0')}",
                                scope = ActionScope.ALTRO,
                            )
                        },
                    ),
                    pagerSwipeModifier = rememberPagerSwipeModifier { pagerSwipeCount++ },
                    onEvent = {},
                    onEditAction = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Azione 30").assertDoesNotExist()
        repeat(5) {
            composeTestRule.onNodeWithTag("actions_list")
                .performTouchInput { swipeUp() }
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithText("Azione 30").assertIsDisplayed()
        assertEquals(0, pagerSwipeCount)
    }

    @Test
    fun `fixed header shows page counts arrows and contextual global actions`() {
        var pageSelectionEvents = 0
        composeTestRule.setContent {
            val selectedPage = remember { mutableStateOf(ActiveListPage.SHOPPING) }
            TogoTheme(darkTheme = false) {
                ActiveListHeader(
                    page = selectedPage.value,
                    activeCount = 2,
                    activeActionsCount = 3,
                    swipeModifier = Modifier,
                    onEvent = { event ->
                        if (event is UiEvent.SelectPage) {
                            pageSelectionEvents++
                            selectedPage.value = event.page
                        }
                    },
                    onShare = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        val previousArrow = composeTestRule.onNodeWithContentDescription("Torna a Spesa")
        previousArrow.assertExists()
        previousArrow.assertIsNotEnabled()
        previousArrow.assertHasClickAction()
        previousArrow.assert(isFocusable())
        composeTestRule.onNodeWithContentDescription("Vai a Da fare")
            .assertHasClickAction()
        composeTestRule.onNodeWithText("Spesa (2)").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
        composeTestRule.onNodeWithContentDescription("Storico").assertExists()
        composeTestRule.onNodeWithContentDescription("Condividi lista").assertExists()
        composeTestRule.onNodeWithContentDescription("Impostazioni").assertExists()

        previousArrow.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        assertEquals(0, pageSelectionEvents)

        composeTestRule.onNodeWithContentDescription("Vai a Da fare").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Da fare (3)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Da fare (3)").assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.LiveRegion,
                LiveRegionMode.Polite,
            ),
        )
        composeTestRule.onNodeWithContentDescription("Torna a Spesa").assertHasClickAction()
        val nextArrow = composeTestRule.onNodeWithContentDescription("Vai a Da fare")
        nextArrow.assertIsNotEnabled()
        nextArrow.assertHasClickAction()
        nextArrow.assert(isFocusable())
        nextArrow.performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Da fare (3)").assertIsDisplayed()
        assertEquals(1, pageSelectionEvents)
        composeTestRule.onNodeWithContentDescription("Storico").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Condividi lista").assertExists()
        composeTestRule.onNodeWithContentDescription("Impostazioni").assertExists()

        composeTestRule.onNodeWithContentDescription("Torna a Spesa").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spesa (2)").assertIsDisplayed()
        assertEquals(2, pageSelectionEvents)
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
