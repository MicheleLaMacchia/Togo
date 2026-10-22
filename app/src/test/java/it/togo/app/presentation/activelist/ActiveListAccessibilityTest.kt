package it.togo.app.presentation.activelist

import android.Manifest
import android.provider.Settings
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import it.togo.app.AppNavigator
import it.togo.app.data.voice.AndroidVoiceRecognitionGateway
import it.togo.app.domain.model.CanonicalProduct
import it.togo.app.domain.model.ShoppingItem
import it.togo.app.domain.model.StandardUnit
import it.togo.app.domain.model.TaxonomyLevel
import it.togo.app.domain.repository.CatalogRepository
import it.togo.app.domain.repository.ShoppingListRepository
import it.togo.app.domain.voice.ErrorCode
import it.togo.app.domain.voice.VoiceRecognitionGateway
import it.togo.app.domain.voice.VoiceRecognitionState
import it.togo.app.presentation.voice.VoiceBottomSheetViewModel
import it.togo.app.ui.theme.TogoTheme
import it.togo.app.ui.theme.componentTokens
import it.togo.app.ui.theme.togoColors
import it.togo.app.ui.theme.togoSpacing
import it.togo.app.ui.theme.togoTypography
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.RobolectricTestRunner

/**
 * Test JVM (Robolectric) per le semantiche TalkBack della Lista Attiva (Story 6.1).
 *
 * Copre righe prodotto, checkbox, sezione "Presi", App Bar/FAB, callback e la
 * mappatura reale prodotti/tassonomia del ViewModel.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ActiveListAccessibilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val uncheckedItem = ShoppingItem(
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.PIECE,
        isChecked = false,
    )

    private val checkedItem = ShoppingItem(
        productId = 1L,
        quantity = 2.0,
        unit = StandardUnit.PIECE,
        isChecked = true,
    )

    @Test
    fun `active row announces name quantity category and state`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = uncheckedItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini, Formaggi, Mozzarella",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        composeTestRule.onNodeWithText("Mozzarella").assertExists()
        composeTestRule.onNodeWithText("2 pz").assertExists()
        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, non selezionato. Doppio tocco per modificare."
            )
            .assertExists()
    }

    @Test
    fun `checked row announces checked state and uncheck action`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = checkedItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini, Formaggi, Mozzarella",
                    rowActionDescription = null,
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, selezionato."
            )
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription("Deseleziona Mozzarella.")
            .assertExists()
            .assertIsOn()
            .assertHasClickAction()
    }

    @Test
    fun `checkbox is a toggleable control that invokes onCheckedChange only`() {
        var checked: Boolean? = null
        var rowClicked = false

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = uncheckedItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini",
                    onCheckChange = { checked = it },
                    onClick = { rowClicked = true },
                    onDelete = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Spunta Mozzarella.")
            .assertExists()
            .assertIsOff()
            .assertHasClickAction()
            .performClick()

        assertEquals(true, checked)
        assertFalse("il tap sulla checkbox non deve aprire la modifica", rowClicked)
    }

    @Test
    fun `row tap invokes edit callback only`() {
        var checked: Boolean? = null
        var rowClicked = false

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = uncheckedItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini",
                    onCheckChange = { checked = it },
                    onClick = { rowClicked = true },
                    onDelete = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Latticini, non selezionato. Doppio tocco per modificare."
            )
            .performClick()

        assertTrue("il tap sulla riga deve aprire la modifica", rowClicked)
        assertEquals(null, checked)
    }

    @Test
    fun `checked presi row is not clickable but checkbox still toggles`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = checkedItem,
                    productName = "Mozzarella",
                    categoryLabel = "Latticini, Formaggi, Mozzarella",
                    rowActionDescription = null,
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, selezionato."
            )
            .assertExists()
            .assertHasNoClickAction()

        composeTestRule
            .onNodeWithContentDescription("Deseleziona Mozzarella.")
            .assertHasClickAction()
    }

    @Test
    fun `category fallback is never empty`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = uncheckedItem,
                    productName = "Mozzarella",
                    categoryLabel = null,
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Categoria non assegnata, non selezionato. Doppio tocco per modificare."
            )
            .assertExists()
    }

    @Test
    fun `presi header announces count and expanded state and toggles`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                PresiSection(
                    items = listOf(checkedItem),
                    productNames = mapOf(1L to "Mozzarella"),
                    categoryLabels = mapOf(1L to "Latticini, Formaggi, Mozzarella"),
                    onUncheck = {},
                    onCheckout = {},
                    colors = togoColors(),
                    typography = togoTypography(),
                    spacing = togoSpacing(),
                    tokens = componentTokens(),
                )
            }
        }

        val expandedDescription = "Presi, 1 voce. Doppio tocco per collassare."
        composeTestRule
            .onNodeWithContentDescription(expandedDescription)
            .assertExists()
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "espansa",
                )
            )

        composeTestRule.onNodeWithText("Presi (1)").assertExists()

        composeTestRule.onNodeWithContentDescription(expandedDescription).performClick()

        composeTestRule
            .onNodeWithContentDescription("Presi, 1 voce. Doppio tocco per espandere.")
            .assertExists()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "collassata",
                )
            )
    }

    @Test
    fun `voice fab exposes labels for every state`() {
        // Un solo setContent: lo stato viene aggiornato tra le asserzioni.
        var state by mutableStateOf<VoiceRecognitionState>(VoiceRecognitionState.Idle)
        var processingInvoked = false

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                VoiceFab(state = state, onClick = { processingInvoked = true })
            }
        }

        // Idle
        composeTestRule
            .onNodeWithContentDescription("Aggiungi prodotto con la voce. Pulsante.")
            .assertExists()

        // Listening
        composeTestRule.runOnIdle { state = VoiceRecognitionState.Listening }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ascolto...").assertExists()
        composeTestRule
            .onNodeWithContentDescription("Interrompi ascolto")
            .assertExists()

        // Processing: etichetta + descrizione e nessuna attivazione
        composeTestRule.runOnIdle { state = VoiceRecognitionState.Processing }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Elaborazione in corso...").assertExists()
        val processingDescription = "Elaborazione in corso. Attendere."
        composeTestRule
            .onNodeWithContentDescription(processingDescription)
            .assertExists()
            .assertIsNotEnabled()
            .performClick()
        assertFalse(
            "durante l'elaborazione il FAB non deve avviare un nuovo ascolto",
            processingInvoked
        )

        // Error: comunica l'azione di riprova/nuovo prodotto
        composeTestRule.runOnIdle {
            state = VoiceRecognitionState.Error(ErrorCode.UNKNOWN, "boom")
        }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Errore di riconoscimento. Tocca per riprovare.")
            .assertExists()
        composeTestRule.onNodeWithContentDescription("Riprova").assertExists()

        // Result: label visibile completa, descrizione FAB limitata all'azione
        composeTestRule.runOnIdle { state = VoiceRecognitionState.Result("Pane") }
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Prodotto riconosciuto. Tocca per aggiungere un altro prodotto.")
            .assertExists()
        composeTestRule
            .onNodeWithContentDescription("Aggiungi un altro prodotto")
            .assertExists()
    }

    @Test
    fun `app settings intent points to package details`() {
        val intent = appSettingsIntent("it.togo.app")

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
        assertEquals("package:it.togo.app", intent.data.toString())
    }

    @Test
    fun `voice fab starts and cancels listening with granted permission`() {
        val gateway = FakeVoiceGateway()
        val voiceViewModel = VoiceBottomSheetViewModel(gateway)
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(),
            catalogRepository = FakeCatalogRepository(),
        )
        shadowOf(RuntimeEnvironment.getApplication())
            .grantPermissions(Manifest.permission.RECORD_AUDIO)

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Aggiungi prodotto con la voce. Pulsante.")
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            gateway.state.value is VoiceRecognitionState.Listening
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Interrompi ascolto").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            gateway.state.value is VoiceRecognitionState.Idle
        }
    }

    @Test
    fun `voice fab does not start listening when permission is denied`() {
        val gateway = FakeVoiceGateway()
        val voiceViewModel = VoiceBottomSheetViewModel(gateway)
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(),
            catalogRepository = FakeCatalogRepository(),
        )
        shadowOf(RuntimeEnvironment.getApplication())
            .denyPermissions(Manifest.permission.RECORD_AUDIO)

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = {},
                )
            }
        }

        composeTestRule
            .onNodeWithContentDescription("Aggiungi prodotto con la voce. Pulsante.")
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "senza permesso il gateway non deve iniziare l'ascolto",
            gateway.state.value is VoiceRecognitionState.Idle,
        )
    }

    @Test
    fun `settings opens learned rules and back returns to active list`() {
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = {},
                    activeListContent = { onEvent ->
                        androidx.compose.foundation.layout.Column {
                            Text("Lista attiva")
                            Button(onClick = { onEvent(UiEvent.NavigateToSettings) }) {
                                Text("Impostazioni")
                            }
                        }
                    },
                    learnedRulesContent = { onBack ->
                        androidx.compose.foundation.layout.Column {
                            Text("Regole apprese")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                    historyContent = { onBack ->
                        androidx.compose.foundation.layout.Column {
                            Text("Storico")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithText("Lista attiva").assertExists()
        composeTestRule.onNodeWithText("Impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()
        composeTestRule.onNodeWithText("Indietro").performClick()
        composeTestRule.onNodeWithText("Lista attiva").assertExists()
    }

    @Test
    fun `real settings button opens learned rules`() {
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(),
            catalogRepository = FakeCatalogRepository(),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                AppNavigator(
                    onActiveListEvent = viewModel::onEvent,
                    activeListContent = { onEvent ->
                        ActiveListScreen(
                            viewModel = viewModel,
                            voiceViewModel = voiceViewModel,
                            voiceGateway = AndroidVoiceRecognitionGateway(),
                            onEvent = onEvent,
                        )
                    },
                    learnedRulesContent = { onBack ->
                        androidx.compose.foundation.layout.Column {
                            Text("Regole apprese")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                    historyContent = { onBack ->
                        androidx.compose.foundation.layout.Column {
                            Text("Storico")
                            Button(onClick = onBack) { Text("Indietro") }
                        }
                    },
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Impostazioni").performClick()
        composeTestRule.onNodeWithText("Regole apprese").assertExists()
        composeTestRule.onNodeWithText("Indietro").performClick()
        composeTestRule
            .onNodeWithContentDescription("Aggiungi voce")
            .assertExists()
    }

    @Test
    fun `voice gateway launcher follows composition lifecycle`() {
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(),
            catalogRepository = FakeCatalogRepository(),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())
        val gateway = AndroidVoiceRecognitionGateway()
        var showScreen by mutableStateOf(true)

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                if (showScreen) {
                    ActiveListScreen(
                        viewModel = viewModel,
                        voiceViewModel = voiceViewModel,
                        voiceGateway = gateway,
                        onEvent = {},
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertTrue("il launcher deve essere registrato mentre la schermata è visibile", gateway.isLauncherRegistered)

        composeTestRule.runOnIdle { showScreen = false }
        composeTestRule.waitForIdle()
        assertFalse("il launcher deve essere rilasciato all'uscita della schermata", gateway.isLauncherRegistered)
    }

    @Test
    fun `all checked items keep presi section visible`() {
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(items = listOf(checkedItem)),
            catalogRepository = FakeCatalogRepository(),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = viewModel::onEvent,
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Concludi spesa").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule
            .onNodeWithContentDescription("Presi, 1 voce. Doppio tocco per collassare.")
            .assertExists()
        composeTestRule.onNodeWithText("Concludi spesa").assertExists()
        composeTestRule.onNode(
            hasTestTag("presi_section") and hasAnyDescendant(hasText("Concludi spesa"))
        ).assertExists()

        composeTestRule.onNodeWithText("Concludi spesa").performClick()
        // Le voci in "Presi" sono quelle da concludere (spec 2-5): il checkout
        // non deve lamentare "nessuna voce attiva".
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithText("Spesa conclusa (1 voci)")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        // Regressione 22/10: una sola snackbar, nessun testo duplicato.
        composeTestRule
            .onAllNodesWithText("Spesa conclusa (1 voci)")
            .assertCountEquals(1)
    }

    @Test
    fun `single flow shows each item only once`() {
        val taxonomy = listOf(
            TaxonomyLevel.Level1(id = 1L, name = "Latticini", sortOrder = 1),
            TaxonomyLevel.Level2(id = 2L, name = "Formaggi", sortOrder = 1, level1Id = 1L),
            TaxonomyLevel.Level3(id = 3L, name = "Mozzarella", sortOrder = 1, level2Id = 2L),
        )
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(items = listOf(uncheckedItem)),
            catalogRepository = FakeCatalogRepository(
                taxonomy = taxonomy,
                products = mapOf(
                    1L to CanonicalProduct(id = 1L, name = "Mozzarella", level3Id = 3L),
                ),
            ),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = {},
                )
            }
        }

        val description = "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, " +
            "non selezionato. Doppio tocco per modificare."
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onAllNodesWithContentDescription(description)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.onAllNodesWithContentDescription(description).assertCountEquals(1)
    }

    @Test
    fun `presi checkout button invokes callback`() {
        var checkoutInvoked = false

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                PresiSection(
                    items = listOf(checkedItem),
                    productNames = mapOf(1L to "Mozzarella"),
                    categoryLabels = mapOf(1L to "Latticini, Formaggi, Mozzarella"),
                    onUncheck = {},
                    onCheckout = { checkoutInvoked = true },
                    colors = togoColors(),
                    typography = togoTypography(),
                    spacing = togoSpacing(),
                    tokens = componentTokens(),
                )
            }
        }

        composeTestRule.onNodeWithText("Concludi spesa").assertExists().performClick()
        assertTrue("il pulsante deve invocare il checkout", checkoutInvoked)
    }

    @Test
    fun `active list app bar exposes meaningful descriptions`() {
        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(),
            catalogRepository = FakeCatalogRepository(),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = {},
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("Storico").assertExists()
        composeTestRule.onNodeWithContentDescription("Condividi lista").assertExists()
        composeTestRule.onNodeWithContentDescription("Impostazioni").assertExists()
        composeTestRule.onNodeWithContentDescription("Aggiungi voce").assertExists()
        composeTestRule
            .onNodeWithContentDescription("Aggiungi prodotto con la voce. Pulsante.")
            .assertExists()
    }

    @Test
    fun `view model maps checked item to canonical category label`() {
        val taxonomy = listOf(
            TaxonomyLevel.Level1(id = 1L, name = "Latticini", sortOrder = 1),
            TaxonomyLevel.Level2(id = 2L, name = "Formaggi", sortOrder = 1, level1Id = 1L),
            TaxonomyLevel.Level3(id = 3L, name = "Mozzarella", sortOrder = 1, level2Id = 2L),
            TaxonomyLevel.Level1(id = 4L, name = "Dispensa", sortOrder = 2),
            TaxonomyLevel.Level2(id = 5L, name = "Panetteria", sortOrder = 1, level1Id = 4L),
            TaxonomyLevel.Level3(id = 6L, name = "Pane", sortOrder = 1, level2Id = 5L),
        )
        val activeItem = ShoppingItem(productId = 2L, quantity = 1.0, unit = StandardUnit.PIECE)
        val checked = ShoppingItem(productId = 1L, quantity = 2.0, unit = StandardUnit.PIECE, isChecked = true)

        val viewModel = ActiveListViewModel(
            shoppingRepository = FakeShoppingListRepository(items = listOf(activeItem, checked)),
            catalogRepository = FakeCatalogRepository(
                taxonomy = taxonomy,
                products = mapOf(
                    1L to CanonicalProduct(id = 1L, name = "Mozzarella", level3Id = 3L),
                    2L to CanonicalProduct(id = 2L, name = "Pane", level3Id = 6L),
                ),
            ),
        )
        val voiceViewModel = VoiceBottomSheetViewModel(FakeVoiceGateway())

        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ActiveListScreen(
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    voiceGateway = AndroidVoiceRecognitionGateway(),
                    onEvent = {},
                )
            }
        }

        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule
                .onAllNodesWithContentDescription(
                    "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, selezionato."
                )
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeTestRule
            .onNodeWithContentDescription(
                "Mozzarella, 2 pezzi, Latticini, Formaggi, Mozzarella, selezionato."
            )
            .assertExists()
    }

    @Test
    fun `quantityLabel formats large numbers without scientific notation`() {
        assertEquals("1000000 pezzi", quantityLabel(1000000.0, StandardUnit.PIECE))
        assertEquals("1000000,5 pezzi", quantityLabel(1000000.5, StandardUnit.PIECE))
        assertEquals("10000000000 pezzi", quantityLabel(1.0E10, StandardUnit.PIECE))
        assertEquals("0,125 grammi", quantityLabel(0.125, StandardUnit.GRAM))
        assertEquals("0 pezzi", quantityLabel(0.0, StandardUnit.PIECE))
    }

    @Test
    fun `quantityLabel uses Italian decimals and full unit names`() {
        assertEquals("2 pezzi", quantityLabel(2.0, StandardUnit.PIECE))
        assertEquals("1 pezzo", quantityLabel(1.0, StandardUnit.PIECE))
        assertEquals("1,5 litri", quantityLabel(1.5, StandardUnit.LITER))
        assertEquals("2,5 bottiglie", quantityLabel(2.5, StandardUnit.BOTTLE))
        assertEquals("1000 grammi", quantityLabel(1000.0, StandardUnit.GRAM))
        assertEquals("1 grammo", quantityLabel(1.0, StandardUnit.GRAM))
        assertEquals("3 lattine", quantityLabel(3.0, StandardUnit.CAN))
        assertEquals("1 pentola", quantityLabel(1.0, StandardUnit.POT))
        assertEquals("1 vasetto", quantityLabel(1.0, StandardUnit.JAR))
    }

    @Test
    fun `quantity badge and talkback agree for large quantities`() {
        val item = uncheckedItem.copy(quantity = 1.0E10)
        composeTestRule.setContent {
            TogoTheme(darkTheme = false) {
                ItemRow(
                    item = item,
                    productName = "Pezzi",
                    categoryLabel = "Varie",
                    onCheckChange = {},
                    onClick = {},
                    onDelete = {},
                )
            }
        }

        composeTestRule.onNodeWithText("10000000000 pz").assertExists()
        composeTestRule
            .onNodeWithContentDescription(
                "Pezzi, 10000000000 pezzi, Varie, non selezionato. Doppio tocco per modificare."
            )
            .assertExists()
    }

    // --- Fakes di dominio per comporre la schermata senza DB ---

    private class FakeShoppingListRepository(
        private val items: List<ShoppingItem> = emptyList(),
    ) : ShoppingListRepository {
        override fun getAllItems(): Flow<List<ShoppingItem>> = flowOf(items)
        override fun getActiveItems(): Flow<List<ShoppingItem>> =
            flowOf(items.filter { !it.isChecked })
        override fun getCheckedItems(): Flow<List<ShoppingItem>> =
            flowOf(items.filter { it.isChecked })
        override suspend fun insert(item: ShoppingItem) = Unit
        override suspend fun update(item: ShoppingItem) = Unit
        override suspend fun delete(itemId: String) = Unit
        override suspend fun getByProductId(productId: Long): ShoppingItem? =
            items.firstOrNull { it.productId == productId }
        override suspend fun checkout(checkedItemIds: List<String>): Boolean = true
    }

    private class FakeCatalogRepository(
        private val taxonomy: List<TaxonomyLevel> = emptyList(),
        private val products: Map<Long, CanonicalProduct> = emptyMap()
    ) : CatalogRepository {
        override fun search(query: String): Flow<List<CanonicalProduct>> =
            flowOf(products.values.filter { it.name.contains(query, ignoreCase = true) })
        override suspend fun getById(id: Long): CanonicalProduct? = products[id]
        override fun getTaxonomy(): Flow<List<TaxonomyLevel>> = flowOf(taxonomy)
    }

    private class FakeVoiceGateway : VoiceRecognitionGateway {
        private val _state = MutableStateFlow<VoiceRecognitionState>(VoiceRecognitionState.Idle)
        override val state: StateFlow<VoiceRecognitionState> = _state
        override suspend fun startListening() {
            _state.value = VoiceRecognitionState.Listening
        }
        override fun stopListening() {
            _state.value = VoiceRecognitionState.Idle
        }
        override fun reset() {
            _state.value = VoiceRecognitionState.Idle
        }
    }
}
